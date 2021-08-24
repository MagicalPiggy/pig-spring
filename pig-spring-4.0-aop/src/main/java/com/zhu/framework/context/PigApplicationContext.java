package com.zhu.framework.context;

import com.zhu.framework.annotation.PIGAutowired;
import com.zhu.framework.annotation.PIGController;
import com.zhu.framework.annotation.PIGService;
import com.zhu.framework.aop.PIGJdkDynamicAopProxy;
import com.zhu.framework.aop.config.PIGAopConfig;
import com.zhu.framework.aop.support.PIGAdvisedSupport;
import com.zhu.framework.beans.PigBeanWrapper;
import com.zhu.framework.beans.config.PigBeanDefinition;
import com.zhu.framework.beans.support.PigBeanDefinitionReader;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * IOC容器的主入口
 */
public class PigApplicationContext {
    private String[] configLocations;
    private PigBeanDefinitionReader reader;

    private final Map<String, PigBeanDefinition> beanDefinitionMap = new HashMap<String, PigBeanDefinition>();

    private Map<String, PigBeanWrapper> factoryBeanInstanceCache = new HashMap<String, PigBeanWrapper>();

    // 单例bean的容器
    private Map<String, Object> factoryBeanObjectCache = new HashMap<String, Object>();

    public PigApplicationContext(String... configLocations) {
        this.configLocations = configLocations;
        try {
            // 1.读取配置文件
            reader = new PigBeanDefinitionReader(configLocations);

            // 2.解析配置文件，将配置信息转为BeanDefinition对象
            List<PigBeanDefinition> beanDefinitions = reader.loadBeanDefinitions();

            // 3.把BeanDefinition对应实例注册到beanDefinitionMap key：beanName,value: beanDefinition对象
            doRegisterBeanDefinition(beanDefinitions);

            // 配置信息初始化阶段完成
            // 4.完成依赖注入，时机为调用getBean()时
            doAutowrited();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public int getBeanDefinitionCount() {
        return factoryBeanInstanceCache.size();
    }

    // 只处理非延时加载的情况
    private void doAutowrited() {
        for (Map.Entry<String, PigBeanDefinition> beanDefinitionEntry : this.beanDefinitionMap.entrySet()) {
            String beanName = beanDefinitionEntry.getKey();
            // 真正的注入需要在getBean方法中完成
            // getBean方法：1.创建实例 2.依赖注入
            getBean(beanName);
        }
    }

    private void doRegisterBeanDefinition(List<PigBeanDefinition> beanDefinitions) throws Exception {
        for (PigBeanDefinition beanDefinition : beanDefinitions) {
            if (this.beanDefinitionMap.containsKey(beanDefinition.getFactoryBeanName())) {
                throw new Exception("The " + beanDefinition.getFactoryBeanName() + " is exists!");
            }
            this.beanDefinitionMap.put(beanDefinition.getFactoryBeanName(), beanDefinition);
            this.beanDefinitionMap.put(beanDefinition.getBeanClassName(), beanDefinition);
        }
    }

    public Object getBean(Class beanClass) {
        return getBean(beanClass.getName());
    }

    public Object getBean(String beanName) {
        // 1.获取BeanDefinition配置信息
        PigBeanDefinition beanDefinition = this.beanDefinitionMap.get(beanName);

        // 2.反射进行实例化
        Object instance = instantiateBean(beanName, beanDefinition);

        // 3.将创建出来的实例包装为BeanWrapper对象
        PigBeanWrapper beanWrapper = new PigBeanWrapper(instance);

        // 4.把BeanWrapper对象存入真正的IoC容器中
        this.factoryBeanInstanceCache.put(beanName, beanWrapper);

        // 5.执行依赖注入
        populateBean(beanName, beanDefinition, beanWrapper);

        return this.factoryBeanInstanceCache.get(beanName).getWrapperInstance();
    }

    // 创建实例化对象
    private Object instantiateBean(String beanName, PigBeanDefinition beanDefinition) {
        // 全类名
        String className = beanDefinition.getBeanClassName();
        Object instance = null;
        try {
            Class<?> clazz = Class.forName(className);
            instance = clazz.getDeclaredConstructor().newInstance();

            //1、读取配置，将通知和目标类建立关系
            PIGAdvisedSupport config = instantionAopConfig(beanDefinition);
            config.setTargetClass(clazz);
            config.setTarget(instance);
            //判断，要不要生成代理类
            if(config.pointCutMatch()){
                instance = new PIGJdkDynamicAopProxy(config).getProxy();
            }

            // spring内部的容器不止一个
            factoryBeanObjectCache.put(beanName, instance);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return instance;
    }

    //从配置文件读取信息保存到config对象
    private PIGAdvisedSupport instantionAopConfig(PigBeanDefinition beanDefinition) {
        PIGAopConfig config = new PIGAopConfig();
        config.setPointCut(this.reader.getConfig().getProperty("pointCut"));
        config.setAspectClass(this.reader.getConfig().getProperty("aspectClass"));
        config.setAspectBefore(this.reader.getConfig().getProperty("aspectBefore"));
        config.setAspectAfter(this.reader.getConfig().getProperty("aspectAfter"));
        config.setAspectAfterThrow(this.reader.getConfig().getProperty("aspectAfterThrow"));
        config.setAspectAfterThrowingName(this.reader.getConfig().getProperty("aspectAfterThrowingName"));
        return new PIGAdvisedSupport(config);
    }

    // 完成依赖注入
    private void populateBean(String beanName, PigBeanDefinition beanDefinition, PigBeanWrapper beanWrapper) {
        Object instance = beanWrapper.getWrapperInstance();
        Class<?> clazz = beanWrapper.getWrapperClass();

        // 只有加了注解的才进行依赖注入
        // @Component
        if (!(clazz.isAnnotationPresent(PIGController.class) || clazz.isAnnotationPresent(PIGService.class))) {
            return;
        }

        // 拿到实例的所有字段（属性）
        // 只能拿到public的属性
        Field[] fields = clazz.getDeclaredFields();

        for (Field field : fields) {
            // 如果没有加Autowired注解则跳过
            if (!field.isAnnotationPresent(PIGAutowired.class)) {
                continue;
            }

            PIGAutowired autowired = field.getAnnotation(PIGAutowired.class);
            String autowiredBeanName = autowired.value().trim();
            // 若注解未指定值，则取其类型作为beanName
            if ("".equals(autowiredBeanName)) {
                autowiredBeanName = field.getType().getName();
            }
            // 暴力访问，保证所有访问类型都可注入成功
            field.setAccessible(true);

            try {
                // 以下举例说明
                // field相当于@PIGAutowired private IDemoService demoService (为DemoAction类中的一个字段)
                // instance相当于DemoAction的实例
                // this.factoryBeanInstanceCache.get(autowiredBeanName).getWrapperInstance()相当于从容器中拿到key为IDemoService（全类名)对应的实例，即demoService的实例
                if (this.factoryBeanObjectCache.get(autowiredBeanName) == null) {
                    continue;
                }
                // 关键步骤
                field.set(instance,  this.factoryBeanInstanceCache.get(autowiredBeanName).getWrapperInstance());
            } catch (IllegalAccessException e) {
                e.printStackTrace();
                continue;
            }

        }
    }

    public String[] getBeanDefinitionNames() {
        return this.beanDefinitionMap.keySet().toArray(new String[this.beanDefinitionMap.size()]);
    }

    public Properties getConfig(){
        return this.reader.getConfig();
    }
}
