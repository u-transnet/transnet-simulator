package com.github.utransnet.simulator.externalapi.h2impl;

import com.github.utransnet.simulator.externalapi.Asset;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;

/**
 * Created by Artem on 16.02.2018.
 */
@AllArgsConstructor
@EqualsAndHashCode(of = "id")
class AssetH2 implements Asset {
    @Getter
    private String id;

    @Override
    public String toString() {
        return id;
    }
}
