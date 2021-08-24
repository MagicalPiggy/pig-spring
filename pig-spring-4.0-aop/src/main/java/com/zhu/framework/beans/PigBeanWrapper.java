package com.zhu.framework.beans;

/**
 * IOC容器实例的包装类
 */
public class PigBeanWrapper {
    private Object wrapperInstance;
    private Class<?> wrapperClass;


    public PigBeanWrapper(Object wrapperInstance) {
        this.wrapperClass = wrapperInstance.getClass();
        this.wrapperInstance = wrapperInstance;
    }

    public Object getWrapperInstance() {
        return wrapperInstance;
    }

    public Class<?> getWrapperClass() {
        return wrapperClass;
    }
}
