package org.example.sso.interceptor;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * @author wenbo.zhangw
 * @date 2023/1/13 14:50
 */
@Component
public class I18nInterceptor implements HandlerInterceptor, InitializingBean {

    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler, ModelAndView modelAndView) throws Exception {
        HandlerInterceptor.super.postHandle(request, response, handler, modelAndView);
        System.out.println("modelAndView = " + modelAndView);
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        System.out.println("true = " + true);
    }
}
