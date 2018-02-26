package com.github.utransnet.simulator.externalapi.h2impl;

import com.github.utransnet.simulator.SpringTest;
import com.github.utransnet.simulator.externalapi.Asset;
import com.github.utransnet.simulator.externalapi.AssetAmount;
import com.github.utransnet.simulator.externalapi.ExternalAPI;
import com.github.utransnet.simulator.externalapi.UserAccount;
import com.github.utransnet.simulator.testservices.ExternalAPIEmptyImpl;
import lombok.Getter;
import lombok.Setter;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;

import static org.junit.Assert.*;

/**
 * Created by Artem on 16.02.2018.
 */
public class APIObjectFactoryH2Test extends SpringTest<APIObjectFactoryH2Test.Config> {

    @SuppressWarnings("SpringJavaAutowiredMembersInspection")
    @Autowired
    APIObjectFactoryH2 apiObjectFactory;

    @Test
    public void getAsset() throws Exception {
        assertEquals("asset", apiObjectFactory.getAsset("asset").getId());

    }

    @Test
    public void getAssetAmount() throws Exception {
        Asset asset = apiObjectFactory.getAsset("asset");
        AssetAmount assetAmount1 = apiObjectFactory.getAssetAmount(asset, 10);
        assertEquals(asset, assetAmount1.getAsset());
        assertEquals(10, assetAmount1.getAmount());

        AssetAmount assetAmount2 = apiObjectFactory.getAssetAmount("asset", 10);
        assertEquals(asset, assetAmount2.getAsset());
        assertEquals(10, assetAmount2.getAmount());
    }

    @Test
    public void userAccount() throws Exception {
        UserAccount userAccount1 = apiObjectFactory.userAccount("test-user");
        assertEquals("test-user", userAccount1.getId());
        UserAccount userAccount2 = apiObjectFactory.userAccount("test-user");
        assertEquals(userAccount1, userAccount2);
        UserAccount userAccount3 = apiObjectFactory.userAccount("other-user");
        assertNotEquals(userAccount1, userAccount3);
    }

    @Override
    public Class<Config> getRootConfig() {
        return Config.class;
    }

    @Configuration
    public static class Config {

        @Bean
        @Scope("singleton")
        @Autowired
        APIObjectFactoryH2 apiObjectFactoryH2(ApplicationContext context){
            return new APIObjectFactoryH2(context);
        }

        @Bean
        @Scope("singleton")
        ExternalAPI externalAPI(){
            return new ExternalAPIEmptyImpl();
        }

        @Bean
        @Scope("prototype")
        @Autowired
        UserAccount userAccount(ExternalAPI externalAPI) {
            return new UserAccountH2(externalAPI);
        }

    }

}