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

import java.io.*;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.servlet.http.*;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileItemFactory;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.springframework.web.servlet.*;
import com.intel.cosbench.controller.entity.User;
import com.intel.cosbench.web.BadRequestException;

import jxl.Sheet;
import jxl.Workbook;
import jxl.read.biff.BiffException;
import jxl.write.Label;
import jxl.write.WritableSheet;
import jxl.write.WritableWorkbook;

public class ImportUser extends UserManagementController {
	
	protected ModelAndView process(HttpServletRequest req) {
    	ModelAndView result = new ModelAndView("userManagement");
    	List<User> allUsers = new ArrayList<User>();
    	writeUser2Excel(req);
		readUserFromExcel(allUsers);
    	result.addObject("users", allUsers);
    	return result;
	}
	
    public void writeUser2Excel(HttpServletRequest req) 
    {  
    	List<User> importUsers = new ArrayList<User>();
        try {
        	InputStream is = retrieveUserStream(req);
            Workbook wb = Workbook.getWorkbook(is);
            Sheet sheet = wb.getSheet(0);
            for (int i = 1; i < sheet.getRows(); i++) 
            {  
            	User user = new User();
                for (int j = 0; j < sheet.getColumns(); j++) 
                {  
                    String cellinfo = sheet.getCell(j, i).getContents();  
                    switch (j) {
					case 0:
						user.setUserName(cellinfo);
						break;
					case 1:
						user.setPassword(cellinfo);
						break;
					case 2:
						user.setUserGroup(cellinfo);
						break;
					case 3:
						user.setDescription(cellinfo);
						break;

					default:
						break;
					}
                }
                if (!user.getUserName().equals("")) {
                	importUsers.add(user);
				}
            }  
            
            judgeUserExist(importUsers);
            
            File directory = new File("");
            String courseFile = directory.getCanonicalPath();
			String userFilePath = courseFile + File.separator + "user" + File.separator + "all-user.xls";
			File userFile = new File(userFilePath);
			InputStream readStream = new FileInputStream(userFile); 
            Workbook workbook = Workbook.getWorkbook(readStream);
            WritableWorkbook wworkbook = Workbook.createWorkbook(userFile, workbook);
            WritableSheet sheetToWrite = wworkbook.getSheet(0);  
            for (int i = 0; i < importUsers.size(); i++) { 
                int rows = sheetToWrite.getRows();
            	User user = importUsers.get(i);
            	int rowToAdd = rows;
            	String userId = "";
            	if (rowToAdd > 1)
				{
            		userId = sheetToWrite.getCell(0, rowToAdd-1).getContents();
				}
            	else 
            	{
            		userId = "1";
				}
            	userId = rowToAdd + "";
            	Label idToWrite = new Label(0, rowToAdd, userId);
            	sheetToWrite.addCell(idToWrite);
            	Label userNameToWrite = new Label(1, rowToAdd, user.getUserName());
            	sheetToWrite.addCell(userNameToWrite);
            	Label passwordToWrite = new Label(2, rowToAdd, user.getPassword());
            	sheetToWrite.addCell(passwordToWrite);
            	Label userGroupToWrite = new Label(3, rowToAdd, user.getUserGroup());
            	sheetToWrite.addCell(userGroupToWrite);
            	Label userDescriptionToWrite = new Label(4, rowToAdd, user.getDescription());
            	sheetToWrite.addCell(userDescriptionToWrite);
            }
            wworkbook.write();
        	wworkbook.close();
        	workbook.close();
        	readStream.close();
        } 
        catch (BiffException e) 
        {  
            e.printStackTrace();  
        } 
        catch (IOException e) 
        {  
            e.printStackTrace();  
        } 
        catch (Exception e)
		{
			e.printStackTrace();
		}
    }  
	
    @SuppressWarnings("unchecked")
    private InputStream retrieveUserStream(HttpServletRequest request)
            throws Exception {
        FileItemFactory factory = new DiskFileItemFactory();
        ServletFileUpload upload = new ServletFileUpload(factory);
        for (FileItem item : (List<FileItem>) upload.parseRequest(request))
            if (item.getFieldName().equals("user"))
                return item.getInputStream();
        throw new BadRequestException();
    }
    
    //judge if users to be imported are existed
    private void judgeUserExist(List<User> importUsers) {
    	List<User> existUsers = new ArrayList<User>();
        super.readUserFromExcel(existUsers);
        System.out.println("ExistUsersSize:" + existUsers.size());
        Iterator<User> existUsersIterator = existUsers.iterator();
        while (existUsersIterator.hasNext()) {
			User existUser = (User) existUsersIterator.next();
			Iterator<User> importUsersIterator = importUsers.iterator();
			while (importUsersIterator.hasNext()) {
				User importUser = (User) importUsersIterator.next();
				if (importUser.getUserName().equals(existUser.getUserName()) && 
						importUser.getUserGroup().equals(existUser.getUserGroup())) {
					importUsersIterator.remove();
					System.out.println("user " + importUser.getUserName() + " is already exist");
				}
			}
		}
    }
}
