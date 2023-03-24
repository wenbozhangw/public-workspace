package org.example.sso.event;

import org.springframework.context.ApplicationEvent;

/**
 * @author wenbo.zhangw
 * @date 2022/11/7 10:46
 */
public class AEvent extends ApplicationEvent {
    /**
     * Create a new {@code ApplicationEvent}.
     *
     * @param source the object on which the event initially occurred or with
     *               which the event is associated (never {@code null})
     */
    public AEvent(Object source) {
        super(source);
    }
}
