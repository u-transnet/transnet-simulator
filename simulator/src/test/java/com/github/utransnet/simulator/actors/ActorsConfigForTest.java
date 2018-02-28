package com.github.utransnet.simulator.actors;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.github.utransnet.simulator.SpringTestConfig;
import com.github.utransnet.simulator.TestWithJpaConfig;
import com.github.utransnet.simulator.actors.factory.ActorConfig;
import com.github.utransnet.simulator.externalapi.APIObjectFactory;
import com.github.utransnet.simulator.externalapi.AssetAmount;
import com.github.utransnet.simulator.externalapi.ExternalAPI;
import com.github.utransnet.simulator.externalapi.h2impl.ExternalAPIH2ImplConfig;
import com.github.utransnet.simulator.route.AssetAmountDeserializer;
import com.github.utransnet.simulator.route.AssetAmountSerializer;
import com.github.utransnet.simulator.route.RouteMap;
import com.github.utransnet.simulator.route.RouteMapFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Scope;

/**
 * Created by Artem on 01.03.2018.
 */
@Configuration
@Import({
        TestWithJpaConfig.class,
        ExternalAPIH2ImplConfig.class,
        ActorConfig.class,
        SpringTestConfig.class
})
public class ActorsConfigForTest {
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
    RouteMapFactory routeMapFactory(
            ApplicationContext context,
            ObjectMapper objectMapper
    ) {
        return new RouteMapFactory(context, objectMapper);
    }

    @Bean
    @Scope("prototype")
    @Autowired
    RouteMap routeMap(ExternalAPI externalAPI) {
        return new RouteMap(externalAPI);
    }
}
