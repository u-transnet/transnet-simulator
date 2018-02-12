package com.github.utransnet.simulator.actors.factory;

import com.github.utransnet.simulator.SpringTest;
import com.github.utransnet.simulator.externalapi.APIObjectFactory;
import com.github.utransnet.simulator.externalapi.ExternalAPI;
import com.github.utransnet.simulator.testservices.APIObjectFactoryTestImpl;
import com.github.utransnet.simulator.testservices.ExternalAPIEmptyImpl;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.Objects;

import static org.junit.Assert.*;

/**
 * Created by Artem on 12.02.2018.
 */

@RunWith(SpringRunner.class)
public class ActorBuilderTest extends SpringTest<ActorBuilderTest.Config> {

    @SuppressWarnings("SpringJavaAutowiredMembersInspection")
    @Autowired
    ActorBuilder<TestActor> actorBuilder;

    @SuppressWarnings("SpringJavaAutowiredMembersInspection")
    @Autowired
    APIObjectFactory objectFactory;

    @Test
    public void testBuild() throws Exception {
        TestActor actor = actorBuilder.id("test").build();
        assertEquals(TestActor.class, actor.getClass());
    }

    @Test
    public void testId() throws Exception {
        String id = "test";
        TestActor actor = actorBuilder.id(id).build();
        assertEquals(id, actor.getId());
        assertEquals(id, actor.getUTransnetAccount().getName());
    }

    @SuppressWarnings("ConstantConditions")
    @Test(expected = IllegalArgumentException.class)
    public void testIdNonNull() throws Exception {
        actorBuilder.id(null).build();
    }

    @Test
    public void testBalance() throws Exception {
        TestActor actor = actorBuilder
                .id("test")
                .addAsset(objectFactory.createAssetAmount("test-asset", 10))
                .build();
        assertEquals(10L, actor.getBalance("test-asset"));
    }

    @Override
    public Class<Config> getRootConfig() {
        return Config.class;
    }

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
        @Autowired
        ActorBuilder<TestActor> actorBuilder(ApplicationContext context, APIObjectFactory objectFactory) {
            return new ActorBuilder<>(TestActor.class, context, objectFactory);
        }

        @Bean
        @Autowired
        TestActor actor(ExternalAPI externalAPI) {
            return new TestActor(externalAPI);
        }
    }

    public static class TestActor extends Actor {
        TestActor(ExternalAPI externalAPI) {
            super(externalAPI);
        }

        @SuppressWarnings({"ConstantConditions", "SameParameterValue"})
        long getBalance(String assetId){
            return super.getBalance()
                    .stream()
                    .filter(assetAmount -> Objects.equals(assetAmount.getAsset().getId(), assetId))
                    .findAny()
                    .get()
                    .getAmount();
        }
    }

}