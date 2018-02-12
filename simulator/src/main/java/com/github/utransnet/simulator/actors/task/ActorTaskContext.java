package com.github.utransnet.simulator.actors.task;

import com.github.utransnet.simulator.externalapi.operations.BaseOperation;
import com.github.utransnet.simulator.externalapi.operations.OperationType;
import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * Created by Artem on 05.02.2018.
 */
public class ActorTaskContext {
    @Getter
    @Nullable
    private final OperationType operationType;

    @Getter
    @Nullable
    private final BiFunction<ActorTaskContext, BaseOperation, Boolean> successPredicate;

    @Getter
    @Nullable
    private final BiFunction<ActorTaskContext, BaseOperation, Boolean> failPredicate;

    @Getter
    private final int waitSeconds;

    Map<String, Object> payload;

    @Getter
    @Setter
    @Nullable
    Exception exception;


    private ActorTaskContext(
            @Nullable OperationType operationType,
            @Nullable BiFunction<ActorTaskContext, BaseOperation, Boolean> successPredicate,
            @Nullable BiFunction<ActorTaskContext, BaseOperation, Boolean> failPredicate,
            int waitSeconds
    ) {
        if(successPredicate == null && waitSeconds == 0) {
            throw new RuntimeException("At least one finish condition should be set");
        }
        this.operationType = operationType;
        this.successPredicate = successPredicate;
        this.failPredicate = failPredicate;
        this.waitSeconds = waitSeconds;
        this.payload = new HashMap<>(10);
    }

    public ActorTaskContext(
            OperationType operationType,
            @Nullable BiFunction<ActorTaskContext, BaseOperation, Boolean> successPredicate,
            @Nullable BiFunction<ActorTaskContext, BaseOperation, Boolean> failPredicate
    ) {
        this(operationType, successPredicate, failPredicate, 0);
    }

    public ActorTaskContext(
            OperationType operationType,
            @Nullable BiFunction<ActorTaskContext, BaseOperation, Boolean> successPredicate
    ) {
        this(operationType, successPredicate, null, 0);
    }

    public ActorTaskContext(int waitSeconds) {
        this(null, null, null, 0);
    }

    @SuppressWarnings("unchecked")
    public <T> T getPayload(String key) {
        Object o = payload.get(key);
        if(o == null){
            throw new RuntimeException("Missing value in context");
        }
        return (T) o;
    }

    public ActorTaskContext addPayload(String key, Object value) {
        payload.put(key, value);
        return this;
    }


    public static final ActorTaskContext immediate() {
        return new ActorTaskContext(0);
    }
}
