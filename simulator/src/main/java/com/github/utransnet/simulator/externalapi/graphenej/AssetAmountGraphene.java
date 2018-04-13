package com.github.utransnet.simulator.externalapi.graphenej;


import com.github.utransnet.simulator.externalapi.AssetAmount;
import com.google.common.primitives.UnsignedLong;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;

/**
 * Created by Artem on 02.04.2018.
 */
@AllArgsConstructor
@EqualsAndHashCode(of = {"asset", "amount"})
public class AssetAmountGraphene implements AssetAmount, GrapheneWrapper<com.github.utransnet.graphenej.AssetAmount> {


    @Getter
    private AssetGraphene asset;
    @Getter
    private long amount;

    @Override
    public String toString() {
        return amount + " " + asset;
    }

    @Override
    public com.github.utransnet.graphenej.AssetAmount getRaw() {
        return new com.github.utransnet.graphenej.AssetAmount(UnsignedLong.valueOf(amount), asset.getRaw());
    }
}
