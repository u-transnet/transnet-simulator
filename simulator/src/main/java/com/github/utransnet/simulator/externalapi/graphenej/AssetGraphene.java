package com.github.utransnet.simulator.externalapi.graphenej;

import com.github.utransnet.simulator.externalapi.Asset;
import lombok.*;

/**
 * Created by Artem on 02.04.2018.
 */
@AllArgsConstructor
@EqualsAndHashCode(of = "id")
public class AssetGraphene implements Asset, GrapheneWrapper<com.github.utransnet.graphenej.Asset> {

    private final ExternalAPIGraphene externalAPI;

    @Getter
    private String id;

    @Getter
    @Setter(AccessLevel.PACKAGE)
    private String symbol;

    AssetGraphene(ExternalAPIGraphene externalAPI) {
        this.externalAPI = externalAPI;
    }


    void fromAsset(com.github.utransnet.graphenej.Asset asset) {
        id = asset.getObjectId();
        symbol = asset.getSymbol();
    }

    void setId(String id) {
        if (id.contains("1.3.")) { // id
            setId(id, true);
        } else { // symbol
            setId(id, false);
        }
    }

    void setId(String id, boolean lazy) {
        this.id = id;
        if (!lazy) {
            refresh();
        }
    }

    @Override
    public String toString() {
        return id + " " + symbol;
    }


    @Override
    public com.github.utransnet.graphenej.Asset getRaw() {
        return new com.github.utransnet.graphenej.Asset(id);
    }

    @Override
    public void refresh() {
        com.github.utransnet.graphenej.Asset assetSymbol = externalAPI.getAssetSymbol(id);
        if (assetSymbol != null) {
            id = assetSymbol.getObjectId();
            symbol = assetSymbol.getSymbol();
        } else {
            throw new IllegalArgumentException("Wrong asset id or symbol: " + id);
        }
    }


}
