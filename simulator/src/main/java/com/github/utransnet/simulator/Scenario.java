package com.github.utransnet.simulator;

import com.github.utransnet.simulator.actors.BaseInfObject;
import com.github.utransnet.simulator.actors.Client;
import com.github.utransnet.simulator.actors.Logist;

import java.util.Set;

/**
 * Created by Artem on 31.01.2018.
 */
public class Scenario {
    public Set<BaseInfObject> infrastructure;
    public Logist logist;
    public Set<Client> clients;
}
