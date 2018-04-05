package com.github.utransnet.simulator.externalapi.graphenej;

import com.github.utransnet.simulator.externalapi.Asset;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;

/**
 * Created by Artem on 02.04.2018.
 */
@AllArgsConstructor
@EqualsAndHashCode(of = "id")
public class AssetGraphene implements Asset, GrapheneWrapper<com.github.utransnet.graphenej.Asset> {

    @Getter
    private String id;

    public AssetGraphene(com.github.utransnet.graphenej.Asset asset) {
        id = asset.getObjectId();
    }

    @Override
    public String toString() {
        return id;
    }


    @Override
    public com.github.utransnet.graphenej.Asset getRaw() {
        return new com.github.utransnet.graphenej.Asset(id);
    }
}
