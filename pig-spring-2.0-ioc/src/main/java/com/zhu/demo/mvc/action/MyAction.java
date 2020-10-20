package com.zhu.demo.mvc.action;

import com.zhu.demo.mvc.service.IModifyService;
import com.zhu.demo.mvc.service.IQueryService;
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
@PIGRequestMapping("/web")
public class MyAction {

	@PIGAutowired
	IQueryService queryService;
	@PIGAutowired
	IModifyService modifyService;

	@PIGRequestMapping("/query.json")
	public void query(HttpServletRequest request, HttpServletResponse response,
					  @PIGRequestParam("name") String name){
		String result = queryService.query(name);
		out(response,result);
	}

	@PIGRequestMapping("/add*.json")
	public void add(HttpServletRequest request,HttpServletResponse response,
					@PIGRequestParam("name") String name,@PIGRequestParam("addr") String addr){
		String result = modifyService.add(name,addr);
		out(response,result);
	}

	@PIGRequestMapping("/remove.json")
	public void remove(HttpServletRequest request,HttpServletResponse response,
					   @PIGRequestParam("id") Integer id){
		String result = modifyService.remove(id);
		out(response,result);
	}

	@PIGRequestMapping("/edit.json")
	public void edit(HttpServletRequest request,HttpServletResponse response,
					 @PIGRequestParam("id") Integer id,
					 @PIGRequestParam("name") String name){
		String result = modifyService.edit(id,name);
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

