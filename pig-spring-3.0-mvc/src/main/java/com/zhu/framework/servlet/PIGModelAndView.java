package com.zhu.framework.servlet;

import java.util.Map;

public class PIGModelAndView {
    private String viewName;
    private Map<String,?> model;

    public PIGModelAndView(String viewName, Map<String, Object> model) {
        this.viewName = viewName;
        this.model = model;
    }

    public PIGModelAndView(String viewName) {
        this.viewName = viewName;
    }


    public String getViewName() {
        return viewName;
    }

    public Map<String,?> getModel() {
        return model;
    }
}
