package com.github.utransnet.simulator.actors.task;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

/**
 * Created by Artem on 05.02.2018.
 */
@Builder
public class ActorTimeTask extends ActorTask {

    @Getter
    private final long duration; // in seconds

    @Getter
    private long executedTime = 0; // in seconds

    public static class ActorTimeTaskBuilder extends ActorTaskBuilder {
        // Executed time should be set in time of execution
        private ActorTimeTaskBuilder executedTime(long executedTime) {
            this.executedTime = executedTime;
            return this;
        }
    }
}
