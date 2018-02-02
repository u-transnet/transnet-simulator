package com.github.utransnet.simulator.actors;

import com.github.utransnet.simulator.externalapi.ExternalAPI;

/**
 * Created by Artem on 31.01.2018.
 */
public abstract class BaseInfObject extends Actor {

    public BaseInfObject(ExternalAPI externalAPI) {
        super(externalAPI);
    }

    @Override
    public void update(int seconds) {
        super.update(seconds);
    }


}
