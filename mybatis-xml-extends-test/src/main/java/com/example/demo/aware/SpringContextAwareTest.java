package com.example.demo.aware;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import javax.annotation.PostConstruct;

/**
 * @author ：wenbo.zhangw
 * @date ：Created in 2021/7/6 10:07 上午
 */
public class SpringContextAwareTest implements ApplicationContextAware {

    private static final Logger logger = LoggerFactory.getLogger(SpringContextAwareTest.class);

    private ApplicationContext applicationContext;

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    @PostConstruct
    public void init() {
        logger.info("{} init success, !", this.getClass().getSimpleName());
    }
}
