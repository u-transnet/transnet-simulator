package com.github.utransnet.simulator.testservices;

import com.github.utransnet.simulator.externalapi.Asset;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;

/**
 * Created by Artem on 28.02.2018.
 */
@AllArgsConstructor
@EqualsAndHashCode(of = "id")
class Asset4Test implements Asset {
    @Getter
    private String id;

    @Override
    public String toString() {
        return id;
    }

    @Override
    public String getSymbol() {
        return id;
    }

    @Override
    public void refresh() {

    }
}
