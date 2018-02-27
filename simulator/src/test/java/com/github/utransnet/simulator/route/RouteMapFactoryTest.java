package com.github.utransnet.simulator.route;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.utransnet.simulator.SpringTest;
import com.github.utransnet.simulator.externalapi.APIObjectFactory;
import com.github.utransnet.simulator.externalapi.AssetAmount;
import com.github.utransnet.simulator.externalapi.ExternalAPI;
import com.github.utransnet.simulator.externalapi.UserAccount;
import com.github.utransnet.simulator.testservices.APIObjectFactoryTestImpl;
import com.github.utransnet.simulator.testservices.ExternalAPIEmptyImpl;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;

import java.util.Arrays;

import static org.junit.Assert.assertEquals;

/**
 * Created by Artem on 27.02.2018.
 */
public class RouteMapFactoryTest extends SpringTest<RouteMapFactoryTest.Config> {

    @SuppressWarnings("SpringJavaAutowiredMembersInspection")
    @Autowired
    APIObjectFactory apiObjectFactory;

    @SuppressWarnings("SpringJavaAutowiredMembersInspection")
    @Autowired
    RouteMapFactory routeMapFactory;

    @SuppressWarnings("SpringJavaAutowiredMembersInspection")
    @Autowired
    ExternalAPI externalAPI;

    @Test
    public void fromJson() throws Exception {
        String json = "{\"id\":\"test-id\"," +
                "\"route\":[" +
                "{\"name\":\"start\",\"distance\":0,\"fee\":\"10 test\",\"railCarFee\":\"10 test\"}" +
                ",{\"name\":\"end\",\"distance\":100,\"fee\":\"10 test\",\"railCarFee\":\"10 test\"}" +
                "]}";

        RouteMap routeMap = routeMapFactory.fromJson(json);
        assertEquals("test-id", routeMap.getId());

        assertEquals("start", routeMap.getRoute().get(0).name);
        assertEquals(0, routeMap.getRoute().get(0).distance);
        assertEquals("test", routeMap.getRoute().get(0).getFee().getAsset().getId());
        assertEquals(10, routeMap.getRoute().get(0).getFee().getAmount());
        assertEquals("test", routeMap.getRoute().get(0).getRailCarFee().getAsset().getId());
        assertEquals(10, routeMap.getRoute().get(0).getRailCarFee().getAmount());

        assertEquals("end", routeMap.getRoute().get(1).name);
        assertEquals(100, routeMap.getRoute().get(1).distance);
        assertEquals("test", routeMap.getRoute().get(1).getFee().getAsset().getId());
        assertEquals(10, routeMap.getRoute().get(1).getFee().getAmount());
        assertEquals("test", routeMap.getRoute().get(1).getRailCarFee().getAsset().getId());
        assertEquals(10, routeMap.getRoute().get(1).getRailCarFee().getAmount());
    }

    @Test
    public void toJson() throws Exception {
        RouteMapContainer routeMapContainer = new RouteMapContainer();
        routeMapContainer.id = "test-id";
        AssetAmount assetAmount = apiObjectFactory.getAssetAmount("test", 10);
        String assetAmountString = assetAmount.getAmount() + " " + assetAmount.getAsset().getId();
        routeMapContainer.route = Arrays.asList(
                new RouteNode("start", 0, assetAmount, assetAmount),
                new RouteNode("end", 100, assetAmount, assetAmount)
        );

        RouteMap routeMap = new RouteMap(externalAPI, apiObjectFactory);
        routeMap.setRoute(routeMapContainer.route);
        routeMap.setId(routeMapContainer.id);

        String json = routeMapFactory.toJson(routeMap);


        JsonNode jsonNode = new ObjectMapper().readTree(json);
        JsonNode route = jsonNode.get("route");
        JsonNode start = route.get(0);
        JsonNode end = route.get(1);
        assertEquals(routeMapContainer.id, jsonNode.get("id").asText());

        assertEquals(routeMapContainer.route.get(0).name, start.get("name").asText());
        assertEquals(routeMapContainer.route.get(0).distance, start.get("distance").asInt());
        assertEquals(assetAmountString, start.get("fee").asText());
        assertEquals(assetAmountString, start.get("railCarFee").asText());

        assertEquals(routeMapContainer.route.get(1).name, end.get("name").asText());
        assertEquals(routeMapContainer.route.get(1).distance, end.get("distance").asInt());
        assertEquals(assetAmountString, end.get("fee").asText());
        assertEquals(assetAmountString, end.get("railCarFee").asText());

    }

    @Override
    public Class<Config> getRootConfig() {
        return Config.class;
    }


    @Configuration
    static class Config {

        @Bean
        ExternalAPI externalAPI() {
            return new ExternalAPIEmptyImpl() {
                @Override
                public UserAccount createAccount(String name) {
                    return new UserAccount(null) {
                        @Override
                        public String getName() {
                            return name;
                        }

                        @Override
                        public String getId() {
                            return name;
                        }
                    };
                }
            };
        }

        @Bean
        @Autowired
        APIObjectFactory apiObjectFactory(ExternalAPI externalAPI) {
            return new APIObjectFactoryTestImpl(externalAPI);
        }

        @Bean
        @Scope("prototype")
        AssetAmountSerializer assetAmountSerializer() {
            return new AssetAmountSerializer();
        }

        @Bean
        @Scope("prototype")
        @Autowired
        AssetAmountDeserializer assetAmountDeserializer(APIObjectFactory objectFactory) {
            return new AssetAmountDeserializer(objectFactory);
        }

        @Bean
        @Scope("singleton")
        @Autowired
        RouteMapFactory routeMapFactory(
                ApplicationContext context,
                AssetAmountDeserializer assetAmountDeserializer,
                AssetAmountSerializer assetAmountSerializer
        ) {
            return new RouteMapFactory(context, assetAmountDeserializer, assetAmountSerializer);
        }

        @Bean
        @Scope("prototype")
        @Autowired
        RouteMap routeMap(ExternalAPI externalAPI, APIObjectFactory objectFactory) {
            return new RouteMap(externalAPI, objectFactory);
        }

    }
}