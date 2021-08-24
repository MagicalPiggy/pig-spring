package com.zhu.demo.mvc.service.impl;

import com.zhu.demo.mvc.service.IQueryService;
import com.zhu.framework.annotation.PIGService;
import lombok.extern.slf4j.Slf4j;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * 查询业务
 * @author Pig
 *
 */
@PIGService
public class QueryService implements IQueryService {

	/**
	 * 查询
	 */
	public String query(String name) {
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		String time = sdf.format(new Date());
		String json = "{name:\"" + name + "\",time:\"" + time + "\"}";
		//log.info("这是在业务方法中打印的：" + json);
		return json;
	}

}
