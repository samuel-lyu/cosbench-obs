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
import java.util.Arrays;

import javax.servlet.http.*;

import org.springframework.web.servlet.ModelAndView;

import com.intel.cosbench.model.WorkloadInfo;
import com.intel.cosbench.service.ControllerService;
import com.intel.cosbench.web.AbstractController;

public class MatrixPageController extends AbstractController {

    private ControllerService controller;
    private static final int PAGE_SIZE = 20;

    public void setController(ControllerService controller) {
        this.controller = controller;
    }

    @Override
    protected ModelAndView process(HttpServletRequest req,
            HttpServletResponse res) throws Exception {
    	WorkloadInfo[] workloads = null;
    	Integer page = PageUtil.getPageParam(req);
        ModelAndView result = new ModelAndView("matrix");
        controller.setloadArch(true);
        String[] ops = req.getParameterValues("ops");
        if (ops != null && ops.length > 0)
            for (String op : ops)
                result.addObject(op, true);
        else
            result.addObject("allOps", true);
        String[] metrics = req.getParameterValues("metrics");
        StringBuffer buffer = new StringBuffer();
        if (metrics != null && metrics.length > 0) {
            for (String metric : metrics) {
            	buffer.append("metrics=").append(metric).append("&");
                result.addObject(metric, true);
            }
        }
        else
            result.addObject("allMetrics", true);
        String[] rthistos = req.getParameterValues("rthisto");
        if (rthistos != null && rthistos.length > 0)
        {
        	for (String rthisto : rthistos)
        	{
        		result.addObject(rthisto, true);
        		buffer.append("rthisto=").append(rthisto).append("&");
        	}
        }
        String[] others = req.getParameterValues("others");
        if (others != null && others.length > 0)
        {
            for (String other : others)
            {
            	result.addObject(other, true);
            	buffer.append("others=").append(other).append("&");
            }
        }
        String param = buffer.toString().substring(0,buffer.toString().lastIndexOf("&"));
        result.addObject("param", param);
        if(req.getParameter("type").equals("histo"))
        	result.addObject("hInfos", controller.getHistoryWorkloads());
        else if (req.getParameter("type").equals("arch")) {
        	for (WorkloadInfo info : controller.getArchivedWorkloads()) {
        		if (info.getReport().getAllMetrics().length==0) {
        			try {
        				controller.getWorkloadLoader().loadWorkloadPageInfo(info);
        			} catch (IOException e) {
        				e.printStackTrace();
        			}
        		}
        	}
        	workloads = controller.getArchivedWorkloads("FINISHED");
        }
        int totalPage = workloads.length/PAGE_SIZE + (workloads.length%PAGE_SIZE ==0 ? 0:1);
        if(page < 1)
		{
			page = 1;
		}else if(page > totalPage)
		{
			page = totalPage;
		}
        int start = (page-1)*PAGE_SIZE;
        int end = page*PAGE_SIZE >= workloads.length? workloads.length : page*PAGE_SIZE ;
        WorkloadInfo[] returnWorkloads =Arrays.copyOfRange(workloads, start, end);
        result.addObject("hInfos", returnWorkloads);
        result.addObject("type", req.getParameter("type"));
        result.addObject("totalPage",totalPage);
        result.addObject("currentPage",page);
        result.addObject("totalWorkLoad",workloads.length);
        
        return result;
    }

	
}
