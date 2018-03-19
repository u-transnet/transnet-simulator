package com.github.utransnet.simulator.actors;

import com.github.utransnet.simulator.Utils;
import com.github.utransnet.simulator.actors.task.ActorTask;
import com.github.utransnet.simulator.actors.task.ActorTaskContext;
import com.github.utransnet.simulator.actors.task.OperationEvent;
import com.github.utransnet.simulator.actors.task.OperationListener;
import com.github.utransnet.simulator.externalapi.APIObjectFactory;
import com.github.utransnet.simulator.externalapi.AssetAmount;
import com.github.utransnet.simulator.externalapi.ExternalAPI;
import com.github.utransnet.simulator.externalapi.UserAccount;
import com.github.utransnet.simulator.externalapi.operations.BaseOperation;
import com.github.utransnet.simulator.externalapi.operations.MessageOperation;
import com.github.utransnet.simulator.externalapi.operations.OperationType;
import com.github.utransnet.simulator.externalapi.operations.TransferOperation;
import com.github.utransnet.simulator.route.RouteMap;
import com.github.utransnet.simulator.route.RouteMapFactory;
import lombok.Setter;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.springframework.util.Assert;

import javax.annotation.PostConstruct;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Created by Artem on 31.01.2018.
 */
@Slf4j
public class Station extends BaseInfObject {

    private final RouteMapFactory routeMapFactory;
    private final APIObjectFactory apiObjectFactory;

    private AssetAmount stationFee;
    private AssetAmount railCarFee;

    @Setter
    private CheckPoint checkPoint;

    public Station(ExternalAPI externalAPI, RouteMapFactory routeMapFactory, APIObjectFactory apiObjectFactory) {
        super(externalAPI);
        this.routeMapFactory = routeMapFactory;
        this.apiObjectFactory = apiObjectFactory;
    }

    @PostConstruct
    private void init() {
        stationFee = apiObjectFactory.getAssetAmount("UTT", 10);
        railCarFee = apiObjectFactory.getAssetAmount("RA", 10);
        addOperationListener(new OperationListener(
                "wait-order",
                OperationType.MESSAGE,
                this::waitOrder

        ));

    }

    private void waitOrder(BaseOperation operation) {
        if (operation instanceof MessageOperation) {
            MessageOperation messageOperation = (MessageOperation) operation;
            debug("New Message: " + operation);
            if (messageOperation.getTo().equals(getUTransnetAccount())) {
                String message = messageOperation.getMessage();
                RouteMap routeMap = routeMapFactory.fromJson(message);
                if (routeMap != null && getUTransnetAccount().equals(routeMap.getStart())) {
                    info("Received trip request from '" + messageOperation.getFrom().getName() + "'");
                    createTasks(messageOperation.getFrom().getId());
                }
            }
        }
    }

    private void createTasks(String clientId) {
        ActorTaskContext context = new ActorTaskContext(1);
        context.addPayload("client-id", clientId);
        ActorTask task = ActorTask.builder()
                .name("call-rail-car")
                .executor(this)
                .onStart(this::findVacantRailCar)
                .context(context)
                .onEnd(this::callRailCar)
                .build();

        task.createNext()
                .name("wait-confirmation-from-rail-car")
                .executor(this)
                .context(new ActorTaskContext(
                        OperationEvent.Type.TRANSFER,
                        this::waitConfirmationFromRailCar
                ))
                .build()

                .createNext()
                .name("ask-payment-from-client")
                .executor(this)
                .onStart(this::askPaymentFromClient)
                .context(new ActorTaskContext(
                        OperationEvent.Type.TRANSFER,
                        this::waitPaymentFromClient
                ))
                .build()

                .createNext()
                .name("pay-to-rail-car")
                .executor(this)
                .context(new ActorTaskContext(1))
                .onStart(this::transferRAToRailCar)
                .build();
        addTask(task);
    }

    private void findVacantRailCar(ActorTaskContext context) {
        UserAccount currentRailCar = checkPoint.getCurrentRailCar();
        if (currentRailCar != null) {
            List<String> free = getUTransnetAccount().getMessages()
                    .stream()
                    .filter(mo -> mo.getTo().equals(getUTransnetAccount()))
                    .filter(mo -> mo.getMessage().equals("FREE"))
                    .map(MessageOperation::getFrom)
                    .map(UserAccount::getId)
                    .collect(Collectors.toList());
            if (free.contains(currentRailCar.getId())) {
                context.addPayload("rail-car-id", currentRailCar.getId());
            }
        }
    }

    private void callRailCar(ActorTaskContext context) {
        UserAccount railCar = getExternalAPI().getAccountById(context.getPayload("rail-car-id"));
        getUTransnetAccount().sendMessage(railCar, getRouteMapString(context));

    }

    private boolean waitConfirmationFromRailCar(ActorTaskContext context, OperationEvent event) {
        if (event.getEventType() == OperationEvent.Type.TRANSFER) {
            TransferOperation transferOperation = ((OperationEvent.TransferEvent) event).getObject();
            if (transferOperation.getFrom().getId().equals(context.getPayload("rail-car-id"))) {
                return true;
            }
        }
        return false;
    }

    private void askPaymentFromClient(ActorTaskContext context) {
        UserAccount client = getExternalAPI().getAccountById(context.getPayload("client-id"));
        getExternalAPI().sendProposal(
                client,
                getUTransnetAccount(),
                client,
                getUTransnetAccount(),
                stationFee,
                getRouteMap(context).getId() + "/" + context.getPayload("rail-car-id")
        );
    }

    private boolean waitPaymentFromClient(ActorTaskContext context, OperationEvent event) {
        if (event.getEventType() == OperationEvent.Type.TRANSFER) {
            TransferOperation transferOperation = ((OperationEvent.TransferEvent) event).getObject();
            if (transferOperation.getFrom().getId().equals(context.getPayload("client-id"))) {
                if (transferOperation.getAssetAmount().equals(stationFee)) {
                    return true;
                }
            }
        }
        return false;
    }

    private void transferRAToRailCar(ActorTaskContext context) {
        String clientId = context.getPayload("client-id");
        RouteMap routeMap = getRouteMap(context);
        getUTransnetAccount().sendAsset(
                getExternalAPI().getAccountById(context.getPayload("rail-car-id")),
                railCarFee,
                routeMap.getId() + "/" + clientId
        );
    }

    // We assume that in case of incorrect RouteMap exception should be thrown before
    @SneakyThrows
    private RouteMap getRouteMap(ActorTaskContext context) {
        return routeMapFactory.fromJsonForce(getRouteMapString(context));
    }

    private String getRouteMapString(ActorTaskContext context) {
        String clientId = context.getPayload("client-id");
        UserAccount client = getExternalAPI().getAccountById(clientId);
        MessageOperation last = Utils.getLast(getUTransnetAccount().getMessagesFrom(client));
        Assert.notNull(last, "Station must have incoming request from client");
        return last.getMessage();
    }

    @Override
    protected Logger logger() {
        return log;
    }
}
