package com.github.utransnet.simulator.actors.factory;

import com.github.utransnet.simulator.actors.Logist;
import com.github.utransnet.simulator.externalapi.APIObjectFactory;
import com.github.utransnet.simulator.externalapi.ExternalAPIConfig;
import com.github.utransnet.simulator.externalapi.ExternalAPI;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Scope;

/**
 * Created by Artem on 02.02.2018.
 */
@Configuration
@Import({
        ExternalAPIConfig.class
})
public class ActorConfig {


    @Bean
    @Scope("singleton")
    @Autowired
    ActorFactory actorFactory(ApplicationContext context) {
        return new ActorFactory(context);
    }

    @Bean
    @Autowired
    LogistBuilder logistBuilder(ApplicationContext context, APIObjectFactory objectFactory) {
        return new LogistBuilder(context, objectFactory);
    }

    @Bean
    @Autowired
    ClientBuilder clientBuilder(ApplicationContext context, APIObjectFactory objectFactory) {
        return new ClientBuilder(context, objectFactory);
    }

    @Bean
    @Autowired
    RailCarBuilder railCarBuilder(ApplicationContext context, APIObjectFactory objectFactory) {
        return new RailCarBuilder(context, objectFactory);
    }

    @Bean
    @Autowired
    StationBuilder stationBuilder(ApplicationContext context, APIObjectFactory objectFactory) {
        return new StationBuilder(context, objectFactory);
    }

    @Bean
    @Autowired
    CheckPointBuilder checkPointBuilder(ApplicationContext context, APIObjectFactory objectFactory) {
        return new CheckPointBuilder(context, objectFactory);
    }

    @Bean
    @Autowired
    Logist logist(ExternalAPI externalAPI){
        return new Logist(externalAPI);
    }
}
