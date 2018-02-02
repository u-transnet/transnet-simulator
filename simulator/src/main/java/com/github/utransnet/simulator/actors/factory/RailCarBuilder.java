package com.github.utransnet.simulator.actors.factory;

import com.github.utransnet.simulator.actors.Logist;
import com.github.utransnet.simulator.actors.RailCar;
import com.github.utransnet.simulator.externalapi.APIObjectFactory;
import org.springframework.context.ApplicationContext;

/**
 * Created by Artem on 02.02.2018.
 */
public class RailCarBuilder extends ActorBuilder<RailCar> {
    RailCarBuilder(ApplicationContext context, APIObjectFactory objectFactory) {
        super(context, objectFactory);
    }
}
