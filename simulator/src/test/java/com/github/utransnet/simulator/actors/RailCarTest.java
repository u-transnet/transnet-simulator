package com.github.utransnet.simulator.actors;

import com.github.utransnet.simulator.SpringTest;
import com.github.utransnet.simulator.Utils;
import com.github.utransnet.simulator.actors.factory.RailCarBuilder;
import com.github.utransnet.simulator.actors.task.ActorTask;
import com.github.utransnet.simulator.actors.task.DelayedAction;
import com.github.utransnet.simulator.externalapi.*;
import com.github.utransnet.simulator.externalapi.operations.MessageOperation;
import com.github.utransnet.simulator.externalapi.operations.OperationType;
import com.github.utransnet.simulator.externalapi.operations.TransferOperation;
import com.github.utransnet.simulator.route.RouteMap;
import com.github.utransnet.simulator.route.RouteMapFactory;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;

import java.lang.reflect.Field;
import java.util.List;

import static org.junit.Assert.*;

/**
 * Created by Artem on 04.03.2018.
 */
public class RailCarTest extends SpringTest<RailCarTest.Config> {


    private final String json = "{\"id\":\"test-id\"," +
            "\"route\":[" +
            "{\"id\":\"start\",\"distance\":0,\"fee\":\"10 test\",\"railCarFee\":\"10 test\"}" +
            ",{\"id\":\"middle\",\"distance\":50,\"fee\":\"10 test\",\"railCarFee\":\"10 test\"}" +
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

    @SuppressWarnings("SpringJavaAutowiredMembersInspection")
    @Autowired
    RailCarBuilder railCarBuilder;


    @Test
    public void builder() throws Exception {
        RailCar4Test railCar = context.getBean(RailCar4Test.class);
        railCar.setUTransnetAccount(externalAPI.createAccount("rail-car"));
        assertNotNull(railCar);
        assertNotNull(railCar.getReservation());
    }

    @Test
    public void init() throws Exception {
        RailCar4Test railCar = context.getBean(RailCar4Test.class);
        railCar.setUTransnetAccount(externalAPI.createAccount("rail-car"));
        assertNull(railCar.getCurrentTask());
        UserAccount station = externalAPI.createAccount("start");
        station.sendMessage(railCar.getUTransnetAccount(), json);
        railCar.update(1);
        assertNotNull(railCar.getCurrentTask());
        assertEquals("pay-for-order", railCar.getCurrentTask().getName());
    }

    @Test
    public void reservation() throws Exception {
        RailCar4Test railCar = context.getBean(RailCar4Test.class);
        railCar.setUTransnetAccount(externalAPI.createAccount("rail-car"));
        UserAccount reserve = externalAPI.createAccount("rail-car-reserve");
        externalAPI.createAccount("start").sendMessage(railCar.getUTransnetAccount(), json);
        railCar.update(1);
        assertNotNull(railCar.getCurrentTask());
        assertEquals("pay-for-order", railCar.getCurrentTask().getName());
        List<TransferOperation> transfersFrom = reserve.getTransfersFrom(railCar.getUTransnetAccount());
        assertEquals("test-id/start", transfersFrom.get(0).getMemo());
        assertEquals("test-id/middle", transfersFrom.get(1).getMemo());
        assertEquals("test-id/end", transfersFrom.get(2).getMemo());
    }

    @Test
    public void addTasksOnStation() throws Exception {
        RailCar4Test railCar = context.getBean(RailCar4Test.class);
        railCar.setUTransnetAccount(externalAPI.createAccount("rail-car"));
        UserAccount station = externalAPI.createAccount("start");
        UserAccount client = externalAPI.createAccount("client");
        RouteMap routeMap = routeMapFactory.fromJsonForce(json);
        railCar.setRouteMap(routeMap);
        AssetAmount assetAmount = apiObjectFactory.getAssetAmount("test", 10);

        railCar.addTasksOnStation();
        railCar.update(1);

        assertNotNull(railCar.getCurrentTask());
        assertEquals("pay-for-order", railCar.getCurrentTask().getName());

        railCar.update(1);
        assertEquals(
                routeMap.getId(),
                Utils.getLast(
                        station.getTransfersFrom(railCar.getUTransnetAccount())
                ).getMemo()
        );
        assertFalse(railCar.isMoving());
        assertTrue(railCar.isDoorsClosed());

        railCar.update(1);
        assertNotNull(railCar.getCurrentTask());
        assertEquals("wait-payment-from-station", railCar.getCurrentTask().getName());

        station.sendAsset(railCar.getUTransnetAccount(), assetAmount, routeMap.getId() + "/" + client.getId());
        railCar.update(1);
        assertFalse(railCar.isMoving());
        assertFalse(railCar.isDoorsClosed());
        assertNotNull(railCar.getCurrentTask());
        assertEquals("wait-payment-from-client", railCar.getCurrentTask().getName());

        client.sendAsset(railCar.getUTransnetAccount(), assetAmount, routeMap.getId());
        railCar.update(0);
        assertNotNull(railCar.getCurrentTask());
        assertEquals("start-movement", railCar.getCurrentTask().getName());
        assertTrue(railCar.isMoving());
        assertTrue(railCar.isDoorsClosed());
        assertEquals("start", railCar.getRouteMap().getNextAccount().getId());

        railCar.update(1);
        assertNotNull(railCar.getCurrentTask());
        assertEquals("request-pass-from-check-point", railCar.getCurrentTask().getName());
        assertEquals("middle", railCar.getRouteMap().getNextAccount().getId());


        assertTrue(railCar.isMoving());
        railCar.update(1000000); // emergency stop
        assertFalse(railCar.isMoving());
    }

    @Test
    public void addTasksWithCheckPoint() throws Exception {
        RailCar4Test railCar = context.getBean(RailCar4Test.class);
        railCar.setUTransnetAccount(externalAPI.createAccount("rail-car"));
        UserAccount reserve = externalAPI.createAccount("rail-car-reserve");
        UserAccount station = externalAPI.createAccount("start");
        UserAccount client = externalAPI.createAccount("client");
        UserAccount checkpoint = externalAPI.createAccount("middle");
        RouteMap routeMap = routeMapFactory.fromJsonForce(json);
        routeMap.goNext();
        railCar.setRouteMap(routeMap);
        AssetAmount assetAmount = apiObjectFactory.getAssetAmount("test", 10);

        railCar.addDelayedAction(new DelayedAction(
                railCar,
                "stop-before-check-point",
                100,
                Assert::fail // fail, if there is no acceptance from check point
        ));

        // RailCar find client from this transaction
        station.sendAsset(railCar.getUTransnetAccount(), assetAmount, routeMap.getId() + "/" + client.getId());


        // set private filed isMoving from reflection, because rail car already moving on this step
        Field isMovingField = railCar.getClass().getSuperclass().getDeclaredField("isMoving");
        isMovingField.setAccessible(true);
        isMovingField.set(railCar, true);
        assertTrue(railCar.isMoving());

        Field nextCheckPointField = railCar.getClass().getSuperclass().getDeclaredField("currentCheckPoint");
        nextCheckPointField.setAccessible(true);
        nextCheckPointField.set(railCar, routeMap.getNextAccount());

        railCar.askNextCheckPoint(routeMap.getNextAccount());

        railCar.update(0);
        assertNotNull(railCar.getCurrentTask());
        assertEquals("request-pass-from-check-point", railCar.getCurrentTask().getName());
        assertEquals("middle", railCar.getRouteMap().getNextAccount().getId());

        railCar.update(1);
        assertNotNull(railCar.getCurrentTask());
        assertEquals("wait-accept-from-check-point", railCar.getCurrentTask().getName());
        Proposal requestRAFromCP = Utils.getLast(checkpoint.getProposals());
        assertNotNull(requestRAFromCP);
        assertEquals(OperationType.TRANSFER, requestRAFromCP.getOperation().getOperationType());
        TransferOperation raFromCP = (TransferOperation) requestRAFromCP.getOperation();
        assertEquals(railCar.getUTransnetAccount(), raFromCP.getTo());
        assertEquals(checkpoint, raFromCP.getFrom());
        assertEquals(routeMap.getId(), raFromCP.getMemo());

        checkpoint.approveProposal(requestRAFromCP);
        externalAPI.sendProposal(
                reserve,
                checkpoint,
                reserve,
                checkpoint,
                assetAmount,
                routeMap.getId()
        );

        railCar.update(0);

        MessageOperation messageOperation = Utils.getLast(client.getMessages());
        assertNotNull(messageOperation);
        assertEquals(railCar.getUTransnetAccount(), messageOperation.getFrom());
        assertEquals("test-id/end", messageOperation.getMessage());

        assertNull(railCar.getCurrentTask());
        railCar.leaveAndGoToNextCP();
        railCar.update(0);
        assertNotNull(railCar.getCurrentTask());
        assertEquals("leave-check-point", railCar.getCurrentTask().getName());

        railCar.update(10000); // fail if rail car not entered checkpoint
        assertTrue(railCar.isMoving()); // still moving, emergency stop removed
        assertNotNull(railCar.getCurrentTask());
        assertEquals("request-payment-from-client", railCar.getCurrentTask().getName());

        assertTrue(Utils.getLast(railCar.getReservation().getProposals()).approved());


        railCar.update(10);
        assertNotNull(railCar.getCurrentTask());
        assertEquals("request-pass-from-check-point", railCar.getCurrentTask().getName());


    }

    @Test
    public void testIfCheckPointSendProposalNotInTime() throws Exception {
        RailCar4Test railCar = context.getBean(RailCar4Test.class);
        railCar.setUTransnetAccount(externalAPI.createAccount("rail-car"));
        UserAccount reserve = externalAPI.createAccount("rail-car-reserve");
        UserAccount checkpoint = externalAPI.createAccount("middle");
        RouteMap routeMap = routeMapFactory.fromJsonForce(json);
        routeMap.goNext();
        railCar.setRouteMap(routeMap);
        AssetAmount assetAmount = apiObjectFactory.getAssetAmount("test", 10);

        railCar.addDelayedAction(new DelayedAction(
                railCar,
                "stop-before-check-point",
                100,
                Assert::fail // fail, if there is no acceptance from check point
        ));

        // RailCar find client from this transaction
        externalAPI.createAccount("start")
                .sendAsset(railCar.getUTransnetAccount(), assetAmount, routeMap.getId() + "/client");


        // set private filed isMoving from reflection, because rail car already moving on this step
        Field isMovingField = railCar.getClass().getSuperclass().getDeclaredField("isMoving");
        isMovingField.setAccessible(true);
        isMovingField.set(railCar, true);
        assertTrue(railCar.isMoving());

        Field nextCheckPointField = railCar.getClass().getSuperclass().getDeclaredField("currentCheckPoint");
        nextCheckPointField.setAccessible(true);
        nextCheckPointField.set(railCar, routeMap.getNextAccount());

        railCar.askNextCheckPoint(routeMap.getNextAccount());
        railCar.update(0);
        railCar.update(1);
        Proposal requestRAFromCP = Utils.getLast(checkpoint.getProposals());
        checkpoint.approveProposal(requestRAFromCP);

        railCar.update(0);
        assertNull(railCar.getCurrentTask());
        railCar.leaveAndGoToNextCP();
        assertNotNull(railCar.getCurrentTask());
        assertEquals("leave-check-point", railCar.getCurrentTask().getName());

        railCar.update(10000); // fail if checkpoint hasn't approved proposal
        assertFalse(railCar.isMoving()); // stopped, coz checkpoint hasn't send proposal yet
//        railCar.update(0);
        assertNotNull(railCar.getCurrentTask());
        assertEquals("wait-proposal-from-check-point", railCar.getCurrentTask().getName());

        externalAPI.sendProposal(
                reserve,
                checkpoint,
                reserve,
                checkpoint,
                assetAmount,
                routeMap.getId()
        );
        railCar.update(0);
        assertTrue(railCar.isMoving());

    }

    @Override
    public Class<Config> getRootConfig() {
        return Config.class;
    }

    @Configuration
    public static class Config extends ActorsConfigForTest {

        @Bean
        @Scope("prototype")
        @Autowired
        RailCar4Test railCar4Test(
                ExternalAPI externalAPI,
                RouteMapFactory routeMapFactory,
                APIObjectFactory objectFactory
        ) {
            return new RailCar4Test(externalAPI, routeMapFactory, objectFactory);
        }


    }

    public static class RailCar4Test extends RailCar {

        RailCar4Test(ExternalAPI externalAPI, RouteMapFactory routeMapFactory, APIObjectFactory objectFactory) {
            super(externalAPI, routeMapFactory, objectFactory);
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

        @Override
        public void setRouteMap(RouteMap routeMap) {
            super.setRouteMap(routeMap);
        }

        @Override
        public void addTasksOnStation() {
            super.addTasksOnStation();
        }

        @Override
        public void askNextCheckPoint(UserAccount nextCheckPoint) {
            super.askNextCheckPoint(nextCheckPoint);
        }

        @Override
        public UserAccount getReservation() {
            return super.getReservation();
        }

        @Override
        public boolean isMoving() {
            return super.isMoving();
        }

        @Override
        public boolean isDoorsClosed() {
            return super.isDoorsClosed();
        }

    }

}