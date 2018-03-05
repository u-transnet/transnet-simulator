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
import lombok.AccessLevel;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.Nullable;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Created by Artem on 31.01.2018.
 */
@Slf4j
public class CheckPoint extends BaseInfObject {


    private final APIObjectFactory apiObjectFactory;
    @Getter(AccessLevel.PROTECTED)
    private UserAccount reservation;
    private String lastOperationOnReserve = null;
    private AssetAmount railCarFee;
    @Getter(AccessLevel.PROTECTED)
    private boolean gateClosed;

    public CheckPoint(ExternalAPI externalAPI, APIObjectFactory apiObjectFactory) {
        super(externalAPI);
        this.apiObjectFactory = apiObjectFactory;
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
    }

    private void makeReservation(OperationEvent event) {
        Proposal proposal = ((OperationEvent.ProposalCreateEvent) event).getObject();
        BaseOperation proposedOperation = proposal.getOperation();
        if (proposedOperation.getOperationType() == OperationType.TRANSFER) {
            TransferOperation operation = (TransferOperation) proposedOperation;
            if (routeMapIdsToServe().contains(operation.getMemo())) {
                getUTransnetAccount().sendAsset(reservation, railCarFee, operation.getMemo() + "/" + operation.getFrom());
            }
        }
    }

    private void createRailCarFlow(OperationEvent event) {
        Proposal proposal = ((OperationEvent.ProposalCreateEvent) event).getObject();
        BaseOperation proposedOperation = proposal.getOperation();
        if (proposedOperation.getOperationType() == OperationType.TRANSFER) {
            ActorTask firstTask = null;
            TransferOperation operation = (TransferOperation) proposedOperation;
            if (operation.getFrom().equals(reservation)) {
                if (paidRouteMapIds().contains(operation.getMemo())) {
                    //client has already paid
                    firstTask = ActorTask.builder()
                            .name("allow-entrance")
                            .executor(this)
                            .context(new ActorTaskContext(1))
                            .onStart(context -> context.addPayload("proposal", proposal))
                            .onEnd(this::allowEntrance)
                            .build();
                } else if (routeMapIdsToServe().contains(operation.getMemo())) {
                    //client hasn't paid yet
                    ActorTask.builder()
                            .name("allow-entrance")
                            .executor(this)
                            .context(new ActorTaskContext(
                                    OperationEvent.Type.TRANSFER,
                                    this::waitPaymentFromClient
                            ))
                            .onStart(context -> context.addPayload("proposal", proposal))
                            .onEnd(this::allowEntrance)
                            .build();
                }

                if (firstTask != null) {
                    firstTask.createNext()
                            .name("ask-payment-from-rail-car")
                            .executor(this)
                            .context(new ActorTaskContext(1))
                            .onEnd(this::askPaymentFromRailCar)
                            .build()
                            .createNext()
                            .name("close-gate")
                            .context(new ActorTaskContext(
                                    OperationEvent.Type.TRANSFER,
                                    this::checkIfRailCarApproveProposal
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
            getExternalAPI().sendProposal(
                    railCar,
                    getUTransnetAccount(),
                    railCar,
                    getUTransnetAccount(),
                    railCarFee.getAsset(),
                    railCarFee.getAmount()
            );
        }
    }

    private void allowEntrance(ActorTaskContext context) {
        Proposal proposal = context.getPayload("proposal");
        openGate();
        getUTransnetAccount().approveProposal(proposal);
    }

    private boolean checkIfRailCarApproveProposal(ActorTaskContext context, OperationEvent event) {
        TransferOperation operation = ((OperationEvent.TransferEvent) event).getObject();
        if (operation.getFrom().equals(getCurrentRailCar())) {
//            if(paidRouteMapIds().contains(operation.getMemo())){ TODO: UK-23
            return true;
//            }
        }
        return false;
    }

    @Nullable
    protected UserAccount getCurrentRailCar() {
        List<UserAccount> allRailCars = reservation.getProposals()
                .stream()
                .map(Proposal::getOperation)
                .filter(operation -> operation.getOperationType() == OperationType.TRANSFER)
                .map(operation -> (TransferOperation) operation)
                .map(TransferOperation::getTo)
                .filter(userAccount -> !userAccount.equals(reservation))
                .collect(Collectors.toList());

        List<UserAccount> wentOutRailCars = getUTransnetAccount().getTransfers()
                .stream()
                .filter(op -> op.getTo().equals(getUTransnetAccount()))
                .map(TransferOperation::getFrom)
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

    protected void openGate() {
        gateClosed = false;
    }

    protected void closeGate() {
        gateClosed = true;
    }

    @Override
    protected void setUTransnetAccount(UserAccount uTransnetAccount) {
        super.setUTransnetAccount(uTransnetAccount);
        reservation = getExternalAPI().getAccountByName(getUTransnetAccount().getName() + "-reserve");
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
}
