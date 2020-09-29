package com.zhu.mvcframework.annotation;

import java.lang.annotation.*;


/**
 * 自动注入
 * @author Tom
 *
 */
@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface PIGAutowired {
	String value() default "";
}
