package com.zhu.framework.beans.config;

/**
 * 保存bean的配置信息
 */
public class PigBeanDefinition {
    // 类名
    private String factoryBeanName;
    // 全类名
    private String beanClassName;

    public String getFactoryBeanName() {
        return factoryBeanName;
    }

    public void setFactoryBeanName(String factoryBeanName) {
        this.factoryBeanName = factoryBeanName;
    }

    public String getBeanClassName() {
        return beanClassName;
    }

    public void setBeanClassName(String beanClassName) {
        this.beanClassName = beanClassName;
    }



}
