package com.gaad.rabbitmq.fgb.server;

import com.gaad.rabbitmq.fgb.annotation.FgbServer;
import com.gaad.rabbitmq.fgb.model.enums.FgbType;
import org.springframework.amqp.core.AcknowledgeMode;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.lang.annotation.Annotation;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * 后置处理器  处理FgbServer注解
 *
 * @author loken
 * @date 2020/12/3
 */
@Component
public class FgbServerPostProcessor implements BeanPostProcessor {

    @Autowired
    private ConfigurableApplicationContext applicationContext;
    @Autowired
    @Lazy
    private ConnectionFactory connectionFactory;
    private DirectExchange syncDirectExchange;
    private DirectExchange asyncDirectExchange;

    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
        return bean;
    }

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        Class<?> fgbServerClass = bean.getClass();
        if (fgbServerClass.getAnnotations() != null && fgbServerClass.getAnnotations().length > 0) {
            for (Annotation annotation : fgbServerClass.getAnnotations()) {
                if (annotation instanceof FgbServer) {
                    fgbServerStart(bean, (FgbServer) annotation);
                }
            }
        } else {
            if (fgbServerClass.getName().contains("CGLIB")) {//cglib代理时，取其父类的注解
                Annotation[] annotations = fgbServerClass.getSuperclass().getAnnotations();
                for (Annotation annotation : annotations) {
                    if (annotation instanceof FgbServer) {
                        fgbServerStart(bean, (FgbServer) annotation);
                    }
                }
            }
        }
        return bean;
    }

    /**
     * 启动服务监听
     *
     * @param fgbServerBean
     * @param fgbServer
     */
    private void fgbServerStart(Object fgbServerBean, FgbServer fgbServer) {
        String fgbName = fgbServer.value();
        for (FgbType fgbType : fgbServer.type()) {
            switch (fgbType) {
                case SYNC:
                    Map<String, Object> params = new HashMap<>(1);
                    params.put("x-message-ttl", fgbServer.xMessageTTL());
                    Queue syncQueue = queue(fgbName, fgbType, params);
                    binding(fgbName, fgbType, syncQueue);
                    FgbServerHandler syncServerHandler = fgbServerHandler(fgbName, fgbType, fgbServerBean);
                    messageListenerContainer(fgbName, fgbType, syncQueue, syncServerHandler, fgbServer.threadNum());
                    break;
                case ASYNC:
                    Queue asyncQueue = queue(fgbName, fgbType, null);
                    binding(fgbName, fgbType, asyncQueue);
                    FgbServerHandler asyncServerHandler = fgbServerHandler(fgbName, fgbType, fgbServerBean);
                    messageListenerContainer(fgbName, fgbType, asyncQueue, asyncServerHandler, fgbServer.threadNum());
                    break;
                default:
                    break;
            }
        }
    }

    /**
     * 实例化队列
     *
     * @param fgbName
     * @param fgbType 类型
     * @param params  参数
     * @return
     */
    private Queue queue(String fgbName, FgbType fgbType, Map<String, Object> params) {
        return registerBean(this.applicationContext, fgbType.getName() + "-Queue-" + fgbName, Queue.class, fgbType == FgbType.ASYNC ? (fgbName + ".async") : fgbName, true, false, false, params);
    }

    /**
     * 实例化binding
     *
     * @param fgbName fgb名称
     * @param fgbType fgb调用类型
     * @param queue   队列
     */
    private void binding(String fgbName, FgbType fgbType, Queue queue) {
        registerBean(this.applicationContext, fgbType.getName() + "-Binding-" + fgbName, Binding.class, queue.getName(), Binding.DestinationType.QUEUE, getDirectExchange(fgbType).getName(), queue.getName(), Collections.<String, Object>emptyMap());
    }

    /**
     * 化实例化 FgbServerHandler
     *
     * @param fgbName       fgb名称
     * @param fgbType       fgb调用类型
     * @param fgbServerBean fgbServerBean
     * @return
     */
    private FgbServerHandler fgbServerHandler(String fgbName, FgbType fgbType, Object fgbServerBean) {
        return registerBean(this.applicationContext, fgbType.getName() + "-FgbServerHandler-" + fgbName, FgbServerHandler.class, fgbServerBean, fgbName, fgbType);
    }

    /**
     * 实例化 SimpleMessageListenerContainer
     *
     * @param fgbName
     * @param fgbType
     * @param queue
     * @param fgbServerHandler
     * @param threadNum
     */
    private void messageListenerContainer(String fgbName, FgbType fgbType, Queue queue, FgbServerHandler fgbServerHandler, int threadNum) {
        SimpleMessageListenerContainer messageListenerContainer = registerBean(this.applicationContext, fgbType.getName() + "-MessageListenerContainer-" + fgbName, SimpleMessageListenerContainer.class, this.connectionFactory);
        messageListenerContainer.setQueueNames(queue.getName());
        messageListenerContainer.setMessageListener(fgbServerHandler);
        //开启ack
        messageListenerContainer.setAcknowledgeMode(AcknowledgeMode.MANUAL);
        messageListenerContainer.setConcurrentConsumers(threadNum);
    }

    /**
     * 初始化exchage 此处实现为DirectExchage
     *
     * @param fgbType
     * @return
     */
    private DirectExchange getDirectExchange(FgbType fgbType) {
        if (fgbType == FgbType.SYNC) {
            if (this.syncDirectExchange == null) {
                this.syncDirectExchange = registerBean(this.applicationContext, "syncDirectExchange", DirectExchange.class, "simple.fgb.sync", true, false);
            }
            return this.syncDirectExchange;
        }
        if (this.asyncDirectExchange == null) {
            this.asyncDirectExchange = registerBean(this.applicationContext, "asyncDirectExchange", DirectExchange.class, "simple.fgb.async", true, false);
        }
        return this.asyncDirectExchange;
    }

    private <T> T registerBean(ConfigurableApplicationContext applicationContext, String name, Class<T> clazz, Object... args) {
        BeanDefinitionBuilder beanDefinitionBuilder = BeanDefinitionBuilder.genericBeanDefinition(clazz);
        if (args.length > 0) {
            for (Object arg : args) {
                beanDefinitionBuilder.addConstructorArgValue(arg);
            }
        }
        BeanDefinition beanDefinition = beanDefinitionBuilder.getRawBeanDefinition();
        BeanDefinitionRegistry beanFactory = (BeanDefinitionRegistry) (applicationContext).getBeanFactory();
        if (beanFactory.isBeanNameInUse(name)) {
            throw new RuntimeException("BeanName: " + name + " 重复");
        }
        beanFactory.registerBeanDefinition(name, beanDefinition);
        return applicationContext.getBean(name, clazz);
    }
}
