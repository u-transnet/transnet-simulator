package com.github.utransnet.simulator.externalapi;

/**
 * Created by Artem on 31.01.2018.
 */
public interface Asset {


    enum ASSETS {
        UTT("");
        String id;

        ASSETS(String id) {
            this.id = id;
        }
    }
}
