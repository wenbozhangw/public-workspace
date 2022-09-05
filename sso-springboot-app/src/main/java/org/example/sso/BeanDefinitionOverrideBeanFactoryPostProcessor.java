package org.example.sso;

import org.example.sso.annotation.OverrideBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.ResolvableType;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.util.CollectionUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * @author wenbo.zhang
 * @date 2022/8/4 15:25
 */
@Configuration
public class BeanDefinitionOverrideBeanFactoryPostProcessor implements BeanFactoryPostProcessor {

    private static final Logger logger = LoggerFactory.getLogger(BeanDefinitionOverrideBeanFactoryPostProcessor.class);

    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
        Map<String, String> beanDefinitionMapping = new HashMap<>(16);
        String[] beanDefinitionNames = beanFactory.getBeanDefinitionNames();
        for (String beanDefinitionName : beanDefinitionNames) {
            BeanDefinition beanDefinition = beanFactory.getBeanDefinition(beanDefinitionName);
            Class<?> rawClass = beanDefinition.getResolvableType().getRawClass();
            OverrideBean annotation = Optional.of(beanDefinition.getResolvableType())
                    .map(ResolvableType::getRawClass)
                    .map(clazz -> AnnotationUtils.findAnnotation(clazz, OverrideBean.class))
                    .orElse(null);
            if (beanDefinition.getResolvableType().getRawClass() == null) {
                logger.warn("{} raw class is null!", beanDefinitionName);
            }
            if (annotation != null) {
                String overrideBeanName = annotation.name();
                beanDefinitionMapping.put(beanDefinitionName, overrideBeanName);
            }
        }
        // if not empty, override bean definition
        if (!CollectionUtils.isEmpty(beanDefinitionMapping)) {
            BeanDefinitionRegistry bdr = (BeanDefinitionRegistry) beanFactory;
            beanDefinitionMapping.forEach((beanName, overrideBeanName) -> {
                bdr.removeBeanDefinition(overrideBeanName);
                bdr.registerAlias(beanName, overrideBeanName);
                logger.info("[{}] Bean Definition override, [{}] registry alias bean definition name ", overrideBeanName, beanName);
            });
        }
    }

}
