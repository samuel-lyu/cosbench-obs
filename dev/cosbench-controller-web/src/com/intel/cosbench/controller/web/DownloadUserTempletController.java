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
import java.util.Map;

import javax.servlet.http.*;

import org.apache.commons.io.IOUtils;
import org.springframework.web.servlet.*;

public class DownloadUserTempletController extends UserManagementController {

    private static final View USER = new UserView();

    private static class UserView implements View {

        @Override
        public String getContentType() {
            return "application/x-xls";
        }

        @Override
        public void render(Map<String, ?> model, HttpServletRequest req,
                HttpServletResponse res) throws Exception {
            File user = (File) model.get("user");
            res.setHeader("Content-Length", String.valueOf(user.length()));
            res.setHeader("Content-Disposition",
                    "attachment; filename=\"user-templet.xls\"");
            FileInputStream input = new FileInputStream(user);
            try {
                IOUtils.copyLarge(input, res.getOutputStream());
            } finally {
                IOUtils.closeQuietly(input);
            }
        }

    }

    protected ModelAndView process() {
        File file = new File("user\\user-templet.xls");
        return new ModelAndView(USER, "user", file);
    }

}
