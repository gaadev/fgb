package com.gaad.rabbitmq.fgb.client;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;

import com.alibaba.fastjson.JSONObject;
import com.gaad.rabbitmq.fgb.annotation.FgbClientMethod;
import com.gaad.rabbitmq.fgb.model.enums.FgbType;
import com.gaad.rabbitmq.fgb.util.ParamsConverterUtil;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;
import org.springframework.util.StringUtils;

/**
 * fgbclient代理 实现具体的调用 todo
 *
 * @author loken
 * @date 2020/12/3
 */
public class FgbClientProxy implements InvocationHandler {

    private final static Logger LOGGER = LoggerFactory.getLogger(FgbClientProxy.class);

    private final Class<?> fgbClientInterface;
    private final String fgbName;
    private final RabbitTemplate syncSender;
    private final RabbitTemplate asyncSender;
    private final SimpleMessageListenerContainer messageListenerContainer;

    FgbClientProxy(Class<?> fgbClientInterface, String fgbName, RabbitTemplate syncSender, RabbitTemplate asyncSender,
            SimpleMessageListenerContainer messageListenerContainer) {
        this.fgbClientInterface = fgbClientInterface;
        this.fgbName = fgbName;
        this.syncSender = syncSender;
        this.asyncSender = asyncSender;
        this.messageListenerContainer = messageListenerContainer;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        // 获取方法注解
        FgbClientMethod fgbClientMethod = method.getAnnotation(FgbClientMethod.class);
        if (fgbClientMethod == null) {
            return method.invoke(this, args);
        }
        FgbType methodFgbType = fgbClientMethod.type();
        String methodName = fgbClientMethod.value();
        if (StringUtils.isEmpty(methodName)) {
            methodName = method.getName();
        }
        // 未初始化完成
        if (methodFgbType == FgbType.SYNC && !this.messageListenerContainer.isRunning()) {
            LOGGER.warn("内部fgb，监听器没启动");
        }
        if (methodFgbType == FgbType.ASYNC && method.getReturnType() != void.class) {
            LOGGER.error("ASYNC-FgbClient 返回类型只能为 void, Class: " + this.fgbClientInterface.getName() + ", Method: "
                    + method.getName());
            throw new RuntimeException("ASYNC-FgbClient 返回类型只能为 void, Class: " + this.fgbClientInterface.getName()
                    + ", Method: " + method.getName());
        }
        JSONObject data = new JSONObject();
        if (args != null) {
            for (int i = 0; i < args.length; i++) {
                data.put(String.valueOf(i), args[i]);
            }
        }
        // 调用参数
        JSONObject paramData = new JSONObject();
        paramData.put("command", methodName);
        paramData.put("data", data);
        String paramDataJsonString = paramData.toJSONString();
        try {
            // 异步处理
            if (methodFgbType == FgbType.ASYNC) {
                CorrelationData correlationData = new CorrelationData();
                correlationData.setId(paramDataJsonString);
                asyncSender.correlationConvertAndSend(paramDataJsonString, correlationData);
                LOGGER.debug(methodFgbType.getName() + "-FgbClient-" + this.fgbName + ", Method: " + methodName
                        + " Call Success, Param: " + paramDataJsonString);
                return null;
            }
            // 发起请求并返回结果
            long start = System.currentTimeMillis();
            Object resultJsonStr = syncSender.convertSendAndReceive(paramDataJsonString);
            if (resultJsonStr == null) {
                // 无返回任何结果，说明服务器负载过高，没有及时处理请求，导致超时
                LOGGER.error("Duration: " + (System.currentTimeMillis() - start) + "ms, " + methodFgbType.getName()
                        + "-FgbClient-" + this.fgbName + ", Method: " + methodName + " Service Unavailable, Param: "
                        + paramDataJsonString);
                throw new RuntimeException("请求超时");
            }
            // 获取调用结果的状态
            JSONObject resultJson = JSONObject.parseObject(resultJsonStr.toString());
            JSONObject _data = resultJson.getJSONObject("_data");
            return ParamsConverterUtil.convertReturnType(method, _data);
        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
            e.printStackTrace();
            return null;
        }
    }
}
