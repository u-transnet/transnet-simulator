package com.github.utransnet.simulator;

import com.github.utransnet.simulator.actors.Client;
import com.github.utransnet.simulator.externalapi.DefaultAssets;
import com.github.utransnet.simulator.queue.InputQueue;
import com.github.utransnet.simulator.queue.InputQueueImpl;
import com.github.utransnet.simulator.route.RouteMap;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;

import java.util.concurrent.LinkedBlockingQueue;

/**
 * Created by Artem on 09.02.2018.
 */
@Configuration
public class SpringTestConfig {
    @Bean
    @Scope("singleton")
    InputQueue<Client> clientInputQueue() {
        return new InputQueueImpl<>(new LinkedBlockingQueue<>(100));
    }

    @Bean
    @Scope("singleton")
    InputQueue<RouteMap> routeMapInputQueue() {
        return new InputQueueImpl<>(new LinkedBlockingQueue<>(100));
    }

    @Bean
    @Scope("singleton")
    DefaultAssets defaultAssets() {
        return new DefaultAssets();
    }


}
