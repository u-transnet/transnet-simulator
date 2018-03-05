package com.github.utransnet.simulator.actors;

import com.github.utransnet.simulator.actors.task.*;
import com.github.utransnet.simulator.externalapi.*;
import com.github.utransnet.simulator.externalapi.operations.BaseOperation;
import com.github.utransnet.simulator.externalapi.operations.MessageOperation;
import com.github.utransnet.simulator.externalapi.operations.OperationType;
import com.github.utransnet.simulator.externalapi.operations.TransferOperation;
import com.github.utransnet.simulator.route.RouteMap;
import com.github.utransnet.simulator.route.RouteMapFactory;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.util.Assert;

import javax.annotation.PostConstruct;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Created by Artem on 31.01.2018.
 */
public class RailCar extends BaseInfObject {

    private final RouteMapFactory routeMapFactory;
    private final APIObjectFactory apiObjectFactory;
    private final String delayedStopNameWaitClientPayment = "client-payment";
    private final String namedelayedStopBeforeCheckPoint = "stop-before-check-point";

    String lastOperationOnReserve = null;
    private AssetAmount checkPointFee;

    private UserAccount nextCheckPoint;
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

    public RailCar(ExternalAPI externalAPI, RouteMapFactory routeMapFactory, APIObjectFactory apiObjectFactory) {
        super(externalAPI);
        this.routeMapFactory = routeMapFactory;
        this.apiObjectFactory = apiObjectFactory;
    }

    @PostConstruct
    private void init() {
        addEventListener(new EventListener("wait-order-from-station",
                OperationEvent.Type.MESSAGE,
                event -> {
                    MessageOperation messageOperation = ((OperationEvent.MessageEvent) event).getObject();
                    RouteMap routeMap = routeMapFactory.fromJson(messageOperation.getMessage());
                    makeReservation(routeMap);
                    addTasksOnStation();
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

    protected void addTasksWithCheckPoint() {
        ActorTask requestPassFromCheckPoint = ActorTask.builder()
                .name("request-pass-from-check-point")
                .executor(this)
                .context(new ActorTaskContext(1))
                .onEnd(this::requestPassFromCheckPoint)
                .build();

        requestPassFromCheckPoint.createNext()
                .name("wait-accept-from-check-point")
                .executor(this)
                .context(new ActorTaskContext(
                        OperationEvent.Type.TRANSFER,
                        this::waitAcceptFromCheckPoint
                ))
                .onEnd(this::goIntoCheckPoint)
                .build()
                .createNext()
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
                .build()
                .createNext()
                .name("request-payment-from-client")
                .executor(this)
                .onStart(context -> System.out.println(context.getWaitSeconds()))
                .onStart(this::requestPaymentFromClient)
                .context(new ActorTaskContext(1))
                .onEnd(this::startMovement)
                .build();
        addTask(requestPassFromCheckPoint);
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
        if (routeMap.goNext()) {
            nextCheckPoint = routeMap.getNextAccount();
            createEmergencyStop(namedelayedStopBeforeCheckPoint, routeMap.getNextDistance());
            addTasksWithCheckPoint();
        }
    }


    private void requestPassFromCheckPoint(ActorTaskContext context) {
        AssetAmount feeAmount = getFeeAmount(nextCheckPoint.getId());
        getExternalAPI().sendProposal(
                nextCheckPoint,
                getUTransnetAccount(),
                nextCheckPoint,
                getUTransnetAccount(),
                feeAmount.getAsset(),
                feeAmount.getAmount()
        );
    }

    private boolean waitAcceptFromCheckPoint(ActorTaskContext context, OperationEvent event) {
        TransferOperation operation = ((OperationEvent.TransferEvent) event).getObject();
        return true;
//        return operation.getFrom().equals(nextCheckPoint) && operation.getMemo().equals(routeMap.getId()); TODO: add memo to proposal
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
            if (transferOperation.getTo().equals(nextCheckPoint)) {
//                if (transferOperation.getMemo().equals(routeMap.getId())) { TODO add memo to proposal
                return true;
//                }
            }
        }
        return false;
    }

    private void goIntoCheckPoint(ActorTaskContext context) {
        removeDelayedAction(namedelayedStopBeforeCheckPoint);
        start(context);
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

        getExternalAPI().sendProposal(
                client,
                getUTransnetAccount(),
                client,
                getUTransnetAccount(),
                routeMap.getNextRailCarFee().getAsset(),
                routeMap.getNextRailCarFee().getAmount()
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

    private void start(@Nullable ActorTaskContext context) {
        isMoving = true;
    }

    private void stop() {
        isMoving = false;
    }

    private void openDoors(@Nullable ActorTaskContext context) {
        isDoorsClosed = false;
    }

    private void closeDoors(@Nullable ActorTaskContext context) {
        isDoorsClosed = true;
    }
}
