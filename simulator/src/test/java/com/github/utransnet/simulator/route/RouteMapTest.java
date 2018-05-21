package com.github.utransnet.simulator.route;

import com.github.utransnet.simulator.SpringTest;
import com.github.utransnet.simulator.externalapi.APIObjectFactory;
import com.github.utransnet.simulator.externalapi.Asset;
import com.github.utransnet.simulator.externalapi.AssetAmount;
import com.github.utransnet.simulator.externalapi.ExternalAPI;
import com.github.utransnet.simulator.testservices.APIObjectFactoryTestImpl;
import com.github.utransnet.simulator.testservices.ExternalAPIEmptyImpl;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;

import java.util.Arrays;
import java.util.Map;

import static org.junit.Assert.*;

/**
 * Created by Artem on 27.02.2018.
 */
public class RouteMapTest extends SpringTest<RouteMapTest.Config> {

    @SuppressWarnings("SpringJavaAutowiredMembersInspection")
    @Autowired
    APIObjectFactory apiObjectFactory;

    @SuppressWarnings("SpringJavaAutowiredMembersInspection")
    @Autowired
    ExternalAPI externalAPI;

    RouteMap createRouteMap() {
        RouteMapContainer routeMapContainer = new RouteMapContainer();
        routeMapContainer.id = "test-id";
        AssetAmount usdAmount = apiObjectFactory.getAssetAmount("USD", 100);
        AssetAmount uttAmount10 = apiObjectFactory.getAssetAmount("UTT", 10);
        AssetAmount uttAmount25 = apiObjectFactory.getAssetAmount("UTT", 25);
        routeMapContainer.route = Arrays.asList(
                new RouteNode("start", 0, usdAmount, usdAmount),
                new RouteNode("middle", 50, uttAmount10, uttAmount25),
                new RouteNode("end", 100, uttAmount10, uttAmount25)
        );

        RouteMap routeMap = new RouteMap(externalAPI);
        routeMap.setRoute(routeMapContainer.route);
        routeMap.setId(routeMapContainer.id);
        return routeMap;
    }


    @Test
    public void getId() throws Exception {
        RouteMap routeMap = createRouteMap();
        assertEquals("test-id", routeMap.getId());
    }

    @Test
    public void getStart() throws Exception {
        RouteMap routeMap = createRouteMap();
        assertEquals("start", routeMap.getStart().getId());
    }

    @Test
    public void getNextAccount() throws Exception {
        RouteMap routeMap = createRouteMap();
        assertEquals("start", routeMap.getNextAccount().getId());
        assertEquals(routeMap.getStart(), routeMap.getNextAccount());
    }

    @Test
    public void goNext() throws Exception {
        RouteMap routeMap = createRouteMap();
        assertEquals("start", routeMap.getNextAccount().getId());
        assertTrue(routeMap.goNext());
        assertEquals("middle", routeMap.getNextAccount().getId());
        assertTrue(routeMap.goNext());
        assertEquals("end", routeMap.getNextAccount().getId());
        assertFalse(routeMap.goNext());
    }

    @Test
    public void getNextFee() throws Exception {
        RouteMap routeMap = createRouteMap();
        assertEquals(100, routeMap.getNextFee().getAmount());
        assertEquals("USD", routeMap.getNextFee().getAsset().getId());
        assertTrue(routeMap.goNext());
        assertEquals(10, routeMap.getNextFee().getAmount());
        assertEquals("UTT", routeMap.getNextFee().getAsset().getId());
    }

    @Test
    public void getNextRailCarFee() throws Exception {
        RouteMap routeMap = createRouteMap();
        assertEquals(100, routeMap.getNextRailCarFee().getAmount());
        assertEquals("USD", routeMap.getNextRailCarFee().getAsset().getId());
        assertTrue(routeMap.goNext());
        assertEquals(25, routeMap.getNextRailCarFee().getAmount());
        assertEquals("UTT", routeMap.getNextRailCarFee().getAsset().getId());
    }

    @Test
    public void getNextDistance() throws Exception {
        RouteMap routeMap = createRouteMap();
        assertEquals(0, routeMap.getNextDistance());
        assertTrue(routeMap.goNext());
        assertEquals(50, routeMap.getNextDistance());
    }

    @Test
    public void getTotalFee() throws Exception {
        RouteMap routeMap = createRouteMap();
        Map<Asset, Long> totalFee = routeMap.getTotalFee();
        assertEquals(2, totalFee.size());
        Asset usd = apiObjectFactory.getAsset("USD");
        Asset utt = apiObjectFactory.getAsset("UTT");
        assertTrue(totalFee.containsKey(usd));
        assertEquals((Long) 100L, totalFee.get(usd));
        assertTrue(totalFee.containsKey(utt));
        assertEquals((Long) 20L, totalFee.get(utt));
    }

    @Test
    public void getTotalRailCarFee() throws Exception {
        RouteMap routeMap = createRouteMap();
        Map<Asset, Long> totalFee = routeMap.getTotalRailCarFee();
        assertEquals(2, totalFee.size());
        Asset usd = apiObjectFactory.getAsset("USD");
        Asset utt = apiObjectFactory.getAsset("UTT");
        assertTrue(totalFee.containsKey(usd));
        assertEquals((Long) 100L, totalFee.get(usd));
        assertTrue(totalFee.containsKey(utt));
        assertEquals((Long) 50L, totalFee.get(utt));
    }

    @Test
    public void getFeeSum() throws Exception {
        RouteMap routeMap = createRouteMap();
        Map<Asset, Long> totalFee = routeMap.getFeeSum();
        assertEquals(2, totalFee.size());
        Asset usd = apiObjectFactory.getAsset("USD");
        Asset utt = apiObjectFactory.getAsset("UTT");
        assertTrue(totalFee.containsKey(usd));
        assertEquals((Long) 200L, totalFee.get(usd));
        assertTrue(totalFee.containsKey(utt));
        assertEquals((Long) 70L, totalFee.get(utt));
    }

    @Test
    public void getTotalDistance() throws Exception {
        RouteMap routeMap = createRouteMap();
        assertEquals(150, routeMap.getTotalDistance());
    }

    @Override
    public Class<Config> getRootConfig() {
        return Config.class;
    }

    @Configuration
    public static class Config {
        @Bean
        @Scope("prototype")
        ExternalAPI externalAPI() {
            return new ExternalAPIEmptyImpl();
        }

        @Bean
        @Scope("prototype")
        @Autowired
        APIObjectFactory apiObjectFactory(ExternalAPI externalAPI) {
            return new APIObjectFactoryTestImpl(externalAPI);
        }
    }

}