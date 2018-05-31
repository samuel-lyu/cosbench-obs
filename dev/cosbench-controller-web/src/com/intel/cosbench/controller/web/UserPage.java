package com.intel.cosbench.controller.web;

import java.util.List;

import org.springframework.web.servlet.ModelAndView;

import com.intel.cosbench.controller.entity.User;

public class UserPage {
	
	private static final int PAGE_SIZE = 20;

	public static ModelAndView page(ModelAndView result, List<User> allUsers, Integer page) {
		int totalPage = allUsers.size()/PAGE_SIZE + (allUsers.size()%PAGE_SIZE ==0 ? 0:1);
		if(page < 1)
		{
			page = 1;
		}else if(page > totalPage)
		{
			page = totalPage;
		}
		int start = (page-1)*PAGE_SIZE;
        int end = page*PAGE_SIZE >= allUsers.size()? allUsers.size() : page*PAGE_SIZE ;
        List<User> returnUsers = allUsers.subList(start, end);
    	result.addObject("users", returnUsers);
    	result.addObject("totalPage",totalPage);
        result.addObject("currentPage",page);
        result.addObject("totalUsers",allUsers.size());
        return result;
	}

}
