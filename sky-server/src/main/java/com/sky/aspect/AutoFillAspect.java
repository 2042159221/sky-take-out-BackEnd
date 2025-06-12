package com.sky.aspect;

import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.time.LocalDateTime;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;

import com.sky.annotation.AutoFill;
import com.sky.constant.AutoFillConstant;
import com.sky.context.BaseContext;
import com.sky.enumeration.OperationType;

/**
 * @author Ming
 * @version 1.0
 * @title AutoFillAspect
 * @description [简要说明类的功能]
 * @date 2025/6/12
 * @project sky-take-out
 * @contact email: millionfire@outlook.com
 * @github https://github.com/2042159221
 * @copyright Copyright (c) 2025
 * All rights reserved.
 * @since 2025/6/12
 */
@Aspect
@Component
@Slf4j
public class AutoFillAspect {
    //切入点
    @Pointcut("execution(* com.sky.mapper.*.*(..)) && @annotation(com.sky.annotation.AutoFill)")
    public void autoFillPointCut(){}

    /**
     * 前置通知，在通知中进行公共字段的赋值
     */
    @Before("autoFillPointCut()")
    public void autoFill(JoinPoint joinPoint){
        log.info("开始进行公共字段自动填充");
        
        // 获取到当前被拦截的方法上的数据库操作类型
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        AutoFill autoFill = method.getAnnotation(AutoFill.class);
        if(autoFill == null){
            log.info("未找到AutoFill注解");
            return;
        }
        
        // 获取到数据库操作类型
        OperationType operationType = autoFill.value();
        
        // 获取到当前方法的参数
        Object[] args = joinPoint.getArgs();
        if(args == null || args.length == 0){
            log.info("方法参数为空，不进行填充");
            return;
        }
        
        // 获取到实体对象
        Object entity = args[0];
        log.info("实体对象类型：{}", entity.getClass().getName());
        
        // 获取当前用户ID
        Long currentId = BaseContext.getCurrentId();
        if(currentId == null){
            log.error("当前用户ID为空，自动填充失败");
            return;
        }
        
        // 准备赋值数据
        LocalDateTime now = LocalDateTime.now();
        
        // 根据不同的操作类型，为对应的属性通过反射来赋值
        if(operationType == OperationType.INSERT){
            // 为4个公共字段赋值
            try{
                log.info("为INSERT操作填充字段");
                Method setCreateTime = entity.getClass().getDeclaredMethod(AutoFillConstant.SET_CREATE_TIME, LocalDateTime.class);
                Method setCreateUser = entity.getClass().getDeclaredMethod(AutoFillConstant.SET_CREATE_USER, Long.class);
                Method setUpdateTime = entity.getClass().getDeclaredMethod(AutoFillConstant.SET_UPDATE_TIME, LocalDateTime.class);
                Method setUpdateUser = entity.getClass().getDeclaredMethod(AutoFillConstant.SET_UPDATE_USER, Long.class);

                // 通过反射为对象属性赋值
                setCreateTime.invoke(entity, now);
                setCreateUser.invoke(entity, currentId);
                setUpdateTime.invoke(entity, now);
                setUpdateUser.invoke(entity, currentId);
                
                log.info("公共字段自动填充成功，用户ID：{}，时间：{}", currentId, now);
            }catch(Exception e){
                log.error("公共字段自动填充失败: {}", e.getMessage(), e);
            }
        }else if(operationType == OperationType.UPDATE){
            try{
                log.info("为UPDATE操作填充字段");
                Method setUpdateTime = entity.getClass().getDeclaredMethod(AutoFillConstant.SET_UPDATE_TIME, LocalDateTime.class);
                Method setUpdateUser = entity.getClass().getDeclaredMethod(AutoFillConstant.SET_UPDATE_USER, Long.class);
                
                // 通过反射为对象属性赋值
                setUpdateTime.invoke(entity, now);
                setUpdateUser.invoke(entity, currentId);
                
                log.info("公共字段自动填充成功，用户ID：{}，时间：{}", currentId, now);
            }catch(Exception e){
                log.error("公共字段自动填充失败: {}", e.getMessage(), e);
            }
        }
    }
}
