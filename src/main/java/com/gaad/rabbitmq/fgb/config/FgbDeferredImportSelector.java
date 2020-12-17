package com.gaad.rabbitmq.fgb.config;

import com.gaad.rabbitmq.fgb.annotation.EnableFgb;
import com.gaad.rabbitmq.fgb.client.FgbClientScannerRegistrar;
import com.gaad.rabbitmq.fgb.model.enums.FgbMode;
import com.gaad.rabbitmq.fgb.server.FgbServerPostProcessor;
import org.springframework.context.annotation.DeferredImportSelector;
import org.springframework.core.annotation.Order;
import org.springframework.core.type.AnnotationMetadata;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 延迟扫描@FgbClient和@FgbServer注解类
 *
 * @author loken
 * @Date 2020/12/3
 */
@Order
public class FgbDeferredImportSelector implements DeferredImportSelector {
    @Override
    public String[] selectImports(AnnotationMetadata importingClassMetadata) {
        List<String> definitionRegistrars = new ArrayList<>();
        Map<String, Object> annotationAttributes = importingClassMetadata.getAnnotationAttributes(EnableFgb.class.getCanonicalName());
        if (annotationAttributes != null) {
            FgbMode[] fgbModes = (FgbMode[]) annotationAttributes.get("mode");
            for (FgbMode fgbMode : fgbModes) {
                switch (fgbMode) {
                    case FGB_CLIENT:
                        definitionRegistrars.add(FgbClientScannerRegistrar.class.getName());
                        break;
                    case FGB_SERVER:
                        definitionRegistrars.add(FgbServerPostProcessor.class.getName());
                        break;
                    default:
                        break;
                }
            }
        }
        return definitionRegistrars.toArray(new String[definitionRegistrars.size()]);
    }
}
