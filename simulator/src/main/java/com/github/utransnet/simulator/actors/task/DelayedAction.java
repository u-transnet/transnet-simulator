package com.github.utransnet.simulator.actors.task;

import com.github.utransnet.simulator.actors.factory.Actor;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;

/**
 * Created by Artem on 05.02.2018.
 */
@AllArgsConstructor
@EqualsAndHashCode(of={"name"})
public class DelayedAction {

    private final Actor parent;

    @Getter
    private final String name;

    @Getter
    int waitSeconds;

    private final Runnable action;

    public void update(int seconds) {
        waitSeconds -= seconds;
        if(waitSeconds <= 0) {
            action.run();
            parent.removeDelayedAction(name);
        }
    }
}
