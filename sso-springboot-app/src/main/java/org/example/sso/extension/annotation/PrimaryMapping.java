package org.example.sso.extension.annotation;

import org.springframework.web.bind.annotation.RequestMethod;

import java.lang.annotation.*;

/**
 * @author wenbo.zhang
 * @date 2022/7/25 10:46
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.ANNOTATION_TYPE, ElementType.METHOD})
public @interface PrimaryMapping {

    String value();

    RequestMethod[] method() default {};
}
