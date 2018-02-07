package com.github.utransnet.simulator.actors;

import com.github.utransnet.simulator.Stub;
import com.github.utransnet.simulator.Utils;
import com.github.utransnet.simulator.actors.task.ActorTask;
import com.github.utransnet.simulator.actors.task.ActorTaskContext;
import com.github.utransnet.simulator.externalapi.ExternalAPI;
import com.github.utransnet.simulator.externalapi.Proposal;
import com.github.utransnet.simulator.externalapi.UserAccount;
import com.github.utransnet.simulator.externalapi.operations.*;
import com.github.utransnet.simulator.route.RouteMap;
import com.github.utransnet.simulator.route.RouteMapFactory;

import javax.annotation.PostConstruct;
import java.util.List;

/**
 * Created by Artem on 31.01.2018.
 */
public class Client extends Actor {
    private final RouteMapFactory routeMapFactory;

    public Client(ExternalAPI externalAPI, RouteMapFactory routeMapFactory) {
        super(externalAPI);
        this.routeMapFactory = routeMapFactory;
    }

    @PostConstruct
    private void init() {
        ActorTask initTask = ActorTask.builder()
                .name("client-init")
                .context(new ActorTaskContext(0))
                .onEnd(this::findLogist)
                .build();
        initTask.createNext()

                .name("buy-route-map")
                .context(new ActorTaskContext(
                        OperationType.MESSAGE,
                        this::checkReceivedRouteMap
                ))
                .onStart(this::buyRouteMap)
                .build()
                .createNext()

                .name("buy-trip")
                .context(new ActorTaskContext(60))
                .onEnd(this::requestTrip)
                .build()
                .createNext()

                .name("wait-rail-car")
                .context(new ActorTaskContext(
                        OperationType.PROPOSAL_CREATE_OPERATION, //or other type?
                        this::waitRailCar
                ))
                .onEnd(this::tellReadyForTrip)
                .build()
                .createNext()

                .name("start-trip")
                .context(new ActorTaskContext(60))
                .onEnd(this::tellInRailCar)
        ;
        addTask(initTask);
    }

    //region operations with Logist
    private void findLogist(ActorTaskContext context) {
        //TODO: find logist account
        context.addPayload("logist-account", new Stub());
    }

    private void buyRouteMap(ActorTaskContext context) {
        UserAccount logistAccount = context.getPayload("logist-account");
        //TODO: request price
        getUTransnetAccount().sendAsset(logistAccount, null, "");

    }

    private boolean checkReceivedRouteMap(ActorTaskContext context, BaseOperation baseOperation) {
        if(baseOperation instanceof MessageOperation){
            MessageOperation operation = (MessageOperation) baseOperation;
            if(operation.getFrom().equals(context.getPayload("logist-account")))
            {
                return true;
            }
        }
        return false;
    }
    //endregion


    //region start trip
    private void requestTrip(ActorTaskContext context) {
        RouteMap routeMap = getRouteMap(context);
        getUTransnetAccount().sendMessage(routeMap.getStart(), routeMapFactory.toJson(routeMap));
    }



    private void payForRoutePart(ActorTaskContext context) {

    }

    private boolean waitRailCar(ActorTaskContext context, BaseOperation baseOperation) {
        //TODO check incoming proposal
        return false;
    }

    private void tellReadyForTrip(ActorTaskContext context) {
        RouteMap routeMap = getRouteMap(context);
        Proposal proposal = Utils.getLast(getUTransnetAccount().getProposalsFrom(routeMap.getStart()));
        getUTransnetAccount().approveProposal(proposal);
    }

    private void tellInRailCar(ActorTaskContext context) {
        RouteMap routeMap = getRouteMap(context);
        TransferOperation payForTrip = Utils.getLast(getUTransnetAccount().getTransfersFrom(routeMap.getStart()));
        String railcCarName = payForTrip.getMemo();
        UserAccount railCarAccount = getExternalAPI().getAccountByName(railcCarName);
        getUTransnetAccount().sendAsset(railCarAccount, routeMap.getNextRailCarFee(), routeMap.getId());
    }
    //endregion

    private void exitRailCar(ActorTaskContext context) {

    }


    private RouteMap getRouteMap(ActorTaskContext context) {
        UserAccount logistAccount = context.getPayload("logist-account");
        TransferOperation transferOperation = Utils.getLast(getUTransnetAccount().getTransfersFrom(logistAccount));
        return routeMapFactory.fromJson(transferOperation.getMemo());
    }


}
