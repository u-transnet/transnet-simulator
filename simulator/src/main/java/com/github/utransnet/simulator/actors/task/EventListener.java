package com.github.utransnet.simulator.actors.task;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;

import java.util.function.Consumer;

/**
 * Created by Artem on 02.02.2018.
 */
@EqualsAndHashCode(of={"name"})
@AllArgsConstructor
@Getter
public class EventListener {

    private final String name;
    private final OperationEvent.Type eventType;
    private final Consumer<OperationEvent> consumer;

    public void fire(OperationEvent operation) {
        consumer.accept(operation);
    }

}
