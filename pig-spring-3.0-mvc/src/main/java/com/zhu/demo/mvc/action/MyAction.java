package com.zhu.demo.mvc.action;

import com.zhu.demo.mvc.service.IModifyService;
import com.zhu.demo.mvc.service.IQueryService;
import com.zhu.framework.annotation.PIGAutowired;
import com.zhu.framework.annotation.PIGController;
import com.zhu.framework.annotation.PIGRequestMapping;
import com.zhu.framework.annotation.PIGRequestParam;
import com.zhu.framework.servlet.PIGModelAndView;

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
	public PIGModelAndView query(HttpServletRequest request, HttpServletResponse response,
								 @PIGRequestParam("name") String name){
		String result = queryService.query(name);
		return out(response,result);
	}

	@PIGRequestMapping("/add*.json")
	public PIGModelAndView add(HttpServletRequest request,HttpServletResponse response,
					@PIGRequestParam("name") String name,@PIGRequestParam("addr") String addr){
		String result = modifyService.add(name,addr);
		return out(response,result);
	}

	@PIGRequestMapping("/remove.json")
	public PIGModelAndView remove(HttpServletRequest request,HttpServletResponse response,
					   @PIGRequestParam("id") Integer id){
		String result = modifyService.remove(id);
		return out(response,result);
	}

	@PIGRequestMapping("/edit.json")
	public PIGModelAndView edit(HttpServletRequest request,HttpServletResponse response,
					 @PIGRequestParam("id") Integer id,
					 @PIGRequestParam("name") String name){
		String result = modifyService.edit(id,name);
		return out(response,result);
	}

	private PIGModelAndView out(HttpServletResponse resp,String str){
		try {
			resp.getWriter().write(str);
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}

}

