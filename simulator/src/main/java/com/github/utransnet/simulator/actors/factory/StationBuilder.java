package com.github.utransnet.simulator.actors.factory;

import com.github.utransnet.simulator.actors.Station;
import com.github.utransnet.simulator.externalapi.APIObjectFactory;
import org.springframework.context.ApplicationContext;

/**
 * Created by Artem on 02.02.2018.
 */
public class StationBuilder extends ActorBuilder<Station> {
    StationBuilder(ApplicationContext context, APIObjectFactory objectFactory) {
        super(Station.class, context, objectFactory);
    }
}
