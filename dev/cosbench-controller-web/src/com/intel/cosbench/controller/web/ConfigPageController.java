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

package com.intel.cosbench.controller.web;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.servlet.http.*;

import org.springframework.web.servlet.ModelAndView;

import com.intel.cosbench.controller.entity.User;
import com.intel.cosbench.service.ControllerService;
import com.intel.cosbench.web.AbstractController;

import jxl.Sheet;
import jxl.Workbook;
import jxl.read.biff.BiffException;

public class ConfigPageController extends AbstractController {

    @SuppressWarnings("unused")
	private ControllerService controller;

    public void setController(ControllerService controller) {
        this.controller = controller;
    }

    @Override
    protected ModelAndView process(HttpServletRequest req,
            HttpServletResponse res) {
        ModelAndView result  =  new ModelAndView("config");
        List<User> allUsers = new ArrayList<User>();
        readUserFromExcel(allUsers);
        Iterator<User> userIterator = allUsers.iterator();
        Set<String> userGroupSet = new HashSet<String>();
        Set<String> userSet = new HashSet<String>();
        while (userIterator.hasNext()) {
			User user = (User) userIterator.next();
			userSet.add(user.getUserName());
			userGroupSet.add(user.getUserGroup());
		}
        List<String> userGroupList = new ArrayList<String>();
        userGroupList.addAll(userGroupSet);
        List<String> userList = new ArrayList<String>();
        userList.addAll(userSet);
        result.addObject("userGroupList", userGroupList);
        result.addObject("userList", userList);
        return result;
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
    
//    private List<?> setToList(Set<?> set) {
//    	List<E> lists = new ArrayList<E>();
//		Iterator<?> iterator = set.iterator();
//		while (iterator.hasNext()) {
//			Object object = (Object) iterator.next();
//			
//		}
//	}
}
