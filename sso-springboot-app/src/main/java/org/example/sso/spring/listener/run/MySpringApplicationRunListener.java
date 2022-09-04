package org.example.sso.spring.listener.run;

import org.slf4j.Logger;
import org.springframework.boot.ConfigurableBootstrapContext;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.SpringApplicationRunListener;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.ConfigurableEnvironment;

import java.time.Duration;

/**
 * @author wenbo.zhangw
 * @date 2022/5/12 22:34
 */
public class MySpringApplicationRunListener implements SpringApplicationRunListener {

    private static final Logger logger = org.slf4j.LoggerFactory.getLogger(MySpringApplicationRunListener.class);

    private SpringApplication application;

    private String[] args;

    public MySpringApplicationRunListener(SpringApplication springApplication, String[] args) {
        this.application = springApplication;
        this.args = args;
    }

    @Override
    public void starting(ConfigurableBootstrapContext bootstrapContext) {
        logger.info("application starting ...");
    }

    @Override
    public void environmentPrepared(ConfigurableBootstrapContext bootstrapContext, ConfigurableEnvironment environment) {
        logger.info("environment prepared ...");
    }

    @Override
    public void contextPrepared(ConfigurableApplicationContext context) {
        logger.info("context prepared ...");
    }

    @Override
    public void contextLoaded(ConfigurableApplicationContext context) {
        logger.info("context loaded ...");
    }

    @Override
    public void started(ConfigurableApplicationContext context, Duration timeTaken) {
        logger.info("application started ...");
    }

    @Override
    public void started(ConfigurableApplicationContext context) {
        logger.info("application started2 ...");
    }

    @Override
    public void ready(ConfigurableApplicationContext context, Duration timeTaken) {
        logger.info("application ready ...");
    }

    @Override
    public void running(ConfigurableApplicationContext context) {
        logger.info("application running ...");
    }

    @Override
    public void failed(ConfigurableApplicationContext context, Throwable exception) {
        logger.info("application failed ...");
    }
}
