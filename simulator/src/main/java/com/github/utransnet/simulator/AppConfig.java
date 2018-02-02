package com.github.utransnet.simulator;

import com.github.utransnet.simulator.actors.factory.ActorConfig;
import com.github.utransnet.simulator.actors.Client;
import com.github.utransnet.simulator.externalapi.ExternalAPIConfig;
import com.github.utransnet.simulator.queue.InputQueue;
import com.github.utransnet.simulator.queue.InputQueueImpl;
import com.github.utransnet.simulator.route.RouteMap;
import com.github.utransnet.simulator.services.Supervisor;
import com.github.utransnet.simulator.services.SupervisorImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Scope;

import java.util.concurrent.LinkedBlockingQueue;

/**
 * Created by Artem on 01.02.2018.
 */
@Configuration
@Import({
        ActorConfig.class,
        ExternalAPIConfig.class
})
public class AppConfig {


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
    @Autowired
    Supervisor supervisor(InputQueue<RouteMap> routeMapInputQueue, InputQueue<Client> clientInputQueue) {
        return new SupervisorImpl(routeMapInputQueue, clientInputQueue);
    }
}
