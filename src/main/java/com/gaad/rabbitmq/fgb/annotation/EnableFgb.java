package com.gaad.rabbitmq.fgb.annotation;

import com.gaad.rabbitmq.fgb.config.FgbDeferredImportSelector;
import com.gaad.rabbitmq.fgb.model.enums.FgbMode;
import org.springframework.context.annotation.Import;

import java.lang.annotation.*;

/**
 * 是否启用fgb
 *
 * @author loken
 * @Date 2020/12/3
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Import(FgbDeferredImportSelector.class)
public @interface EnableFgb {
    /**
     * 模式
     *
     * @return
     */
    FgbMode[] mode() default {FgbMode.FGB_CLIENT, FgbMode.FGB_SERVER};


}
