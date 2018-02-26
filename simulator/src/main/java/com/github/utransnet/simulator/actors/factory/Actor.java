package com.github.utransnet.simulator.actors.factory;

import com.github.utransnet.simulator.actors.task.*;
import com.github.utransnet.simulator.externalapi.AssetAmount;
import com.github.utransnet.simulator.externalapi.ExternalAPI;
import com.github.utransnet.simulator.externalapi.Proposal;
import com.github.utransnet.simulator.externalapi.UserAccount;
import com.github.utransnet.simulator.externalapi.operations.*;
import lombok.*;
import org.jetbrains.annotations.Nullable;

import java.util.AbstractQueue;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Created by Artem on 31.01.2018.
 */
@EqualsAndHashCode(of = "uTransnetAccount")
public class Actor {

    @Getter(AccessLevel.PROTECTED)
    private final ExternalAPI externalAPI;

    private final Set<OperationListener> operationListeners = new HashSet<>(16);
    private final Set<EventListener<OperationEvent>> eventListeners = new HashSet<>(16);

    private final Set<DelayedAction> delayedActions = new HashSet<>(16);
    private final AbstractQueue<ActorTask> tasksQueue = new LinkedBlockingQueue<>(100);
    @Getter(AccessLevel.PROTECTED)
    @Nullable
    private ActorTask currentTask;
    @Getter(AccessLevel.PROTECTED)
    @Setter(AccessLevel.PACKAGE)
    private Set<AssetAmount> balance;

    @Getter
    private UserAccount uTransnetAccount;

    private String lastOperationId;
    private String lastProposalId;

    public Actor(ExternalAPI externalAPI) {
        this.externalAPI = externalAPI;
    }

    public void update(int seconds) {
        if (checkNewOperations()) {
            externalAPI.operationsAfter(uTransnetAccount, lastOperationId)
                    .forEach(this::processEachOperation);
        }
        delayedActions.forEach(delayedAction -> delayedAction.update(seconds));
        if (currentTask == null && !tasksQueue.isEmpty()) {
            setCurrentTask(tasksQueue.poll());
        }
    }

    private void processEachOperation(BaseOperation operation) {
        switch (operation.getOperationType()) {
            case TRANSFER:
                BaseOperation.<TransferOperation>convert(operation, operation.getOperationType())
                        .ifPresent(transferOperation -> {
                            OperationEvent.TransferEvent event = new OperationEvent.TransferEvent(
                                    transferOperation
                            );
                            fireEvent(event);
                        });
                break;
            case MESSAGE:
                BaseOperation.<MessageOperation>convert(operation, operation.getOperationType())
                        .ifPresent(transferOperation -> {
                            OperationEvent.MessageEvent event = new OperationEvent.MessageEvent(
                                    transferOperation
                            );
                            fireEvent(event);
                        });
                break;
            case PROPOSAL_UPDATE:
                BaseOperation.<ProposalUpdateOperation>convert(operation, operation.getOperationType())
                        .ifPresent(proposalUpdateOperation -> {
                            OperationEvent.ProposalUpdateEvent event = new OperationEvent.ProposalUpdateEvent(
                                    proposalUpdateOperation.getProposal()
                            );
                            fireEvent(event);
                        });
                break;
            case PROPOSAL_CREATE:
                BaseOperation.<ProposalCreateOperation>convert(operation, operation.getOperationType())
                        .ifPresent(proposalCreateOperation -> {
                            Proposal createdProposal = proposalCreateOperation.getProposal();
                            lastProposalId = createdProposal.getId();
                            OperationEvent.ProposalCreateEvent event = new OperationEvent.ProposalCreateEvent(
                                    createdProposal
                            );
                            fireEvent(event);
                        });
                break;
            case PROPOSAL_DELETE:
                BaseOperation.<ProposalDeleteOperation>convert(operation, operation.getOperationType())
                        .ifPresent(proposalDeleteOperation -> {
                            OperationEvent.ProposalDeleteEvent event = new OperationEvent.ProposalDeleteEvent(
                                    proposalDeleteOperation.getProposalId()
                            );
                            fireEvent(event);
                        });
                break;
        }
        operationListeners.stream()
                .filter(listener ->
                        listener.getOperationType() == operation.getOperationType()
                )
                .forEach(listener -> listener.fire(operation));

    }

    private void fireEvent(OperationEvent operationEvent) {
        eventListeners.stream()
                .filter(eventListener -> eventListener.getEventType() == operationEvent.getEventType())
                .forEach(listener -> listener.fire(operationEvent));
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

    public final void addEventListener(EventListener<OperationEvent> operationListener) {
        eventListeners.add(operationListener);
    }

    public final void removeEventListener(String name) {
        eventListeners.removeIf(listener -> Objects.equals(listener.getName(), name));
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
