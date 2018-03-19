package com.github.utransnet.simulator.actors;

import com.github.utransnet.simulator.SpringTest;
import com.github.utransnet.simulator.actors.factory.ClientBuilder;
import com.github.utransnet.simulator.actors.task.ActorTask;
import com.github.utransnet.simulator.externalapi.APIObjectFactory;
import com.github.utransnet.simulator.externalapi.ExternalAPI;
import com.github.utransnet.simulator.externalapi.UserAccount;
import com.github.utransnet.simulator.externalapi.operations.BaseOperation;
import com.github.utransnet.simulator.externalapi.operations.MessageOperation;
import com.github.utransnet.simulator.externalapi.operations.OperationType;
import com.github.utransnet.simulator.externalapi.operations.TransferOperation;
import com.github.utransnet.simulator.logging.ActionLogger;
import com.github.utransnet.simulator.route.RouteMap;
import com.github.utransnet.simulator.route.RouteMapFactory;
import org.jetbrains.annotations.NotNull;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Scope;

import java.util.Optional;

import static org.junit.Assert.*;

/**
 * Created by Artem on 22.02.2018.
 */
public class ClientTest extends SpringTest<ClientTest.Config> {


    private final String json = "{\"id\":\"test-id\"," +
            "\"route\":[" +
            "{\"id\":\"start\",\"distance\":0,\"fee\":\"10 test\",\"railCarFee\":\"10 test\"}" +
            ",{\"id\":\"end\",\"distance\":100,\"fee\":\"10 test\",\"railCarFee\":\"10 test\"}" +
            "]}";
    private final String logist = "logist";

    @SuppressWarnings("SpringJavaAutowiredMembersInspection")
    @Autowired
    ExternalAPI externalAPI;

    @SuppressWarnings("SpringJavaAutowiredMembersInspection")
    @Autowired
    RouteMapFactory routeMapFactory;
    @SuppressWarnings("SpringJavaAutowiredMembersInspection")
    @Autowired
    APIObjectFactory apiObjectFactory;
    @SuppressWarnings("SpringJavaAutowiredMembersInspection")
    @Autowired
    ApplicationContext context;
    @SuppressWarnings("SpringJavaAutowiredMembersInspection")
    @Autowired
    ClientBuilder clientBuilder;
    @SuppressWarnings("SpringJavaAutowiredMembersInspection")
    @Autowired
    ActionLogger actionLogger;

    @Test
    public void getRouteMap() throws Exception {
        //TODO
    }

    @Test
    public void setGetLogist() throws Exception {
        Client client = new Client(externalAPI, routeMapFactory, actionLogger);
        client.setLogistName(logist);
        assertEquals("logist", client.getLogist().getName());
    }

    @Test
    public void taskSetUp() throws Exception {
        Client4Test client = createClient4Test();
        assertNull(client.getCurrentTask());
        client.update(0);
        assertNotNull(client.getCurrentTask());

    }

    @Test
    public void init() throws Exception {
        Client4Test client = createClient4Test();
        client.update(0);

        ActorTask buyRouteMap = client.getCurrentTask();
        assertNotNull(buyRouteMap);
        assertEquals("buy-route-map", buyRouteMap.getName());

        ActorTask buyTrip = buyRouteMap.getNext();
        assertNotNull(buyTrip);
        assertEquals("buy-trip", buyTrip.getName());

        ActorTask waitRailCar = buyTrip.getNext();
        assertNotNull(waitRailCar);
        assertEquals("wait-rail-car", waitRailCar.getName());

        ActorTask startTrip = waitRailCar.getNext();
        assertNotNull(startTrip);
        assertEquals("start-trip", startTrip.getName());
    }

    @Test
    public void testTasks() throws Exception {


        Client4Test client = createClient4Test();
        client.update(0);

        ActorTask buyRouteMap = client.getCurrentTask();
        assertNotNull(buyRouteMap);

        UserAccount logistAccount = apiObjectFactory.userAccount(logist);

        //region buy-route-map
        // test onStart
        Optional<? extends BaseOperation> buyRouteMapOperation = logistAccount.getLastOperation();
        assertTrue(buyRouteMapOperation.isPresent());
        assertEquals(OperationType.TRANSFER, buyRouteMapOperation.get().getOperationType());
        TransferOperation operation = (TransferOperation) buyRouteMapOperation.get();
        assertEquals(apiObjectFactory.getAssetAmount("UTT", 10), operation.getAssetAmount());
        assertEquals(client.getUTransnetAccount(), operation.getFrom());
        assertEquals(logistAccount, operation.getTo());

        // test getting route map
        logistAccount.sendMessage(client.getUTransnetAccount(), json);
        client.update(0);
        assertNotNull(client.getCurrentTask());
        assertEquals("buy-trip", client.getCurrentTask().getName());

        RouteMap routeMap = client.getRouteMap();
        assertNotNull(routeMap);
        assertEquals("start", routeMap.getStart().getId());
        //endregion

        //region buy-trip
        // test onEnd
        client.update(70);
        Optional<? extends BaseOperation> buyTripOperation = routeMap.getStart().getLastOperation();
        assertTrue(buyTripOperation.isPresent());
        assertEquals(OperationType.MESSAGE, buyTripOperation.get().getOperationType());
        MessageOperation buyTripMessage = (MessageOperation) buyTripOperation.get();
        assertEquals(client.getUTransnetAccount(), buyTripMessage.getFrom());
        assertEquals(routeMap.getStart(), buyTripMessage.getTo());
        assertEquals(json, buyTripMessage.getMessage());

        assertNotNull(client.getCurrentTask());
        assertEquals("wait-rail-car", client.getCurrentTask().getName());
        //endregion
        //region wait-rail-car
        externalAPI.sendProposal(
                client.getUTransnetAccount(),
                routeMap.getStart(),
                client.getUTransnetAccount(),
                routeMap.getStart(),
                routeMap.getNextFee(),
                "route-map-id"
        );


        client.update(0);
        assertNotNull(client.getCurrentTask());
        assertEquals("start-trip", client.getCurrentTask().getName());

        //check if proposal was approved
        Optional<? extends BaseOperation> transferAfterProposal = routeMap.getStart().getLastOperation();
        assertTrue(transferAfterProposal.isPresent());
        assertEquals(OperationType.TRANSFER, transferAfterProposal.get().getOperationType());
        TransferOperation payToStation = (TransferOperation) transferAfterProposal.get();
        assertEquals(client.getUTransnetAccount(), payToStation.getFrom());
        assertEquals(routeMap.getStart(), payToStation.getTo());
        assertEquals(routeMap.getNextFee(), payToStation.getAssetAmount());

        //endregion
    }

    @NotNull
    private ClientTest.Client4Test createClient4Test() {
        Client4Test client = context.getBean(Client4Test.class);
        client.setUTransnetAccount(externalAPI.createAccount("client"));
        client.setLogistName(logist);
        client.setRouteMapPrice(apiObjectFactory.getAssetAmount("UTT", 10));
        return client;
    }

    @Override
    public Class<Config> getRootConfig() {
        return Config.class;
    }

    @Configuration
    @Import({
            ActorsConfigForTest.class
    })
    public static class Config {

        @Bean
        @Scope("prototype")
        @Autowired
        Client4Test client(ExternalAPI externalAPI, RouteMapFactory routeMapFactory, ActionLogger actionLogger) {
            return new Client4Test(externalAPI, routeMapFactory, actionLogger);
        }

    }

    public static class Client4Test extends Client {

        Client4Test(ExternalAPI externalAPI, RouteMapFactory routeMapFactory, ActionLogger actionLogger) {
            super(externalAPI, routeMapFactory, actionLogger);
        }

        @Override
        public ActorTask getCurrentTask() {
            return super.getCurrentTask();
        }

        @Override
        public void setUTransnetAccount(UserAccount userAccount) {
            super.setUTransnetAccount(userAccount);
        }

        @Override
        public RouteMap getRouteMap() {
            return super.getRouteMap();
        }
    }
}