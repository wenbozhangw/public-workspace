package org.example.sso.listener;

import org.example.sso.event.AEvent;
import org.example.sso.service.impl.CycleRefB;
import org.springframework.context.ApplicationListener;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * @author wenbo.zhangw
 * @date 2022/11/7 17:52
 */
@Component
@Order(value = 1)
public class AEventListener2 implements ApplicationListener<AEvent> {

    private final CycleRefB b;

    public AEventListener2(CycleRefB b) {
        this.b = b;
    }

    @Override
    public void onApplicationEvent(AEvent event) {
        System.out.println("2 on event = " + event);
    }
}
