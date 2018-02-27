package com.github.utransnet.simulator.route;

import com.github.utransnet.simulator.actors.BaseInfObject;
import com.github.utransnet.simulator.actors.Client;
import com.github.utransnet.simulator.actors.Logist;
import com.github.utransnet.simulator.actors.RailCar;
import com.github.utransnet.simulator.externalapi.AssetAmount;
import lombok.Getter;
import lombok.Setter;

import java.util.HashSet;
import java.util.Set;

/**
 * Created by Artem on 31.01.2018.
 */

@Getter
public class Scenario {
    private final Set<BaseInfObject> infrastructure = new HashSet<>(32);
    private final Set<Client> clients = new HashSet<>(32);
    private final Set<RailCar> railCars = new HashSet<>(32);
    @Setter
    private Logist logist;
    @Setter
    private AssetAmount routeMapPrice;

    public void addActor(Client client) {
        clients.add(client);
    }

    public void addActor(RailCar railCar) {
        railCars.add(railCar);
    }

    public void addActor(BaseInfObject infObject) {
        infrastructure.add(infObject);
    }
}
