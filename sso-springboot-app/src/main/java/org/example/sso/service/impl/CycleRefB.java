package org.example.sso.service.impl;

import org.example.sso.event.BEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

/**
 * @author wenbo.zhangw
 * @date 2022/11/7 10:38
 */
@Component
public class CycleRefB {

    private final ApplicationEventPublisher publisher;


    public CycleRefB(ApplicationEventPublisher publisher) {
        this.publisher = publisher;
    }

    public void doSomething(){
        System.out.println("B publish event !");
        publisher.publishEvent(new BEvent("B"));
    }

    public void onEvent(){
        System.out.println("B on event !");
    }

    @PostConstruct
    public void init(){
        System.out.println("B initial success!");
    }
}
