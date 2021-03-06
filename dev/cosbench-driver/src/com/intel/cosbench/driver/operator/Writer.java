/** 
 
Copyright 2013 Intel Corporation, All Rights Reserved.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

   http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License. 
*/

package com.intel.cosbench.driver.operator;

import java.io.InputStream;
import java.util.Date;
import java.util.Random;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;

import com.intel.cosbench.api.storage.StorageInterruptedException;
import com.intel.cosbench.bench.Result;
import com.intel.cosbench.bench.Sample;
import com.intel.cosbench.config.Config;
import com.intel.cosbench.config.ConfigException;
import com.intel.cosbench.driver.generator.RandomInputStream;
import com.intel.cosbench.driver.generator.XferCountingInputStream;
import com.intel.cosbench.driver.util.ObjectPicker;
import com.intel.cosbench.driver.util.SizePicker;
import com.intel.cosbench.log.LogFactory;
import com.intel.cosbench.log.Logger;
import com.intel.cosbench.service.AbortedException;

/**
 * This class represents primitive WRITE operation.
 * 
 * @author ywang19, qzheng7
 * 
 */
class Writer extends AbstractOperator {
	
	private static final int DEFAULT_SLEEP_TIME = 5000;
	private static Logger logger = LogFactory.getSystemLogger();
	private static final int OBS_ERROR = -1;
	private static final Logger LOGGER = LogFactory.getSystemLogger();
	public static final String OP_TYPE = "write";

	private boolean isFinished;
	private boolean chunked;
	private boolean isRandom;
	private long partSize = 0;
	private long base;
	private boolean hashCheck = false;
	private ObjectPicker objPicker = new ObjectPicker();
	private SizePicker sizePicker = new SizePicker();

	public Writer() {
		/* empty */
	}

	@Override
	protected void init(String id, int ratio, String division, Config config) {
		super.init(id, ratio, division, config);
		objPicker.init(division, config);
		sizePicker.init(config);
		chunked = config.getBoolean("chunked", false);
		isRandom = !config.get("content", "random").equals("zero");
		hashCheck = config.getBoolean("hashCheck", false);
		if (config.contains("partSize")) {
			partSize = pase(config.get("partSize"));
			LOGGER.debug("Upload a single segment size:" + partSize);
		}
	}

	@Override
	public String getOpType() {
		return OP_TYPE;
	}

	@Override
	protected void operate(int idx, int all, Session session) {
		Sample sample = null;
		Random random = session.getRandom();
		long size = sizePicker.pickObjSize(random);
		long len = chunked ? OBS_ERROR : size;
		String[] path = objPicker.pickObjPath(random, idx, all);

		// Determine whether to Multistage
		if (partSize == 0) 
		{
			RandomInputStream in = new RandomInputStream(size, random, isRandom, hashCheck);
			sample = doWrite(in, len, path[0], path[1], config, session, this);
			
			session.getListener().onSampleCreated(sample);
			Date now = sample.getTimestamp();
			Result result = new Result(now, getId(), getOpType(), getSampleType(), getName(), sample.isSucc());
			session.getListener().onOperationCompleted(result);
		} 
		else 
		{
			long count = size / partSize;
			if(size % partSize != 0)
			{
				count++;
			}
			int partNum = 0;
			isFinished = false;
			Result result = null;
			while (size > 0) {
				RandomInputStream in = new RandomInputStream(size / partSize != 0 ? partSize : size % partSize, random,
						isRandom, hashCheck);
				partNum++;
				logger.debug("begin upload num {} paragraph, total count is {}", partNum , count);
				if(partNum == count )
				{
					isFinished = true;
				}
				sample = putObjectByMultistage(in, size / partSize != 0 ? partSize : size % partSize, path[0], path[1],
						config, session, this, isFinished, partNum);
				session.getListener().onSampleCreated(sample);
				Date now = sample.getTimestamp();
				result = new Result(now, getId(), getOpType(), getSampleType(), getName(), sample.isSucc());
				if(partNum == count)
				{
					//Wait page display Sample��if not the last result of sample will lose display
					try {
						Thread.sleep(DEFAULT_SLEEP_TIME); 
					} catch (InterruptedException e) {
						logger.error("thread sleep error,operate id is {}", this.id);
					}
				}
				session.getListener().onOperationCompleted(result);
//				if (sample.isSucc() == false) {
//					break;
//				}
				size -= partSize;
			}
		}
	}

	public static Sample doWrite(InputStream in, long length, String conName, String objName, Config config,
			Session session, Operator op) {
		if (Thread.interrupted())
			throw new AbortedException();

		XferCountingInputStream cin = new XferCountingInputStream(in);
		long start = System.nanoTime();

		try {
			session.getApi().createObject(conName, objName, cin, length, config);
		} catch (StorageInterruptedException sie) {
			doLogErr(session.getLogger(), sie.getMessage(), sie);
			throw new AbortedException();
		} catch (Exception e) {
			isUnauthorizedException(e, session);
			errorStatisticsHandle(e, session, conName + "/" + objName);

			return new Sample(new Date(), op.getId(), op.getOpType(), op.getSampleType(), op.getName(), false);

		} finally {
			IOUtils.closeQuietly(cin);
		}

		long end = System.nanoTime();
		return new Sample(new Date(), op.getId(), op.getOpType(), op.getSampleType(), op.getName(), true,
				(end - start) / 1000000, cin.getXferTime(), cin.getByteCount());
	}

	public static Sample putObjectByMultistage(InputStream in, long size, String conName, String objName,
			Config config, Session session, Operator op, boolean isFinish, int partNum) {
		if (Thread.interrupted())
			throw new AbortedException();
		long time;
		XferCountingInputStream cin = new XferCountingInputStream(in);
		try {
			time = session.getApi().multiPartUpload(conName, objName, size, cin, isFinish,partNum);
		} catch (StorageInterruptedException sie) {
			doLogErr(session.getLogger(), sie.getMessage(), sie);
			throw new AbortedException();
		} catch (Exception e) {
			isUnauthorizedException(e, session);
			errorStatisticsHandle(e, session, conName + "/" + objName);

			return new Sample(new Date(), op.getId(), op.getOpType(), op.getSampleType(), op.getName(), false);

		} finally {
			IOUtils.closeQuietly(cin);
		}
		//if ObsStorage error occurred time will be -1
		if(time != OBS_ERROR)
		{
			return new Sample(new Date(), op.getId(), op.getOpType(), op.getSampleType(), op.getName(), true,
					time, cin.getXferTime(), cin.getByteCount());
		}
		return new Sample(new Date(), op.getId(), op.getOpType(), op.getSampleType(), op.getName(), false);
	}

	private long pase(String pattern) {
		base = setUnit(pattern);
		pattern = StringUtils.substringBetween(pattern, "(", ")");
		String[] args = StringUtils.split(pattern, ',');
		Integer value = Integer.parseInt(args[0]);
		return value * base;
	}

	private long setUnit(String unit) {
		if (StringUtils.endsWith(unit, "GB"))
			return (base = 1000 * 1000 * 1000);
		if (StringUtils.endsWith(unit, "MB"))
			return (base = 1000 * 1000);
		if (StringUtils.endsWith(unit, "KB"))
			return (base = 1000);
		if (StringUtils.endsWith(unit, "B"))
			return (base = 1);
		String msg = "unrecognized size unit: " + unit;
		throw new ConfigException(msg);
	}
	/*
	 * public static Sample doWrite(byte[] data, String conName, String objName,
	 * Config config, Session session) { if (Thread.interrupted()) throw new
	 * AbortedException();
	 * 
	 * long start = System.currentTimeMillis();
	 * 
	 * try { session.getApi().createObject(conName, objName, data, config); } catch
	 * (StorageInterruptedException sie) { throw new AbortedException(); } catch
	 * (Exception e) { doLog(session.getLogger(), "fail to perform write operation",
	 * e); return new Sample(new Date(), OP_TYPE, false); }
	 * 
	 * long end = System.currentTimeMillis();
	 * 
	 * Date now = new Date(end); return new Sample(now, OP_TYPE, true, end - start,
	 * data.length); }
	 */
}
