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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;

import org.apache.commons.lang.StringUtils;

import com.intel.cosbench.config.Auth;
import com.intel.cosbench.config.Storage;
import com.intel.cosbench.config.Work;
import com.intel.cosbench.controller.entity.User;
import com.intel.cosbench.controller.model.*;
import com.intel.cosbench.log.LogFactory;
import com.intel.cosbench.log.Logger;

import jxl.Sheet;
import jxl.Workbook;
import jxl.read.biff.BiffException;

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
        String[] authCfgArray = seperateUserFromCfg(primitiveAuthConfig);
        String[] storageCfgArray = seperateUserFromCfg(primitiveStorageConfig);
        //count the number of driver that the work will perform on
        int toUseDriverNum = 0;
        for (int i = 0; i < allocMap.length; i++) {
			if (allocMap[i] > 0) {
				toUseDriverNum = i+1;
			}
		}
        /*balance users*/
        String[] authConfigArray = balanceUserConfig(toUseDriverNum, authCfgArray);
        String[] storageConfigArray = balanceUserConfig(toUseDriverNum, storageCfgArray);
        /*new work for every driver*/
        Work[] balanceUserWork = new Work[toUseDriverNum];
        for (int i = 0; i < balanceUserWork.length; i++) {
			balanceUserWork[i] = workConfigClone(work, authConfigArray[i], storageConfigArray[i]);
		}
    	return balanceUserWork;
    }
    
    /**
     * 
     * @param toUseDriverNum
     * 		toUseDriverNum determines the number of parts that the users will be divided into.
     * @param primitiveCfgArray
     * 		primitiveCfgArray contains all users and other configuration;
     * 		primitiveCfgArray[0] contains zero or more than one users
     * 		primitiveCfgArray[1] contains other configuration
     * @return
     */
    private String[] balanceUserConfig(int toUseDriverNum, String[] primitiveCfgArray) {
    	String primitiveUserStr = primitiveCfgArray[0];
    	String[] configArray = new String[toUseDriverNum];
    	/*if there is no more than one users ,it is not nessesary to balance user,just use the primitive configuration*/
    	if (StringUtils.isEmpty(primitiveUserStr)) {
			for (int i = 0; i < configArray.length; i++) {
				configArray[i] = primitiveCfgArray[1];
			}
			return configArray;
		}
    	
    	/*balance more than one users to drivers*/
    	String[] primitiveUserArray = primitiveUserStr.split(";");
        for (int i = 0; i < configArray.length; i++) configArray[i] = "";
        if (!StringUtils.isEmpty(primitiveUserStr)) {
        	int totalUserNum = primitiveUserArray.length / 2;
        	int base = totalUserNum / toUseDriverNum;
        	int extra = totalUserNum % toUseDriverNum;
        	
        	for (int i = 0; i < toUseDriverNum; i++) {
        		for (int j = 0; j < base; j++) {
        			configArray[i] += primitiveUserArray[(i * base + j) * 2] + ";" 
        		                   + primitiveUserArray[(i * base + j) * 2 + 1] + ";";
				}
        	}
            for (int i = 0; i < extra; i++) {
            	configArray[i] += primitiveUserArray[(base * toUseDriverNum + i) * 2] + ";" 
            	               + primitiveUserArray[(base * toUseDriverNum + i) * 2 + 1] + ";";
            }
            if (base == 0 && totalUserNum > 0) {
				for (int i = extra; i < toUseDriverNum; i++) {
					Random random = new Random();
					int reed = random.nextInt(totalUserNum);
					configArray[i] = primitiveUserArray[(reed) * 2] + ";";
					configArray[i] = primitiveUserArray[(reed) * 2 + 1] + ";";
				}
			}
            /*If the driver is assigned more than one user,user six asterisk to mark the config*/
            for (int i = 0; i < toUseDriverNum; i++) {
            	if (base > 1) {
            		configArray[i] = removeTheEndingSemicolon(configArray[i]);
            		configArray[i] += "******" + primitiveCfgArray[1];
				} else if (base == 1) {
	            	if (i < extra) {
	            		configArray[i] = removeTheEndingSemicolon(configArray[i]);
	            		configArray[i] += "******" + primitiveCfgArray[1];
					} else {
						configArray[i] += primitiveCfgArray[1];
					}
				} else {
					configArray[i] += primitiveCfgArray[1];
				}
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
        newWork.setOperations(work.getOperations(),true);
    	return newWork;
	}
    
    /**
     * Separating the user configuration from the other configuration in the configuration
     * @param originalCfg
     * @return
     */
    private String[] seperateUserFromCfg(String originalCfg) {
    	if (originalCfg == null) {
    		originalCfg = "";
		}
    	if (originalCfg.contains("userGroup:") || originalCfg.contains("user:")) {
			String[] user_cfg_seperation = divideUserFromCfg(originalCfg);
    		return user_cfg_seperation;
		} else {
			return new String[]{"",originalCfg};
		}
    }
    
    /**
     * Separating the user configuration from the other configuration in the configuration
     * @param originalCfg
     * @return
     */
    private String[] divideUserFromCfg(String originalCfg) {
    	List<User> allUsers = new ArrayList<User>();
    	readUserFromExcel(allUsers);
		String[] cfgs = originalCfg.split(";");
		int totalUser = 0;
		String userCfg = "";
		String otherCfg = "";
		for (int i = 0; i < cfgs.length; i++) {
			if (cfgs[i].contains("userGroup:") || cfgs[i].contains("user:")) {
				String[] userArray = cfgs[i].split(":");
				if (userArray[0].equalsIgnoreCase("userGroup")) {
					String groupName = userArray[1];
					userCfg += obtainConfigByUserGroupName(allUsers, groupName) + ";";
				} else if (userArray[0].equals("user")) {
					String userName = userArray[1];
					userCfg += obtainConfigByUserName(allUsers, userName) + ";";
		    		totalUser++;
				}
			} else {
				otherCfg += cfgs[i] + ";";
			}
		}
		
		/*If there is only one user, there is no need to redistribute.*/
		userCfg = removeTheEndingSemicolon(userCfg);
		otherCfg = removeTheEndingSemicolon(otherCfg);
		totalUser = userCfg.split(";").length / 2;
		if (totalUser < 2) {
			otherCfg = userCfg + ";" + otherCfg;
			return new String[]{"",otherCfg};
		} else {
			return new String[]{userCfg, otherCfg};
		}
	}
    
    private String removeTheEndingSemicolon(String originalStr) {
    	if (!StringUtils.isEmpty(originalStr) && originalStr.endsWith(";")) {
			return originalStr.substring(0, originalStr.length()-1);
		}
		return originalStr;
	}
    
    /**
     * obtain all user whose group is @param groupName
     * @param allUsers
     * @param groupName
     * @return
     */
    private String obtainConfigByUserGroupName(List<User> allUsers, String groupName) {
    	String userConfig = "";
		for (User user : allUsers) {
			if (user.getUserGroup().equals(groupName)) {
				userConfig = userConfig + user.getUserName() + ";" + user.getPassword() + ";";
			}
		}
    	return removeTheEndingSemicolon(userConfig);
	}
    
    /**
     * obtain user whose userName is @param userName
     * @param allUsers
     * @param userName
     * @return
     */
    private String obtainConfigByUserName(List<User> allUsers, String userName) {
    	String userConfig = "";
		for (User user : allUsers) {
			if (user.getUserName().equals(userName)) {
				userConfig = userConfig + user.getUserName() + ";" + user.getPassword();
				break;
			}
		}
    	return userConfig;
    }
    
    /**
     * read all users from local excel file
     * @param allUsers
     * allUsers is a collection to storage all users read from excel
     */
    public void readUserFromExcel(List<User> allUsers) {  
    	try {  
    		File directory = new File("");
    		String courseFile = directory.getCanonicalPath();
    		String userFilePath = courseFile + File.separator + "user" + File.separator + "all-user.xls";
    		File userFile = new File(userFilePath);
    		InputStream is = new FileInputStream(userFile.getAbsolutePath()); 
    		Workbook wb = Workbook.getWorkbook(is);
    		Sheet sheet = wb.getSheet(0);  
    		for (int i = 1; i < sheet.getRows(); i++) {  
    			User user = new User();
    			for (int j = 0; j < sheet.getColumns(); j++) {  
    				String cellinfo = sheet.getCell(j, i).getContents();  
    				switch (j) {
    				case 0:
    					user.setId(cellinfo);
    					break;
    				case 1:
    					user.setUserName(cellinfo);
    					break;
    				case 2:
    					user.setPassword(cellinfo);
    					break;
    				case 3:
    					user.setUserGroup(cellinfo);
    					break;
    				case 4:
    					user.setDescription(cellinfo);
    					break;
    					
    				default:
    					break;
    				}
    			}
    			if (!user.getId().equals("")) {
    				allUsers.add(user);
    			}
    		}  
    	} catch (FileNotFoundException e) {  
    		e.printStackTrace();  
    	} catch (BiffException e) {  
    		e.printStackTrace();  
    	} catch (IOException e) {  
    		e.printStackTrace();  
    	}  
    } 
    
    private DriverContext fetchDriver(String name) {
        if (StringUtils.isEmpty(name))
            return null;
        if (StringUtils.equals(name, "none"))
            return null;
        return drivers.get(name);
    }

}
