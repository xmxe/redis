package com.xmxe.anno;

import java.lang.annotation.*;

/**
 * 接口增加幂等性
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface IdempotenceRequired {

}