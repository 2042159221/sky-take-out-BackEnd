package com.sky.handler;

import com.sky.constant.MessageConstant;
import com.sky.exception.AccountLockedException;
import com.sky.exception.AccountNotFoundException;
import com.sky.exception.PasswordErrorException;
import com.sky.result.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.sql.SQLIntegrityConstraintViolationException;

/**
 * 全局异常处理器
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    /**
     * 捕获业务异常
     * @param ex
     * @return
     */
    @ExceptionHandler
    public Result exceptionHandler(AccountLockedException ex){
        log.error("异常信息：{}", ex.getMessage());
        return Result.error(ex.getMessage());
    }

    /**
     * 捕获业务异常
     * @param ex
     * @return
     */
    @ExceptionHandler
    public Result exceptionHandler(AccountNotFoundException ex){
        log.error("异常信息：{}", ex.getMessage());
        return Result.error(ex.getMessage());
    }

    /**
     * 捕获业务异常
     * @param ex
     * @return
     */
    @ExceptionHandler
    public Result exceptionHandler(PasswordErrorException ex){
        log.error("异常信息：{}", ex.getMessage());
        return Result.error(ex.getMessage());
    }

    /**
     * 处理HTTP请求方法不支持的异常
     * @param ex
     * @return
     */
    @ExceptionHandler
    public Result exceptionHandler(HttpRequestMethodNotSupportedException ex){
        log.error("异常信息：{}", ex.getMessage());
        return Result.error("请求方法不支持，请使用正确的HTTP方法");
    }

    /**
     * 处理重复键异常
     * @param ex
     * @return
     */
    @ExceptionHandler
    public Result exceptionHandler(DuplicateKeyException ex){
        log.error("数据重复异常：{}", ex.getMessage());
        String message = ex.getCause().getMessage();
        if(message.contains("Duplicate entry")) {
            try {
                // 提取重复的值，格式通常是：Duplicate entry 'xxx' for key 'yyy'
                String duplicateValue = message.split("'")[1];
                return Result.error(duplicateValue + MessageConstant.ALREADY_EXISTS);
            } catch (Exception e) {
                log.error("解析重复值失败", e);
            }
        }
        return Result.error("该数据已存在");
    }

    /**
     * 处理SQL异常
     * @param ex
     * @return
     */
   @ExceptionHandler
   public Result exceptionHandler(SQLIntegrityConstraintViolationException ex){
       //Duplicate entry 'zhangsan' for key 'employee.idx_username'
       String message = ex.getMessage();
       if(message.contains("Duplicate entry")){
           String[] split = message.split(" ");
           String username = split[2];
           String msg = username + MessageConstant.ALREADY_EXISTS;
           return Result.error(msg);
       }else{
           return Result.error(MessageConstant.UNKNOWN_ERROR);
       }
   }

    /**
     * 处理数据完整性异常qing
     * @param ex
     * @return
     */
    @ExceptionHandler
    public Result exceptionHandler(DataIntegrityViolationException ex){
        log.error("数据完整性异常：{}", ex.getMessage(), ex);
        return Result.error("数据格式不正确或缺少必要数据");
    }

    /**
     * 处理未知异常
     * @param ex
     * @return
     */
    @ExceptionHandler
    public Result exceptionHandler(Exception ex){
        log.error("异常信息：{}", ex.getMessage());
        return Result.error("系统繁忙，请稍后再试");
    }
}
