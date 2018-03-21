package com.github.utransnet.simulator.actors;

import com.github.utransnet.simulator.Utils;
import com.github.utransnet.simulator.actors.task.ActorTask;
import com.github.utransnet.simulator.actors.task.ActorTaskContext;
import com.github.utransnet.simulator.actors.task.EventListener;
import com.github.utransnet.simulator.actors.task.OperationEvent;
import com.github.utransnet.simulator.externalapi.*;
import com.github.utransnet.simulator.externalapi.operations.BaseOperation;
import com.github.utransnet.simulator.externalapi.operations.MessageOperation;
import com.github.utransnet.simulator.externalapi.operations.OperationType;
import com.github.utransnet.simulator.externalapi.operations.TransferOperation;
import com.github.utransnet.simulator.logging.ActionLogger;
import com.github.utransnet.simulator.logging.LoggedAction;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Created by Artem on 31.01.2018.
 */
@Slf4j
public class CheckPoint extends BaseInfObject {


    private final APIObjectFactory apiObjectFactory;
    private final ActionLogger actionLogger;
    @Getter
    private UserAccount reservation;
    private String lastOperationOnReserve = null;
    private AssetAmount railCarFee;
    @Getter(AccessLevel.PROTECTED)
    private boolean gateClosed = true;

    @Setter
    private UserAccount logist;

    public CheckPoint(ExternalAPI externalAPI, APIObjectFactory apiObjectFactory, ActionLogger actionLogger) {
        super(externalAPI);
        this.apiObjectFactory = apiObjectFactory;
        this.actionLogger = actionLogger;
    }

    @PostConstruct
    private void init() {
        addEventListener(new EventListener(
                "accept-route-map",
                OperationEvent.Type.MESSAGE,
                this::acceptRouteMap
        ));

        addEventListener(new EventListener(
                "make-resevation",
                OperationEvent.Type.PROPOSAL_CREATE,
                this::makeReservation
        ));

        addEventListener(new EventListener(
                "create-rail-car-flow",
                OperationEvent.Type.PROPOSAL_CREATE,
                this::createRailCarFlow
        ));

        railCarFee = apiObjectFactory.getAssetAmount("RA", 10);
    }


    private void acceptRouteMap(OperationEvent event) {
        MessageOperation messageOperation = ((OperationEvent.MessageEvent) event).getObject();
        info("Received upcoming RouteMap id: " + messageOperation.getMessage());
    }

    private void makeReservation(OperationEvent event) {
        Proposal proposal = ((OperationEvent.ProposalCreateEvent) event).getObject();
        BaseOperation proposedOperation = proposal.getOperation();
        if (proposedOperation.getOperationType() == OperationType.TRANSFER) {
            TransferOperation operation = (TransferOperation) proposedOperation;
            if (proposal.getFeePayer().equals(logist)) {
                if (routeMapIdsToServe().contains(operation.getMemo())) {
                    info("Making reservation for '" + operation.getMemo()
                            + "/" + operation.getFrom().getId() + "'");
                    getUTransnetAccount().sendAsset(
                            reservation,
                            railCarFee,
                            operation.getMemo() + "/" + operation.getFrom().getId()
                    );
                }
            }
        }
    }

    private void createRailCarFlow(OperationEvent event) {
        Proposal proposal = ((OperationEvent.ProposalCreateEvent) event).getObject();
        BaseOperation proposedOperation = proposal.getOperation();
        if (proposedOperation.getOperationType() == OperationType.TRANSFER) {
            debug("Received proposal for " + proposedOperation);
            ActorTask firstTask = null;
            TransferOperation operation = (TransferOperation) proposedOperation;
            if (operation.getFrom().equals(reservation)) {
                info("Creating RailCar flow");
                if (paidRouteMapIds().contains(operation.getMemo())) {
                    debug("client has already paid");
                    //client has already paid
                    firstTask = ActorTask.builder()
                            .name("allow-entrance")
                            .executor(this)
                            .context(new ActorTaskContext(1))
                            .onStart(context -> context.addPayload("proposal", proposal))
                            .onEnd(this::allowEntrance)
                            .build();
                } else if (routeMapIdsToServe().contains(operation.getMemo())) {
                    debug("client hasn't paid yet");
                    //client hasn't paid yet
                    firstTask = ActorTask.builder()
                            .name("wait-and-allow-entrance")
                            .executor(this)
                            .context(new ActorTaskContext(
                                    OperationEvent.Type.TRANSFER,
                                    this::waitPaymentFromClient
                            ))
                            .onStart(context -> context.addPayload("proposal", proposal))
                            .onEnd(this::allowEntrance)
                            .build();
                } else {
                    debug("Unrecognized RouteMapId: " + operation.getMemo());
                }

                if (firstTask != null) {
                    addTask(firstTask);
                    firstTask.createNext()
                            .name("ask-payment-from-rail-car")
                            .executor(this)
                            .context(new ActorTaskContext(1))
                            .onEnd(this::askPaymentFromRailCar)
                            .build()
                            .createNext()
                            .name("wait-railc-car-exit-and-close-gate")
                            .executor(this)
                            .context(new ActorTaskContext(
                                    OperationEvent.Type.TRANSFER,
                                    this::checkIfRailCarApprovedProposal
                            ))
                            .onEnd(context -> closeGate())
                            .build();
                }
            }
        }
    }

    private boolean waitPaymentFromClient(ActorTaskContext context, OperationEvent event) {
        TransferOperation operation = ((OperationEvent.TransferEvent) event).getObject();
        return routeMapIdsToServe().contains(operation.getMemo());
    }

    private void askPaymentFromRailCar(ActorTaskContext context) {
        UserAccount railCar = getCurrentRailCar();
        if (railCar != null) {
            context.addPayload("rail-car", railCar);
            context.addPayload("rail-car-reserve", getExternalAPI().getAccountByName(railCar.getId() + "-reserve"));
            String routeMapId = getCurrentRouteMapId();
            if (routeMapId != null) {
                getExternalAPI().sendProposal(
                        getExternalAPI().getAccountByName(railCar.getId() + "-reserve"),
                        getUTransnetAccount(),
                        getUTransnetAccount(),
                        railCarFee,
                        routeMapId
                );
            } else {
                throw new RuntimeException(
                        "[" + getUTransnetAccount().getName() + "]: can't find current route map id"
                );
            }
        } else {
            throw new RuntimeException(
                    "[" + getUTransnetAccount().getName() + "]: can't find current rail car to ask for payment"
            );
        }
    }

    private void allowEntrance(ActorTaskContext context) {
        Proposal proposal = context.getPayload("proposal");
        openGate();
        reservation.approveProposal(proposal);
    }

    private boolean checkIfRailCarApprovedProposal(ActorTaskContext context, OperationEvent event) {
        TransferOperation operation = ((OperationEvent.TransferEvent) event).getObject();
        if (operation.getFrom().equals(context.getPayload("rail-car-reserve"))) {
            if (paidRouteMapIds().contains(operation.getMemo())) {
                return true;
            }
        }
        return false;
    }

    @Nullable
    public UserAccount getCurrentRailCar() {
        List<UserAccount> allRailCars = reservation.getProposals()
                .stream()
                .map(Proposal::getOperation)
                .filter(operation -> operation.getOperationType() == OperationType.TRANSFER)
                .map(operation -> (TransferOperation) operation)
                .map(TransferOperation::getTo)
                .filter(userAccount -> !userAccount.equals(reservation))
                .collect(Collectors.toList());

        List<UserAccount> wentOutRailCars = getUTransnetAccount().getProposals()
                .stream()
                .filter(Proposal::approved)
                .map(Proposal::getOperation)
                .filter(operation -> operation.getOperationType() == OperationType.TRANSFER)
                .map(operation -> (TransferOperation) operation)
                .filter(op -> op.getTo().equals(getUTransnetAccount()))
                .map(TransferOperation::getFrom)
                .map(this::findAccountFromReservation)
                .collect(Collectors.toList());

        List<UserAccount> railCarsInCheckPoint = new ArrayList<>(3);
        allRailCars.forEach(userAccount -> {
            if (wentOutRailCars.contains(userAccount)) {
                wentOutRailCars.remove(userAccount);
            } else {
                railCarsInCheckPoint.add(userAccount);
            }
        });
        if (railCarsInCheckPoint.size() == 0) {
            return null; // no current rail car
        }
        if (railCarsInCheckPoint.size() > 1) {
            log.warn("[" + getUTransnetAccount().getName() + "]: there are "
                    + railCarsInCheckPoint.size() + " cars, that not leaved chack point");
        }
        return Utils.getLast(allRailCars);
    }

    protected List<String> routeMapIdsToServe() {
        return getUTransnetAccount().getMessages()
                .stream()
                .map(MessageOperation::getMessage)
                .collect(Collectors.toList());
    }

    protected List<String> paidRouteMapIds() {
        return getUTransnetAccount().getTransfers()
                .stream()
                .filter(op -> op.getAsset().getId().equals("UTT"))
                .map(TransferOperation::getMemo)
                .collect(Collectors.toList());
    }

    @Nullable
    protected String getCurrentRouteMapId() {
        Optional<TransferOperation> reduce = reservation.getTransfers()
                .stream()
                .filter(op -> op.getFrom().equals(reservation))
                .reduce((first, second) -> second); //get last element
        if (reduce.isPresent()) {
            return reduce.get().getMemo();
        }
        return null;
    }

    @LoggedAction
    private void openGate() {
        actionLogger.logActorAction(this, "checkPointInUse", "CheckPoint '%s' opened gate");
        gateClosed = false;
    }

    @LoggedAction
    private void closeGate() {
        actionLogger.logActorAction(this, "checkPointFree", "CheckPoint '%s' closed gate");
        gateClosed = true;
    }

    @Override
    protected void setUTransnetAccount(UserAccount uTransnetAccount) {
        super.setUTransnetAccount(uTransnetAccount);
        reservation = getExternalAPI().getAccountByName(getUTransnetAccount().getId() + "-reserve");
    }

   /* @Override
    public void update(int seconds) {
        super.update(seconds);
        String lastOperationId = this.lastOperationOnReserve;
        String[] lastOperationIdWrapper = {lastOperationOnReserve};
        if (checkNewOperations(reservation, lastOperationIdWrapper)) {
            lastOperationOnReserve = lastOperationIdWrapper[0];
            getExternalAPI().operationsAfter(reservation, lastOperationId)
                    .forEach(this::processEachOperation);
        }
    }*/

    @Override
    protected Logger logger() {
        return log;
    }
}
