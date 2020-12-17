package com.gaad.rabbitmq.fgb.client;

import com.gaad.rabbitmq.fgb.annotation.FgbClient;
import com.gaad.rabbitmq.fgb.model.enums.FgbType;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.retry.policy.SimpleRetryPolicy;
import org.springframework.retry.support.RetryTemplate;

import java.lang.reflect.Proxy;
import java.util.Collections;
import java.util.UUID;

/**
 * @author loken
 * @date 2020/12/3
 */
public class FgbClientProxyFactory implements FactoryBean, BeanFactoryAware {

    private BeanFactory beanFactory;
    private Class<?> fgbClientInterface;
    private ConnectionFactory connectionFactory;
    private DirectExchange syncReplyDirectExchange;

    public FgbClientProxyFactory(Class<?> fgbClientInterface) {
        this.fgbClientInterface = fgbClientInterface;
    }


    @Override
    public boolean isSingleton() {
        return true;
    }

    @Override
    public Class<?> getObjectType() {
        return this.fgbClientInterface;
    }

    @Override
    public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
        this.beanFactory = beanFactory;
    }

    @Override
    public Object getObject() throws Exception {
        RabbitTemplate sender;
        SimpleMessageListenerContainer replyMessageListenerContainer = null;
        FgbClient fgbClient = this.fgbClientInterface.getAnnotation(FgbClient.class);
        String fgbName = fgbClient.value();
        int replyTimeout = fgbClient.replyTimeout();
        int maxAttempts = fgbClient.maxAttempts();
        /**
         * 初始化同步队列
         */
        Queue replyQueue = replyQueue(fgbName, UUID.randomUUID().toString());
        replyBinding(fgbName, replyQueue);
        RabbitTemplate syncSender = syncSender(fgbName, replyQueue, replyTimeout, maxAttempts, getConnectionFactory());
        replyMessageListenerContainer = replyMessageListenerContainer(fgbName, replyQueue, syncSender, getConnectionFactory());
        /**
         * 初始化异步队列
         */
        RabbitTemplate asyncSender = asyncSender(fgbName, getConnectionFactory());
        return Proxy.newProxyInstance(this.fgbClientInterface.getClassLoader(), new Class[]{this.fgbClientInterface}, new FgbClientProxy(this.fgbClientInterface, fgbName, syncSender, asyncSender, replyMessageListenerContainer));
    }

    /**
     * 实例化答复队列
     *
     * @param fgbName        fgb名称
     * @param rabbitClientId rabbitmq客户端ID
     * @return
     */
    private Queue replyQueue(String fgbName, String rabbitClientId) {
        return registerBean(FgbType.SYNC.getName() + "-ReplyQueue-" + fgbName, Queue.class, fgbName + ".reply." + rabbitClientId, false, false, true);
    }

    /**
     * 实例化答复binding
     *
     * @param fgbName fgb名称
     * @param queue   答复队列
     */
    private void replyBinding(String fgbName, Queue queue) {
        registerBean(FgbType.SYNC.getName() + "-ReplyBinding-" + fgbName, Binding.class, queue.getName(), Binding.DestinationType.QUEUE, getSyncReplyDirectExchange().getName(), queue.getName(), Collections.<String, Object>emptyMap());
    }

    /**
     * 实例化答复队列的Exchange
     * 这里使用DirectExchange
     *
     * @return
     */
    private DirectExchange getSyncReplyDirectExchange() {
        if (this.syncReplyDirectExchange == null) {
            this.syncReplyDirectExchange = registerBean("syncReplyDirectExchange", DirectExchange.class, "simple.fgb.sync.reply", true, false);
        }
        return this.syncReplyDirectExchange;
    }

    /**
     * 实例化 ConnectionFactory
     */
    private ConnectionFactory getConnectionFactory() {
        if (this.connectionFactory == null) {
            this.connectionFactory = this.beanFactory.getBean(ConnectionFactory.class);
        }
        return this.connectionFactory;
    }

    /**
     * 实例化同步发送队列
     *
     * @param fgbName           fgb名称
     * @param replyQueue        答复队列
     * @param replyTimeout      答复超时时间
     * @param maxAttempts       最大尝试次数
     * @param connectionFactory rabbitmq连接工厂
     * @return
     */
    private RabbitTemplate syncSender(String fgbName, Queue replyQueue, int replyTimeout, int maxAttempts, ConnectionFactory connectionFactory) {
        SimpleRetryPolicy simpleRetryPolicy = new SimpleRetryPolicy();
        simpleRetryPolicy.setMaxAttempts(maxAttempts);
        RetryTemplate retryTemplate = new RetryTemplate();
        retryTemplate.setRetryPolicy(simpleRetryPolicy);
        RabbitTemplate syncSender = registerBean(FgbType.SYNC.getName() + "-Sender-" + fgbName, RabbitTemplate.class, connectionFactory);
        syncSender.setDefaultReceiveQueue(fgbName);
        syncSender.setRoutingKey(fgbName);
        syncSender.setReplyAddress(replyQueue.getName());
        syncSender.setReplyTimeout(replyTimeout);
        syncSender.setRetryTemplate(retryTemplate);
        return syncSender;
    }

    /**
     * 实现答复队列的监听
     *
     * @param fgbName           fgb名称
     * @param queue             答复队列
     * @param syncSender        发送者
     * @param connectionFactory rabbitmq连接工厂
     * @return
     */
    private SimpleMessageListenerContainer replyMessageListenerContainer(String fgbName, Queue queue, RabbitTemplate syncSender, ConnectionFactory connectionFactory) {
        SimpleMessageListenerContainer replyMessageListenerContainer = registerBean(FgbType.SYNC.getName() + "-ReplyMessageListenerContainer-" + fgbName, SimpleMessageListenerContainer.class, connectionFactory);
        replyMessageListenerContainer.setQueueNames(queue.getName());
        replyMessageListenerContainer.setMessageListener(syncSender);
        return replyMessageListenerContainer;
    }

    /**
     * 实例化异步发送者的RabbitTemplate
     * 为保证消息的百分百投递，可以在此处加上 RabbitTemplate.ConfirmCallback 处理和  RabbitTemplate.ReturnCallback 处理
     * todo 给出默认的实现
     *
     * @param fgbName           fgb名称
     * @param connectionFactory rabbitmq连接工厂
     * @return
     */
    private RabbitTemplate asyncSender(String fgbName, ConnectionFactory connectionFactory) {
        RabbitTemplate asyncSender = registerBean(FgbType.ASYNC.getName() + "-Sender-" + fgbName, RabbitTemplate.class, connectionFactory);
        asyncSender.setDefaultReceiveQueue(fgbName + ".async");
        asyncSender.setRoutingKey(fgbName + ".async");
        return asyncSender;
    }

    /**
     * 对象实例化并注册到Spring上下文
     */
    private <T> T registerBean(String name, Class<T> clazz, Object... args) {
        BeanDefinitionBuilder beanDefinitionBuilder = BeanDefinitionBuilder.genericBeanDefinition(clazz);
        if (args != null && args.length > 0) {
            for (Object arg : args) {
                beanDefinitionBuilder.addConstructorArgValue(arg);
            }
        }
        BeanDefinition beanDefinition = beanDefinitionBuilder.getRawBeanDefinition();
        BeanDefinitionRegistry beanDefinitionRegistry = (BeanDefinitionRegistry) this.beanFactory;
        if (beanDefinitionRegistry.isBeanNameInUse(name)) {
            //如果存在重复的则覆盖
            return this.beanFactory.getBean(name, clazz);
        }
        beanDefinitionRegistry.registerBeanDefinition(name, beanDefinition);
        return this.beanFactory.getBean(name, clazz);
    }
}
