package com.example.charity.common;

import cn.dev33.satoken.exception.NotLoginException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    //1、捕获Sa-Token的未登录异常
    @ExceptionHandler(NotLoginException.class)
    public Result<String> handleNotLoginException(NotLoginException e)
    {
        //返回code=401，前端拦截器识别401弹出登录框
        return Result.unauthorized();
    }

    //2、捕获其他未处理异常
    @ExceptionHandler(Exception.class)
    public Result<String> handleException(Exception e)
    {
        e.printStackTrace();//控制台打印错误堆栈
        return Result.error("系统繁忙，请稍后再试："+e.getMessage());
    }
}
