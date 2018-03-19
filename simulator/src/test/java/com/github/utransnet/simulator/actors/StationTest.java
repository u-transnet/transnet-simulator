package com.github.utransnet.simulator.actors;

import com.github.utransnet.simulator.SpringTest;
import com.github.utransnet.simulator.Utils;
import com.github.utransnet.simulator.actors.task.ActorTask;
import com.github.utransnet.simulator.externalapi.APIObjectFactory;
import com.github.utransnet.simulator.externalapi.ExternalAPI;
import com.github.utransnet.simulator.externalapi.Proposal;
import com.github.utransnet.simulator.externalapi.UserAccount;
import com.github.utransnet.simulator.externalapi.operations.BaseOperation;
import com.github.utransnet.simulator.externalapi.operations.MessageOperation;
import com.github.utransnet.simulator.externalapi.operations.OperationType;
import com.github.utransnet.simulator.externalapi.operations.TransferOperation;
import com.github.utransnet.simulator.logging.ActionLogger;
import com.github.utransnet.simulator.route.RouteMap;
import com.github.utransnet.simulator.route.RouteMapFactory;
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
 * Created by Artem on 01.03.2018.
 */
public class StationTest extends SpringTest<StationTest.Config> {

    private final String stationId = "station";
    private final String json = "{\"id\":\"test-id\"," +
            "\"route\":[" +
            "{\"id\":\"" + stationId + "\",\"distance\":0,\"fee\":\"10 test\",\"railCarFee\":\"10 test\"}" +
            ",{\"id\":\"end\",\"distance\":100,\"fee\":\"10 test\",\"railCarFee\":\"10 test\"}" +
            "]}";
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

    @Test
    public void testTasks() throws Exception {
        Station4Test station = context.getBean(Station4Test.class);
        station.setUTransnetAccount(externalAPI.createAccount(stationId));

        UserAccount client = externalAPI.createAccount("client");
        UserAccount railCar = externalAPI.createAccount("rail-car");
        RouteMap routeMap = routeMapFactory.fromJsonForce(json);

        client.sendMessage(station.getUTransnetAccount(), json);
        station.update(0);
        assertNotNull(station.getCurrentTask());
        assertEquals("call-rail-car", station.getCurrentTask().getName());
        station.update(1);

        Optional<? extends BaseOperation> lastOperation = railCar.getLastOperation();
        assertTrue(lastOperation.isPresent());
        assertEquals(OperationType.MESSAGE, lastOperation.get().getOperationType());
        MessageOperation messageOperation = (MessageOperation) lastOperation.get();
        assertEquals(station.getUTransnetAccount(), messageOperation.getFrom());
        assertEquals(json, messageOperation.getMessage());

        assertEquals("wait-confirmation-from-rail-car", station.getCurrentTask().getName());
        railCar.sendAsset(
                station.getUTransnetAccount(),
                apiObjectFactory.getAssetAmount("RA", 1),
                routeMap.getId()
        );
        station.update(1);
        assertEquals("ask-payment-from-client", station.getCurrentTask().getName());


        station.update(1);
        Proposal proposal = Utils.getLast(client.getProposals());
        assertNotNull(proposal);
        assertEquals(station.getUTransnetAccount(), proposal.getFeePayer());
        assertEquals(1, proposal.neededApproves().size());
        assertEquals(client.getId(), proposal.neededApproves().get(0));

        assertEquals(OperationType.TRANSFER, proposal.getOperation().getOperationType());
        TransferOperation transferOperation = (TransferOperation) proposal.getOperation();
        assertEquals(client, transferOperation.getFrom());
        assertEquals(station.getUTransnetAccount(), transferOperation.getTo());

        client.approveProposal(proposal);
        station.update(0);
        assertEquals("pay-to-rail-car", station.getCurrentTask().getName());
        station.update(1);
        assertNull(station.getCurrentTask());
        assertEquals("test-id/client", Utils.getLast(railCar.getTransfers()).getMemo());

    }

    @Test
    public void init() throws Exception {
        Station4Test station = context.getBean(Station4Test.class);
        station.setUTransnetAccount(externalAPI.createAccount(stationId));

        UserAccount client = externalAPI.createAccount("client");

        assertNull(station.getCurrentTask());

        client.sendMessage(station.getUTransnetAccount(), json);
        station.update(0);

        ActorTask callRailCar = station.getCurrentTask();
        assertNotNull(callRailCar);
        assertEquals("call-rail-car", callRailCar.getName());

        ActorTask waitRailCar = callRailCar.getNext();
        assertNotNull(waitRailCar);
        assertEquals("wait-confirmation-from-rail-car", waitRailCar.getName());

        ActorTask askPayment = waitRailCar.getNext();
        assertNotNull(askPayment);
        assertEquals("ask-payment-from-client", askPayment.getName());

        ActorTask payToRailCar = askPayment.getNext();
        assertNotNull(payToRailCar);
        assertEquals("pay-to-rail-car", payToRailCar.getName());

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
        Station4Test station4Test(
                ExternalAPI externalAPI,
                RouteMapFactory routeMapFactory,
                APIObjectFactory apiObjectFactory,
                ActionLogger actionLogger
        ) {
            return new Station4Test(externalAPI, routeMapFactory, apiObjectFactory, actionLogger);
        }

    }

    public static class Station4Test extends Station {

        Station4Test(
                ExternalAPI externalAPI,
                RouteMapFactory routeMapFactory,
                APIObjectFactory apiObjectFactory,
                ActionLogger actionLogger
        ) {
            super(externalAPI, routeMapFactory, apiObjectFactory, actionLogger);
        }

        @Override
        public ActorTask getCurrentTask() {
            return super.getCurrentTask();
        }

        @Override
        public void setUTransnetAccount(UserAccount userAccount) {
            super.setUTransnetAccount(userAccount);
        }
    }
}