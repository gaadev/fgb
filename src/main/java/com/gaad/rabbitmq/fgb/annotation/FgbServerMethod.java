package com.gaad.rabbitmq.fgb.annotation;

import java.lang.annotation.*;

/**
 * 服务端方法注解
 *
 * @author loken
 * @Date 2020/12/3
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface FgbServerMethod {
    /**
     * command名称，需与FgbClientMethod保持一致才能正常调用
     *
     * @return
     */
    String value() default "";
}
