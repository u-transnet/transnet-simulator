package com.github.utransnet.simulator.externalapi;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Created by Artem on 02.02.2018.
 */
@Configuration
public class ExternalAPIConfig {
    @Bean
    APIObjectFactory apiObjectFactory(){
        return null;
    }

    @Bean
    ExternalAPI grapheneAPI() {
        return null;
    }
}
