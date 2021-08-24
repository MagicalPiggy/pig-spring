package com.zhu.framework.annotation;

import java.lang.annotation.*;

/**
 * 业务逻辑,注入接口
 *
 */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface PIGService {
	String value() default "";
}
