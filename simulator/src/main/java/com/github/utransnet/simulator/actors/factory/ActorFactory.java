package com.github.utransnet.simulator.actors.factory;

import org.springframework.context.ApplicationContext;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Created by Artem on 02.02.2018.
 */
public class ActorFactory {
    final ApplicationContext context;

    @Autowired
    public ActorFactory(ApplicationContext context) {
        this.context = context;
    }

    public LogistBuilder createLogistBuilder(){
        return context.getBean(LogistBuilder.class);
    }

    public ClientBuilder createClientBuilder(){
        return context.getBean(ClientBuilder.class);
    }

    public RailCarBuilder createRailCarBuilder(){
        return context.getBean(RailCarBuilder.class);
    }

    public StationBuilder createStationBuilder(){
        return context.getBean(StationBuilder.class);
    }

    public CheckPointBuilder createCheckPointBuilder(){
        return context.getBean(CheckPointBuilder.class);
    }
}
