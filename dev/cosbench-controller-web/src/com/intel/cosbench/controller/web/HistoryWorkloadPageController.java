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

import java.io.IOException;

import javax.servlet.http.*;

import org.apache.commons.lang.StringUtils;
import org.springframework.web.servlet.ModelAndView;

import com.intel.cosbench.service.ControllerService;
import com.intel.cosbench.web.AbstractController;

/**
 * The home page of controller web console.
 * 
 * @author ywang19, qzheng7
 *
 */
public class HistoryWorkloadPageController extends AbstractController {

    private ControllerService controller;

    public void setController(ControllerService controller) {
        this.controller = controller;
    }

	@Override
	protected ModelAndView process(HttpServletRequest req,
			HttpServletResponse res) throws IOException {
		ModelAndView result = new ModelAndView("historyWorkload");
		String resubmitIds = req.getParameter("resubmitIds");
		String resubmit = req.getParameter("resubmit");
		String loadArch = req.getParameter("loadArch");
		if (!StringUtils.isEmpty(resubmit)&&!StringUtils.isEmpty(resubmitIds)){
			String[] ids = resubmitIds.split("_");
			for (String resubmitId : ids){
				String newId = controller.resubmit(resubmitId);
				controller.fire(newId);
			}
		}
		if (!StringUtils.isEmpty(loadArch) && loadArch.equals("true"))
			controller.setloadArch(true);
		else if (!StringUtils.isEmpty(loadArch) && loadArch.equals("false"))
			controller.setloadArch(false);
		
        result.addObject("hInfos", controller.getHistoryWorkloads());
        result.addObject("archInfos", controller.getArchivedWorkloads());
        result.addObject("loadArch", controller.getloadArch());
		return result;
	}
}
