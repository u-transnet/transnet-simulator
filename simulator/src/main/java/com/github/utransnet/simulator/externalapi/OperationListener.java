package com.github.utransnet.simulator.externalapi;

import lombok.*;

import java.util.function.Consumer;

/**
 * Created by Artem on 02.02.2018.
 */
@EqualsAndHashCode(of={"name"})
@AllArgsConstructor
@Getter
@Setter
public class OperationListener {
    public OperationListener(String name) {
        this.name = name;
    }

    String name;
    OperationType operationType;
    Consumer<BaseOperation> consumer;

    public void fire(BaseOperation operation) {
        consumer.accept(operation);
    }
}
