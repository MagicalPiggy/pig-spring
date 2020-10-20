package com.zhu.framework.servlet;

import com.zhu.framework.annotation.*;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.*;

public class PIGDispatcherServlet extends HttpServlet {
    // 初始化ioc容器
    private Map<String, Object> ioc = new HashMap<String, Object>();

    // 配置文件的Properties类
    private Properties contextConfig = new Properties();

    // 扫描到的类名
    private List<String> classNames = new ArrayList<String>();

    // RequestMapping映射关系
    private Map<String, Method> handlerMapping = new HashMap<String, Method>();

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

        // 投机取巧，获取实例对象
        String beanName = toLowerFirstCase(method.getDeclaringClass().getSimpleName());

        // 反射调用方法
        // 第一个参数，Method所在的实例
        // 第二个参数，Method的实参列表
        method.invoke(ioc.get(beanName), paramValues);
    }

    // 初始化阶段
    @Override
    public void init(ServletConfig config) throws ServletException {
        // ================== IoC =======================
        // 1. 加载配置文件
        doLoadConfig(config.getInitParameter("contextConfigLocation"));

        // 2. 扫描相关类
        doScanner(contextConfig.getProperty("scanPackage"));

        // 3. 初始化扫描到的类，并创建实例保存到ioc容器中
        doInstance();

        // ================== DI =======================
        // 4. 完成DI依赖注入，自动赋值
        doAutowired();

        // ================== MVC =======================
        // 5. 初始化HandlerMapping
        doInitHandlerMapping();

        System.out.println("PIG Spring framework is initialized!");
    }

    private void doInitHandlerMapping() {
        if (ioc.isEmpty()) {
            return;
        }

        for (Map.Entry<String, Object> entry : ioc.entrySet()) {
            Class<?> clazz = entry.getValue().getClass();
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

    private void doAutowired() {
        if (ioc.isEmpty()) {
            return;
        }

        for (Map.Entry<String, Object> entry : ioc.entrySet()) {
            // 依次拿到IOC容器中实例的所有字段（属性）
            // 只能拿到public的属性
            Field[] fields = entry.getValue().getClass().getDeclaredFields();

            for (Field field : fields) {
                // 如果没有加注解则跳过
                if (!field.isAnnotationPresent(PIGAutowired.class)) {
                    continue;
                }

                PIGAutowired autowired = field.getAnnotation(PIGAutowired.class);
                String beanName = autowired.value().trim();
                // 若注解未指定值，则取其类型作为beanName
                if ("".equals(beanName)) {
                    beanName = field.getType().getName();
                }
                // 暴力访问，保证所有访问类型都可注入成功
                field.setAccessible(true);

                try {
                    // 以下举例说明
                    // field相当于@PIGAutowired private IDemoService demoService (为DemoAction类中的一个字段)
                    // entry.getValue()相当于DemoAction的实例
                    // ioc.get(beanName)相当于从ioc容器中拿到key为IDemoService（全类名)对应的实例，即demoService的实例
                    field.set(entry.getValue(),  ioc.get(beanName));
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                    continue;
                }
            }
        }
    }

    private void doInstance() {
        if (classNames.isEmpty()) {
            return;
        }
        try {
            for (String className : classNames) {
                Class<?> clazz = Class.forName(className);

                // 加了注解的才能实例化
                // 使用了Controller注解的情况
                if (clazz.isAnnotationPresent(PIGController.class)) {
                    // 首字母小写
                    // getSimpleName()是无包名的
                    String beanName = toLowerFirstCase(clazz.getSimpleName());
                    Object instance = clazz.newInstance();

                    ioc.put(beanName,instance);

                } else if (clazz.isAnnotationPresent(PIGService.class)) {
                    // 使用了Service注解的情况
                    // 1.默认类名首字母小写
                    String beanName = toLowerFirstCase(clazz.getSimpleName());

                    // 2.自定义beanName，保证唯一（处理不同包下类名相同的情况）
                    // 拿到注解的值，也就是自定义的beanName
                    PIGService service = clazz.getAnnotation(PIGService.class);
                    if (!"".equals(service.value())) {
                        beanName = service.value();
                    }
                    Object instance = clazz.newInstance();
                    ioc.put(beanName,instance);

                    // 3.用全类名作为beanName(如果需要注入的类型是一个接口，那么就需要用全类名找到其实现类)
                    for (Class<?> i : clazz.getInterfaces()) {
                        // 若一个接口被多个类实现
                        if (ioc.containsKey(i.getName())) {
                            throw new Exception("The beanName is exits !");
                        }
                        ioc.put(i.getName(), instance);
                    }
                }

            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private String toLowerFirstCase(String simpleName) {
        char[] chars = simpleName.toCharArray();
        chars[0] += 32;
        return String.valueOf(chars);
    }

    private void doScanner(String scanPackage) {
        // 到ClassPath下找相关的.class文件
        // scanPackage包路径对应一个文件夹
        // 下面转为文件路径
        URL url = this.getClass().getClassLoader().getResource("/" + scanPackage.replaceAll("\\.", "/"));
        File classpath = new File(url.getFile());
        for (File file : classpath.listFiles()) {
            // 如果是文件夹，则递归遍历之
            if (file.isDirectory()) {
                doScanner(scanPackage + "." + file.getName());
            } else {
                if (!file.getName().endsWith(".class")) {continue;}
                String className = scanPackage + "." + file.getName().replaceAll(".class", "");
                // 可以在实例化阶段通过调用Class.forName(className)反射创建实例
                classNames.add(className);
            }
        }
    }

    private void doLoadConfig(String contextConfigLocation) {
        // 直接从ClassPath下找spring的配置文件
        // 相当于把application.properties文件读到了内存中(contextConfig这个对象)
        InputStream is = this.getClass().getClassLoader().getResourceAsStream(contextConfigLocation.replaceAll("classpath:",""));
        try {
            contextConfig.load(is);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (null != is) {
                try {
                    is.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
