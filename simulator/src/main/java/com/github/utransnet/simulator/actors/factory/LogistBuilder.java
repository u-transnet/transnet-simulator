package com.github.utransnet.simulator.actors.factory;

import com.github.utransnet.simulator.actors.Logist;
import com.github.utransnet.simulator.externalapi.APIObjectFactory;
import org.springframework.context.ApplicationContext;

/**
 * Created by Artem on 02.02.2018.
 */
public class LogistBuilder extends ActorBuilder<Logist> {
    LogistBuilder(ApplicationContext context, APIObjectFactory objectFactory) {
        super(Logist.class, context, objectFactory);
    }
}
