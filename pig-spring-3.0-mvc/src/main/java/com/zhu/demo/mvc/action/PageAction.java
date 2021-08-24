package com.zhu.demo.mvc.action;

import com.zhu.demo.mvc.service.IQueryService;
import com.zhu.framework.annotation.PIGAutowired;
import com.zhu.framework.annotation.PIGController;
import com.zhu.framework.annotation.PIGRequestMapping;
import com.zhu.framework.annotation.PIGRequestParam;
import com.zhu.framework.servlet.PIGModelAndView;

import java.util.HashMap;
import java.util.Map;

/**
 * 公布接口url
 *
 */
@PIGController
@PIGRequestMapping("/")
public class PageAction {
    @PIGAutowired
    IQueryService queryService;

    @PIGRequestMapping("/first.html")
    public PIGModelAndView query(@PIGRequestParam("teacher") String teacher){
        String result = queryService.query(teacher);
        Map<String,Object> model = new HashMap<String,Object>();
        model.put("teacher", teacher);
        model.put("data", result);
        model.put("token", "123456");
        return new PIGModelAndView("first.html",model);
    }
}
