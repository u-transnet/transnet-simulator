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
                checkPoint.getReservation(),
                car,
                apiObjectFactory.getAsset("RA"),
                10
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
                checkPoint.getReservation(),
                car2,
                apiObjectFactory.getAsset("RA"),
                10
        );

        assertNotNull(checkPoint.getCurrentRailCar());
        assertEquals(car2, checkPoint.getCurrentRailCar());

        car2.sendAsset(checkPoint.getUTransnetAccount(), apiObjectFactory.getAssetAmount("RA", 10), "");
        assertNull(checkPoint.getCurrentRailCar());


        externalAPI.sendProposal(
                checkPoint.getReservation(),
                car,
                checkPoint.getReservation(),
                car,
                apiObjectFactory.getAsset("RA"),
                10
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

        //TODO UK-27
        externalAPI.sendProposal(
                externalAPI.createAccount("client"),
                checkPoint.getUTransnetAccount(),
                externalAPI.createAccount("client"),
                externalAPI.createAccount("logist"),
                apiObjectFactory.getAsset("UTT"),
                10
        );
        checkPoint.update(0);

        List<TransferOperation> transfers = checkPoint.getReservation().getTransfers();
        assertEquals(1, transfers.size());
        assertEquals("id-1", transfers.get(0).getMemo() + "/client");
        assertEquals(checkPoint.getUTransnetAccount(), transfers.get(0).getFrom());
    }

    @Test
    public void createRailCarFlow() throws Exception {
        //TODO UK-27
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
            super(externalAPI, apiObjectFactory);
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