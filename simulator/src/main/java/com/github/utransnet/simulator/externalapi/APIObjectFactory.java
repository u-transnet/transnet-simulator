package com.github.utransnet.simulator.externalapi;

/**
 * Created by Artem on 02.02.2018.
 */
public interface APIObjectFactory {

    Asset getAsset(String idOrSymbol);

    AssetAmount getAssetAmount(Asset asset, long amount);

    default AssetAmount getAssetAmount(String idOrSymbol, long amount) {
        return getAssetAmount(getAsset(idOrSymbol), amount);
    }

    UserAccount userAccount(String id);

}
