package com.github.utransnet.simulator.actors;

import com.github.utransnet.simulator.externalapi.UserAccount;

/**
 * Created by Artem on 20.03.2018.
 */
public interface ActorWithReservation {
    UserAccount getReservation();

}
