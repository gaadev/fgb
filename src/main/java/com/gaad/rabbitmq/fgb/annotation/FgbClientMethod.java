package com.gaad.rabbitmq.fgb.annotation;

import com.gaad.rabbitmq.fgb.model.enums.FgbType;

import org.springframework.context.annotation.Configuration;

import java.lang.annotation.*;

/**
 * 客户端方法注解
 *
 * @author loken
 * @Date 2020/12/3
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Configuration
public @interface FgbClientMethod {
    /**
     * command名称  需与FgbServerMethod保持一致才能正常调用
     *
     * @return
     */
    String value() default "";

    /**
     * 调用类型
     *
     * @return
     */
    FgbType type() default FgbType.SYNC;
}
