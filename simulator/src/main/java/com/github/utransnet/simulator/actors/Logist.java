package com.github.utransnet.simulator.actors;

import com.github.utransnet.simulator.actors.task.ActorTask;
import com.github.utransnet.simulator.actors.task.ActorTaskContext;
import com.github.utransnet.simulator.actors.task.OperationListener;
import com.github.utransnet.simulator.externalapi.AssetAmount;
import com.github.utransnet.simulator.externalapi.ExternalAPI;
import com.github.utransnet.simulator.externalapi.UserAccount;
import com.github.utransnet.simulator.externalapi.operations.BaseOperation;
import com.github.utransnet.simulator.externalapi.operations.OperationType;
import com.github.utransnet.simulator.externalapi.operations.TransferOperation;
import com.github.utransnet.simulator.queue.InputQueue;
import com.github.utransnet.simulator.route.RouteMap;
import com.github.utransnet.simulator.route.RouteMapFactory;

import javax.annotation.PostConstruct;

/**
 * Created by Artem on 31.01.2018.
 */
public class Logist extends Actor {
    private final InputQueue<RouteMap> routeMapInputQueue;
    private final RouteMapFactory routeMapFactory;
    private final int routeMapCreationTime = 60;
    private int routeMapPrice = 10;

    public Logist(ExternalAPI externalAPI, RouteMapFactory routeMapFactory, InputQueue<RouteMap> routeMapInputQueue) {
        super(externalAPI);
        this.routeMapInputQueue = routeMapInputQueue;
        this.routeMapFactory = routeMapFactory;
    }

    @PostConstruct
    private void init() {
        addOperationListener(new OperationListener(
                "logist-waiting-order",
                OperationType.TRANSFER,
                this::orderReceived
        ));

    }

    private void orderReceived(BaseOperation operation) {
        if (operation instanceof TransferOperation) {
            TransferOperation transferOperation = (TransferOperation) operation;
            if (transferOperation.getAmount() > routeMapPrice) {
                addTask(
                        ActorTask.builder()
                                .name("create-route-map")
                                .context(
                                        new ActorTaskContext(routeMapCreationTime)
                                                .addPayload("client", transferOperation.getFrom())
                                                .addPayload("paid-asset", transferOperation.getAssetAmount())
                                )
                                .onStart(this::createRouteMap)
                                .onEnd(this::sendRouteMap)
                                .onCancel(this::cancelRouteMapCreation)
                                .build()
                );
            }
        }
    }

    private void createRouteMap(ActorTaskContext context) {
        context.addPayload("route-map", routeMapInputQueue.poll());
    }

    private void sendRouteMap(ActorTaskContext context) {
        RouteMap routeMap = context.getPayload("route-map");
        UserAccount client = context.getPayload("client");
        getUTransnetAccount().sendMessage(client, routeMapFactory.toJson(routeMap));
    }

    private void cancelRouteMapCreation(ActorTaskContext context) {
        UserAccount client = context.getPayload("client");
        AssetAmount assetAmount = context.getPayload("paid-asset");
        getUTransnetAccount().sendAsset(client, assetAmount, "");
    }
}
