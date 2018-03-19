package com.github.utransnet.simulator.actors;

import com.github.utransnet.simulator.actors.task.*;
import com.github.utransnet.simulator.externalapi.*;
import com.github.utransnet.simulator.externalapi.operations.BaseOperation;
import com.github.utransnet.simulator.externalapi.operations.MessageOperation;
import com.github.utransnet.simulator.externalapi.operations.OperationType;
import com.github.utransnet.simulator.externalapi.operations.TransferOperation;
import com.github.utransnet.simulator.logging.ActionLogger;
import com.github.utransnet.simulator.logging.LoggedAction;
import com.github.utransnet.simulator.route.RouteMap;
import com.github.utransnet.simulator.route.RouteMapFactory;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.springframework.util.Assert;

import javax.annotation.PostConstruct;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Created by Artem on 31.01.2018.
 */
@Slf4j
public class RailCar extends BaseInfObject {

    private final RouteMapFactory routeMapFactory;
    private final APIObjectFactory apiObjectFactory;
    private final ActionLogger actionLogger;
    private final String delayedStopNameWaitClientPayment = "client-payment";
    private final String namedelayedStopBeforeCheckPoint = "stop-before-check-point";

    private String lastOperationOnReserve = null;
    private AssetAmount checkPointFee;

    private UserAccount currentCheckPoint;
    @Getter(AccessLevel.PROTECTED)
    private UserAccount reservation;

    private int avgSpeed = 60;
    private int safeDistance = 60;
    @Setter(AccessLevel.PROTECTED)
    @Getter(AccessLevel.PROTECTED)
    private RouteMap routeMap; //TODO: get RouteMap from blockchain
    @Getter(AccessLevel.PROTECTED)
    private boolean isMoving = false;
    @Getter(AccessLevel.PROTECTED)
    private boolean isDoorsClosed = true;

    public RailCar(ExternalAPI externalAPI, RouteMapFactory routeMapFactory, APIObjectFactory apiObjectFactory, ActionLogger actionLogger) {
        super(externalAPI);
        this.routeMapFactory = routeMapFactory;
        this.apiObjectFactory = apiObjectFactory;
        this.actionLogger = actionLogger;
    }

    @PostConstruct
    private void init() {
        addEventListener(new EventListener("wait-order-from-station",
                OperationEvent.Type.MESSAGE,
                event -> {
                    MessageOperation messageOperation = ((OperationEvent.MessageEvent) event).getObject();
                    if (messageOperation.getTo().equals(getUTransnetAccount())) {
                        RouteMap routeMap = routeMapFactory.fromJson(messageOperation.getMessage());
                        if (routeMap != null) {
                            makeReservation(routeMap);
                            addTasksOnStation();
                        }
                    }
                }));
        checkPointFee = apiObjectFactory.getAssetAmount("RA", 10);
    }

    @Override
    protected void setUTransnetAccount(UserAccount uTransnetAccount) {
        super.setUTransnetAccount(uTransnetAccount);
        reservation = getExternalAPI().getAccountByName(getUTransnetAccount().getName() + "-reserve");
    }

    protected void addTasksOnStation() {
        ActorTask payForOrder = ActorTask.builder()
                .name("pay-for-order")
                .executor(this)
                .context(new ActorTaskContext(1))
                .onEnd(this::payForOrder)
                .build();
        // TODO: check if cleint payed before
       /* payForOrder.createNext()
                .name("check-payment-from-client")
                .executor(this)
                .context(new ActorTaskContext(1))
                .onEnd(context -> {
                    UserAccount client = getClient();
                    Assert.notNull(
                            client,
                            "[" + getUTransnetAccount().getName() +
                                    "]: Client can't be null when we are waiteng payment");
                    getUTransnetAccount().getTransfersFrom(client).stream().filter(op -> op.getMemo())
                })*/
        payForOrder.createNext()
                .name("wait-payment-from-station")
                .executor(this)
                .context(new ActorTaskContext(
                        OperationEvent.Type.TRANSFER,
                        this::waitPaymentFromStation
                ))
                .onEnd(this::openDoors)
                .build()

                .createNext()
                .name("wait-payment-from-client")
                .executor(this)
                .context(new ActorTaskContext(
                        OperationEvent.Type.TRANSFER,
                        this::waitPaymentFromClient
                ))
                .onEnd(this::closeDoors)
                .build()

                .createNext()
                .name("start-movement")
                .executor(this)
                .onStart(this::start)
                .context(new ActorTaskContext(1))
                .onEnd(this::startMovement)
                .build();
        addTask(payForOrder);
    }

    protected void askNextCheckPoint(UserAccount nextCheckPoint) {
        ActorTask requestPassFromCheckPoint = ActorTask.builder()
                .name("request-pass-from-check-point")
                .executor(this)
                .context(new ActorTaskContext(1))
                .onEnd(context -> requestPassFromCheckPoint(context, nextCheckPoint))
                .build();
        addTask(requestPassFromCheckPoint);

        requestPassFromCheckPoint.createNext()
                .name("wait-accept-from-check-point")
                .executor(this)
                .context(new ActorTaskContext(
                        OperationEvent.Type.TRANSFER,
                        (context, event) -> waitAcceptFromCheckPoint(context, event, nextCheckPoint)
                ))
                .onEnd(this::goIntoCheckPoint)
                .build();
    }

    protected void leaveAndGoToNextCP() {
        ActorTask leaveCheckpoint = ActorTask.builder()
                .name("leave-check-point")
                .executor(this)
                .context(new ActorTaskContext(10))
                .onEnd(this::leaveCheckPoint)
                .onCancel(context -> {
                    stop();
                    ActorTask waitProposalFromCheckPoint = ActorTask.builder()
                            .name("wait-proposal-from-check-point")
                            .executor(this)
                            .context(new ActorTaskContext(
                                    OperationEvent.Type.PROPOSAL_CREATE,
                                    this::waitProposalFromCheckPoint
                            ))
                            .onEnd(this::leaveCheckPoint)
                            .build();
                    waitProposalFromCheckPoint.createNext()
                            .name("request-payment-from-client")
                            .executor(this)
                            .onStart(this::requestPaymentFromClient)
                            .context(new ActorTaskContext(1))
                            .onEnd(this::startMovement)
                            .build();
                    addTask(waitProposalFromCheckPoint);
                })
                .build();
        addTask(leaveCheckpoint);

        leaveCheckpoint
                .createNext()
                .name("request-payment-from-client")
                .executor(this)
                .onStart(context -> System.out.println(context.getWaitSeconds()))
                .onStart(this::requestPaymentFromClient)
                .context(new ActorTaskContext(1))
                .onEnd(this::startMovement)
                .build();
    }

    @Override
    public void update(int seconds) {
        super.update(seconds);
        String lastOperationId = this.lastOperationOnReserve;
        String[] lastOperationIdWrapper = {lastOperationOnReserve};
        if (checkNewOperations(reservation, lastOperationIdWrapper)) {
            lastOperationOnReserve = lastOperationIdWrapper[0];
            getExternalAPI().operationsAfter(reservation, lastOperationId)
                    .forEach(this::processEachOperation);
        }
    }

    @NotNull
    private Boolean waitProposalFromCheckPoint(ActorTaskContext context, OperationEvent event) {
        Proposal proposal = ((OperationEvent.ProposalCreateEvent) event).getObject();
        return checkIfProposalFromCheckPoint(proposal);
    }

    protected void makeReservation(RouteMap routeMap) {
        this.routeMap = routeMap;
        routeMap.getRoute().forEach(routeNode -> {
            getUTransnetAccount().sendAsset(
                    reservation,
                    getFeeAmount(routeNode.getId()),
                    routeMap.getId() + "/" + routeNode.getId()
            );
        });
    }

    private AssetAmount getFeeAmount(String checkPointId) {
        return checkPointFee;
    }


    private void payForOrder(ActorTaskContext context) {
        UserAccount stationAccount = getStationAccount();
        Assert.notNull(
                stationAccount,
                "[" + getUTransnetAccount().getName() + "]: Can't pay if Station is unknown"
        );
        getUTransnetAccount().sendAsset(stationAccount, getFeeAmount(stationAccount.getId()), routeMap.getId());

    }

    private boolean waitPaymentFromStation(ActorTaskContext context, OperationEvent event) {
        TransferOperation operation = ((OperationEvent.TransferEvent) event).getObject();
        return Objects.equals(operation.getFrom(), getStationAccount());
    }

    private boolean waitPaymentFromClient(@Nullable ActorTaskContext context, OperationEvent event) {
        TransferOperation operation = ((OperationEvent.TransferEvent) event).getObject();
        return Objects.equals(operation.getFrom().getId(), getClientId());
    }

    private void startMovement(ActorTaskContext context) {
        currentCheckPoint = routeMap.getNextAccount();
        if (routeMap.goNext()) {
            askNextCheckPoint(routeMap.getNextAccount());
            leaveAndGoToNextCP();
            createEmergencyStop(namedelayedStopBeforeCheckPoint, routeMap.getNextDistance());
        } else {
            info("Arrived at destination station");
            getUTransnetAccount().sendMessage(currentCheckPoint, "FREE");
            openDoors(context);
        }
    }


    private void requestPassFromCheckPoint(ActorTaskContext context, UserAccount nextCheckPoint) {
        AssetAmount feeAmount = getFeeAmount(nextCheckPoint.getId());
        UserAccount client = getClient();
        Assert.notNull(
                client,
                "[" + getUTransnetAccount().getName() +
                        "]: Client can't be null when we are entering checkpoint"
        );

        info("ask payment for next checkpoint");
        getExternalAPI().sendProposal(
                client,
                nextCheckPoint,
                client,
                getUTransnetAccount(),
                routeMap.getNextFee(),
                routeMap.getId()
        );

        info("request pass from next check point");
        getExternalAPI().sendProposal(
                getExternalAPI().getAccountByName(nextCheckPoint.getId() + "-reserve"),
                getUTransnetAccount(),
                nextCheckPoint,
                getUTransnetAccount(),
                feeAmount,
                routeMap.getId()
        );
    }

    private boolean waitAcceptFromCheckPoint(ActorTaskContext context, OperationEvent event, UserAccount nextCheckPoint) {
        TransferOperation operation = ((OperationEvent.TransferEvent) event).getObject();
        UserAccount nextCPReserve = getExternalAPI().getAccountByName(nextCheckPoint.getId() + "-reserve");
        if (operation.getFrom().equals(nextCPReserve)
                && operation.getMemo().equals(routeMap.getId())) {
            info("Received acceptance from check point '" + nextCheckPoint.getName() + "'");
            return true;
        } else {
            return false;
        }
    }

    private void checkProposalFromCheckPoint(ActorTaskContext context) {
        List<Proposal> proposals = getExternalAPI().getAccountProposals(reservation)
                .stream()
                .filter(this::checkIfProposalFromCheckPoint)
                .collect(Collectors.toList());
        if (proposals.isEmpty()) {
            stop();
        }
    }

    private void leaveCheckPoint(ActorTaskContext context) {
        List<Proposal> proposals = getExternalAPI().getAccountProposals(reservation)
                .stream()
                .filter(this::checkIfProposalFromCheckPoint)
                .collect(Collectors.toList());

        if (proposals.size() != 1) {
            throw new RuntimeException(
                    "[" + getUTransnetAccount().getName() + "]: Can't find proposal from current check point"
            );
        }

        reservation.approveProposal(proposals.get(0));

        start(context);
    }

    private boolean checkIfProposalFromCheckPoint(Proposal proposal) {
        BaseOperation operation = proposal.getOperation();
        if (operation.getOperationType() == OperationType.TRANSFER) {
            TransferOperation transferOperation = (TransferOperation) operation;
            if (transferOperation.getTo().equals(currentCheckPoint)) {
                if (
                        transferOperation.getMemo().equals(routeMap.getId())
                                || transferOperation.getMemo().equals("free-" + getId())
                        ) {
                    return true;
                }
            }
        }
        return false;
    }

    private void goIntoCheckPoint(ActorTaskContext context) {
        if (!routeMap.isFinished()) {
            info("Entering check point");
            removeDelayedAction(namedelayedStopBeforeCheckPoint);

            // ask Client to pay to next check point
            String clientId = getClientId();
            Assert.notNull(
                    clientId,
                    "[" + getUTransnetAccount().getName() + "]: Can't request payment if Client is unknown"
            );
            UserAccount client = getExternalAPI().getAccountById(clientId);
            getUTransnetAccount().sendMessage(
                    client,
                    routeMap.getId() + "/" + routeMap.getRoute().get(routeMap.getStep() + 1).getId()
            );
            start(context);
        }
    }

    private void requestPaymentFromClient(ActorTaskContext context) {
        int nextStep = routeMap.getStep() + 1;
        if (nextStep > routeMap.getRoute().size()) {
            createEmergencyStop(delayedStopNameWaitClientPayment, routeMap.getRoute().get(nextStep).getDistance());
        }
        String clientId = getClientId();
        Assert.notNull(
                clientId,
                "[" + getUTransnetAccount().getName() + "]: Can't request payment if Client is unknown"
        );
        UserAccount client = getExternalAPI().getAccountById(clientId);


        info("ask payment for traveled distance between previous and current check point");
        getExternalAPI().sendProposal(
                client,
                getUTransnetAccount(),
                client,
                getUTransnetAccount(),
                routeMap.getNextRailCarFee(),
                routeMap.getId() + "/" + routeMap.getNextAccount().getId()
        );
        addEventListener(new EventListener(
                "wait-payment-from-client",
                OperationEvent.Type.TRANSFER,
                event -> {
                    if (waitPaymentFromClient(null, event)) {
                        removeDelayedAction(delayedStopNameWaitClientPayment);
                        removeEventListener("wait-payment-from-client");
                        start(null);
                    }
                }
        ));
    }

    @Nullable
    private UserAccount getStationAccount() {
        if (routeMap != null) {
            return routeMap.getStart();
        }
        return null;
    }

    @Nullable
    private String getClientId() {
        UserAccount stationAccount = getStationAccount();
        if (stationAccount != null) {
            List<TransferOperation> transfers = getUTransnetAccount().getTransfersFrom(stationAccount);
            for (int i = transfers.size() - 1; i >= 0; i++) {
                String[] info = transfers.get(i).getMemo().split("/");
                if (info[0].equals(routeMap.getId())) {
                    return info[1];
                }
            }

        }
        return null;
    }

    @Nullable
    private UserAccount getClient() {
        String clientId = getClientId();
        if (clientId == null) {
            return null;
        } else {
            return getExternalAPI().getAccountById(clientId);
        }
    }

    private void createEmergencyStop(String id, int distanceToCheckPoint) {
        int distanceToMove = distanceToCheckPoint - safeDistance;
        int timeToMove = distanceToMove / avgSpeed;
        addDelayedAction(new DelayedAction(
                this,
                id,
                timeToMove,
                this::stop
        ));
    }

    @LoggedAction
    private void start(@Nullable ActorTaskContext context) {
        if (!isMoving) {
            actionLogger.logActorAction(this, "start", "RailCar '%s' started movement");
            isMoving = true;
        }
    }

    @LoggedAction
    private void stop() {
        if (isMoving) {
            actionLogger.logActorAction(this, "stop", "RailCar '%s' stopped movement");
            isMoving = false;
        }
    }

    @LoggedAction
    private void openDoors(@Nullable ActorTaskContext context) {
        if (isDoorsClosed) {
            actionLogger.logActorAction(this, "openDoors", "RailCar '%s' opened doors");
            isDoorsClosed = false;
        }
    }

    @LoggedAction
    private void closeDoors(@Nullable ActorTaskContext context) {
        if (!isDoorsClosed) {
            actionLogger.logActorAction(this, "closeDoors", "RailCar '%s' closed doors");
            isDoorsClosed = true;
        }
    }

    @Override
    protected Logger logger() {
        return log;
    }
}
