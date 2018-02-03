package com.github.utransnet.simulator.actors.factory;

import com.github.utransnet.simulator.actors.Client;
import com.github.utransnet.simulator.externalapi.APIObjectFactory;
import org.springframework.context.ApplicationContext;

/**
 * Created by Artem on 02.02.2018.
 */
public class ClientBuilder extends ActorBuilder<Client> {
    ClientBuilder(ApplicationContext context, APIObjectFactory objectFactory) {
        super(context, objectFactory);
    }
}
