package com.gaad.rabbitmq.fgb.client;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.util.StringUtils;

/**
 * @author loken
 * @date 2020/12/3
 */
public class FgbClientScannerConfigurer implements BeanDefinitionRegistryPostProcessor, ApplicationContextAware {

    private String basePackage;

    private ApplicationContext applicationContext;

    @Override
    public void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry registry) throws BeansException {
        ClassPathFgbClientScanner scanner = new ClassPathFgbClientScanner(registry);
        scanner.setResourceLoader(this.applicationContext);
        scanner.registerFilters();
        scanner.scan(StringUtils.tokenizeToStringArray(this.basePackage, ConfigurableApplicationContext.CONFIG_LOCATION_DELIMITERS));
    }

    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {

    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    public void setBasePackage(String basePackage) {
        this.basePackage = basePackage;
    }
}
