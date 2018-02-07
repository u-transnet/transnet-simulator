package com.github.utransnet.simulator.actors;

import com.github.utransnet.simulator.actors.task.ActorTask;
import com.github.utransnet.simulator.actors.task.DelayedAction;
import com.github.utransnet.simulator.actors.task.OperationListener;
import com.github.utransnet.simulator.externalapi.*;
import com.github.utransnet.simulator.queue.InputQueue;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.Nullable;

import javax.annotation.PostConstruct;
import java.util.AbstractQueue;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Created by Artem on 31.01.2018.
 */
public abstract class Actor {

    @Getter(AccessLevel.PROTECTED)
    private final ExternalAPI externalAPI;

    private final Set<OperationListener> operationListeners = new HashSet<>(16);

    private final Set<DelayedAction> delayedActions = new HashSet<>(16);

    @Getter(AccessLevel.PROTECTED)
    @Nullable
    private ActorTask currentTask;

    private final AbstractQueue<ActorTask> tasksQueue = new LinkedBlockingQueue<>(100);

    @Setter(AccessLevel.PACKAGE)
    private Set<AssetAmount> balance;

    @Getter
    @Setter(AccessLevel.PACKAGE)
    private UserAccount uTransnetAccount;

    private String lastOperationId;

    public Actor(ExternalAPI externalAPI) {
        this.externalAPI = externalAPI;
    }

    public void update(int seconds) {
        if (checkNewOperations()) {
            externalAPI.operationsBefore(lastOperationId)
                    .forEach(operation ->
                            operationListeners.stream()
                            .filter(listener ->
                                    listener.getOperationType() == operation.getOperationType()
                            )
                            .forEach(listener -> listener.fire(operation)));
        }
        delayedActions.forEach(delayedAction -> delayedAction.update(seconds));
        if(currentTask == null && !tasksQueue.isEmpty()) {
            setCurrentTask(tasksQueue.poll());
        }
    }

    public void setCurrentTask(ActorTask currentTask) {
        this.currentTask = currentTask;
        currentTask.start();
    }

    public String getId() {
        return uTransnetAccount.getId();
    }

    @PostConstruct
    private void init() {
        lastOperationId = externalAPI.getLastOperation(uTransnetAccount).getId();
    }

    protected boolean checkNewOperations() {
        return !Objects.equals(externalAPI.getLastOperation(uTransnetAccount).getId(), lastOperationId);
    }

    public final void addOperationListener(OperationListener operationListener) {
        operationListeners.add(operationListener);
    }

    public final void removeOperationListener(String name) {
        operationListeners.removeIf(listener -> Objects.equals(listener.getName(), name));
    }

    public final void addDelayedAction(DelayedAction delayedAction) {
        delayedActions.add(delayedAction);
    }

    public final void removeDelayedAction(String name) {
        delayedActions.removeIf(delayedAction -> Objects.equals(delayedAction.getName(), name));
    }

    protected void payTo(UserAccount receiver, AssetAmount assetAmount) {
        externalAPI.sendAsset(uTransnetAccount, receiver, assetAmount);
    }

    protected void payTo(UserAccount receiver, Asset asset, long amount) {
        externalAPI.sendAsset(uTransnetAccount, receiver, asset, amount);
    }

    protected void addTask(ActorTask task) {
        tasksQueue.offer(task);
    }



    public boolean equals(Object o) {
        if (o == this) return true;
        if (!(o instanceof Actor)) return false;
        final Actor other = (Actor) o;
        if (!other.canEqual(this)) return false;
        final Object this$id = this.getId();
        final Object other$id = other.getId();
        return this$id == null ? other$id == null : this$id.equals(other$id);
    }

    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final Object $id = this.getId();
        result = result * PRIME + ($id == null ? 43 : $id.hashCode());
        return result;
    }

    protected boolean canEqual(Object other) {
        return other instanceof Actor;
    }
}
