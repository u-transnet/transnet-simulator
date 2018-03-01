package com.github.utransnet.simulator.actors.task;

import com.github.utransnet.simulator.actors.factory.Actor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.Nullable;
import org.springframework.util.Assert;

import java.util.function.Consumer;

/**
 * Created by Artem on 05.02.2018.
 */
@Slf4j
@Builder
public class ActorTask {

    @Getter
    private final Actor executor;
    @Getter
    private final String name;
    @Getter
    private final ActorTaskContext context;
    @Nullable
    @Getter
    private ActorTask previous;
    @Nullable
    @Getter
    @Setter
    private ActorTask next;
    @Nullable
    @Getter
    private Consumer<ActorTaskContext> onStart;
    @Nullable
    @Getter
    private Consumer<ActorTaskContext> onEnd;
    @Nullable
    @Getter
    private Consumer<ActorTaskContext> onCancel;

    @java.beans.ConstructorProperties({
            "previous", "next", "onStart", "onEnd", "onCancel", "executor", "name", "context"
    })
    private ActorTask(
            @Nullable ActorTask previous,
            @Nullable ActorTask next,
            @Nullable Consumer<ActorTaskContext> onStart,
            @Nullable Consumer<ActorTaskContext> onEnd,
            @Nullable Consumer<ActorTaskContext> onCancel,
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
            EventListener sEventListener = new EventListener(
                    name + "-finish",
                    context.getSuccessEventType(),
                    event -> {
                        try {
                            if (context.getSuccessPredicate().apply(context, event)) {
                                finish();
                            }
                        } catch (Exception e) {
                            log.error("Error in ActorTask[" + name + "]", e);
                            context.setException(e);
                            cancel();
                        }
                    }
            );
            executor.addEventListener(sEventListener);
        }
        if (context.getFailPredicate() != null && context.getFailEventType() != null) {
            EventListener fEventListener = new EventListener(
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
            try {
                onEnd.accept(context);
            } catch (Exception e) {
                log.error("Error in ActorTask[" + name + "]", e);
                context.setException(e);
                cancel();
            }
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
            Assert.notNull(name, "ActorTask name not set");
            Assert.notNull(executor, "ActorTask [" + name + "] executor not set");
            Assert.notNull(context, "ActorTask [" + name + "] context not set");
            ActorTask actorTask = new ActorTask(previous, next, onStart, onEnd, onCancel, executor, name, context);
            if (previous != null) {
                previous.setNext(actorTask);
            }
            return actorTask;
        }

    }


}
