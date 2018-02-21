package com.github.utransnet.simulator.actors.task;

import com.github.utransnet.simulator.actors.factory.Actor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;

/**
 * Created by Artem on 05.02.2018.
 */
@Builder
public class ActorTask<S extends OperationEvent, F extends OperationEvent> {

    @Getter
    private final Actor executor;
    @Getter
    private final String name;
    @Getter
    private final ActorTaskContext<S, F> context;
    @Nullable
    @Getter
    private ActorTask previous;
    @Nullable
    @Getter
    @Setter
    private ActorTask next;
    @Nullable
    @Getter
    private Consumer<ActorTaskContext<S, F>> onStart;
    @Nullable
    @Getter
    private Consumer<ActorTaskContext<S, F>> onEnd;
    @Nullable
    @Getter
    private Consumer<ActorTaskContext<S, F>> onCancel;

    @java.beans.ConstructorProperties({
            "previous", "next", "onStart", "onEnd", "onCancel", "executor", "name", "context"
    })
    private ActorTask(
            @Nullable ActorTask previous,
            @Nullable ActorTask next,
            @Nullable Consumer<ActorTaskContext<S, F>> onStart,
            @Nullable Consumer<ActorTaskContext<S, F>> onEnd,
            @Nullable Consumer<ActorTaskContext<S, F>> onCancel,
            Actor executor,
            String name,
            ActorTaskContext context
    ) {
        this.previous = previous;
        this.next = next;
        this.onStart = onStart;
        this.onEnd = onEnd;
        this.onCancel = onCancel;
        this.executor = executor;
        this.name = name;
        this.context = context;
    }

    public void start() {
        if (onStart != null) {
            onStart.accept(context);
        }
        if (context.getSuccessPredicate() != null && context.getSuccessEventType() != null) {
            EventListener sEventListener = new EventListener<S>(
                    name + "-finish",
                    context.getSuccessEventType(),
                    event -> {
                        try {
                            if (context.getSuccessPredicate().apply(context, event)) {
                                finish();
                            }
                        } catch (Exception e) {
                            context.setException(e);
                            cancel();
                        }
                    }
            );
            executor.addEventListener(sEventListener);
        }
        if (context.getFailPredicate() != null && context.getFailEventType() != null) {
            EventListener fEventListener = new EventListener<F>(
                    name + "-cancel",
                    context.getFailEventType(),
                    event -> {
                        if (context.getFailPredicate().apply(context, event)) {
                            cancel();
                        }
                    }
            );
            executor.addEventListener(fEventListener);
        }
        if (context.getWaitSeconds() > 0) {
            executor.addDelayedAction(new DelayedAction(
                    executor,
                    name + "-finish",
                    context.getWaitSeconds(),
                    this::finish
            ));
        }
    }

    public void finish() {
        if (onEnd != null) {
            onEnd.accept(context);
        }
        destroy();
        if (next != null) {
            this.context.payload.forEach(next.getContext()::addPayload);
            executor.setCurrentTask(next);
        }
    }

    private void cancel() {
        if (onCancel != null) {
            onCancel.accept(context);
        }
        destroy();
    }

    private void destroy() {
        executor.removeOperationListener(name + "-finish");
        executor.removeOperationListener(name + "-cancel");
        executor.removeDelayedAction(name + "-finish");
    }

    public ActorTaskBuilder createNext() {
        return ActorTask.builder().previous(this);
    }


    public static class ActorTaskBuilder {
        private @Nullable ActorTask previous;
        private @Nullable ActorTask next;
        private @Nullable Consumer<ActorTaskContext> onStart;
        private @Nullable Consumer<ActorTaskContext> onEnd;
        private @Nullable Consumer<ActorTaskContext> onCancel;
        private Actor executor;
        private String name;
        private ActorTaskContext context;

        public ActorTask build() {
            ActorTask actorTask = new ActorTask(previous, next, onStart, onEnd, onCancel, executor, name, context);
            if (previous != null) {
                previous.setNext(actorTask);
            }
            return actorTask;
        }

    }


}
