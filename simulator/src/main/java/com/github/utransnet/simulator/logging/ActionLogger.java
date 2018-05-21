package com.github.utransnet.simulator.logging;

import com.github.utransnet.simulator.actors.factory.Actor;
import lombok.extern.slf4j.Slf4j;

/**
 * Created by Artem on 16.03.2018.
 */
@Slf4j
public class ActionLogger {
    public void logActorAction(Actor actor, String action, String label) {
        log.trace(
                String.format("<%s>|<%s>: " + label,
                        action,
                        actor.getUTransnetAccount().getName(),
                        actor.getUTransnetAccount().getName()
                )
        );
    }
}
