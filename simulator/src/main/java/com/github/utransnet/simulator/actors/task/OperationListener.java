package com.github.utransnet.simulator.actors.task;

import com.github.utransnet.simulator.externalapi.operations.BaseOperation;
import com.github.utransnet.simulator.externalapi.operations.OperationType;
import lombok.*;

import java.util.function.Consumer;

/**
 * Created by Artem on 02.02.2018.
 */
@EqualsAndHashCode(of={"name"})
@AllArgsConstructor
@Getter
public class OperationListener {

    private final String name;
    private final OperationType operationType;
    private final Consumer<? super BaseOperation> consumer;

    public void fire(BaseOperation operation) {
        consumer.accept(operation);
    }

}
