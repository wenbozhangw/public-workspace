package org.example.sso.service.impl;

import org.example.sso.event.AEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

/**
 * @author wenbo.zhangw
 * @date 2022/11/7 10:38
 */
@Component
public class CycleRefA {

    private final ApplicationEventPublisher publisher;

    public CycleRefA(ApplicationEventPublisher publisher) {
        this.publisher = publisher;
    }

    public void doSomething(){
        System.out.println("A publish event !");
        publisher.publishEvent(new AEvent("A"));
    }

    public void onEvent(){
        System.out.println("A on event !");
    }


    @PostConstruct
    public void init(){
        System.out.println("A initial success!");
    }
}
