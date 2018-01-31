package com.github.utransnet.simulator.services;

import com.github.utransnet.simulator.Scenario;
import com.github.utransnet.simulator.actors.Actor;
import com.github.utransnet.simulator.actors.Client;
import com.github.utransnet.simulator.route.RouteMap;

import java.util.Set;

/**
 * Created by Artem on 31.01.2018.
 */
public class SupervisorImpl implements Supervisor {

    private Set<Actor> actors;

    @Override
    public void loadScenario(Scenario scenario) {

    }

    @Override
    public void addRouteMaps(RouteMap... routeMaps) {

    }

    @Override
    public void addClient(Client client) {

    }

    @Override
    public void update(int seconds) {
        actors.forEach(actor -> actor.update(seconds));
    }
}
