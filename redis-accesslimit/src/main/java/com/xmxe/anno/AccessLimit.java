package com.xmxe.anno;

import java.lang.annotation.*;

@Inherited
@Documented
@Target({ElementType.FIELD, ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface AccessLimit {

	/**
	 * 指定second时间内API请求次数
	 */
	int maxCount() default 5;

	/**
	 * 请求次数的指定时间范围秒数(redis数据过期时间)
	 */
	int second() default 60;
}