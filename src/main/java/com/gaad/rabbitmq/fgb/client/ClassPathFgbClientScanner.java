package com.gaad.rabbitmq.fgb.client;

import java.util.Arrays;
import java.util.Set;

import com.gaad.rabbitmq.fgb.annotation.FgbClient;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.AnnotatedBeanDefinition;
import org.springframework.beans.factory.config.BeanDefinitionHolder;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.GenericBeanDefinition;
import org.springframework.context.annotation.ClassPathBeanDefinitionScanner;
import org.springframework.core.type.filter.AnnotationTypeFilter;

/**
 * FgbClient扫描分析
 *
 * @author loken
 * @date 2020/12/3
 */
public class ClassPathFgbClientScanner extends ClassPathBeanDefinitionScanner {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClassPathFgbClientScanner.class);

    ClassPathFgbClientScanner(BeanDefinitionRegistry registry) {
        super(registry, false);
    }

    void registerFilters() {
        addIncludeFilter(new AnnotationTypeFilter(FgbClient.class));
    }

    @Override
    protected boolean isCandidateComponent(AnnotatedBeanDefinition beanDefinition) {
        return beanDefinition.getMetadata().isInterface() && beanDefinition.getMetadata().isIndependent();
    }


    @Override
    public Set<BeanDefinitionHolder> doScan(String... basePackages) {
        Set<BeanDefinitionHolder> beanDefinitions = super.doScan(basePackages);
        if (beanDefinitions.isEmpty()) {
            LOGGER.warn("No @FgbClient was found in '" + Arrays.toString(basePackages)
                    + "' package. Please check your configuration.");
        } else {
            processBeanDefinitions(beanDefinitions);
        }
        return beanDefinitions;
    }

    private void processBeanDefinitions(Set<BeanDefinitionHolder> beanDefinitions) {
        GenericBeanDefinition fgbClientBeanDefinition;
        for (BeanDefinitionHolder holder : beanDefinitions) {
            fgbClientBeanDefinition = (GenericBeanDefinition) holder.getBeanDefinition();
            String beanClassName = fgbClientBeanDefinition.getBeanClassName();
            // 获取真实接口class，并作为构造方法的参数
            fgbClientBeanDefinition.getConstructorArgumentValues().addGenericArgumentValue(beanClassName);
            // 修改类为 FgbClientProxyFactory
            fgbClientBeanDefinition.setBeanClass(FgbClientProxyFactory.class);
            // 采用按照类型注入的方式
            fgbClientBeanDefinition.setAutowireMode(AbstractBeanDefinition.AUTOWIRE_BY_TYPE);
        }
    }
}
