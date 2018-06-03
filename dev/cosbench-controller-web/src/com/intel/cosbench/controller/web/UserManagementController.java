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
import java.util.List;

import javax.servlet.http.*;

import org.apache.commons.lang.StringUtils;
import org.springframework.web.servlet.ModelAndView;

import com.intel.cosbench.controller.entity.User;
import com.intel.cosbench.service.ControllerService;
import com.intel.cosbench.web.*;

import jxl.Sheet;
import jxl.Workbook;
import jxl.read.biff.BiffException;
import jxl.write.Label;
import jxl.write.WritableSheet;
import jxl.write.WritableWorkbook;
import jxl.write.WriteException;
import jxl.write.biff.RowsExceededException;

public class UserManagementController extends AbstractController {

    private ControllerService controller;

    public void setController(ControllerService controller) {
        this.controller = controller;
    }

    @Override
    protected ModelAndView process(HttpServletRequest req,
            HttpServletResponse res) throws Exception {
    	return process(req);
    }
    
    protected ModelAndView process(HttpServletRequest req) {
    	ModelAndView result = new ModelAndView("userManagement");
    	String deleteUserIds = req.getParameter("deleteUserIds");
    	if (deleteUserIds != null && deleteUserIds.length() > 0)
		{
			deleteUserFromExcel(deleteUserIds);
		}
    	List<User> allUsers = new ArrayList<User>();
		readUserFromExcel(allUsers);
		Integer page = PageUtil.getPageParam(req);
    	return UserPage.page(result, allUsers, page);
	}
    
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
    
    public void deleteUserFromExcel(String uids) {
    	if (StringUtils.isEmpty(uids))
		{
			return;
		}
    	String[] userIds = uids.split("_");
    	try {  
    		File directory = new File("");
            String courseFile = directory.getCanonicalPath();
			String userFilePath = courseFile + File.separator + "user" + File.separator + "all-user.xls";
			File userFile = new File(userFilePath);
			InputStream readStream = new FileInputStream(userFile); 
            Workbook workbook = Workbook.getWorkbook(readStream);
            WritableWorkbook wworkbook = Workbook.createWorkbook(userFile, workbook);
            WritableSheet sheetToDelete = wworkbook.getSheet(0);
            for (int i = 0; i < userIds.length; i++) { 
            	int totaluser = Integer.parseInt(sheetToDelete.getCell(9, 0).getContents());
            	for (int j = totaluser; j > 0; j--)
				{
            		String userId = sheetToDelete.getCell(0, j).getContents();
            		if (userIds[i].equals(userId))
					{
            			sheetToDelete.removeRow(j);
            			--totaluser;
            			Label totalUsers = new Label(9, 0, totaluser + "");
            			sheetToDelete.addCell(totalUsers);
            			break;
					}
				}
            }
            wworkbook.write();
        	wworkbook.close();
        	workbook.close();
        	readStream.close();
    	} catch (FileNotFoundException e) {  
    		e.printStackTrace();  
    	} catch (BiffException e) {  
    		e.printStackTrace();  
    	} catch (IOException e) {  
    		e.printStackTrace();  
    	} catch (RowsExceededException e)
		{
			e.printStackTrace();
		} catch (WriteException e)
		{
			e.printStackTrace();
		}  
    }  
}
