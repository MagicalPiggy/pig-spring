package com.zhu.demo.mvc.service.impl;

import com.zhu.demo.mvc.service.IModifyService;
import com.zhu.framework.annotation.PIGService;

/**
 * 增删改业务
 * @author Pig
 *
 */
@PIGService
public class ModifyService implements IModifyService {

	/**
	 * 增加
	 */
	public String add(String name,String addr) {
		return "modifyService add,name=" + name + ",addr=" + addr;
	}

	/**
	 * 修改
	 */
	public String edit(Integer id,String name) {
		return "modifyService edit,id=" + id + ",name=" + name;
	}

	/**
	 * 删除
	 */
	public String remove(Integer id) {
		return "modifyService id=" + id;
	}
	
}
