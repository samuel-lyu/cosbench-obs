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
import jxl.write.WritableCell;
import jxl.write.WritableSheet;
import jxl.write.WritableWorkbook;

public class ImportUser extends UserManagementController {

	@Override
    protected ModelAndView process(HttpServletRequest req,
            HttpServletResponse res) throws Exception {
		writeUser2Excel(req);
		return process();
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
                importUsers.add(user);
            }  
            System.out.println("importUsers:" + importUsers.toString());
            System.out.println("importUsers.size():" + importUsers.size());
            System.out.println(importUsers.get(0).getUserName());
            System.out.println(importUsers.get(1).getUserName());
            System.out.println(importUsers.get(2).getUserName());
            
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
}
