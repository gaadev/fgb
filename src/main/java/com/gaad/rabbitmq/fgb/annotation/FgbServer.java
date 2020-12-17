package com.gaad.rabbitmq.fgb.annotation;

import com.gaad.rabbitmq.fgb.model.enums.FgbType;
import org.springframework.stereotype.Component;

import java.lang.annotation.*;

/**
 * 服务端类注解
 *
 * @author loken
 * @Date 2020/12/3
 */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Component
public @interface FgbServer {
    /**
     * value  相对于队列名称，需要与FgbClient保持一致才能正常通信
     *
     * @return
     */
    String value();

    /**
     * 一个消息推送到队列中的存活时间。设置的值之后还没消费就会被删除
     *
     * @return
     */
    int xMessageTTL() default 1000;

    /**
     * @return
     */
    int threadNum() default 1;

    /**
     * @return
     */
    FgbType[] type() default {FgbType.SYNC, FgbType.ASYNC};
}
