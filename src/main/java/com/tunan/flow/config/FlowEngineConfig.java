package com.tunan.flow.config;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@ComponentScan(basePackages = {
        "com.tunan.flow.engine.component",
        "com.tunan.flow.engine.gateway"
})
public class FlowEngineConfig {
}
