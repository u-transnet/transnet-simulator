package com.github.utransnet.simulator.actors.factory;

import com.github.utransnet.simulator.SpringTest;
import com.github.utransnet.simulator.actors.*;
import com.github.utransnet.simulator.externalapi.APIObjectFactory;
import com.github.utransnet.simulator.externalapi.ExternalAPI;
import com.github.utransnet.simulator.testservices.APIObjectFactoryTestImpl;
import com.github.utransnet.simulator.testservices.ExternalAPIEmptyImpl;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;

import static org.junit.Assert.assertEquals;

/**
 * Created by Artem on 09.02.2018.
 */
public class ActorFactoryTest extends SpringTest<ActorFactoryTest.Config> {

    @SuppressWarnings("SpringJavaAutowiredMembersInspection")
    @Autowired
    ActorFactory actorFactory;


    @Test
    public void testBuilder() {

    }

    @Test
    public void createLogist() throws Exception {
        Logist logist = actorFactory.createLogistBuilder()
                .id("test")
                .build();
        assertEquals(Logist.class, logist.getClass());
    }

    @Test
    public void createClient() throws Exception {
        Client client = actorFactory.createClientBuilder()
                .id("test")
                .build();
        assertEquals(Client.class, client.getClass());
    }

    @Test
    public void createRailCarBuilder() throws Exception {
        RailCar client = actorFactory.createRailCarBuilder()
                .id("test")
                .build();
        assertEquals(RailCar.class, client.getClass());
    }

    @Test
    public void createStationBuilder() throws Exception {
        Station client = actorFactory.createStationBuilder()
                .id("test")
                .build();
        assertEquals(Station.class, client.getClass());
    }

    @Test
    public void createCheckPointBuilder() throws Exception {
        CheckPoint client = actorFactory.createCheckPointBuilder()
                .id("test")
                .build();
        assertEquals(CheckPoint.class, client.getClass());
    }

    @Override
    public Class<Config> getRootConfig() {
        return Config.class;
    }

    @SuppressWarnings("ConstantConditions")
    @Configuration
    static class Config {

        @Bean
        @Scope("singleton")
        ExternalAPI externalAPI(){
            return new ExternalAPIEmptyImpl();
        }


        @Bean
        @Scope("singleton")
        @Autowired
        APIObjectFactory apiObjectFactory(ExternalAPI externalAPI){
            return new APIObjectFactoryTestImpl(externalAPI);
        }

        @Bean
        @Scope("singleton")
        @Autowired
        ActorFactory actorFactory(ApplicationContext context) {
            return new ActorFactory(context);
        }

        @Bean
        @Autowired
        LogistBuilder logistBuilder(ApplicationContext context, APIObjectFactory objectFactory) {
            return new LogistBuilder(context, objectFactory);
        }

        @Bean
        @Autowired
        ClientBuilder clientBuilder(ApplicationContext context, APIObjectFactory objectFactory) {
            return new ClientBuilder(context, objectFactory);
        }

        @Bean
        @Autowired
        RailCarBuilder railCarBuilder(ApplicationContext context, APIObjectFactory objectFactory) {
            return new RailCarBuilder(context, objectFactory);
        }

        @Bean
        @Autowired
        StationBuilder stationBuilder(ApplicationContext context, APIObjectFactory objectFactory) {
            return new StationBuilder(context, objectFactory);
        }

        @Bean
        @Autowired
        CheckPointBuilder checkPointBuilder(ApplicationContext context, APIObjectFactory objectFactory) {
            return new CheckPointBuilder(context, objectFactory);
        }

        @Bean
        @Autowired
        Logist logist(ExternalAPI externalAPI){
            return new Logist(externalAPI, null, null);
        }

        @Bean
        @Autowired
        Client client(ExternalAPI externalAPI) {
            return new Client(externalAPI, null);
        }

        @Bean
        @Autowired
        RailCar railCar(ExternalAPI externalAPI) {
            return new RailCar(externalAPI);
        }

        @Bean
        @Autowired
        Station station(ExternalAPI externalAPI, APIObjectFactory objectFactory) {
            return new Station(externalAPI, null, objectFactory);
        }

        @Bean
        @Autowired
        CheckPoint checkPoint(ExternalAPI externalAPI) {
            return new CheckPoint(externalAPI);
        }

    }

}