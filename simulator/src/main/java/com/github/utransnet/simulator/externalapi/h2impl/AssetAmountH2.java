package com.github.utransnet.simulator.externalapi.h2impl;

import com.github.utransnet.simulator.externalapi.Asset;
import com.github.utransnet.simulator.externalapi.AssetAmount;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;

/**
 * Created by Artem on 16.02.2018.
 */
@AllArgsConstructor
@EqualsAndHashCode(of = {"asset", "amount"})
class AssetAmountH2 implements AssetAmount {
    @Getter
    private Asset asset;
    @Getter
    private long amount;
}
