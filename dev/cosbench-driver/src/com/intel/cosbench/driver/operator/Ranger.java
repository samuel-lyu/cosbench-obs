package com.intel.cosbench.driver.operator;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.NoSuchAlgorithmException;
import java.util.Date;

import org.apache.commons.io.IOUtils;
import org.apache.commons.io.output.CountingOutputStream;
import org.apache.commons.io.output.NullOutputStream;

import com.intel.cosbench.api.storage.StorageInterruptedException;
import com.intel.cosbench.bench.Result;
import com.intel.cosbench.bench.Sample;
import com.intel.cosbench.config.Config;
import com.intel.cosbench.driver.util.ContainerPicker;
import com.intel.cosbench.driver.util.HashUtil;
import com.intel.cosbench.driver.util.ObjectPicker;
import com.intel.cosbench.driver.util.SizePicker;
import com.intel.cosbench.service.AbortedException;

public class Ranger extends AbstractOperator{
	public static final String OP_TYPE = "range";

	private ContainerPicker contPicker = new ContainerPicker();
	private boolean hashCheck = false;

    private ObjectPicker objPicker = new ObjectPicker();
    private SizePicker   sizePicker = new SizePicker();
    
    private byte buffer[] = new byte[1024*1024];
    
	public Ranger() {
        /* empty */
    }
	@Override
	protected void init(String id, int ratio, String division, Config config) {
		 super.init(id, ratio, division, config);
		 objPicker.init(division, config);
		 sizePicker.init(config);
	}

	@Override
	public String getOpType() {
		return OP_TYPE;
	}

	@Override
	protected void operate(int idx, int all, Session session) {
		String[] path = objPicker.pickObjPath(session.getRandom(), idx, all);
		long range = sizePicker.pickObjSize(session.getRandom()); 
		NullOutputStream out = new NullOutputStream();
	
		System.out.println("bucket name£º"+path[0]);
		System.out.println("object name£º"+path[1]);
		System.out.println("range size£º"+range);
		
		Sample sample = doRange(out, path[0], path[1], config, session,range);
		
		session.getListener().onSampleCreated(sample);
		Date now = sample.getTimestamp();
		Result result = new Result(now, getId(), getOpType(), getSampleType(),
				getName(), sample.isSucc());
		session.getListener().onOperationCompleted(result);
	}
	 private Sample doRange(OutputStream out, String conName, String objName,
	            Config config, Session session,long range) {
		 	
	        if (Thread.interrupted())
	            throw new AbortedException();

	        InputStream in = null;
	        InputStream shardFlow = null;
	        CountingOutputStream cout = new CountingOutputStream(out);

	        long start = System.nanoTime();
	        long xferTime = 0L;
	        try {
	        	
	        	in =session.getApi().getObject(conName, objName, config);
	            long xferStart = System.nanoTime();
	            if (!hashCheck) {
	                copyLarge(in, cout);
	                
	                long size = cout.getByteCount();
	                //long size = 10000;
	                
	                System.out.println("object size£º"+size);
	                
	                long shards = (size%range)==0 ? (size/range) : (size/range+1);
	                System.out.println("shard number£º"+shards);
	                for (int i = 0; i < shards; i++) {
	                	long startRange = i*range;
	                	long endRange   = startRange+range-1;
	                	if(i==shards-1) {
	                		shardFlow = session.getApi().getObjectByRange(conName, objName, config, startRange, size);
	                	}else {
						shardFlow = session.getApi().getObjectByRange(conName, objName, config, startRange, endRange);
						copy(shardFlow,cout);
	                	}
					}
	            } else if (!validateChecksum(conName, objName, session, in, cout)) {
					return new Sample(new Date(), getId(), getOpType(),
							getSampleType(), getName(), false);
	            }
	            long xferEnd = System.nanoTime();
	            xferTime = (xferEnd - xferStart) / 1000000;
	            
	        } catch (StorageInterruptedException sie) {
	            doLogErr(session.getLogger(), sie.getMessage(), sie);
	            throw new AbortedException();
	        } catch (Exception e) {
	        	isUnauthorizedException(e, session);
	        	errorStatisticsHandle(e, session, conName + "/" + objName);

	            return new Sample(new Date(), getId(), getOpType(), getSampleType(), getName(), false);
	        } finally {
	            IOUtils.closeQuietly(in);
	            IOUtils.closeQuietly(cout);
	        }
	        long end = System.nanoTime();

			return new Sample(new Date(), getId(), getOpType(), getSampleType(),
					getName(), true, (end - start)/1000000,
					xferTime, cout.getByteCount());
	    }
	public OutputStream copyLarge(InputStream input, OutputStream output)
			throws IOException
	{
		IOUtils.copyLarge(input, output);
		return output;
	}
	public OutputStream copy(InputStream input,OutputStream output) 
			throws IOException
	{
		IOUtils.copy(input, output);
		return output;
	}
	private static boolean validateChecksum(String conName, String objName,
            Session session, InputStream in, OutputStream out)
            throws IOException {
        HashUtil util;
        try {
            util = new HashUtil();
            int hashLen = util.getHashLen();

            byte buf1[] = new byte[4096];
            byte buf2[] = new byte[4096];

            String storedHash = new String();
            String calculatedHash = new String();

            int br1 = in.read(buf1);

            if (br1 <= hashLen) {
                out.write(buf1, 0, br1);
                String warn = "The size is too small to embed checksum, will skip integrity checking.";
                doLogWarn(session.getLogger(), warn);
            }

            while (br1 > hashLen) { // hash is attached in the end.
                int br2 = in.read(buf2);

                if (br2 < 0) { // reach end of stream
                    out.write(buf1, 0, br1);
                    util.update(buf1, 0, br1 - hashLen);

                    calculatedHash = util.calculateHash();
                    storedHash = new String(buf1, br1 - hashLen, hashLen);

                    br1 = 0;
                } else if (br2 <= hashLen) {
                    out.write(buf1, 0, br1 + br2);
                    util.update(buf1, 0, br1 + br2 - hashLen);

                    calculatedHash = util.calculateHash();
                    storedHash = new String(buf1, br1 + br2 - hashLen, hashLen - br2) + new String(buf2, 0, br2);

                    br1 = 0;
                } else {
                    out.write(buf1, 0, br1);
                    util.update(buf1, 0, br1);

                    System.arraycopy(buf2, 0, buf1, 0, br2);

                    br1 = br2;
                }
            }

            if (!calculatedHash.equals(storedHash)) {
                if (storedHash.startsWith(HashUtil.GUARD)) {
                    String err =
                            "Inconsistent Hashes for " + conName + "\\" + objName + ": calculated=" + calculatedHash
                                    + ", stored=" + storedHash;
                    doLogErr(session.getLogger(), err);
                    return false;
                } else {
                    String warn = "No checksum embedded in " + conName + "\\" + objName;
                    doLogWarn(session.getLogger(), warn);
                }
            }

            return true; /* checksum - okay */
        } catch (NoSuchAlgorithmException e) {
            doLogErr(session.getLogger(), "Alogrithm not found", e);
        }
        return false; // if we reach this, something went wrong when trying to calculate the hash
    }
}
