package com.github.utransnet.simulator.actors;

import com.github.utransnet.simulator.actors.task.*;
import com.github.utransnet.simulator.externalapi.*;
import com.github.utransnet.simulator.externalapi.operations.BaseOperation;
import com.github.utransnet.simulator.externalapi.operations.MessageOperation;
import com.github.utransnet.simulator.externalapi.operations.OperationType;
import com.github.utransnet.simulator.externalapi.operations.TransferOperation;
import com.github.utransnet.simulator.route.RouteMap;
import com.github.utransnet.simulator.route.RouteMapFactory;
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

    private UserAccount reservation;
    private AssetAmount checkPointFee;

    private UserAccount nextCheckPoint;

    private RouteMap routeMap; //TODO: get RouteMap from blockchain
    private int avgSpeed = 60;
    private int safeDistance = 60;

    public RailCar(ExternalAPI externalAPI, RouteMapFactory routeMapFactory, APIObjectFactory apiObjectFactory) {
        super(externalAPI);
        this.routeMapFactory = routeMapFactory;
        this.apiObjectFactory = apiObjectFactory;
    }

    @PostConstruct
    private void init() {
        addOperationListener(new OperationListener(
                "wait-order-from-station",
                OperationType.MESSAGE,
                operation -> {
                    MessageOperation messageOperation = (MessageOperation) operation;
                    RouteMap routeMap = routeMapFactory.fromJson(messageOperation.getMessage());
                    makeReservation(routeMap);
                    addTasksOnStation();
                }
        ));
        reservation = getExternalAPI().getAccountByName(getUTransnetAccount().getName() + "-reserve");
        checkPointFee = apiObjectFactory.getAssetAmount("RA", 10);
    }

    private void addTasksOnStation() {
        ActorTask payForOrder = ActorTask.builder()
                .name("pay-for-order")
                .executor(this)
                .context(new ActorTaskContext(0))
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
                .onStart(this::start)
                .context(new ActorTaskContext(1))
                .onEnd(this::startMovement)
                .build();
        addTask(payForOrder);
    }

    private void addTasksWithCheckPoint() {
        ActorTask requestPassFromChackPoint = ActorTask.builder()
                .name("request-pass-from-check-point")
                .executor(this)
                .context(new ActorTaskContext(1))
                .onEnd(this::requestPassFromCheckPoint)
                .build();

        requestPassFromChackPoint.createNext()
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
                .context(new ActorTaskContext(1))
                .onEnd(this::leaveCheckPoint)
                .build()
                .createNext()
                .name("request-payment-from-client")
                .executor(this)
                .onStart(this::requestPaymentFromClient)
                .context(new ActorTaskContext(1))
                .onEnd(this::startMovement)
                .build();
        addTask(requestPassFromChackPoint);
    }

    private void makeReservation(RouteMap routeMap) {
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

    private boolean waitPaymentFromClient(ActorTaskContext context, OperationEvent event) {
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
        return operation.getFrom().equals(nextCheckPoint) && operation.getMemo().equals(routeMap.getId());
    }

    private void leaveCheckPoint(ActorTaskContext context) {
        List<Proposal> proposals = getExternalAPI().getAccountProposals(reservation)
                .stream()
                .filter(proposal -> {
                    BaseOperation operation = proposal.getOperation();
                    if (operation.getOperationType() == OperationType.TRANSFER) {
                        TransferOperation transferOperation = (TransferOperation) operation;
                        if (transferOperation.getTo().equals(nextCheckPoint)) {
                            if (transferOperation.getMemo().equals(routeMap.getId())) {
                                return true;
                            }
                        }
                    }
                    return false;
                }).collect(Collectors.toList());

        if (proposals.size() != 1) {
            throw new RuntimeException(
                    "[" + getUTransnetAccount().getName() + "]: Can't find proposal from current chek point"
            );
        }

        getUTransnetAccount().approveProposal(proposals.get(0));

        addTask(ActorTask.builder()
                .name("wait-payment-from-client")
                .executor(this)
                .onStart(this::requestPaymentFromClient)
                .context(new ActorTaskContext(
                        OperationEvent.Type.TRANSFER,
                        this::waitPaymentFromClient
                ))
                .onEnd(context1 -> removeDelayedAction(delayedStopNameWaitClientPayment))
                .build());
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

    private void start(ActorTaskContext context) {

    }

    private void stop() {

    }

    private void openDoors(ActorTaskContext context) {

    }

    private void closeDoors(ActorTaskContext context) {

    }
}
