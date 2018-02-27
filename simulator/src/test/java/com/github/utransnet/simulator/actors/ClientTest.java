package com.github.utransnet.simulator.actors;

import com.github.utransnet.simulator.SpringTest;
import com.github.utransnet.simulator.TestWithJpaConfig;
import com.github.utransnet.simulator.externalapi.APIObjectFactory;
import com.github.utransnet.simulator.externalapi.ExternalAPI;
import com.github.utransnet.simulator.externalapi.h2impl.ExternalAPIH2ImplConfig;
import com.github.utransnet.simulator.route.AssetAmountDeserializer;
import com.github.utransnet.simulator.route.AssetAmountSerializer;
import com.github.utransnet.simulator.route.RouteMapFactory;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Scope;

import static org.junit.Assert.assertEquals;

/**
 * Created by Artem on 22.02.2018.
 */
public class ClientTest extends SpringTest<ClientTest.Config> {


    @SuppressWarnings("SpringJavaAutowiredMembersInspection")
    @Autowired
    ExternalAPI externalAPI;


    @SuppressWarnings("SpringJavaAutowiredMembersInspection")
    @Autowired
    RouteMapFactory routeMapFactory;

    @Test
    public void getRouteMap() throws Exception {
        //TODO
    }

    @Test
    public void setGetLogist() throws Exception {
        Client client = new Client(externalAPI, routeMapFactory);
        client.setLogistName("logist");
        assertEquals("logist", client.getLogist().getName());
    }



    @Override
    public Class<Config> getRootConfig() {
        return Config.class;
    }

    @Configuration
    @Import({
            TestWithJpaConfig.class,
            ExternalAPIH2ImplConfig.class
    })
    public static class Config {

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
            return new RouteMapFactory(context, objectMapper);
        }

    }

    public static class Client4Test extends Client {

        public Client4Test(ExternalAPI externalAPI, RouteMapFactory routeMapFactory) {
            super(externalAPI, routeMapFactory);
        }
    }
}