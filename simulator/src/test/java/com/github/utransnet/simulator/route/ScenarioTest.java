package com.github.utransnet.simulator.route;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.utransnet.simulator.AppConfig;
import com.github.utransnet.simulator.SpringTest;
import com.github.utransnet.simulator.TestWithJpaConfig;
import com.github.utransnet.simulator.externalapi.APIObjectFactory;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import static org.junit.Assert.assertNotNull;

/**
 * Created by Artem on 13.03.2018.
 */
@Slf4j
public class ScenarioTest extends SpringTest<ScenarioTest.Config> {

    @SuppressWarnings("SpringJavaAutowiredMembersInspection")
    @Autowired
    ObjectMapper objectMapper;

    @SuppressWarnings("SpringJavaAutowiredMembersInspection")
    @Autowired
    APIObjectFactory apiObjectFactory;

    @Test
    public void loadScenario() throws Exception {
        ScenarioContainer container = new ScenarioContainer();

        createCP(container, "cp1", true);
        createCP(container, "cp2", false);
        createCP(container, "cp3", true);
        createCP(container, "cp4", true);
        createCP(container, "cp5", false);
        createCP(container, "cp6", false);
        createCP(container, "cp7", true);


        SerializedUserInfo client = new SerializedUserInfo();
        client.id = "client";
        client.name = "client";
        client.balance.add(apiObjectFactory.getAssetAmount("UTT", 100));
        container.clients.add(client);


        SerializedUserInfo car = new SerializedUserInfo();
        car.id = "car";
        car.name = "car";
        car.balance.add(apiObjectFactory.getAssetAmount("RA", 100));
        SerializedRailCarInfo carInfo = new SerializedRailCarInfo(car, "cp1");
        container.railCars.add(carInfo);


        SerializedUserInfo logist = new SerializedUserInfo();
        logist.id = "logist";
        logist.name = "logist";

        container.logist = logist;
        container.routeMapPrice = apiObjectFactory.getAssetAmount("UTT", 2);

        String s = objectMapper.writeValueAsString(container);
        assertNotNull(s);
        log.info(s);
    }

    private void createCP(ScenarioContainer container, String id, boolean station) {
        SerializedUserInfo cp7 = new SerializedUserInfo();
        cp7.id = id;
        cp7.name = id;
        cp7.balance.add(apiObjectFactory.getAssetAmount("RA", 100));
        container.infrastructure.add(new SerializedInfrastructureInfo(cp7, station));
    }

    @Override
    public Class<Config> getRootConfig() {
        return Config.class;
    }

    @Configuration
    @Import({
            TestWithJpaConfig.class,
            AppConfig.class
    })
    static class Config {

    }
}