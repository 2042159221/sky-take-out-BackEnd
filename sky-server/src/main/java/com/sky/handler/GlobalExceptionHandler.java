package com.sky.handler;

import com.sky.exception.AccountLockedException;
import com.sky.exception.AccountNotFoundException;
import com.sky.exception.PasswordErrorException;
import com.sky.result.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

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
