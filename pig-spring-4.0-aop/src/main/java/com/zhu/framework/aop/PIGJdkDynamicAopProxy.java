package com.zhu.framework.aop;

import com.zhu.framework.aop.aspect.PIGAdvice;
import com.zhu.framework.aop.support.PIGAdvisedSupport;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Map;

//动态代理字节码重组，有两种方式，JdkDynamic、Cglib
public class  PIGJdkDynamicAopProxy implements InvocationHandler {
    private PIGAdvisedSupport config;
    public PIGJdkDynamicAopProxy(PIGAdvisedSupport config) {
        this.config = config;
    }

    public Object getProxy() {
        //第一个参数，生成的新类用什么方式加载
        //第二个参数，生成的新类要实现哪个接口
        //第三个参数，通过反射触发调用invoke
        return Proxy.newProxyInstance(this.getClass().getClassLoader(),
                this.config.getTargetClass().getInterfaces(),this);
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        Map<String,PIGAdvice> advices = this.config.getAdvices(method,this.config.getTargetClass());
        Object returnValue;

        //织入前置通知
        invokeAdivce(advices.get("before"));

        try {
            returnValue = method.invoke(this.config.getTarget(), args);
        }catch (Exception e){
            //织入异常通知
            invokeAdivce(advices.get("afterThrowing"));
            e.printStackTrace();
            throw e;
        }

        invokeAdivce(advices.get("after"));
        return returnValue;
    }

    private void invokeAdivce(PIGAdvice advice) {
        try {
            advice.getAdviceMethod().invoke(advice.getAspect());
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }
    }
}
