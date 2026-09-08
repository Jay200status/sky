package com.sky.annotation;

import com.sky.enumeration.OperationType;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 用于标识需要进行公共字段填充的方法
 */
//需要进行公共字段填充的程序方法
@Target(ElementType.METHOD)
//指定被修饰的注解可以保留到什么时候
@Retention(RetentionPolicy.RUNTIME)
public @interface AutoFill{
    OperationType value();
}
