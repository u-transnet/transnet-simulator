package com.github.utransnet.simulator;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.github.utransnet.simulator.actors.Client;
import com.github.utransnet.simulator.actors.factory.ActorConfig;
import com.github.utransnet.simulator.actors.factory.ActorFactory;
import com.github.utransnet.simulator.externalapi.APIObjectFactory;
import com.github.utransnet.simulator.externalapi.AssetAmount;
import com.github.utransnet.simulator.externalapi.DefaultAssets;
import com.github.utransnet.simulator.externalapi.ExternalAPI;
import com.github.utransnet.simulator.externalapi.graphenej.ExternalAPIGrapheneConfig;
import com.github.utransnet.simulator.externalapi.h2impl.ExternalAPIH2ImplConfig;
import com.github.utransnet.simulator.logging.LoggingConfig;
import com.github.utransnet.simulator.logging.PositionMonitoring;
import com.github.utransnet.simulator.queue.InputQueue;
import com.github.utransnet.simulator.queue.InputQueueImpl;
import com.github.utransnet.simulator.route.AssetAmountDeserializer;
import com.github.utransnet.simulator.route.AssetAmountSerializer;
import com.github.utransnet.simulator.route.RouteMap;
import com.github.utransnet.simulator.route.RouteMapFactory;
import com.github.utransnet.simulator.services.Supervisor;
import com.github.utransnet.simulator.services.SupervisorImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
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
        ExternalAPIH2ImplConfig.class,
        ExternalAPIGrapheneConfig.class,
        LoggingConfig.class
//        ExternalAPIConfig.class
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
    PositionMonitoring positionMonitoring(ExternalAPI externalAPI, DefaultAssets defaultAssets) {
        return new PositionMonitoring(externalAPI, defaultAssets);
    }


    @Bean
    @Scope("singleton")
    DefaultAssets defaultAssets() {
        return new DefaultAssets();
    }


    @Bean
    @Scope("singleton")
    @Autowired
    Supervisor supervisor(
            InputQueue<RouteMap> routeMapInputQueue,
            InputQueue<Client> clientInputQueue,
            ActorFactory actorFactory,
            ExternalAPI externalAPI,
            APIObjectFactory apiObjectFactory,
            PositionMonitoring positionMonitoring,
            DefaultAssets defaultAssets
    ) {
        return new SupervisorImpl(
                routeMapInputQueue,
                clientInputQueue,
                actorFactory,
                externalAPI,
                apiObjectFactory,
                positionMonitoring,
                defaultAssets
        );
    }

    @Bean
    @Scope("prototype")
    @Autowired
    RouteMap routeMap(ExternalAPI externalAPI) {
        return new RouteMap(externalAPI);
    }

    @Bean
    @Scope("prototype")
    AssetAmountSerializer assetAmountSerializer() {
        return new AssetAmountSerializer();
    }

    @Bean
    @Scope("prototype")
    @Autowired
    AssetAmountDeserializer assetAmountDeserializer(APIObjectFactory objectFactory) {
        return new AssetAmountDeserializer(objectFactory);
    }

    @Bean
    @Scope("singleton")
    @Autowired
    ObjectMapper objectMapper(
            AssetAmountDeserializer assetAmountDeserializer,
            AssetAmountSerializer assetAmountSerializer
    ) {
        ObjectMapper objectMapper = new ObjectMapper();
        SimpleModule module = new SimpleModule();
        module.addDeserializer(AssetAmount.class, assetAmountDeserializer);
        module.addSerializer(AssetAmount.class, assetAmountSerializer);
        objectMapper.registerModule(module);
        return objectMapper;
    }

    @Bean
    @Scope("singleton")
    @Autowired
    RouteMapFactory routeMapFactory(ApplicationContext context, ObjectMapper objectMapper) {
        return new RouteMapFactory(context, objectMapper);
    }

}
