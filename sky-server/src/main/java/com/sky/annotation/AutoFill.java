package com.sky.annotation;

import com.sky.enumeration.OperationType;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;


/**
 * @author Ming
 * @version 1.0
 * @title AutoFill
 * @description [简要说明类的功能]
 * @date 2025/6/12
 * @project sky-take-out
 * @contact email: millionfire@outlook.com
 * @github https://github.com/2042159221
 * @copyright Copyright (c) 2025
 * All rights reserved.
 * @since 2025/6/12
 */

/**
 * 自定义注解，用于标识某个方法需要进行功能字段填充处理
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface AutoFill {
    //数据库操作类型：UPDATE INSERT
    OperationType value();
}
