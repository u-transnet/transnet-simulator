package com.github.utransnet.simulator.externalapi;

/**
 * Created by Artem on 02.02.2018.
 */
public interface APIObjectFactory {

    AssetAmount createAssetAmount();
    AssetAmount createAssetAmount(Asset asset, long amount);
    Asset createAsset();

}
