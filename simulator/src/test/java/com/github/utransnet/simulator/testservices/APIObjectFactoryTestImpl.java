package com.github.utransnet.simulator.testservices;

import com.github.utransnet.simulator.externalapi.*;

/**
 * Created by Artem on 09.02.2018.
 */
public class APIObjectFactoryTestImpl implements APIObjectFactory {

    private final ExternalAPI externalAPI;

    public APIObjectFactoryTestImpl(ExternalAPI externalAPI) {
        this.externalAPI = externalAPI;
    }

    @Override
    public Asset getAsset(String id) {
        return new Asset4Test(id);
    }

    @Override
    public AssetAmount getAssetAmount(Asset asset, long amount) {
        return new AssetAmount4Test(asset, amount);
    }

    @Override
    public UserAccount userAccount(String name) {
        return new UserAccount(externalAPI) {
            @Override
            public String getName() {
                return getId();
            }

            @Override
            public String getId() {
                return name;
            }
        };
    }

}
