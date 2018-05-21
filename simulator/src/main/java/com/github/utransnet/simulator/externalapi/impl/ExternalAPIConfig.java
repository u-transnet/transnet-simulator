package com.github.utransnet.simulator.externalapi.impl;

import com.github.utransnet.simulator.externalapi.APIObjectFactory;
import com.github.utransnet.simulator.externalapi.ExternalAPI;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/**
 * Created by Artem on 02.02.2018.
 */
@Configuration
@Profile("graphenej")
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
