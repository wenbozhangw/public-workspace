package org.example.sso.config;

import org.example.sso.interceptor.I18nInterceptor;
import org.example.sso.interceptor.I18nViewResolver;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ViewResolverRegistry;

import javax.annotation.Resource;

/**
 * @author wenbo.zhangw
 * @date 2023/1/13 15:11
 */
@Configuration
public class WebMvcConfigurer implements org.springframework.web.servlet.config.annotation.WebMvcConfigurer {

    @Resource
    private I18nInterceptor i18nInterceptor;

    @Resource
    private I18nViewResolver i18nViewResolver;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        org.springframework.web.servlet.config.annotation.WebMvcConfigurer.super.addInterceptors(registry);
        registry.addInterceptor(i18nInterceptor);
    }

    @Override
    public void configureViewResolvers(ViewResolverRegistry registry) {
        org.springframework.web.servlet.config.annotation.WebMvcConfigurer.super.configureViewResolvers(registry);
        registry.viewResolver(i18nViewResolver);
    }


}
