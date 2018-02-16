package com.github.utransnet.simulator.externalapi;

/**
 * Created by Artem on 02.02.2018.
 */
public interface APIObjectFactory {

    Asset getAsset(String id);

    AssetAmount getAssetAmount(Asset asset, long amount);

    default AssetAmount getAssetAmount(String id, long amount) {
        return getAssetAmount(getAsset(id), amount);
    }

    UserAccount userAccount(String name);

}
