package com.github.utransnet.simulator.actors;

import com.github.utransnet.simulator.SpringTest;
import com.github.utransnet.simulator.Utils;
import com.github.utransnet.simulator.externalapi.APIObjectFactory;
import com.github.utransnet.simulator.externalapi.ExternalAPI;
import com.github.utransnet.simulator.externalapi.UserAccount;
import com.github.utransnet.simulator.externalapi.operations.MessageOperation;
import com.github.utransnet.simulator.externalapi.operations.TransferOperation;
import com.github.utransnet.simulator.queue.InputQueue;
import com.github.utransnet.simulator.route.RouteMap;
import com.github.utransnet.simulator.route.RouteMapFactory;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Scope;

import static org.junit.Assert.*;

/**
 * Created by Artem on 01.03.2018.
 */
public class LogistTest extends SpringTest<LogistTest.Config> {

    public final String json = "{\"id\":\"test-id\"," +
            "\"route\":[" +
            "{\"name\":\"start\",\"distance\":0,\"fee\":\"10 test\",\"railCarFee\":\"10 test\"}" +
            ",{\"name\":\"end\",\"distance\":100,\"fee\":\"10 test\",\"railCarFee\":\"10 test\"}" +
            "]}";
    @SuppressWarnings("SpringJavaAutowiredMembersInspection")
    @Autowired
    ApplicationContext context;
    @SuppressWarnings("SpringJavaAutowiredMembersInspection")
    @Autowired
    ExternalAPI externalAPI;
    @SuppressWarnings("SpringJavaAutowiredMembersInspection")
    @Autowired
    APIObjectFactory apiObjectFactory;
    @SuppressWarnings("SpringJavaAutowiredMembersInspection")
    @Autowired
    RouteMapFactory routeMapFactory;
    @SuppressWarnings("SpringJavaAutowiredMembersInspection")
    @Autowired
    InputQueue<RouteMap> inputQueue;

    @Test
    public void insufficientAmountPaid() throws Exception {
        Logist4Test logist = context.getBean(Logist4Test.class);
        logist.setUTransnetAccount(externalAPI.createAccount("logist"));

        UserAccount client = externalAPI.createAccount("client");
        client.sendAsset(logist.getUTransnetAccount(), apiObjectFactory.getAssetAmount("UTT", 2), "");
        logist.update(0);

        MessageOperation messageOperation = Utils.getLast(client.getMessagesFrom(logist.getUTransnetAccount()));
        assertNull(messageOperation);

    }

    @Test
    public void orderReceived() throws Exception {
        Logist4Test logist = context.getBean(Logist4Test.class);
        logist.setUTransnetAccount(externalAPI.createAccount("logist"));
        inputQueue.offer(routeMapFactory.fromJson(json));

        UserAccount client = externalAPI.createAccount("client");
        client.sendAsset(logist.getUTransnetAccount(), apiObjectFactory.getAssetAmount("UTT", 10), "");
        logist.update(0);

        assertNull(Utils.getLast(client.getMessagesFrom(logist.getUTransnetAccount())));
        logist.update(60);
        MessageOperation routeMapMessage = Utils.getLast(client.getMessagesFrom(logist.getUTransnetAccount()));
        assertNotNull(routeMapMessage);
        assertEquals(logist.getUTransnetAccount(), routeMapMessage.getFrom());
        assertEquals(client, routeMapMessage.getTo());
        RouteMap routeMap = routeMapFactory.fromJson(routeMapMessage.getMessage());
        assertNotNull(routeMap);
        assertEquals("start", routeMap.getStart().getId());

    }

    @Test
    public void refund() throws Exception {
        Logist4Test logist = context.getBean(Logist4Test.class);
        logist.setUTransnetAccount(externalAPI.createAccount("logist"));
        UserAccount client = externalAPI.createAccount("client");
        client.sendAsset(logist.getUTransnetAccount(), apiObjectFactory.getAssetAmount("UTT", 10), "");
        logist.update(0); // check incoming transfer
        logist.update(60); // create RouteMap

        TransferOperation payBack = Utils.getLast(client.getTransfersFrom(logist.getUTransnetAccount()));
        assertNotNull(payBack);
        assertEquals(logist.getUTransnetAccount(), payBack.getFrom());
        assertEquals(client, payBack.getTo());
        assertEquals("10 UTT", payBack.getAssetAmount().toString());
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
        Logist4Test logist(ExternalAPI externalAPI, RouteMapFactory routeMapFactory, InputQueue<RouteMap> inputQueue) {
            return new Logist4Test(externalAPI, routeMapFactory, inputQueue);
        }
    }

    public static class Logist4Test extends Logist {

        public Logist4Test(ExternalAPI externalAPI, RouteMapFactory routeMapFactory, InputQueue<RouteMap> routeMapInputQueue) {
            super(externalAPI, routeMapFactory, routeMapInputQueue);
        }

        @Override
        public void setUTransnetAccount(UserAccount uTransnetAccount) {
            super.setUTransnetAccount(uTransnetAccount);
        }

    }

}