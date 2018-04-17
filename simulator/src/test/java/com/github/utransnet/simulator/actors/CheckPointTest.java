package com.github.utransnet.simulator.actors;

import com.github.utransnet.simulator.SpringTest;
import com.github.utransnet.simulator.Utils;
import com.github.utransnet.simulator.actors.factory.Actor;
import com.github.utransnet.simulator.actors.task.ActorTask;
import com.github.utransnet.simulator.externalapi.*;
import com.github.utransnet.simulator.externalapi.operations.TransferOperation;
import com.github.utransnet.simulator.logging.ActionLogger;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Scope;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.*;

/**
 * Created by Artem on 06.03.2018.
 */
public class CheckPointTest extends SpringTest<CheckPointTest.Config> {
    @SuppressWarnings("SpringJavaAutowiredMembersInspection")
    @Autowired
    ExternalAPI externalAPI;
    @SuppressWarnings("SpringJavaAutowiredMembersInspection")
    @Autowired
    APIObjectFactory apiObjectFactory;
    @SuppressWarnings("SpringJavaAutowiredMembersInspection")
    @Autowired
    ApplicationContext context;
    @SuppressWarnings("SpringJavaAutowiredMembersInspection")
    @Autowired
    DefaultAssets defaultAssets;

    @Test
    public void getCurrentRailCar() throws Exception {
        CheckPoint4Test checkPoint = context.getBean(CheckPoint4Test.class);
        checkPoint.setUTransnetAccount(externalAPI.createAccount("cp"));
        UserAccount car = externalAPI.createAccount("car");

        assertNull(checkPoint.getCurrentRailCar());

        externalAPI.sendProposal(
                checkPoint.getReservation(),
                car,
                car,
                apiObjectFactory.getAssetAmount(defaultAssets.getResourceAsset(), 10),
                "route-map-id"
        );

        assertNotNull(checkPoint.getCurrentRailCar());
        assertEquals(car, checkPoint.getCurrentRailCar());

        Proposal proposal = Utils.getLast(checkPoint.getReservation().getProposals());
        checkPoint.getReservation().approveProposal(proposal);
        assertNotNull(checkPoint.getCurrentRailCar());
        assertEquals(car, checkPoint.getCurrentRailCar());

        car.sendAsset(checkPoint.getUTransnetAccount(), apiObjectFactory.getAssetAmount(defaultAssets.getResourceAsset(), 10), "");
        assertNull(checkPoint.getCurrentRailCar());

        UserAccount car2 = externalAPI.createAccount("car2");

        assertNull(checkPoint.getCurrentRailCar());

        externalAPI.sendProposal(
                checkPoint.getReservation(),
                car2,
                car2,
                apiObjectFactory.getAssetAmount(defaultAssets.getResourceAsset(), 10),
                "route-map-id"
        );

        assertNotNull(checkPoint.getCurrentRailCar());
        assertEquals(car2, checkPoint.getCurrentRailCar());

        car2.sendAsset(checkPoint.getUTransnetAccount(), apiObjectFactory.getAssetAmount(defaultAssets.getResourceAsset(), 10), "");
        assertNull(checkPoint.getCurrentRailCar());


        externalAPI.sendProposal(
                checkPoint.getReservation(),
                car,
                car,
                apiObjectFactory.getAssetAmount(defaultAssets.getResourceAsset(), 10),
                "route-map-id"
        );

        assertNotNull(checkPoint.getCurrentRailCar());
        assertEquals(car, checkPoint.getCurrentRailCar());
    }

    @Test
    public void routeMapIdsToServe() throws Exception {
        CheckPoint4Test checkPoint = context.getBean(CheckPoint4Test.class);
        checkPoint.setUTransnetAccount(externalAPI.createAccount("cp"));
        UserAccount logist = externalAPI.createAccount("logist");
        logist.sendMessage(checkPoint.getUTransnetAccount(), "id-1");
        logist.sendMessage(checkPoint.getUTransnetAccount(), "id-2");
        logist.sendMessage(checkPoint.getUTransnetAccount(), "id-3");

        assertThat(checkPoint.routeMapIdsToServe(), is(Arrays.asList("id-1", "id-2", "id-3")));
    }

    @Test
    public void paidRouteMapIds() throws Exception {
        CheckPoint4Test checkPoint = context.getBean(CheckPoint4Test.class);
        checkPoint.setUTransnetAccount(externalAPI.createAccount("cp"));
        UserAccount logist = externalAPI.createAccount("logist");
        logist.sendMessage(checkPoint.getUTransnetAccount(), "id-1");
        logist.sendMessage(checkPoint.getUTransnetAccount(), "id-2");
        logist.sendMessage(checkPoint.getUTransnetAccount(), "id-3");

        assertTrue(checkPoint.paidRouteMapIds().isEmpty());

        UserAccount client = externalAPI.createAccount("logist");
        client.sendAsset(checkPoint.getUTransnetAccount(), apiObjectFactory.getAssetAmount(defaultAssets.getMainAsset(), 10), "id-1");
        assertThat(checkPoint.paidRouteMapIds(), is(Collections.singletonList("id-1")));
        client.sendAsset(checkPoint.getUTransnetAccount(), apiObjectFactory.getAssetAmount(defaultAssets.getMainAsset(), 10), "id-2");
        assertThat(checkPoint.paidRouteMapIds(), is(Arrays.asList("id-1", "id-2")));

    }

    @Test
    public void makeReservation() throws Exception {
        CheckPoint4Test checkPoint = context.getBean(CheckPoint4Test.class);
        checkPoint.setUTransnetAccount(externalAPI.createAccount("cp"));

        UserAccount client = externalAPI.createAccount("client");
        UserAccount logist = externalAPI.createAccount("logist");
        logist.sendMessage(checkPoint.getUTransnetAccount(), "route-map-id");
        externalAPI.sendProposal(
                client,
                checkPoint.getUTransnetAccount(),
                logist,
                apiObjectFactory.getAssetAmount(defaultAssets.getMainAsset(), 10),
                "route-map-id"
        );
        checkPoint.update(0);

        List<TransferOperation> transfers = checkPoint.getReservation().getTransfers();
        assertEquals(1, transfers.size());
        assertEquals("route-map-id/client", transfers.get(0).getMemo());
        assertEquals(checkPoint.getUTransnetAccount(), transfers.get(0).getFrom());
    }

    @Test
    public void createRailCarFlowClientAlreadyPayed() throws Exception {
        CheckPoint4Test checkPoint = context.getBean(CheckPoint4Test.class);
        checkPoint.setUTransnetAccount(externalAPI.createAccount("cp"));

        UserAccount railCar = externalAPI.createAccount("rail-car");
        UserAccount railCarReserve = externalAPI.createAccount("rail-car-reserve");
        UserAccount client = externalAPI.createAccount("client");
        UserAccount reservation = externalAPI.createAccount(checkPoint.getUTransnetAccount().getName() + "-reserve");
        UserAccount logist = externalAPI.createAccount("logist");

        startMainFlow(checkPoint, railCar, client, reservation, logist);

        client.approveProposal(Utils.getLast(client.getProposals()));

        checkPoint.update(1); //wait proposal from car
        checkPoint.update(1); // check client payment
        assertNotNull(checkPoint.getCurrentTask());
        assertEquals("allow-entrance", checkPoint.getCurrentTask().getName());
        assertTrue(checkPoint.isGateClosed());

        checkPoint.update(1);
        assertFalse(checkPoint.isGateClosed());
        TransferOperation paymentFromCPToRailCar = Utils.getLast(railCar.getTransfersFrom(reservation));
        assertNotNull(paymentFromCPToRailCar);
        assertEquals("route-map-id", paymentFromCPToRailCar.getMemo());
        assertEquals("ask-payment-from-rail-car", checkPoint.getCurrentTask().getName());

        checkPoint.update(1);
        Proposal paymentRequestFromCP = Utils.getLast(railCarReserve.getProposals());
        assertNotNull(paymentRequestFromCP);
        TransferOperation paymentRequestFromCPOperation = (TransferOperation) paymentRequestFromCP.getOperation();
        assertEquals("route-map-id", paymentRequestFromCPOperation.getMemo());
        assertEquals("wait-rail-car-exit-and-close-gate", checkPoint.getCurrentTask().getName());

        railCarReserve.approveProposal(paymentRequestFromCP);
        checkPoint.update(1);
        assertNull(checkPoint.getCurrentTask());


    }

    @Test
    public void createRailCarFlowWaitClientPayment() throws Exception {
        CheckPoint4Test checkPoint = context.getBean(CheckPoint4Test.class);
        checkPoint.setUTransnetAccount(externalAPI.createAccount("cp"));

        UserAccount railCar = externalAPI.createAccount("rail-car");
        UserAccount client = externalAPI.createAccount("client");
        UserAccount reservation = externalAPI.createAccount(checkPoint.getUTransnetAccount().getId() + "-reserve");
        UserAccount logist = externalAPI.createAccount("logist");

        startMainFlow(checkPoint, railCar, client, reservation, logist);

        checkPoint.update(1); //wait proposal from car
        checkPoint.update(1); // check client payment
        assertNotNull(checkPoint.getCurrentTask());
        assertEquals("wait-and-allow-entrance", checkPoint.getCurrentTask().getName());


        client.approveProposal(Utils.getLast(client.getProposals()));
        checkPoint.update(0);
        assertNotNull(checkPoint.getCurrentTask());
        assertEquals("ask-payment-from-rail-car", checkPoint.getCurrentTask().getName());
    }

    private void startMainFlow(CheckPoint4Test checkPoint, UserAccount railCar, UserAccount client, UserAccount reservation, UserAccount logist) {
        logist.sendMessage(checkPoint.getUTransnetAccount(), "route-map-id");

        // make reservation
        externalAPI.sendProposal(
                client,
                checkPoint.getUTransnetAccount(),
                logist,
                apiObjectFactory.getAssetAmount(defaultAssets.getMainAsset(), 10),
                "route-map-id"
        );

        // rail car attempt to enter
        externalAPI.sendProposal(
                reservation,
                railCar,
                railCar,
                apiObjectFactory.getAssetAmount(defaultAssets.getResourceAsset(), 10),
                "route-map-id"
        );

        reservation.approveProposal(Utils.getLast(railCar.getProposals()));
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
        CheckPoint4Test checkPoint4Test(
                ExternalAPI externalAPI,
                APIObjectFactory apiObjectFactory,
                DefaultAssets defaultAssets
        ) {
            return new CheckPoint4Test(externalAPI, apiObjectFactory, defaultAssets);
        }

    }

    public static class CheckPoint4Test extends CheckPoint {

        CheckPoint4Test(ExternalAPI externalAPI, APIObjectFactory apiObjectFactory, DefaultAssets defaultAssets) {
            super(externalAPI, apiObjectFactory, new ActionLogger() {
                @Override
                public void logActorAction(Actor actor, String action, String label) {

                }
            }, defaultAssets);
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
        public boolean isGateClosed() {
            return super.isGateClosed();
        }
    }
}