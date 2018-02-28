package com.github.utransnet.simulator.actors;

import com.github.utransnet.simulator.Utils;
import com.github.utransnet.simulator.actors.factory.Actor;
import com.github.utransnet.simulator.actors.task.ActorTask;
import com.github.utransnet.simulator.actors.task.ActorTaskContext;
import com.github.utransnet.simulator.actors.task.OperationEvent;
import com.github.utransnet.simulator.externalapi.AssetAmount;
import com.github.utransnet.simulator.externalapi.ExternalAPI;
import com.github.utransnet.simulator.externalapi.Proposal;
import com.github.utransnet.simulator.externalapi.UserAccount;
import com.github.utransnet.simulator.externalapi.operations.BaseOperation;
import com.github.utransnet.simulator.externalapi.operations.MessageOperation;
import com.github.utransnet.simulator.externalapi.operations.OperationType;
import com.github.utransnet.simulator.externalapi.operations.TransferOperation;
import com.github.utransnet.simulator.route.RouteMap;
import com.github.utransnet.simulator.route.RouteMapFactory;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.Assert;

import javax.annotation.PostConstruct;
import java.util.List;
import java.util.stream.Collectors;


/**
 * Created by Artem on 31.01.2018.
 */
@Slf4j
public class Client extends Actor {
    private final RouteMapFactory routeMapFactory;

    @Setter
    private String logistName;

    @Setter
    private AssetAmount routeMapPrice;

    public Client(ExternalAPI externalAPI, RouteMapFactory routeMapFactory) {
        super(externalAPI);
        this.routeMapFactory = routeMapFactory;
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
                .context(new ActorTaskContext(60))
                .onEnd(this::tellInRailCar)
                .build()
        ;
        addTask(buyRoputeMapTask);
    }

    //region operations with Logist
    private void buyRouteMap(ActorTaskContext context) {
        UserAccount logistAccount = getLogist();
        getUTransnetAccount().sendAsset(logistAccount, routeMapPrice, "");

    }

    private boolean checkReceivedRouteMap(ActorTaskContext context, OperationEvent event) {
        if (event instanceof OperationEvent.MessageEvent) {
            OperationEvent.MessageEvent messageEvent = (OperationEvent.MessageEvent) event;
            MessageOperation messageOperation = messageEvent.getObject();
            if (messageOperation.getFrom().equals(getLogist())) {
                String json = messageOperation.getMessage();
                try {
                    routeMapFactory.fromJson(json);
                    return true;
                } catch (Exception e) {
                    log.error("Error in decoding received RouteMap", e);
                }
            }
        }
        return false;
    }
    //endregion


    //region start trip
    private void requestTrip(ActorTaskContext context) {
        RouteMap routeMap = getRouteMap();
        getUTransnetAccount().sendMessage(routeMap.getStart(), routeMapFactory.toJson(routeMap));
    }


    private void payForRoutePart(ActorTaskContext context) {

    }

    private boolean waitRailCar(ActorTaskContext context, OperationEvent event) {
        if (event instanceof OperationEvent.ProposalCreateEvent) {
            OperationEvent.ProposalCreateEvent proposalCreateEvent = (OperationEvent.ProposalCreateEvent) event;
            Proposal proposal = proposalCreateEvent.getObject();
            BaseOperation operation = proposal.getOperation();

            RouteMap routeMap = getRouteMap();

            if (operation instanceof TransferOperation) {
                TransferOperation transferOperation = (TransferOperation) operation;
                if (transferOperation.getFrom().equals(getUTransnetAccount())
                        && transferOperation.getTo().equals(routeMap.getStart())) {
                    return true;
                }
            }
        }
        return false;
    }

    private void tellReadyForTrip(ActorTaskContext context) {
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

    private void tellInRailCar(ActorTaskContext context) {
        RouteMap routeMap = getRouteMap();
        TransferOperation payForTrip = Utils.getLast(getUTransnetAccount().getTransfersFrom(routeMap.getStart()));
        String railcCarName = payForTrip.getMemo();
        UserAccount railCarAccount = getExternalAPI().getAccountByName(railcCarName);
        getUTransnetAccount().sendAsset(railCarAccount, routeMap.getNextRailCarFee(), routeMap.getId());
    }
    //endregion

    private void exitRailCar(ActorTaskContext context) {

    }


    protected RouteMap getRouteMap() {
        UserAccount logistAccount = getLogist();
        MessageOperation messageOperation = Utils.getLast(getUTransnetAccount().getMessagesFrom(logistAccount));
        return routeMapFactory.fromJson(messageOperation.getMessage());
    }

    protected UserAccount getLogist() {
        return getExternalAPI().getAccountByName(logistName);
    }


}
