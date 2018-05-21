package com.github.utransnet.simulator.actors.factory;

import com.github.utransnet.simulator.actors.CheckPoint;
import com.github.utransnet.simulator.externalapi.APIObjectFactory;
import org.springframework.context.ApplicationContext;

/**
 * Created by Artem on 02.02.2018.
 */
public class CheckPointBuilder extends ActorBuilder<CheckPoint> {
    CheckPointBuilder(ApplicationContext context, APIObjectFactory objectFactory) {
        super(CheckPoint.class, context, objectFactory);
    }
}
