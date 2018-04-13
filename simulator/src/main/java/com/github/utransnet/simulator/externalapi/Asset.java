package com.github.utransnet.simulator.externalapi;

import javax.annotation.Nullable;

/**
 * Created by Artem on 31.01.2018.
 */
public interface Asset extends ExternalObject {

    @Nullable
    String getSymbol();

    void refresh();


    enum ASSETS {
        UTT("");
        String id;

        ASSETS(String id) {
            this.id = id;
        }
    }
}
