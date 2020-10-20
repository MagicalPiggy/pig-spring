package com.zhu.framework.servlet;

import com.zhu.framework.annotation.*;
import com.zhu.framework.context.PigApplicationContext;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.*;

public class PIGDispatcherServlet extends HttpServlet {
    // 初始化ioc容器
    private Map<String, Object> ioc = new HashMap<String, Object>();

    // RequestMapping映射关系
    private Map<String, Method> handlerMapping = new HashMap<String, Method>();

    private PigApplicationContext applicationContext;

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        this.doPost(req, resp);
    }

    // 运行阶段
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        // 6. 根据URL完成方法的调度
        try {
            doDispatch(req, resp);
        } catch (Exception e) {
            e.printStackTrace();
            resp.getWriter().write("500 Exception Detail " + Arrays.toString(e.getStackTrace()));
        }
    }

    private void doDispatch(HttpServletRequest req, HttpServletResponse resp) throws Exception {
        // 获取URI：统一资源标识符  如/HttpStudy/demo1
        String url = req.getRequestURI();
        // 获取虚拟目录 如/HttpStudy
        String contextPath = req.getContextPath();
        // 将 /HttpStudy/demo1 替换为 /demo1
        url = url.replaceAll(contextPath, "").replaceAll("/+", "/");

        if (!this.handlerMapping.containsKey(url)) {
            resp.getWriter().write("404 Not Found !!");
            return;
        }

        Method method = this.handlerMapping.get(url);

        // 获取请求中所有参数的map集合
        Map<String,String[]> paramsMap = req.getParameterMap();

        // 拿到方法的形参列表
        Class<?>[] parameterTypes = method.getParameterTypes();

        // 声明实参列表
        Object[] paramValues = new Object[parameterTypes.length];

        // 根据形参列表的配置 给实参列表赋值
        for (int i = 0; i < parameterTypes.length; i++) {
            Class paramterType = parameterTypes[i];
            if (paramterType == HttpServletRequest.class) {
                paramValues[i] = req;
                continue;
            } else if (paramterType == HttpServletResponse.class) {
                paramValues[i] = resp;
                continue;
            } else if (paramterType == String.class) {
                // 二维数组：一个方法有多个注解，一个注解有多个值
                Annotation[][] pa = method.getParameterAnnotations();
                for (int j = 0; j < pa.length; j++) {
                    for (Annotation annotation : pa[i]) {
                        if (annotation instanceof PIGRequestParam) {
                            // 从注解配置中取出参数名称
                            String paramName = ((PIGRequestParam) annotation).value();
                            if (!"".equals(paramName.trim())) {
                                // 从请求体中取出参数的值
                                String value = Arrays.toString(paramsMap.get(paramName))
                                        .replaceAll("\\[|\\]","")
                                        .replaceAll("\\s","");
                                paramValues[i] = value;
                            }
                        }
                    }
                }
            }
        }

        // 反射调用方法
        // 第一个参数，Method所在的实例
        // 第二个参数，Method的实参列表
        method.invoke(applicationContext.getBean(method.getDeclaringClass()), paramValues);
    }

    // 初始化阶段
    @Override
    public void init(ServletConfig config) throws ServletException {
        // ================== IoC =======================
        applicationContext = new PigApplicationContext(config.getInitParameter("contextConfigLocation"));

        // ================== MVC =======================
        // 初始化HandlerMapping
        doInitHandlerMapping();

        System.out.println("PIG Spring framework is initialized!");
    }

    private void doInitHandlerMapping() {
        if (applicationContext.getBeanDefinitionCount() == 0) {
            return;
        }

        String [] beanNames = applicationContext.getBeanDefinitionNames();

        for (String beanName : beanNames) {
            Object instance = applicationContext.getBean(beanName);
            Class<?> clazz = instance.getClass();
            // 只针对加了Controller注解的类
            if (!clazz.isAnnotationPresent(PIGController.class)) {
                continue;
            }

            // 处理类上的url
            String baseUrl = "";
            if (clazz.isAnnotationPresent(PIGRequestMapping.class)) {
                PIGRequestMapping requestMapping = clazz.getAnnotation(PIGRequestMapping.class);
                baseUrl = requestMapping.value();
            }


            // 默认只获取public方法
            for (Method method : clazz.getMethods()) {
                // 只处理加了PIGRequestMapping注解的方法
                if (!method.isAnnotationPresent(PIGRequestMapping.class)) {
                    continue;
                }

                // 拼接URL
                PIGRequestMapping requestMapping = method.getAnnotation(PIGRequestMapping.class);
                String url = ("/" + baseUrl + "/" + requestMapping.value()).replaceAll("/+","/");

                // 存入map中
                handlerMapping.put(url, method);
                System.out.println("Mapped " + url +"," + method);
            }

        }
    }
}
