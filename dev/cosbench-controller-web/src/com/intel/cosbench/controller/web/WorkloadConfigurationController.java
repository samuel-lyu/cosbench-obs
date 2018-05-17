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
import java.util.Map;

import javax.servlet.http.*;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.View;

import com.intel.cosbench.config.*;
import com.intel.cosbench.config.castor.*;
import com.intel.cosbench.controller.entity.User;
import com.intel.cosbench.log.LogFactory;
import com.intel.cosbench.log.Logger;
import com.intel.cosbench.service.ControllerService;
import com.intel.cosbench.web.*;

import jxl.Sheet;
import jxl.Workbook;
import jxl.read.biff.BiffException;

public class WorkloadConfigurationController extends AbstractController {
	
	private static final Logger LOGGER = LogFactory.getSystemLogger();

    private static final View XML = new XMLView();    

    protected ControllerService controller;

    public void setController(ControllerService controller) {
        this.controller = controller;
    }
    
    private static class XMLView implements View {

        @Override
        public String getContentType() {
            return "application/xml";
        }

        @Override
        public void render(Map<String, ?> model, HttpServletRequest req,
                HttpServletResponse res) throws Exception {
            String xml = (String)model.get("xml");
            res.setHeader("Content-Length", String.valueOf(xml.length()));
            res.setHeader("Content-Disposition",
                    "attachment; filename=\"workload-config.xml\"");

            try {
                IOUtils.write(xml, res.getOutputStream());
            } finally {
            }
        }

    }

    @Override
    protected ModelAndView process(HttpServletRequest req,
            HttpServletResponse res) {
        
    	Workload workload = null;
    	String xml = "";
    	
        try {
            workload = constructWorkloadFromPostData(req);             
        	xml =  CastorConfigTools.getWorkloadWriter().toXmlString(workload);

        } catch (Exception e) {
            return createErrResult(xml, e.getMessage());
        }

        return createSuccResult(xml);
    }

    private ArrayList<Object> constructInitStage(HttpServletRequest req)
    {
		String initChecked[] = req.getParameterValues("init.checked");
		if (initChecked != null) {
			String workStageName = new String("init");
			ArrayList<Object> workStageList = new ArrayList<Object>();
			for (int i = 0; i < initChecked.length; i++) {
				if (i > 0) {
					workStageName = new String("init" + i);
				}
				Stage stage = new Stage(workStageName);
				Work work = new Work(workStageName, "init");
				work.setWorkers(getParmInt(
						req.getParameterValues("init.workers")[i], 1));
				work.setDivision("container");
				String config = "";
				String cprefix = req.getParameter("init.cprefix");
				if (cprefix != null && cprefix != "") {
					config += "cprefix=" + cprefix + ";";
				}
				String selector = req.getParameterValues("init.containers")[i];
				String min = req.getParameterValues("init.containers.min")[i];
				String max = req.getParameterValues("init.containers.max")[i];
				String sexp = parseSelectorToString(selector, min, max);
				config += "containers=" + sexp;
				
				work.setConfig(config);
				
				stage.addWork(work);
				
				workStageList.add(stage);
				
				checkAndAddDelay(req, "init", workStageList, i);
			}
			return workStageList;
		}
		return null;
    }
    
	private ArrayList<Object> constructPrepareStage(HttpServletRequest req) {
		String prepareChecked[] = req.getParameterValues("prepare.checked");
		if (prepareChecked != null) {
			String workStageName = new String("prepare");
			ArrayList<Object> workStageList = new ArrayList<Object>();
			for (int i = 0; i < prepareChecked.length; i++) {
				if (i > 0) {
					workStageName = new String("prepare" + i);
				}
				Stage stage = new Stage(workStageName);
				Work work = new Work(workStageName, "prepare");
				work.setWorkers(getParmInt(
						req.getParameterValues("prepare.workers")[i], 1));
				work.setDivision("object");
				String config = "";
				
				String cprefix = req.getParameter("prepare.cprefix");
				if (cprefix != null && cprefix != "") {
					config += "cprefix=" + cprefix + ";";
				}
				// "containers" section in config
				String cselector = req.getParameterValues("prepare.containers")[i];
				String cmin = req.getParameterValues("prepare.containers.min")[i];
				String cmax = req.getParameterValues("prepare.containers.max")[i];
				String cexp = parseSelectorToString(cselector, cmin, cmax);
				config += "containers=" + cexp + ";";

				// "objects" section in config
				String oselector = req.getParameterValues("prepare.objects")[i];
				String omin = req.getParameterValues("prepare.objects.min")[i];
				String omax = req.getParameterValues("prepare.objects.max")[i];
				String oexp = parseSelectorToString(oselector, omin, omax);
				config += "objects=" + oexp + ";";

				// "sizes" section in config
				String sselector = getParm(req, "prepare.sizes");
				String smin = req.getParameterValues("prepare.sizes.min")[i];
				String smax = req.getParameterValues("prepare.sizes.max")[i];
				String sunit = req.getParameterValues("prepare.sizes.unit")[i];
				String sexp = parseSelectorToString(sselector, smin, smax);
				config += "sizes=" + sexp + sunit;

				work.setConfig(config);

				stage.addWork(work);

				workStageList.add(stage);
				
				checkAndAddDelay(req,"prepare",workStageList,i);
			}

			return workStageList;
		}

		return null;

	}
    
    private ArrayList<Object> constructCleanupStage(HttpServletRequest req)
    {
		String cleanupChecked[] = req.getParameterValues("cleanup.checked");
		if (cleanupChecked != null) {
			String workStageName = new String("cleanup");
			ArrayList<Object> workStageList = new ArrayList<Object>();
			for (int i = 0; i < cleanupChecked.length; i++) {
				if (i > 0) {
					workStageName = new String("cleanup" + i);
				}
				Stage stage = new Stage(workStageName);
				Work work = new Work(workStageName, "cleanup");

				work.setWorkers(getParmInt(
						req.getParameterValues("cleanup.workers")[i], 1));
				work.setDivision("object");
				String config = "";

				String cprefix = req.getParameter("cleanup.cprefix");
				if (cprefix != null && cprefix != "") {
					config += "cprefix=" + cprefix + ";";
				}
				// "containers" section in config
				String cselector = req.getParameterValues("cleanup.containers")[i];
				String cmin = req.getParameterValues("cleanup.containers.min")[i];
				String cmax = req.getParameterValues("cleanup.containers.max")[i];
				String cexp = parseSelectorToString(cselector, cmin, cmax);
				config += "containers=" + cexp + ";";

				// "objects" section in config
				String oselector = req.getParameterValues("cleanup.objects")[i];
				String omin = req.getParameterValues("cleanup.objects.min")[i];
				String omax = req.getParameterValues("cleanup.objects.max")[i];
				String oexp = parseSelectorToString(oselector, omin, omax);
				config += "objects=" + oexp;

				work.setConfig(config);

				stage.addWork(work);

				workStageList.add(stage);
				
				checkAndAddDelay(req, "cleanup", workStageList, i); 
			}

			return workStageList;
		}

		return null;
    }
    
    private ArrayList<Object> constructDisposeStage(HttpServletRequest req)
    {
    	String disposeChecked[] = req.getParameterValues("dispose.checked");
    	if (disposeChecked != null) {
    	String workStageName = new String("dispose");
    	ArrayList<Object> workStageList = new ArrayList<Object>();
    	for (int i = 0; i < disposeChecked.length; i++) {
    	if (i > 0) {
    	workStageName = new String("dispose" + i);
    	}
    	Stage stage = new Stage(workStageName);
    	Work work = new Work(workStageName, "dispose");
    	work.setWorkers(getParmInt(
    	req.getParameterValues("dispose.workers")[i], 1));
    	work.setDivision("container");
    	String config = "";

    	String cprefix = req.getParameter("dispose.cprefix");
		if (cprefix != null && cprefix != "") {
			config += "cprefix=" + cprefix + ";";
		}
    	// "containers" section in config
    	String dcselector = req.getParameterValues("dispose.containers")[i];
    	String dcmin = req.getParameterValues("dispose.containers.min")[i];
    	String dcmax = req.getParameterValues("dispose.containers.max")[i];
    	String dcexp = parseSelectorToString(dcselector, dcmin, dcmax);
    	config += "containers=" + dcexp;

    	work.setConfig(config);

    	stage.addWork(work);

    	workStageList.add(stage);
    	
    	checkAndAddDelay(req, "dispose", workStageList, i);
    	}

    	return workStageList;
    	}

    	return null;
    }
    
    private String getParm(HttpServletRequest req, String parm)
    {
    	return req.getParameter(parm);
    }
    
//    private String getParm(HttpServletRequest req, String parm, String defVal)
//    {
//    	String val = getParm(req, parm);
//    	if(val == null || val.isEmpty())
//    		return defVal;
//    	return val;
//    }
    
//    private int getParmInt(HttpServletRequest req, String parm)
//    {
//    	return Integer.parseInt(getParm(req, parm));
//    }
    
//    private int getParmInt(HttpServletRequest req, String parm, int defVal)
//    {
//    	String val = getParm(req, parm);
//    	if(val == null || val.isEmpty())
//    		return defVal;
//    	
//    	return Integer.parseInt(val);
//    }
    
	private ArrayList<Object> constructNormalStage(HttpServletRequest req) {
		String normalChecked[] = req.getParameterValues("normal.checked");
		if (normalChecked != null) {

			String workStageName = new String("normal");
			ArrayList<Object> workStageList = new ArrayList<Object>();
			for (int i = 0; i < normalChecked.length; i++) {
				if (i > 0) {
					workStageName = new String("normal" + i);
				}
				Stage stage = new Stage(workStageName);
				Work work = new Work(workStageName, "normal");
				work.setWorkers(getParmInt(req
						.getParameterValues("normal.workers")[i]));
				work.setRampup(getParmInt(req
						.getParameterValues("normal.rampup")[i]));
				work.setRuntime(getParmInt(req
						.getParameterValues("normal.runtime")[i]));

				String config = "";
		    	String cprefix = req.getParameter("normal.cprefix");
				if (cprefix != null && cprefix != "") {
					config += "cprefix=" + cprefix;
				}
		    	work.setConfig(config);
		    	
				// read operation
				int rRatio = getParmInt(
						req.getParameterValues("read.ratio")[i], 0);
				
				if (rRatio > 0) {
					String rconfig = "";
					Operation rOp = new Operation("read");
					rOp.setRatio(rRatio);
	
					String rcselector = req.getParameterValues("read.containers")[i];
					String rcmin = req.getParameterValues("read.containers.min")[i];
					String rcmax = req.getParameterValues("read.containers.max")[i];
					String rcexp = parseSelectorToString(rcselector, rcmin, rcmax);
					rconfig += "containers=" + rcexp + ";";
	
					// "objects" section in config
					String roselector = req.getParameterValues("read.objects")[i];
					String romin = req.getParameterValues("read.objects.min")[i];
					String romax = req.getParameterValues("read.objects.max")[i];
					String roexp = parseSelectorToString(roselector, romin, romax);
					rconfig += "objects=" + roexp;
					rOp.setConfig(rconfig);
	
					work.addOperation(rOp);
				}

				// write operation
				int wRatio = getParmInt(
						req.getParameterValues("write.ratio")[i], 0);
				if (wRatio > 0) {
					String wconfig = "";
					Operation wOp = new Operation("write");
					wOp.setRatio(wRatio);
	
					String wcselector = req.getParameterValues("write.containers")[i];
					String wcmin = req.getParameterValues("write.containers.min")[i];
					String wcmax = req.getParameterValues("write.containers.max")[i];
					String wcexp = parseSelectorToString(wcselector, wcmin, wcmax);
					wconfig += "containers=" + wcexp + ";";
	
					// "objects" section in config
					String woselector = req.getParameterValues("write.objects")[i];
					String womin = req.getParameterValues("write.objects.min")[i];
					String womax = req.getParameterValues("write.objects.max")[i];
					String woexp = parseSelectorToString(woselector, womin, womax);
					wconfig += "objects=" + woexp + ";";
	
					// "sizes" section in config
					String wsselector = req.getParameterValues("write.sizes")[i];
					String wsmin = req.getParameterValues("write.sizes.min")[i];
					String wsmax = req.getParameterValues("write.sizes.max")[i];
					String wsunit = req.getParameterValues("write.sizes.unit")[i];
					String wsexp = parseSelectorToString(wsselector, wsmin, wsmax);
					wconfig += "sizes=" + wsexp + wsunit;
					
					//""partSize" section in config
					String wpartSizeName = req.getParameterValues("write.partsizeName")[i];
					if (wpartSizeName.equals("partSize")) {
						String wpselector = req.getParameterValues("write.partsize")[i];
						String wpmin = req.getParameterValues("write.partsize.min")[i];
						String wpmax = req.getParameterValues("write.partsize.max")[i];
						String wpunit = req.getParameterValues("write.partsize.unit")[i];
						String wpexp = parseSelectorToString(wpselector, wpmin, wpmax);		
						wconfig += ";partSize=" + wpexp + wpunit;
					}
	
					wOp.setConfig(wconfig);
	
					work.addOperation(wOp);
				}

				// filewrite operation
				int fwRatio = getParmInt(
						req.getParameterValues("filewrite.ratio")[i], 0);
				if (fwRatio > 0) {
					String fwconfig = "";
					Operation fwOp = new Operation("filewrite");
					fwOp.setRatio(fwRatio);
	
					// "containers" section in config
					String fwcselector = req.getParameterValues("filewrite.containers")[i];
					String fwcmin = req.getParameterValues("filewrite.containers.min")[i];
					String fwcmax = req.getParameterValues("filewrite.containers.max")[i];
					String fwcexp = parseSelectorToString(fwcselector, fwcmin, fwcmax);
					fwconfig += "containers=" + fwcexp + ";";
	
					// "objects" section in config
					String fwoselector = req.getParameterValues("filewrite.fileselection")[i];
					fwconfig += "fileselection=" + fwoselector + ";";
	
					// "files" section in config
					String fwfselector = req.getParameterValues("filewrite.files")[i];
					fwconfig += "files=" + fwfselector;
	
					fwOp.setConfig(fwconfig);
	
					work.addOperation(fwOp);
				}

				// delete operation
				int dRatio = getParmInt(
						req.getParameterValues("delete.ratio")[i], 0);
				if (dRatio > 0) {
				String dconfig = "";
				Operation dOp = new Operation("delete");

				
				dOp.setRatio(dRatio);

				String dcselector = req.getParameterValues("delete.containers")[i];
				String dcmin = req.getParameterValues("delete.containers.min")[i];
				String dcmax = req.getParameterValues("delete.containers.max")[i];
				String dcexp = parseSelectorToString(dcselector, dcmin, dcmax);
				dconfig += "containers=" + dcexp + ";";

				// "objects" section in config
				String doselector = req.getParameterValues("delete.objects")[i];
				String domin = req.getParameterValues("delete.objects.min")[i];
				String domax = req.getParameterValues("delete.objects.max")[i];
				String doexp = parseSelectorToString(doselector, domin, domax);
				dconfig += "objects=" + doexp;
				
				//""batch" section in config
				String dbatchName = req.getParameterValues("delete.batchName")[i];
				System.out.println(dbatchName);
				if (dbatchName.equals("batch")) {
					String dbselector = req.getParameterValues("delete.batch")[i];
					String dbmin = req.getParameterValues("delete.batch.min")[i];
					String dbmax = req.getParameterValues("delete.batch.max")[i];
					String wpexp = parseSelectorToString(dbselector, dbmin, dbmax);		
					dconfig += ";batch=" + wpexp;
				}
				
				dOp.setConfig(dconfig);

				work.addOperation(dOp);
				}
				
				stage.addWork(work);

				workStageList.add(stage);
				
				checkAndAddDelay(req, "normal", workStageList, i);
			}
			return workStageList;
		}
		
		return null;
	}
    
	private int getParmInt(String string, int defVal) {
		if (string != null)
			return Integer.parseInt(string);
		else
			return defVal;
	}

	private int getParmInt(String string) {
		return Integer.parseInt(string);
	}
	
	private void checkAndAddDelay(HttpServletRequest req, String stage,
			ArrayList<Object> workStageList, int iteration) {
		String delayChecked[] = req
				.getParameterValues(stage + ".delay.checked");
		if (delayChecked != null) {
			boolean hasDelay = ("on".equalsIgnoreCase(delayChecked[iteration]));
			if (hasDelay) {
				Stage delayStage = new Stage("delay");
				delayStage
						.setClosuredelay(getParmInt(
								req.getParameterValues("init.delay.closuredelay")[iteration],
								60));
				Work work = new Work("delay", "delay");
				work.addOperation(new Operation("delay"));
				delayStage.addWork(work);
				workStageList.add(delayStage);
			}
		}
	}
	
	// method for removing nsroot config from prepare, normal and cleanup stages
	private Storage removeNSROOTConfig(Storage storage) {
		if(storage.getConfig() == null)
			return storage;
		if (!storage.getConfig().contains("nsroot"))
			return storage;
		else {
			Storage newStorage = new Storage();
			String configParams[] = storage.getConfig().split(";");
			StringBuffer newConfig = new StringBuffer("");
			for (String configParam : configParams) {
				if (!configParam.toLowerCase().contains("nsroot"))
					newConfig.append(configParam + ";");
			}
			newConfig.deleteCharAt(newConfig.length() - 1);
			newStorage.setType(storage.getType());
			newStorage.setConfig(newConfig.toString());
			return newStorage;
		}
	}

    private Workload constructWorkloadFromPostData(HttpServletRequest req)
            throws Exception {
    	Workload workload = new Workload();
    	
    	String name = getParm(req, "workload.name");
    	if(name == null || name.isEmpty())
    		name = "workload";
    	String desc = getParm(req, "workload.desc");
    	
    	workload.setName(name);
    	workload.setDescription(desc);
    	
    	String authType = getParm(req, "auth.type");
    	String authUrlConfig = getParm(req, "auth.url");
    	String authConfig = "";
		if (!authType.toLowerCase().equals("none") && !StringUtils.isEmpty(authUrlConfig)) {
			String authUserConfig = parseUserFromPostData(req, "auth.user");
			authConfig = authUserConfig + authUrlConfig;
		}
    	LOGGER.debug("The authConfig of the workload {} is {}", name, authConfig);
    	System.out.println(authConfig);
    	workload.setAuth(new Auth(authType, authConfig));
    	
    	String storageType = getParm(req, "storage.type");
    	String storageUrlConfig = getParm(req, "storage.url");
    	String storageConfig = "";
		if (!StringUtils.isEmpty(storageUrlConfig)) {
			String storageUserConfig = parseUserFromPostData(req, "storage.user");
			storageConfig = storageUserConfig + storageUrlConfig;
		}
		LOGGER.debug("The storageConfig of the workload {} is {}", name, storageConfig);
    	workload.setStorage(new Storage(storageType, storageConfig));

    	Workflow workflow = new Workflow();
    	
		ArrayList<Object> initStageList = constructInitStage(req);
		if (initStageList != null) {
			for (int i = 0; i < initStageList.size(); i++) {
				workflow.addStage((Stage) initStageList.get(i));
			}
		}
    	
		ArrayList<Object> prepareStageList = constructPrepareStage(req);
		if (prepareStageList != null) {
			for (int i = 0; i < prepareStageList.size(); i++) {
				Stage stage = (Stage) prepareStageList.get(i);
				stage.setStorage(removeNSROOTConfig(workload.getStorage()));
				workflow.addStage(stage);
			}
		}   	
    	
		ArrayList<Object> normalStageList = constructNormalStage(req);
		if (normalStageList != null) {
			for (int i = 0; i < normalStageList.size(); i++) {
				Stage stage = (Stage) normalStageList.get(i);
				stage.setStorage(removeNSROOTConfig(workload.getStorage()));
				workflow.addStage(stage);
			}
		}	
    	
		ArrayList<Object> cleanupStageList = constructCleanupStage(req);
		if (cleanupStageList != null) {
			for (int i = 0; i < cleanupStageList.size(); i++) {
				Stage stage = (Stage) cleanupStageList.get(i);
				stage.setStorage(removeNSROOTConfig(workload.getStorage()));
				workflow.addStage(stage);
			}
		}  
    	
		ArrayList<Object> disposeStageList = constructDisposeStage(req);
		if (disposeStageList != null) {
			for (int i = 0; i < disposeStageList.size(); i++) {
				workflow.addStage((Stage) disposeStageList.get(i));
			}
		}    
    	
    	workload.setWorkflow(workflow);
    	
    	workload.validate();
    	
    	return workload;
    }
    
    private String parseSelectorToString(String selector, String selectorMin, String selectorMax) {
    	String sexp = "";
		if ("u".equals(selector) || "s".equals(selector) || "r".equals(selector))
			sexp = selector + "(" + selectorMin + "," + selectorMax + ")";
		if ("c".equals(selector))
			sexp = selector + "(" + selectorMin + ")";
		return sexp;
    }
    
    /**
     * 
     * @param req
     * @param type 
     * type refers to authUser or storageUser
     * @return
     */
    private String parseUserFromPostData(HttpServletRequest req, String type) {
    	//obtain existing users
    	List<User> allUsers = new ArrayList<User>();
    	readUserFromExcel(allUsers);
    	//according to the userType, parsing user to userConfig
    	String user = getParm(req, type);
    	String[] userArray = user.split(":");
    	if (userArray[0].equals("userGroup")) {
    		String groupName = userArray[1];
			return obtainConfigByUserGroupName(allUsers, groupName);
		}
    	else if (userArray[0].equals("user")) 
    	{
    		String userName = userArray[1];
    		return obtainConfigByUserName(allUsers, userName);
		}
    	return null;
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
    	return userConfig;
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
				userConfig = userConfig + user.getUserName() + ";" + user.getPassword() + ";";
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
    
    private ModelAndView createErrResult(String xml, String msg) {
        ModelAndView result = new ModelAndView("config", "xml", xml);
        result.addObject("error", "ERROR: " + msg);
        
        return result;
    }

    private ModelAndView createSuccResult(String xml) {    	
        ModelAndView result = new ModelAndView(XML, "xml", xml);

        return result;
    }
    
    
}
