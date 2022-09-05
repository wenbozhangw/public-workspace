package org.example.sso.annotation;

import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.lang.annotation.*;

/**
 * @author wenbo.zhang
 * @date 2022/8/4 15:30
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Component
public @interface OverrideBean {

    String name();
}
