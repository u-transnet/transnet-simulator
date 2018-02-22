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

import javax.annotation.PostConstruct;


/**
 * Created by Artem on 31.01.2018.
 */
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
                .context(new ActorTaskContext(
                        OperationEvent.Type.MESSAGE,
                        this::checkReceivedRouteMap
                ))
                .onStart(this::buyRouteMap)
                .build();
        buyRoputeMapTask.createNext()

                .name("buy-trip")
                .context(new ActorTaskContext(60))
                .onEnd(this::requestTrip)
                .build()
                .createNext()

                .name("wait-rail-car")
                .context(new ActorTaskContext(
                        OperationEvent.Type.PROPOSAL_CREATE,
                        this::waitRailCar
                ))
                .onEnd(this::tellReadyForTrip)
                .build()
                .createNext()

                .name("start-trip")
                .context(new ActorTaskContext(60))
                .onEnd(this::tellInRailCar)
        ;
        addTask(buyRoputeMapTask);
    }

    //region operations with Logist
    private void buyRouteMap(ActorTaskContext context) {
        UserAccount logistAccount = getLogist();
        getUTransnetAccount().sendAsset(logistAccount, routeMapPrice, "");

    }

    private boolean checkReceivedRouteMap(ActorTaskContext context, OperationEvent event) {
        if (event instanceof MessageOperation) {
            OperationEvent.MessageEvent messageEvent = (OperationEvent.MessageEvent) event;
            if (messageEvent.getObject().getFrom().equals(getLogist())) {
                return true;
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

            if(operation instanceof TransferOperation) {
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
        Proposal proposal = Utils.getLast(getUTransnetAccount().getProposalsFrom(routeMap.getStart()));
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
        TransferOperation transferOperation = Utils.getLast(getUTransnetAccount().getTransfersFrom(logistAccount));
        return routeMapFactory.fromJson(transferOperation.getMemo());
    }

    protected UserAccount getLogist() {
        return getExternalAPI().getAccountByName(logistName);
    }


}
