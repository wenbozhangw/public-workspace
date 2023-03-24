package org.example.sso.listener;

import org.example.sso.event.BEvent;
import org.example.sso.service.impl.CycleRefA;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

/**
 * @author wenbo.zhangw
 * @date 2022/11/7 10:47
 */
@Component
public class BEventListener implements ApplicationListener<BEvent> {

    private final CycleRefA a;

    public BEventListener(CycleRefA a) {
        this.a = a;
    }

    @Override
    public void onApplicationEvent(BEvent event) {
        a.onEvent();
    }
}
