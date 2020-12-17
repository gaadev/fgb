package com.gaad.rabbitmq.fgb.autoconfiguration;

import com.gaad.rabbitmq.fgb.config.FgbDeferredImportSelector;

import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.Import;

/**
 * FgbAutoConfiguration
 */
@EnableAutoConfiguration
@Import(FgbDeferredImportSelector.class)
public class FgbAutoConfiguration {

    
}
