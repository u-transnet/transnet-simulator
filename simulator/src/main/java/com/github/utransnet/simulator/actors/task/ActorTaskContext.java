package com.github.utransnet.simulator.actors.task;

import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiFunction;

/**
 * Created by Artem on 05.02.2018.
 */
public class ActorTaskContext {
    @Getter
    @Nullable
    private final OperationEvent.Type successEventType;

    @Getter
    @Nullable
    private final OperationEvent.Type failEventType;

    @Getter
    @Nullable
    private final BiFunction<ActorTaskContext, OperationEvent, Boolean> successPredicate;

    @Getter
    @Nullable
    private final BiFunction<ActorTaskContext, OperationEvent, Boolean> failPredicate;

    @Getter
    private final int waitSeconds;

    Map<String, Object> payload;

    @Getter
    @Setter
    @Nullable
    Exception exception;


    private ActorTaskContext(
            @Nullable OperationEvent.Type successEventType,
            @Nullable OperationEvent.Type failEventType,
            @Nullable BiFunction<ActorTaskContext, OperationEvent, Boolean> successPredicate,
            @Nullable BiFunction<ActorTaskContext, OperationEvent, Boolean> failPredicate,
            int waitSeconds
    ) {
        if (successPredicate == null && waitSeconds == 0) {
            throw new RuntimeException("At least one finish condition should be set");
        }
        this.successEventType = successEventType;
        this.failEventType = failEventType;
        this.successPredicate = successPredicate;
        this.failPredicate = failPredicate;
        this.waitSeconds = waitSeconds;
        this.payload = new HashMap<>(10);
    }

    public ActorTaskContext(
            OperationEvent.Type successEventType,
            OperationEvent.Type failEventType,
            @Nullable BiFunction<ActorTaskContext, OperationEvent, Boolean> successPredicate,
            @Nullable BiFunction<ActorTaskContext, OperationEvent, Boolean> failPredicate
    ) {
        this(successEventType, failEventType, successPredicate, failPredicate, 0);
    }

    public ActorTaskContext(
            OperationEvent.Type successEventType,
            @Nullable BiFunction<ActorTaskContext, OperationEvent, Boolean> successPredicate
    ) {
        this(successEventType, null, successPredicate, null, 0);
    }

    public ActorTaskContext(int waitSeconds) {
        this(null, null, null, null, waitSeconds);
    }

    @SuppressWarnings("unchecked")
    public <T> T getPayload(String key) {
        Object o = payload.get(key);
        if (o == null) {
            throw new RuntimeException("Missing value in context for key '" + key + "'");
        }
        return (T) o;
    }

    public ActorTaskContext addPayload(String key, Object value) {
        payload.put(key, value);
        return this;
    }

}
