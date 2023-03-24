package org.example.sso.interceptor;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.View;
import org.springframework.web.servlet.view.AbstractCachingViewResolver;

import java.util.Locale;

/**
 * @author wenbo.zhangw
 * @date 2023/1/13 15:23
 */
@Component
@Slf4j
public class I18nViewResolver extends AbstractCachingViewResolver {
    @Override
    protected View loadView(String viewName, Locale locale) throws Exception {
        System.out.println("viewName = " + viewName);
        return null;
    }
}
