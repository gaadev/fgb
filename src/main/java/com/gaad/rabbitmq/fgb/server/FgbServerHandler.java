package com.gaad.rabbitmq.fgb.server;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONException;
import com.alibaba.fastjson.JSONObject;
import com.gaad.rabbitmq.fgb.annotation.FgbServerMethod;
import com.gaad.rabbitmq.fgb.model.enums.FgbType;
import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import net.sf.cglib.reflect.FastClass;
import net.sf.cglib.reflect.FastMethod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.listener.api.ChannelAwareMessageListener;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.util.StringUtils;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * fgbServer调用处理
 *
 * @author loken
 * @date 2020/12/3
 */
public class FgbServerHandler implements ChannelAwareMessageListener, InitializingBean {

    private final static Logger LOGGER = LoggerFactory.getLogger(FgbServerHandler.class);

    private final static Map<String, FastMethod> FAST_METHOD_MAP = new ConcurrentHashMap<>();

    @Value("${spring.rabbitmq.slow-call-time:1000}")
    private int slowCallTime;

    private final Object fgbServerBean;

    private final String fgbName;

    private final FgbType fgbType;

    FgbServerHandler(Object fgbServerBean, String fgbName, FgbType fgbType) {
        this.fgbServerBean = fgbServerBean;
        this.fgbName = fgbName;
        this.fgbType = fgbType;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        // 初始化所有接口
        Class<?> fgbServerClass = this.fgbServerBean.getClass();
        if (this.fgbServerBean.getClass().getName().contains("CGLIB")) {
            fgbServerClass = this.fgbServerBean.getClass().getSuperclass();
        }
        for (Method targetMethod : fgbServerClass.getMethods()) {
            if (targetMethod != null && targetMethod.isAnnotationPresent(FgbServerMethod.class)) {
                String methodName = targetMethod.getAnnotation(FgbServerMethod.class).value();
                if (StringUtils.isEmpty(methodName)) {
                    methodName = targetMethod.getName();
                }
                String key = this.fgbType.getName() + "_" + this.fgbName + "_" + methodName;
                if (FAST_METHOD_MAP.containsKey(key)) {
                    throw new RuntimeException("Class: " + fgbServerClass.getName() + ", Method: " + methodName + " 重复");
                }
                FastMethod fastMethod = FastClass.create(fgbServerClass).getMethod(targetMethod.getName(), targetMethod.getParameterTypes());
                if (fastMethod == null) {
                    throw new RuntimeException("Class: " + fgbServerClass.getName() + ", Method: " + targetMethod.getName() + " Invoke Exception");
                }
                FAST_METHOD_MAP.put(key, fastMethod);
                LOGGER.debug(this.fgbType.getName() + "-FgbServer-" + this.fgbName + ", Method: " + methodName + " 已启动");
            }
        }
        LOGGER.info(this.fgbType.getName() + "-FgbServerHandler-" + this.fgbName + " 已启动");
    }

    @Override
    public void onMessage(Message message, Channel channel) throws Exception {
        MessageProperties messageProperties = null;
        String messageStr = null;
        try {
            messageProperties = message.getMessageProperties();
            messageStr = new String(message.getBody(), StandardCharsets.UTF_8);
            // 构建返回JSON值
            JSONObject resultJson = new JSONObject();
            try {
                // 组装参数json
                JSONObject paramData = JSON.parseObject(messageStr);
                // 获得当前command
                String command = paramData.getString("command");
                if (StringUtils.isEmpty(command)) {
                    LOGGER.error("Method Invoke Exception: Command 参数为空, " + this.fgbType.getName() + "-FgbServer-" + this.fgbName + ", Received: " + messageStr);
                    return;
                }
                // 获取data数据
                JSONObject data = paramData.getJSONObject("data");
                if (data == null) {
                    LOGGER.error("Method Invoke Exception: Data 参数错误, " + this.fgbType.getName() + "-FgbServer-" + this.fgbName + ", Method: " + command + ", Received: " + messageStr);
                    return;
                }
                // 异步执行任务
                if (FgbType.ASYNC == this.fgbType) {
                    long start = System.currentTimeMillis();
                    asyncExecute(command, data);
                    double offset = System.currentTimeMillis() - start;
                    LOGGER.info("Duration: " + offset + "ms, " + this.fgbType.getName() + "-FgbServer-" + this.fgbName + ", Method: " + command + ", Received: " + messageStr);
                    if (offset > this.slowCallTime) {
                        LOGGER.warn("Duration: " + offset + "ms, " + this.fgbType.getName() + "-FgbServer-" + this.fgbName + ", Method: " + command + ", Slower Called, Received: " + messageStr);
                    }
                    return;
                }
                // 同步执行任务并返回结果
                long start = System.currentTimeMillis();
                Object result = syncExecute(command, data);
                if (result != null) {
                    double offset = System.currentTimeMillis() - start;
                    LOGGER.info("Duration: " + offset + "ms, " + this.fgbType.getName() + "-FgbServer-" + this.fgbName + ", Method: " + command + ", Received: " + messageStr);
                    if (offset > this.slowCallTime) {
                        LOGGER.warn("Duration: " + offset + "ms, " + this.fgbType.getName() + "-FgbServer-" + this.fgbName + ", Method: " + command + ", Call Slowing");
                    }
                    resultJson.put("_data", result);
                }
            } catch (InvocationTargetException e) {
                LOGGER.error("Method Invoke Target Exception! Received: " + messageStr);
                e.printStackTrace();
            } catch (Exception e) {
                LOGGER.error("Method Invoke Exception! Received: " + messageStr);
                e.printStackTrace();
            }
            // 构建配置
            AMQP.BasicProperties replyProps = new AMQP.BasicProperties.Builder().correlationId(messageProperties.getCorrelationId()).contentEncoding(StandardCharsets.UTF_8.name()).contentType(messageProperties.getContentType()).build();
            // 反馈消息
            channel.basicPublish(messageProperties.getReplyToAddress().getExchangeName(), messageProperties.getReplyToAddress().getRoutingKey(), replyProps, resultJson.toJSONString().getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            LOGGER.error(this.fgbType.getName() + "-FgbServer-" + this.fgbName + " Exception! Received: " + messageStr);
            e.printStackTrace();
        } finally {
            // 确认处理任务
            if (messageProperties != null) {
                channel.basicAck(messageProperties.getDeliveryTag(), false);
            }
        }
    }

    /**
     * 同步调用
     *
     * @param command 既fgbservermethod注解的Value值
     * @param data    参数
     * @throws InvocationTargetException
     */
    private void asyncExecute(String command, JSONObject data) throws InvocationTargetException {
        // 获取当前服务的反射方法调用
        String key = this.fgbType.getName() + "_" + this.fgbName + "_" + command;
        // 通过缓存来优化性能
        FastMethod fastMethod = FAST_METHOD_MAP.get(key);
        if (fastMethod == null) {
            LOGGER.error(this.fgbType.getName() + "-FgbServer-" + this.fgbName + ", Method: " + command + " Not Found");
            return;
        }
        List args = convertParamsTypes(command, data);
        // 通过发射来调用方法
        fastMethod.invoke(this.fgbServerBean, args.toArray());
    }

    /**
     * 参数转换
     *
     * @param command 既fgbservermethod注解的Value值
     * @param data    调用参数
     * @return
     */
    @SuppressWarnings("all")
    private List convertParamsTypes(String command, JSONObject data) {
        List args = new ArrayList();
        Class<?> targetClazz = fgbServerBean.getClass();
        if (fgbServerBean.getClass().getName().contains("CGLIB")) {
            targetClazz = fgbServerBean.getClass().getSuperclass();
        }
        for (Method targetMethod : targetClazz.getMethods()) {
            if (targetMethod.getName().equals(command)) {
                Parameter[] parameters = targetMethod.getParameters();
                for (int j = 0; j < parameters.length; j++) {
                    String param = data.getString(String.valueOf(j));
                    Type type = parameters[j].getParameterizedType();
                    if (!StringUtils.isEmpty(param)) {
                        try {
                            args.add(JSON.parseObject(param, type));
                        } catch (JSONException e) {
                            args.add(JSON.parseObject(JSON.toJSONString(param), type));
                        }
                    } else {
                        args.add(param);
                    }
                }
            }
        }
        return args;
    }

    /**
     * 异步调用
     *
     * @param command 既fgbservermethod注解的Value值
     * @param data    调用参数
     * @return
     * @throws InvocationTargetException
     * @throws IllegalAccessException
     * @throws InstantiationException
     */
    private Object syncExecute(String command, JSONObject data) throws InvocationTargetException, IllegalAccessException, InstantiationException {
        // 获取当前服务的反射方法调用
        String key = this.fgbType.getName() + "_" + this.fgbName + "_" + command;
        // 通过缓存来优化性能
        FastMethod fastMethod = FAST_METHOD_MAP.get(key);
        if (fastMethod == null) {
            LOGGER.error(this.fgbType.getName() + "-FgbServer-" + this.fgbName + ", Method: " + command + " Not Found");
            return null;
        }
        List args = convertParamsTypes(command, data);
        // 通过反射来调用方法
        return fastMethod.invoke(this.fgbServerBean, args.toArray());
    }

}
