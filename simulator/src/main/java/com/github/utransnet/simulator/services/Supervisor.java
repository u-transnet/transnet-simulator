package com.github.utransnet.simulator.services;

import com.github.utransnet.simulator.Scenario;
import com.github.utransnet.simulator.actors.Client;
import com.github.utransnet.simulator.route.RouteMap;

/**
 * Created by Artem on 31.01.2018.
 */
public interface Supervisor {
    void loadScenario(Scenario scenario);
    void addRouteMaps(RouteMap... routeMaps);
    void addClient(Client client);

    void update(int seconds);
}
