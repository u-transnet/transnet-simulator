package com.github.utransnet.simulator.actors;

import com.github.utransnet.simulator.SpringTest;
import com.github.utransnet.simulator.Utils;
import com.github.utransnet.simulator.actors.task.ActorTask;
import com.github.utransnet.simulator.externalapi.APIObjectFactory;
import com.github.utransnet.simulator.externalapi.ExternalAPI;
import com.github.utransnet.simulator.externalapi.Proposal;
import com.github.utransnet.simulator.externalapi.UserAccount;
import com.github.utransnet.simulator.externalapi.operations.TransferOperation;
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
                apiObjectFactory.getAssetAmount("RA", 10),
                "route-map-id"
        );

        assertNotNull(checkPoint.getCurrentRailCar());
        assertEquals(car, checkPoint.getCurrentRailCar());

        Proposal proposal = Utils.getLast(checkPoint.getReservation().getProposals());
        checkPoint.getReservation().approveProposal(proposal);
        assertNotNull(checkPoint.getCurrentRailCar());
        assertEquals(car, checkPoint.getCurrentRailCar());

        car.sendAsset(checkPoint.getUTransnetAccount(), apiObjectFactory.getAssetAmount("RA", 10), "");
        assertNull(checkPoint.getCurrentRailCar());

        UserAccount car2 = externalAPI.createAccount("car2");

        assertNull(checkPoint.getCurrentRailCar());

        externalAPI.sendProposal(
                checkPoint.getReservation(),
                car2,
                car2,
                apiObjectFactory.getAssetAmount("RA", 10),
                "route-map-id"
        );

        assertNotNull(checkPoint.getCurrentRailCar());
        assertEquals(car2, checkPoint.getCurrentRailCar());

        car2.sendAsset(checkPoint.getUTransnetAccount(), apiObjectFactory.getAssetAmount("RA", 10), "");
        assertNull(checkPoint.getCurrentRailCar());


        externalAPI.sendProposal(
                checkPoint.getReservation(),
                car,
                car,
                apiObjectFactory.getAssetAmount("RA", 10),
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
        client.sendAsset(checkPoint.getUTransnetAccount(), apiObjectFactory.getAssetAmount("UTT", 10), "id-1");
        assertThat(checkPoint.paidRouteMapIds(), is(Collections.singletonList("id-1")));
        client.sendAsset(checkPoint.getUTransnetAccount(), apiObjectFactory.getAssetAmount("UTT", 10), "id-2");
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
                apiObjectFactory.getAssetAmount("RA", 10),
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
        UserAccount client = externalAPI.createAccount("client");
        UserAccount reservation = externalAPI.createAccount(checkPoint.getUTransnetAccount().getId() + "-reserve");
        UserAccount logist = externalAPI.createAccount("logist");

        startMainFlow(checkPoint, railCar, client, reservation, logist);

        client.approveProposal(Utils.getLast(client.getProposals()));

        checkPoint.update(1);
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
        Proposal paymentRequestFromCP = Utils.getLast(railCar.getProposals());
        assertNotNull(paymentRequestFromCP);
        TransferOperation paymentRequestFromCPOperation = (TransferOperation) paymentRequestFromCP.getOperation();
        assertEquals("route-map-id", paymentRequestFromCPOperation.getMemo());
        assertEquals("close-gate", checkPoint.getCurrentTask().getName());

        railCar.approveProposal(paymentRequestFromCP);
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

        checkPoint.update(0);
        assertNotNull(checkPoint.getCurrentTask());
        assertEquals("wait-and-allow-entrance", checkPoint.getCurrentTask().getName());


        client.approveProposal(Utils.getLast(client.getProposals()));
        checkPoint.update(0);
        assertEquals("ask-payment-from-rail-car", checkPoint.getCurrentTask().getName());
    }

    private void startMainFlow(CheckPoint4Test checkPoint, UserAccount railCar, UserAccount client, UserAccount reservation, UserAccount logist) {
        logist.sendMessage(checkPoint.getUTransnetAccount(), "route-map-id");

        // make reservation
        externalAPI.sendProposal(
                client,
                checkPoint.getUTransnetAccount(),
                logist,
                apiObjectFactory.getAssetAmount("UTT", 10),
                "route-map-id"
        );

        // rail car attempt to enter
        externalAPI.sendProposal(
                reservation,
                railCar,
                railCar,
                apiObjectFactory.getAssetAmount("RA", 10),
                "route-map-id"
        );

        railCar.approveProposal(Utils.getLast(railCar.getProposals()));
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
        CheckPoint4Test checkPoint4Test(ExternalAPI externalAPI, APIObjectFactory apiObjectFactory) {
            return new CheckPoint4Test(externalAPI, apiObjectFactory);
        }

    }

    public static class CheckPoint4Test extends CheckPoint {

        CheckPoint4Test(ExternalAPI externalAPI, APIObjectFactory apiObjectFactory) {
            super(externalAPI, apiObjectFactory, null, null);
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