package com.example.demo.service;

import org.springframework.aop.framework.AopContext;
import org.springframework.stereotype.Component;

/**
 * @author ：wenbo.zhangw
 * @version :
 * @description ：
 * @date ：Created in 2021/2/23 12:38 下午
 * @modified By ：
 */
@Component
public class SimpleService extends AbsSimpleService {

    @Override
    public void say() {
        System.out.println("SimpleService say  " + true);
        ((SimpleService) AopContext.currentProxy()).hello();
    }
}
