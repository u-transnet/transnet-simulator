package com.github.utransnet.simulator.services;

import com.github.utransnet.simulator.route.ScenarioContainer;

/**
 * Created by Artem on 31.01.2018.
 */
public interface Supervisor {

    void init(ScenarioContainer scenario) throws SimulationStartedException;
}
