package com.github.utransnet.simulator.actors.factory;

import com.github.utransnet.simulator.actors.task.ActorTask;
import com.github.utransnet.simulator.actors.task.DelayedAction;
import com.github.utransnet.simulator.actors.task.OperationListener;
import com.github.utransnet.simulator.externalapi.*;
import com.github.utransnet.simulator.externalapi.operations.BaseOperation;
import lombok.*;
import org.jetbrains.annotations.Nullable;

import javax.annotation.PostConstruct;
import java.util.*;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Created by Artem on 31.01.2018.
 */
@EqualsAndHashCode(of = "uTransnetAccount")
public class Actor {

    @Getter(AccessLevel.PROTECTED)
    private final ExternalAPI externalAPI;

    private final Set<OperationListener> operationListeners = new HashSet<>(16);

    private final Set<DelayedAction> delayedActions = new HashSet<>(16);

    @Getter(AccessLevel.PROTECTED)
    @Nullable
    private ActorTask currentTask;

    private final AbstractQueue<ActorTask> tasksQueue = new LinkedBlockingQueue<>(100);

    @Getter(AccessLevel.PROTECTED)
    @Setter(AccessLevel.PACKAGE)
    private Set<AssetAmount> balance;

    @Getter
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


    protected boolean checkNewOperations() {
        return uTransnetAccount
                .getLastOperation()
                .map(BaseOperation::getId)
                .filter(id -> !Objects.equals(id, lastOperationId))
                .isPresent();
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

    protected void payTo(UserAccount receiver, AssetAmount assetAmount, String memo) {
        uTransnetAccount.sendAsset(receiver, assetAmount, memo);
    }

    protected void addTask(ActorTask task) {
        tasksQueue.offer(task);
    }


    void setUTransnetAccount(@NonNull UserAccount uTransnetAccount) {
        this.uTransnetAccount = uTransnetAccount;
        uTransnetAccount.getLastOperation().ifPresent(baseOperation -> lastOperationId = baseOperation.getId());
    }

    protected boolean canEqual(Object other) {
        return other instanceof Actor;
    }
}
