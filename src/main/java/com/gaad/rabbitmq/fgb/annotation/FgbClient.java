package com.gaad.rabbitmq.fgb.annotation;

import java.lang.annotation.*;

/**
 * 客户端类注解
 *
 * @author loken
 * @Date 2020/12/3
 */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface FgbClient {
    /**
     * value  相对于队列名称，需要与FgbServer保持一致才能正常通信
     *
     * @return
     */
    String value();

    /**
     * 等待回复超时时间
     *
     * @return
     */
    int replyTimeout() default 2000;

    /**
     * 最大重试次数
     *
     * @return
     */
    int maxAttempts() default 3;
}
