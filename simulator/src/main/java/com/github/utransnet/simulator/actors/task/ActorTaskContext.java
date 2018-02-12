package com.github.utransnet.simulator.actors.task;

import com.github.utransnet.simulator.externalapi.BaseOperation;
import com.github.utransnet.simulator.externalapi.OperationType;
import lombok.Getter;
import org.jetbrains.annotations.Nullable;

import java.util.function.Function;

/**
 * Created by Artem on 05.02.2018.
 */
public class ActorTaskContext {
    @Getter
    private final OperationType operationType;

    @Getter
    @Nullable
    private final Function<BaseOperation, Boolean> successPredicate;

    @Getter
    @Nullable
    private final Function<BaseOperation, Boolean> failPredicate;

    @Getter
    private final int waitSeconds;

    public ActorTaskContext(
            OperationType operationType,
            @Nullable Function<BaseOperation, Boolean> successPredicate,
            @Nullable Function<BaseOperation, Boolean> failPredicate,
            int waitSeconds
    ) {
        if(successPredicate == null && waitSeconds == 0) {
            throw new RuntimeException("At least one finish condition should be set");
        }
        this.operationType = operationType;
        this.successPredicate = successPredicate;
        this.failPredicate = failPredicate;
        this.waitSeconds = waitSeconds;
    }

    public ActorTaskContext(
            OperationType operationType,
            @Nullable Function<BaseOperation, Boolean> successPredicate,
            @Nullable Function<BaseOperation, Boolean> failPredicate
    ) {
        this(operationType, successPredicate, failPredicate, 0);
    }

    public ActorTaskContext(
            OperationType operationType,
            @Nullable Function<BaseOperation, Boolean> successPredicate
    ) {
        this(operationType, successPredicate, null, 0);
    }

    public ActorTaskContext(OperationType operationType, int waitSeconds) {
        this(operationType, null, null, 0);
    }
}
