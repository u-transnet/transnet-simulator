package com.github.utransnet.simulator.route;

import com.github.utransnet.simulator.externalapi.AssetAmount;

import java.util.HashSet;
import java.util.Set;

/**
 * Created by Artem on 28.02.2018.
 */
public class ScenarioContainer {

    public Set<SerializedInfrastructureInfo> infrastructure;
    public SerializedUserInfo logist;
    public AssetAmount routeMapPrice;
    public Set<SerializedUserInfo> clients;
    public Set<SerializedRailCarInfo> railCars;

    public ScenarioContainer() {
        infrastructure = new HashSet<>(16);
        clients = new HashSet<>(4);
        railCars = new HashSet<>(8);
    }
}
