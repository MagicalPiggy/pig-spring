package com.zhu.demo.mvc.action;

import com.zhu.demo.mvc.service.IDemoService;
import com.zhu.framework.annotation.PIGAutowired;
import com.zhu.framework.annotation.PIGController;
import com.zhu.framework.annotation.PIGRequestMapping;
import com.zhu.framework.annotation.PIGRequestParam;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * 公布接口url
 *
 */
@PIGController
@PIGRequestMapping("/demo")
public class MyAction {

	@PIGAutowired
	private IDemoService demoService;

	private IDemoService demoService2;

	@PIGRequestMapping("/query")
	public void query(HttpServletRequest request, HttpServletResponse response,
					  @PIGRequestParam("name") String name, @PIGRequestParam("id") String id) {
		String result = "My name is " + name + ", id = " + id;
		out(response,result);
	}
	
	
	private void out(HttpServletResponse resp,String str){
		try {
			resp.getWriter().write(str);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
}
