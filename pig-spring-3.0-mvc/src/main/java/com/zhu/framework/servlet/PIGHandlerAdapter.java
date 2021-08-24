package com.zhu.framework.servlet;

import com.zhu.framework.annotation.PIGRequestParam;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class PIGHandlerAdapter {
    //动态匹配参数
    public PIGModelAndView handle(HttpServletRequest req, HttpServletResponse resp,PIGHandlerMapping handler) throws Exception {

        //形参列表：编译后就能拿到值

        Map<String,Integer> paramIndexMapping = new HashMap<String, Integer>();

        //提取加了PIGRequestParam注解的参数的位置
        Annotation[][] pa = handler.getMethod().getParameterAnnotations();
        for (int i = 0; i < pa.length; i ++){
            for (Annotation a : pa[i]) {
                if(a instanceof PIGRequestParam){
                    String paramName = ((PIGRequestParam) a).value();
                    if(!"".equals(paramName.trim())){
                        paramIndexMapping.put(paramName,i);
                    }
                }
            }
        }

        //提取request和response的位置
        Class<?> [] paramTypes = handler.getMethod().getParameterTypes();
        for (int i = 0; i < paramTypes.length; i++) {
            Class<?> type = paramTypes[i];
            if(type == HttpServletRequest.class || type == HttpServletResponse.class){
                paramIndexMapping.put(type.getName(),i);
            }
        }


        //实参列表：要运行时才能拿到值
        Map<String,String[]> paramsMap = req.getParameterMap();
        //声明实参列表
        Object [] parameValues = new Object[paramTypes.length];
        for (Map.Entry<String,String[]> param : paramsMap.entrySet()) {
            String value = Arrays.toString(paramsMap.get(param.getKey()))
                    .replaceAll("\\[|\\]","")
                    .replaceAll("\\s","");
            if(!paramIndexMapping.containsKey(param.getKey())){continue;}

            int index = paramIndexMapping.get(param.getKey());
            parameValues[index] = caseStringVlaue(value,paramTypes[index]);
        }

        if(paramIndexMapping.containsKey(HttpServletRequest.class.getName())){
            int index = paramIndexMapping.get(HttpServletRequest.class.getName());
            parameValues[index] = req;
        }

        if(paramIndexMapping.containsKey(HttpServletResponse.class.getName())){
            int index = paramIndexMapping.get(HttpServletResponse.class.getName());
            parameValues[index] = resp;
        }


        Object result = handler.getMethod().invoke(handler.getController(),parameValues);

        if(result == null || result instanceof Void){return null;}

        boolean isModelAndView = handler.getMethod().getReturnType() == PIGModelAndView.class;
        if (isModelAndView){
            return (PIGModelAndView)result;
        }
        return  null;
    }

    private Object caseStringVlaue(String value, Class<?> paramType) {
        if(String.class == paramType){
            return value;
        }
        if(Integer.class == paramType){
            return Integer.valueOf(value);
        }else if(Double.class == paramType){
            return Double.valueOf(value);
        }else {
            if(value != null){
                return value;
            }
            return null;
        }
    }
}
