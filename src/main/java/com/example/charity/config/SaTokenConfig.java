package com.example.charity.config;

import cn.dev33.satoken.interceptor.SaInterceptor;
import cn.dev33.satoken.router.SaRouter;
import cn.dev33.satoken.stp.StpUtil;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class SaTokenConfig implements WebMvcConfigurer{
    @Override
    public void addInterceptors(InterceptorRegistry registry)
    {
        registry.addInterceptor(new SaInterceptor(handle ->StpUtil.checkLogin()))
                .addPathPatterns("/**")//拦截所有接口
                .excludePathPatterns(
                        "/user/login",
                        "/user/register",
                        "/test/**",
                        "/error"
                );
    }
}
