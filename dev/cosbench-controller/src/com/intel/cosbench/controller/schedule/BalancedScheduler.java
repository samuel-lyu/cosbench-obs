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

package com.intel.cosbench.controller.schedule;

import java.util.*;

import org.apache.commons.lang.StringUtils;

import com.intel.cosbench.config.Auth;
import com.intel.cosbench.config.Storage;
import com.intel.cosbench.config.Work;
import com.intel.cosbench.controller.model.*;
import com.intel.cosbench.log.LogFactory;
import com.intel.cosbench.log.Logger;

/**
 * This class encapsulates one balanced scheduler, which tries best to evenly
 * distribute work to different driver.
 * 
 * @author ywang19, qzheng7
 * 
 */
class BalancedScheduler extends AbstractScheduler {
	private static final Logger LOGGER = LogFactory.getSystemLogger();

    private int allocIdx;
    private int[] allocMap;

    private ScheduleRegistry schedules = new ScheduleRegistry();

    public BalancedScheduler() {
        /* empty */
    }

    @Override
    public ScheduleRegistry schedule() {
        honorUserSchedules();
        scheduleRestWorks();
        return schedules;
    }

    private void honorUserSchedules() {
        Set<String> toRemove = new HashSet<String>();
        List<Work> unscheduled = new ArrayList<Work>();
        for (Work work : works) {
            DriverContext driver = fetchDriver(work.getDriver());
            if (driver == null) {
                unscheduled.add(work);
                continue;
            }
            toRemove.add(driver.getName());
            schedules.addSchedule(createSchedule(work, driver));
        }
        for (String driver : toRemove)
            drivers.remove(driver);
        works = unscheduled;
    }

    private void scheduleRestWorks() {
        if (works.size() == 0)
            return;
        if (drivers.size() == 0)
            throw new ScheduleException("no free driver available");
        allocIdx = 0;
        allocMap = new int[drivers.size()];
        for (Work work : works)
            doSchedule(work);
    }

    private void doSchedule(Work work) {
        int driverNum = allocMap.length;
        int base = work.getWorkers() / driverNum;
        int extra = work.getWorkers() % driverNum;

        for (int i = 0; i < driverNum; i++)
            allocMap[i] = base;
        int lower = allocIdx;
        int upper = lower + extra;
        for (int i = lower; i < upper; i++)
            allocMap[i % driverNum]++;
        allocIdx = upper % driverNum;

        int idx = 0;
        int offset = 0;
        int workers = 0;
        Work[] scheduleUserWorkArray = scheduleUserWork(work, allocMap);
        for (DriverContext driver : drivers.values()) {
            if ((workers = allocMap[idx]) == 0)
                continue;
            schedules.addSchedule(createSchedule(scheduleUserWorkArray[idx], driver, offset, workers));
            offset += workers;
            idx++;
        }
    }

    /**
     * according to the workers of the driver , balance users
     * @param work
     * @param allocMap
     * 		allocMap is an array which record workers that each driver has,
     * 		zero means that the @param work does not perform on this driver
     * @return
     *     return workArray after balance users
     */
    private Work[] scheduleUserWork(Work work, int[] allocMap) {
        String primitiveAuthConfig = work.getAuth().getConfig();
        LOGGER.debug("The primitive authConfig of the work {} is : {}", work.getName(), primitiveAuthConfig);
        String primitiveStorageConfig = work.getStorage().getConfig();
        LOGGER.debug("The primitive storageConfig  of the work {} is : {}", work.getName(), primitiveStorageConfig);
        //count the number of driver that the work will perform on
        int toUseDriverNum = 0;
        for (int i = 0; i < allocMap.length; i++) {
			if (allocMap[i] > 0) {
				toUseDriverNum = i+1;
			}
		}
        
        String[] authConfigArray = balanceUserConfig(toUseDriverNum, primitiveAuthConfig);
        String[] storageConfigArray = balanceUserConfig(toUseDriverNum, primitiveStorageConfig);
        
        Work[] balanceUserWork = new Work[toUseDriverNum];
        for (int i = 0; i < balanceUserWork.length; i++) {
			balanceUserWork[i] = workConfigClone(work, authConfigArray[i], storageConfigArray[i]);
		}
    	return balanceUserWork;
    }
    
    /**
     * 
     * @param toUseDriverNum
     * 		TousDeCurvNUM determines the number of parts that the user will be divided into.
     * @param primitiveConfig
     * 		primitiveConfig contains all users
     * @return
     */
    private String[] balanceUserConfig(int toUseDriverNum, String primitiveConfig) {
    	String primitiveConfigStr = StringUtils.isEmpty(primitiveConfig) ? "" : primitiveConfig;
    	String[] primitiveConfigArray = primitiveConfigStr.split(";");
        String[] configArray = new String[toUseDriverNum];
        for (int i = 0; i < configArray.length; i++) configArray[i] = "";
        if (!StringUtils.isEmpty(primitiveConfigStr)) {
        	int totalUserNum = primitiveConfigArray.length / 2;
        	int base = totalUserNum / toUseDriverNum;
        	int extra = totalUserNum % toUseDriverNum;
        	
        	for (int i = 0; i < toUseDriverNum; i++) {
        		for (int j = 0; j < base; j++) {
        			configArray[i] += primitiveConfigArray[(i * base + j) * 2] + ";";
        			configArray[i] += primitiveConfigArray[(i * base + j) * 2 + 1] + ";";
				}
        	}
            for (int i = 0; i < extra; i++) {
            	configArray[i] += primitiveConfigArray[(base * toUseDriverNum + i) * 2] + ";";
            	configArray[i] += primitiveConfigArray[(base * toUseDriverNum + i) * 2 + 1] + ";";
            }
            if (base == 0 && totalUserNum > 0) {
				for (int i = extra; i < toUseDriverNum; i++) {
					Random random = new Random();
					int reed = random.nextInt(totalUserNum);
					configArray[i] = primitiveConfigArray[(reed) * 2] + ";";
					configArray[i] = primitiveConfigArray[(reed) * 2 + 1] + ";";
				}
			}
            for (int i = 0; i < toUseDriverNum; i++) {
        		configArray[i] += primitiveConfigArray[primitiveConfigArray.length - 1];
        	}
		}
        
        return configArray;
	}
    
    /**
     * Generate different work according to original work and different user configuration.
     * @param work
     * @param authConfig
     * @param storageConfig
     * @return
     */
    private Work workConfigClone(Work work, String authConfig, String storageConfig) {
    	Work newWork = new Work();
    	newWork.setName(work.getName());
        newWork.setType(work.getType());
        newWork.setWorkers(work.getWorkers());
        newWork.setInterval(work.getInterval());
        newWork.setDivision(work.getDivision());
        newWork.setRuntime(work.getRuntime());
        newWork.setRampup(work.getRampup());
        newWork.setRampdown(work.getRampdown());
        newWork.setAfr(work.getAfr());
        newWork.setTotalOps(work.getTotalOps());
        newWork.setTotalBytes(work.getTotalBytes());
        newWork.setDriver(work.getDriver());
        newWork.setConfig(work.getConfig());
        newWork.setAuth(new Auth(work.getAuth().getType(), authConfig));
        newWork.setStorage(new Storage(work.getStorage().getType(), storageConfig));
        newWork.setOperations(work.getOperations());
    	return newWork;
	}
    
    private DriverContext fetchDriver(String name) {
        if (StringUtils.isEmpty(name))
            return null;
        if (StringUtils.equals(name, "none"))
            return null;
        return drivers.get(name);
    }

}
