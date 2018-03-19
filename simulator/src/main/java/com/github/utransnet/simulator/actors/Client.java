package com.github.utransnet.simulator.actors;

import com.github.utransnet.simulator.Utils;
import com.github.utransnet.simulator.actors.factory.Actor;
import com.github.utransnet.simulator.actors.task.ActorTask;
import com.github.utransnet.simulator.actors.task.ActorTaskContext;
import com.github.utransnet.simulator.actors.task.EventListener;
import com.github.utransnet.simulator.actors.task.OperationEvent;
import com.github.utransnet.simulator.externalapi.AssetAmount;
import com.github.utransnet.simulator.externalapi.ExternalAPI;
import com.github.utransnet.simulator.externalapi.Proposal;
import com.github.utransnet.simulator.externalapi.UserAccount;
import com.github.utransnet.simulator.externalapi.operations.BaseOperation;
import com.github.utransnet.simulator.externalapi.operations.MessageOperation;
import com.github.utransnet.simulator.externalapi.operations.OperationType;
import com.github.utransnet.simulator.externalapi.operations.TransferOperation;
import com.github.utransnet.simulator.logging.ActionLogger;
import com.github.utransnet.simulator.logging.LoggedAction;
import com.github.utransnet.simulator.route.RouteMap;
import com.github.utransnet.simulator.route.RouteMapFactory;
import lombok.Setter;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.springframework.util.Assert;

import javax.annotation.PostConstruct;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;


/**
 * Created by Artem on 31.01.2018.
 */
@Slf4j
public class Client extends Actor {
    private final RouteMapFactory routeMapFactory;

    private final ActionLogger actionLogger;

    @Setter
    private String logistName;

    @Setter
    private AssetAmount routeMapPrice;

    public Client(ExternalAPI externalAPI, RouteMapFactory routeMapFactory, ActionLogger actionLogger) {
        super(externalAPI);
        this.routeMapFactory = routeMapFactory;
        this.actionLogger = actionLogger;
    }

    @PostConstruct
    private void init() {
        ActorTask buyRoputeMapTask = ActorTask.builder()
                .name("buy-route-map")
                .executor(this)
                .context(new ActorTaskContext(
                        OperationEvent.Type.MESSAGE,
                        this::checkReceivedRouteMap
                ))
                .onStart(this::buyRouteMap)
                .build();
        buyRoputeMapTask.createNext()

                .name("buy-trip")
                .executor(this)
                .context(new ActorTaskContext(60))
                .onEnd(this::requestTrip)
                .build()
                .createNext()

                .name("wait-rail-car")
                .executor(this)
                .context(new ActorTaskContext(
                        OperationEvent.Type.PROPOSAL_CREATE,
                        this::waitRailCar
                ))
                .onEnd(this::tellReadyForTrip)
                .build()
                .createNext()

                .name("start-trip")
                .executor(this)
                .context(new ActorTaskContext(600))
                .onEnd(this::tellInRailCar)
                .build()
                .createNext()
                .name("wait-finish-and-leave-rail-car")
                .executor(this)
                .context(new ActorTaskContext(
                        OperationEvent.Type.TRANSFER,
                        this::checkIfPaymentForLastCheckPoint
                ))
                .onEnd(this::exitRailCar)
                .build()
        ;
        addTask(buyRoputeMapTask);
    }

    //region operations with Logist
    private void buyRouteMap(ActorTaskContext context) {
        UserAccount logistAccount = getLogist();
        getUTransnetAccount().sendAsset(logistAccount, routeMapPrice, "");
        info("Request RouteMap from '" + logistAccount.getName() + "'");
    }

    private boolean checkReceivedRouteMap(ActorTaskContext context, OperationEvent event) {
        if (event instanceof OperationEvent.MessageEvent) {
            OperationEvent.MessageEvent messageEvent = (OperationEvent.MessageEvent) event;
            MessageOperation messageOperation = messageEvent.getObject();
            debug("New message: " + messageOperation);
            if (messageOperation.getFrom().equals(getLogist())) {
                String json = messageOperation.getMessage();
                try {
                    RouteMap routeMap = routeMapFactory.fromJsonForce(json);
                    info("Got Route Map: " + routeMap.getId());
                    return true;
                } catch (Exception e) {
                    error("Error in decoding received RouteMap", e);
                }
            }
        }
        return false;
    }
    //endregion


    //region start trip
    @LoggedAction
    private void requestTrip(ActorTaskContext context) {
        actionLogger.logActorAction(this, "requestTrip", "Client '%s' requesting trip");
        RouteMap routeMap = getRouteMap();
        getUTransnetAccount().sendMessage(routeMap.getStart(), routeMapFactory.toJson(routeMap));
        info("Requesting trip from '" + routeMap.getStart().getId() + "'");
    }


    private void payForRoutePart(ActorTaskContext context) {

    }

    private boolean waitRailCar(ActorTaskContext context, OperationEvent event) {
        if (event instanceof OperationEvent.ProposalCreateEvent) {
            OperationEvent.ProposalCreateEvent proposalCreateEvent = (OperationEvent.ProposalCreateEvent) event;
            Proposal proposal = proposalCreateEvent.getObject();
            BaseOperation operation = proposal.getOperation();

            RouteMap routeMap = getRouteMap();

            debug("New proposal: " + proposal);

            if (operation instanceof TransferOperation) {
                TransferOperation transferOperation = (TransferOperation) operation;
                if (transferOperation.getFrom().equals(getUTransnetAccount())
                        && transferOperation.getTo().equals(routeMap.getStart())
                        && Objects.equals(transferOperation.getMemo().split("/")[0], routeMap.getId())) {
                    info("Received approval from Station '" + routeMap.getStart().getName() + "'");
                    return true;
                }
            }
        }
        return false;
    }

    private void tellReadyForTrip(ActorTaskContext context) {
        info("Ready for trip, approving payment to station");
        RouteMap routeMap = getRouteMap();
        List<Proposal> proposalsFromStation = getUTransnetAccount().getProposals()
                .stream()
                .filter(proposal -> {
                    BaseOperation operation = proposal.getOperation();
                    if (operation.getOperationType() == OperationType.TRANSFER) {
                        TransferOperation transferOperation = (TransferOperation) operation;
                        if (
                                transferOperation.getTo().equals(routeMap.getStart())
                                        &&
                                        transferOperation.getFrom().equals(getUTransnetAccount())
                                ) {
                            return true;
                        }
                    }
                    return false;
                })
                .collect(Collectors.toList());
        Proposal proposal = Utils.getLast(proposalsFromStation);
        Assert.notNull(proposal, "proposal must exist");
        getUTransnetAccount().approveProposal(proposal);
    }

    @LoggedAction
    private void tellInRailCar(ActorTaskContext context) {
        actionLogger.logActorAction(this, "clientInRailCar", "Client '%s' entering into rail car");
        RouteMap routeMap = getRouteMap();
        UserAccount railCarAccount = getRailCar();
        if (railCarAccount == null) {
            // exception shouldn't be thrown, because we approve this proposal on previous step
            throw new RuntimeException("Can't find transfer from station");
        }
        getUTransnetAccount().sendAsset(railCarAccount, routeMap.getNextRailCarFee(), routeMap.getId());
        inTrip(context);
    }

    private void inTrip(ActorTaskContext context) {
        addEventListener(new EventListener(
                "pay-to-rail-car-for-traveled-distance",
                OperationEvent.Type.PROPOSAL_CREATE,
                event -> {
                    UserAccount railCar = getRailCar();
                    RouteMap routeMap = getRouteMap();
                    if (railCar != null) {
                        Proposal proposal = ((OperationEvent.ProposalCreateEvent) event).getObject();
                        BaseOperation baseOperation = proposal.getOperation();
                        if (baseOperation.getOperationType() == OperationType.TRANSFER) {
                            TransferOperation operation = (TransferOperation) baseOperation;
                            if (operation.getFrom().equals(getUTransnetAccount()) && operation.getTo().equals(railCar)) {
                                String[] split = operation.getMemo().split("/");
                                if (split.length == 2 && routeMap.getId().equals(split[0])) {
                                    getUTransnetAccount().approveProposal(proposal);
                                    // TODO: check if this part already payed
                                }
                            }
                        }
                    }
                }
        ));
        addEventListener(new EventListener(
                "pay-to-checkpoint-for-entrance",
                OperationEvent.Type.PROPOSAL_CREATE,
                event -> {
                    UserAccount railCar = getRailCar();
                    RouteMap routeMap = getRouteMap();
                    if (railCar != null) {
                        Proposal proposal = ((OperationEvent.ProposalCreateEvent) event).getObject();
                        if (proposal.getFeePayer().equals(railCar)) {
                            BaseOperation baseOperation = proposal.getOperation();
                            if (baseOperation.getOperationType() == OperationType.TRANSFER) {
                                TransferOperation operation = (TransferOperation) baseOperation;
                                // TODO: check if CP in current RouteMap
                                if (operation.getFrom().equals(getUTransnetAccount())) {
                                    if (routeMap.getId().equals(operation.getMemo())) {
                                        info("Approve payment for '" + operation.getTo().getName() + "'");
                                        getUTransnetAccount().approveProposal(proposal);
                                    }
                                }
                            }
                        }
                    }
                }
        ));
    }

    @Nullable
    private UserAccount getRailCar() {
        RouteMap routeMap = getRouteMap();
        Optional<TransferOperation> payForTrip = getUTransnetAccount()
                .getTransfers()
                .stream()
                .filter(op -> op.getTo().equals(routeMap.getStart()))
                .filter(op -> Objects.equals(op.getMemo().split("/")[0], routeMap.getId()))
                .reduce((first, second) -> second); // find last;
        if (!payForTrip.isPresent()) {
            return null;
        }
        String railCarName = payForTrip.get().getMemo().split("/")[1];
        return getExternalAPI().getAccountByName(railCarName);
    }
    //endregion

    private boolean checkIfPaymentForLastCheckPoint(ActorTaskContext context, OperationEvent event) {
        if (event instanceof OperationEvent.TransferEvent) {
            TransferOperation operation = ((OperationEvent.TransferEvent) event).getObject();
            RouteMap routeMap = getRouteMap();
            String[] split = operation.getMemo().split("/");
            if (split.length != 2) {
                return false;
            }
            if (!Objects.equals(routeMap.getId(), split[0])) {
                return false;
            }
            if (!Objects.equals(Utils.getLast(routeMap.getRoute()).getId(), split[1])) {
                return false;
            }
            // payment for last check point
            info("Made payment for last check point");
            return true;
        }
        return false;
    }

    @LoggedAction
    private void exitRailCar(ActorTaskContext context) {
        actionLogger.logActorAction(this, "exitRailCar", "Client '%s' leaving rail car");
        RouteMap routeMap = getRouteMap();
        info("Leave RailCar on destination station '" + Utils.getLast(routeMap.getRoute()).getId() + "'");
        removeEventListener("pay-to-rail-car-for-traveled-distance");
        removeEventListener("pay-to-checkpoint-for-entrance");
    }


    // Client without RouteMap is useless
    @SneakyThrows
    protected RouteMap getRouteMap() {
        UserAccount logistAccount = getLogist();
        MessageOperation messageOperation = Utils.getLast(getUTransnetAccount().getMessagesFrom(logistAccount));
        return routeMapFactory.fromJsonForce(messageOperation.getMessage());
    }

    protected UserAccount getLogist() {
        return getExternalAPI().getAccountByName(logistName);
    }

    @Override
    protected Logger logger() {
        return log;
    }
}
