package com.github.utransnet.simulator.externalapi;

/**
 * Created by Artem on 31.01.2018.
 */
public interface AssetAmount extends ExternalObject {
    Asset getAsset();
    long getAmount();
}
