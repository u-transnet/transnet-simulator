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
public class EventListener<T extends OperationEvent> {

    private final String name;
    private final OperationEvent.Type eventType;
    private final Consumer<T> consumer;

    public void fire(T operation) {
        consumer.accept(operation);
    }

}
