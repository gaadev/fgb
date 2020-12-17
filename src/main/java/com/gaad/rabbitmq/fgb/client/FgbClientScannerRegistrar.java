package com.gaad.rabbitmq.fgb.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.boot.autoconfigure.AutoConfigurationPackages;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * 自定义扫描，将带有@FgbClient注解的类 注册到spring的ioc中
 *
 * @author loken
 * @date 2020/12/3
 */
public class FgbClientScannerRegistrar implements BeanFactoryAware, ImportBeanDefinitionRegistrar {

    private static final Logger LOGGER = LoggerFactory.getLogger(FgbClientScannerRegistrar.class);

    private BeanFactory beanFactory;

    @Override
    public void registerBeanDefinitions(AnnotationMetadata importingClassMetadata, BeanDefinitionRegistry registry) {
        if (!AutoConfigurationPackages.has(this.beanFactory)) {
            LOGGER.debug("Could not determine auto-configuration package, automatic fgb-client scanning disabled.");
            return;
        }
        LOGGER.debug("Searching for fgb-client annotated with @FgbClient");
        List<String> packages = AutoConfigurationPackages.get(this.beanFactory);
        if (LOGGER.isDebugEnabled()) {
            packages.forEach(pkg -> LOGGER.debug("Using auto-configuration base package '{}'", pkg));
        }
        BeanDefinitionBuilder builder = BeanDefinitionBuilder.genericBeanDefinition(FgbClientScannerConfigurer.class);
        builder.addPropertyValue("basePackage", StringUtils.collectionToCommaDelimitedString(packages));
        registry.registerBeanDefinition(FgbClientScannerConfigurer.class.getName(), builder.getBeanDefinition());
    }

    @Override
    public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
        this.beanFactory = beanFactory;
    }
}
