package com.zhu.framework.annotation;

import java.lang.annotation.*;

/**
 * 请求url
 *
 */
@Target({ElementType.METHOD,ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface PIGRequestMapping {
	String value() default "";
}
