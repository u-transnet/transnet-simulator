package com.github.utransnet.simulator.route;

import com.github.utransnet.simulator.actors.BaseInfObject;
import com.github.utransnet.simulator.actors.Client;
import com.github.utransnet.simulator.actors.Logist;
import com.github.utransnet.simulator.externalapi.AssetAmount;

import java.util.Set;

/**
 * Created by Artem on 31.01.2018.
 */
public class Scenario {
    public Set<BaseInfObject> infrastructure;
    public Logist logist;
    public AssetAmount routeMapPrice;
    public Set<Client> clients;
}
