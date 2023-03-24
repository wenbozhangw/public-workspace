package com.example.demo.service;

import org.springframework.aop.framework.AopContext;

/**
 * @author ：wenbo.zhangw
 * @version :
 * @description ：
 * @date ：Created in 2021/2/23 1:33 下午
 * @modified By ：
 */
public abstract class AbsSimpleService {

    public void hello(){
        System.out.println("AbsSimpleService say hello");
        ((AbsSimpleService)AopContext.currentProxy()).hi();
    }

    public void hi(){
        System.out.println("AbsSimpleService say hi");
    }

    public abstract void say();
}
