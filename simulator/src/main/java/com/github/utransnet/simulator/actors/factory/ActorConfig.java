package com.github.utransnet.simulator.actors.factory;

import com.github.utransnet.simulator.actors.*;
import com.github.utransnet.simulator.externalapi.APIObjectFactory;
import com.github.utransnet.simulator.externalapi.ExternalAPI;
import com.github.utransnet.simulator.queue.InputQueue;
import com.github.utransnet.simulator.route.RouteMap;
import com.github.utransnet.simulator.route.RouteMapFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;

/**
 * Created by Artem on 02.02.2018.
 */
@Configuration
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
    @Scope("prototype")
    @Autowired
    Logist logist(ExternalAPI externalAPI, RouteMapFactory routeMapFactory, InputQueue<RouteMap> routeMapInputQueue){
        return new Logist(externalAPI, routeMapFactory, routeMapInputQueue);
    }

    @Bean
    @Scope("prototype")
    @Autowired
    Client client(ExternalAPI externalAPI, RouteMapFactory routeMapFactory){
        return new Client(externalAPI, routeMapFactory);
    }

    @Bean
    @Scope("prototype")
    @Autowired
    Station station(ExternalAPI externalAPI, RouteMapFactory routeMapFactory, APIObjectFactory objectFactory) {
        return new Station(externalAPI, routeMapFactory, objectFactory);
    }

    @Bean
    @Scope("prototype")
    @Autowired
    RailCar railCar(ExternalAPI externalAPI, RouteMapFactory routeMapFactory, APIObjectFactory objectFactory) {
        return new RailCar(externalAPI, routeMapFactory, objectFactory);
    }

    @Bean
    @Scope("prototype")
    @Autowired
    CheckPoint checkPoint(ExternalAPI externalAPI, APIObjectFactory objectFactory) {
        return new CheckPoint(externalAPI, objectFactory);
    }
}
