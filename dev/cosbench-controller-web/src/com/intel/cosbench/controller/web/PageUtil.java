package com.intel.cosbench.controller.web;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang.StringUtils;

public class PageUtil {

	public static Integer getPageParam(HttpServletRequest req) {
		if (req.getParameter("page") == null || StringUtils.isEmpty(req.getParameter("page"))) {
			return 1;
		}
		try {
			return Integer.valueOf(req.getParameter("page"));
		} catch (NumberFormatException e) {
			return 1;
		}
	}

}
