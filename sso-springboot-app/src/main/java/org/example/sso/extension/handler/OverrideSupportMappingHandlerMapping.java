package org.example.sso.extension.handler;

import org.example.sso.extension.annotation.PrimaryMapping;
import org.springframework.context.EmbeddedValueResolverAware;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.core.annotation.Order;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Controller;
import org.springframework.util.Assert;
import org.springframework.util.StringValueResolver;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.handler.MatchableHandlerMapping;
import org.springframework.web.servlet.handler.RequestMatchResult;
import org.springframework.web.servlet.mvc.condition.RequestCondition;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.RequestMappingInfoHandlerMapping;
import org.springframework.web.util.UrlPathHelper;

import javax.servlet.http.HttpServletRequest;
import java.lang.reflect.Method;

@Component
public class OverrideSupportMappingHandlerMapping extends RequestMappingInfoHandlerMapping implements EmbeddedValueResolverAware {

    private RequestMappingInfo.BuilderConfiguration config = new RequestMappingInfo.BuilderConfiguration();

    @Nullable
    private StringValueResolver embeddedValueResolver;

    @Override
    protected boolean isHandler(Class<?> beanType) {
        return (AnnotatedElementUtils.hasAnnotation(beanType, Controller.class) ||
                AnnotatedElementUtils.hasAnnotation(beanType, RequestMapping.class));

    }

    @Override
    protected RequestMappingInfo getMappingForMethod(Method method, Class<?> handlerType) {
        RequestMappingInfo info = null;
        PrimaryMapping mapping = method.getAnnotation(PrimaryMapping.class);
        if (mapping != null) {
            info = createRequestMappingInfo(mapping);
        }
        return info;
    }

    private RequestMappingInfo createRequestMappingInfo(PrimaryMapping requestMapping) {
        RequestMappingInfo.Builder builder = RequestMappingInfo.paths(
                resolveEmbeddedValuesInPatterns(new String[]{requestMapping.value()}))
                .methods(requestMapping.method()).mappingName("");
        return builder.options(this.config).build();
    }

    @Override
    public void afterPropertiesSet() {
        // 优先处理获取当前 handlerMapping
        super.setOrder(0);
        super.afterPropertiesSet();
    }

    @Override
    public void setEmbeddedValueResolver(StringValueResolver resolver) {
        this.embeddedValueResolver = resolver;
    }

    protected String[] resolveEmbeddedValuesInPatterns(String[] patterns) {
        if (this.embeddedValueResolver == null) {
            return patterns;
        } else {
            String[] resolvedPatterns = new String[patterns.length];
            for (int i = 0; i < patterns.length; i++) {
                resolvedPatterns[i] = this.embeddedValueResolver.resolveStringValue(patterns[i]);
            }
            return resolvedPatterns;
        }
    }

    @Nullable
    protected RequestCondition<?> getCustomMethodCondition(Method method) {
        return null;
    }
}