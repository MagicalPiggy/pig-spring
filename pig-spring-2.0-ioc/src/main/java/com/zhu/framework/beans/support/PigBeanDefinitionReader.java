package com.zhu.framework.beans.support;

import com.zhu.framework.beans.config.PigBeanDefinition;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/**
 * 读取配置文件的工具类
 */
public class PigBeanDefinitionReader {
    // 配置文件的Properties类
    private Properties contextConfig = new Properties();

    // 扫描到的类名(需要注册到ioc容器的class)
    private List<String> registryBeanClasses = new ArrayList<String>();

    public PigBeanDefinitionReader(String[] configLocations) {
        // 1.读取配置文件
        doLoadConfig(configLocations[0]);

        // 2.扫描相关类
        doScanner(contextConfig.getProperty("scanPackage"));
    }

    public List<PigBeanDefinition> loadBeanDefinitions() {
        List<PigBeanDefinition> result = new ArrayList<PigBeanDefinition>();
        try {
            for (String className : registryBeanClasses) {
                Class<?> beanClass = Class.forName(className);
                // 如果扫描到的是一个接口则不处理
                if (beanClass.isInterface()) {continue;}

                // 将信息封装为PigBeanDefinition对象，并添加到列表中
                result.add(doCreateBeanDefinition(toLowerFirstCase(beanClass.getSimpleName()), beanClass.getName()));
                // 如果这个类实现了某个接口，也需要将接口名+类名封装存入
                for (Class<?> i : beanClass.getInterfaces()) {
                    result.add(doCreateBeanDefinition(i.getName(), beanClass.getName()));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return result;
    }

    private PigBeanDefinition doCreateBeanDefinition(String factoryBeanName, String beanClassName) {
        PigBeanDefinition pigBeanDefinition = new PigBeanDefinition();
        pigBeanDefinition.setFactoryBeanName(factoryBeanName);
        pigBeanDefinition.setBeanClassName(beanClassName);
        return pigBeanDefinition;
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
                registryBeanClasses.add(className);
            }
        }
    }

    private String toLowerFirstCase(String simpleName) {
        char[] chars = simpleName.toCharArray();
        chars[0] += 32;
        return String.valueOf(chars);
    }

}
