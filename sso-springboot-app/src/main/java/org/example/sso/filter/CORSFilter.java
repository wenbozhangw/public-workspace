package org.example.sso.filter;

import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import javax.servlet.*;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author ：wenbo.zhangw
 * @date ：Created in 2021/5/31 4:49 下午
 */
@Order(Integer.MAX_VALUE - 2)
@Component
@WebFilter(displayName = "CORSFilter")
public class CORSFilter implements Filter {


    private static final Pattern CORS_ALLOW_ORIGIN_REGEX = Pattern.compile("http://10.57.16.237:19090");

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain chain) throws IOException, ServletException {
        chain.doFilter(servletRequest, servletResponse);
//        HttpServletRequest request = (HttpServletRequest) servletRequest;
//        HttpServletResponse response = (HttpServletResponse) servletResponse;
//
//        String orignalHeader = request.getHeader("Origin");
//
//        if (orignalHeader != null ) {
//            Matcher m = CORS_ALLOW_ORIGIN_REGEX.matcher(orignalHeader);
//            if (m.matches()) {
//                response.addHeader("Access-Control-Allow-Origin", orignalHeader);
//                response.addHeader("Access-Control-Allow-Credentials", "true");
//                response.addHeader("Access-Control-Allow-Methods", "POST, GET, OPTIONS, DELETE, PUT");
//                response.addHeader("Access-Control-Allow-Headers", request.getHeader("Access-Control-Request-Headers"));
//            }
//        }
//
//        if ("OPTIONS".equals(request.getMethod())) {
//            response.setStatus(HttpServletResponse.SC_OK);
//        } else {
//            chain.doFilter(request, response);
//        }
    }
}

