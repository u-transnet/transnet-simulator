package com.github.utransnet.simulator.actors.factory;

import com.github.utransnet.simulator.actors.task.*;
import com.github.utransnet.simulator.actors.task.EventListener;
import com.github.utransnet.simulator.externalapi.ExternalAPI;
import com.github.utransnet.simulator.externalapi.Proposal;
import com.github.utransnet.simulator.externalapi.UserAccount;
import com.github.utransnet.simulator.externalapi.operations.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.util.*;
import java.util.concurrent.LinkedBlockingQueue;

import static java.util.stream.Collectors.toList;

/**
 * Created by Artem on 31.01.2018.
 */
@Slf4j
public class Actor {

    @Getter(AccessLevel.PROTECTED)
    private final ExternalAPI externalAPI;

    private final Set<OperationListener> operationListeners = new HashSet<>(16);
    private final Set<EventListener> eventListeners = new HashSet<>(16);

    private final Set<DelayedAction> delayedActions = new HashSet<>(16);
    private final AbstractQueue<ActorTask> tasksQueue = new LinkedBlockingQueue<>(100);

    @Getter(AccessLevel.PROTECTED)
    @Nullable
    private ActorTask currentTask;

    @Getter
    private UserAccount uTransnetAccount;

    private String lastOperationId;
    private String lastProposalId;

    public Actor(ExternalAPI externalAPI) {
        this.externalAPI = externalAPI;
    }

    public void update(int seconds) {
        String lastOperationId = this.lastOperationId;
        if (checkNewOperations()) {
            externalAPI.operationsAfter(uTransnetAccount, lastOperationId)
                    .forEach(this::processEachOperation);
        }
        // new list allows to avoid ConcurrentModificationException
        // if action adds nor removes a delayed action
        new ArrayList<>(delayedActions).forEach(delayedAction -> delayedAction.update(seconds));
        if (currentTask == null && !tasksQueue.isEmpty()) {
            setCurrentTask(tasksQueue.poll());
        }
    }

    protected void processEachOperation(BaseOperation operation) {
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
                .collect(toList())
                // calling toList forces stream computing
                // and allows to avoid ConcurrentModificationException
                .forEach(listener -> {
                    trace("Firing event '" + operationEvent.getEventType().name() + "' for listener '" + listener.getName() + "'");
                    listener.fire(operationEvent);
                });
    }

    public void setCurrentTask(@Nullable ActorTask newTask) {
        debug("Switching task '" + currentTask + "' -> '" + newTask + "'");
        currentTask = newTask;
        if (newTask != null) {
            newTask.start();
        }
    }

    public String getId() {
        return uTransnetAccount.getId();
    }


    protected boolean checkNewOperations() {
        String[] lastOperationIdWrapper = {this.lastOperationId};
        boolean newOperations = checkNewOperations(uTransnetAccount, lastOperationIdWrapper);
        lastOperationId = lastOperationIdWrapper[0];
        return newOperations;
    }

    protected boolean checkNewOperations(UserAccount uTransnetAccount, String[] lastOperationId) {
        Optional<? extends BaseOperation> lastOperation = uTransnetAccount.getLastOperation();
        if (lastOperation.isPresent()) {
            BaseOperation operation = lastOperation.get();
            if (!Objects.equals(operation.getId(), lastOperationId[0])) {
                lastOperationId[0] = operation.getId();
                return true;
            }
        }
        return false;
    }

    protected final void addOperationListener(OperationListener operationListener) {
        operationListeners.add(operationListener);
    }

    public final void removeOperationListener(String name) {
        operationListeners.removeIf(listener -> Objects.equals(listener.getName(), name));
    }

    public final void addEventListener(EventListener operationListener) {
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

    protected void addTask(ActorTask task) {
        tasksQueue.offer(task);
    }


    protected void setUTransnetAccount(@NonNull UserAccount uTransnetAccount) {
        this.uTransnetAccount = uTransnetAccount;
        uTransnetAccount.getLastOperation().ifPresent(baseOperation -> lastOperationId = baseOperation.getId());
    }


    protected void info(String msg) {
        logger().info("[" + uTransnetAccount.getName() + "]: " + msg);
    }

    protected void debug(String msg) {
        logger().debug("[" + uTransnetAccount.getName() + "]: " + msg);
    }

    protected void error(String msg, Exception e) {
        logger().error("[" + uTransnetAccount.getName() + "]: " + msg, e);
    }

    protected void warn(String msg) {
        logger().warn("[" + uTransnetAccount.getName() + "]: " + msg);
    }

    protected void trace(String msg) {
        logger().trace("[" + uTransnetAccount.getName() + "]: " + msg);
    }

    protected Logger logger() {
        return log;
    }

    public UserAccount findAccountFromReservation(UserAccount userAccount) {
        return getExternalAPI().getAccountByName(userAccount.getName().replace("-reserve", ""));
    }

    public UserAccount findReservationFromAccount(UserAccount userAccount) {
        return getExternalAPI().getAccountByName(userAccount.getName() + "-reserve");
    }
}
