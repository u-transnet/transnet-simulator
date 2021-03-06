package com.github.utransnet.simulator.testservices;

import com.github.utransnet.simulator.externalapi.Asset;
import com.github.utransnet.simulator.externalapi.AssetAmount;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;

/**
 * Created by Artem on 28.02.2018.
 */
@AllArgsConstructor
@EqualsAndHashCode(of = {"asset", "amount"})
class AssetAmount4Test implements AssetAmount {
    @Getter
    private Asset asset;
    @Getter
    private long amount;

    @Override
    public String toString() {
        return amount + " " + asset;
    }
}
