package com.github.utransnet.simulator.externalapi;

/**
 * Created by Artem on 02.04.2018.
 */
public class DefaultAssets {

    private final APIObjectFactory apiObjectFactory;

    public DefaultAssets(APIObjectFactory apiObjectFactory) {
        this.apiObjectFactory = apiObjectFactory;
    }

    public Asset getMainAsset() {
        return apiObjectFactory.getAsset("UTT");
    }

    public Asset getFeeAsset() {
        return apiObjectFactory.getAsset("UTT");
    }

    public Asset getResourceAsset() {
        return apiObjectFactory.getAsset("RA");
    }

    public Asset getMessageAsset() {
        return apiObjectFactory.getAsset("MSG");
    }
}
