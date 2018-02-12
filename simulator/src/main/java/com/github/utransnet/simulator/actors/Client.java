package com.github.utransnet.simulator.actors;

import com.github.utransnet.simulator.externalapi.ExternalAPI;

/**
 * Created by Artem on 31.01.2018.
 */
public class Client extends Actor {
    public Client(ExternalAPI externalAPI) {
        super(externalAPI);
    }

    @Override
    public void update(int seconds) {
        super.update(seconds);
    }

    private void buyRouteMap() {

    }

    private void buyTrip() {

    }

    private void payForRoutePart() {

    }

    private void enterRailCar() {

    }

    private void exitRailCar() {

    }
}
