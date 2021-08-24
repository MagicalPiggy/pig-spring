package com.zhu.framework.servlet;

import com.zhu.framework.annotation.*;
import com.zhu.framework.context.PigApplicationContext;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PIGDispatcherServlet extends HttpServlet {

    private List<PIGHandlerMapping> handlerMappings = new ArrayList<PIGHandlerMapping>();

    private Map<PIGHandlerMapping,PIGHandlerAdapter> handlerAdapters = new HashMap<PIGHandlerMapping, PIGHandlerAdapter>();

    private List<PIGViewResolver> viewResolvers = new ArrayList<PIGViewResolver>();

    private PigApplicationContext context;

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
        //1、根据URL拿到对应的HandlerMapping对象
        PIGHandlerMapping handler = getHandler(req);

        if(null == handler){
            processDispatchResult(req,resp,new PIGModelAndView("404"));
            return;
        }

        //2、根据HandlerMapping获得一个HandlerAdapter
        PIGHandlerAdapter ha = getHandlerAdapter(handler);
        
        //3、根据HandlerAdapter拿到一个ModelAndView
        PIGModelAndView mv = ha.handle(req,resp,handler);

        //4、根据ViewResolver根据ModelAndView去拿到View
        processDispatchResult(req,resp,mv);
    }

    private PIGHandlerMapping getHandler(HttpServletRequest req) {
        String url = req.getRequestURI();
        String contextPath = req.getContextPath();
        url = url.replaceAll(contextPath,"").replaceAll("/+","/");

        for (PIGHandlerMapping handlerMapping : this.handlerMappings) {
            Matcher matcher = handlerMapping.getPattern().matcher(url);
            if(!matcher.matches()){continue;}
            return handlerMapping;
        }

        return null;
    }

    //404 、500 、自定义模板
    private void processDispatchResult(HttpServletRequest req, HttpServletResponse resp, PIGModelAndView mv) throws Exception {
        if(null == mv){ return; }

        if(this.viewResolvers.isEmpty()){return;}

        for (PIGViewResolver viewResolver : this.viewResolvers) {
            PIGView view = viewResolver.resolveViewName(mv.getViewName());
            view.render(mv.getModel(),req,resp);
            return;
        }
    }

    private PIGHandlerAdapter getHandlerAdapter(PIGHandlerMapping handler) {
        if(this.handlerAdapters.isEmpty()){return null;}
        return this.handlerAdapters.get(handler);
    }


    // 初始化阶段
    @Override
    public void init(ServletConfig config) throws ServletException {
        // ================== IoC =======================
        context = new PigApplicationContext(config.getInitParameter("contextConfigLocation"));

        //===========  MVC九大组件  ============
        initStrategies(context);

        System.out.println("PIG Spring framework is initialized!");
    }

    private void initStrategies(PigApplicationContext context) {
        //handlerMapping
        initHandlerMappings(context);
        //初始化参数适配器
        initHandlerAdapters(context);
        //初始化视图转换器
        initViewResolvers(context);
    }

    private void initViewResolvers(PigApplicationContext context) {
        //模板引擎的根路径
        String tempateRoot = context.getConfig().getProperty("templateRoot");
        String templateRootPath = this.getClass().getClassLoader().getResource(tempateRoot).getFile();

        File templateRootDir = new File(templateRootPath);
        for (File file : templateRootDir.listFiles()) {
            this.viewResolvers.add(new PIGViewResolver(tempateRoot));
        }
    }

    private void initHandlerAdapters(PigApplicationContext context) {
        for (PIGHandlerMapping handlerMapping : handlerMappings) {
            this.handlerAdapters.put(handlerMapping,new PIGHandlerAdapter());
        }
    }

    private void initHandlerMappings(PigApplicationContext context) {
        if(context.getBeanDefinitionCount() == 0){ return; }

        String [] beanNames = context.getBeanDefinitionNames();
        for (String beanName : beanNames) {
            Object instance = context.getBean(beanName);
            Class<?> clazz = instance.getClass();
            if(!clazz.isAnnotationPresent(PIGController.class)){ continue; }

            String baseUrl = "";
            if(clazz.isAnnotationPresent(PIGRequestMapping.class)){
                PIGRequestMapping requestMapping = clazz.getAnnotation(PIGRequestMapping.class);
                baseUrl = requestMapping.value();
            }

            //默认只获取public方法
            for (Method method : clazz.getMethods()) {
                if(!method.isAnnotationPresent(PIGRequestMapping.class)){continue;}
                PIGRequestMapping requestMapping = method.getAnnotation(PIGRequestMapping.class);
                //   //demo//query
                String regex = ("/" + baseUrl + "/" + requestMapping.value().replaceAll("\\*",".*")).replaceAll("/+","/");
                Pattern pattern = Pattern.compile(regex);
                handlerMappings.add(new PIGHandlerMapping(pattern,instance,method));
                System.out.println("Mapped " + regex + "," + method);

            }
        }
    }


}
