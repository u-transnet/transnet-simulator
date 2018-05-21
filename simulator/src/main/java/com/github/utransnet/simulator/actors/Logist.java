package com.github.utransnet.simulator.actors;

import com.github.utransnet.simulator.actors.factory.Actor;
import com.github.utransnet.simulator.actors.task.ActorTask;
import com.github.utransnet.simulator.actors.task.ActorTaskContext;
import com.github.utransnet.simulator.actors.task.OperationListener;
import com.github.utransnet.simulator.externalapi.AssetAmount;
import com.github.utransnet.simulator.externalapi.ExternalAPI;
import com.github.utransnet.simulator.externalapi.UserAccount;
import com.github.utransnet.simulator.externalapi.operations.BaseOperation;
import com.github.utransnet.simulator.externalapi.operations.OperationType;
import com.github.utransnet.simulator.externalapi.operations.TransferOperation;
import com.github.utransnet.simulator.logging.ActionLogger;
import com.github.utransnet.simulator.logging.LoggedAction;
import com.github.utransnet.simulator.queue.InputQueue;
import com.github.utransnet.simulator.route.RouteMap;
import com.github.utransnet.simulator.route.RouteMapFactory;
import com.github.utransnet.simulator.route.RouteNode;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.springframework.util.Assert;

import javax.annotation.PostConstruct;
import java.util.List;

/**
 * Created by Artem on 31.01.2018.
 */
@Slf4j
public class Logist extends Actor {
    private final InputQueue<RouteMap> routeMapInputQueue;
    private final RouteMapFactory routeMapFactory;
    private final ActionLogger actionLogger;
    private final int routeMapCreationTime = 60;

    @Setter
    private long routeMapPrice;

    public Logist(
            ExternalAPI externalAPI,
            RouteMapFactory routeMapFactory,
            InputQueue<RouteMap> routeMapInputQueue,
            ActionLogger actionLogger
    ) {
        super(externalAPI);
        this.routeMapInputQueue = routeMapInputQueue;
        this.routeMapFactory = routeMapFactory;
        this.actionLogger = actionLogger;
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
            if (transferOperation.getTo().equals(getUTransnetAccount())) {
                if (transferOperation.getAmount() >= routeMapPrice) {
                    info("Request received from '" + transferOperation.getFrom().getName() + "'");
                    addTask(
                            ActorTask.builder()
                                    .name("create-route-map")
                                    .executor(this)
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
    }

    private void createRouteMap(ActorTaskContext context) {
        RouteMap routeMap = routeMapInputQueue.poll();
        context.addPayload("route-map", routeMap);
        UserAccount client = context.getPayload("client");
        List<RouteNode> route = routeMap.getRoute();
        for (int i = 1; i < route.size(); i++) {
            debug("Creating proposal from '" + client.getName() + "' to '" + route.get(i).getId() + "'");
            UserAccount checkPoint = getExternalAPI().getAccountById(route.get(i).getId());
            getUTransnetAccount().sendMessage(checkPoint, routeMap.getId());
            getExternalAPI().sendProposal(
                    client,
                    checkPoint,
                    getUTransnetAccount(),
                    route.get(i).getFee(),
                    routeMap.getId()
            );
        }
    }

    @LoggedAction
    private void sendRouteMap(ActorTaskContext context) {
        actionLogger.logActorAction(this, "sendRouteMap", "Logist '%s' sending route map");
        RouteMap routeMap = context.getPayload("route-map");
        Assert.notNull(routeMap, "Logist can't send null map");
        UserAccount client = context.getPayload("client");
        getUTransnetAccount().sendMessage(client, routeMapFactory.toJson(routeMap));
        info("Route map was sent to '" + client.getName() + "'");
    }

    private void cancelRouteMapCreation(ActorTaskContext context) {
        UserAccount client = context.getPayload("client");
        AssetAmount assetAmount = context.getPayload("paid-asset");
        getUTransnetAccount().sendAsset(client, assetAmount, "");
    }

    @Override
    protected Logger logger() {
        return log;
    }
}
