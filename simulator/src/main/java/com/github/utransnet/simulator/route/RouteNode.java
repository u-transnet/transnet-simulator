package com.github.utransnet.simulator.route;

import com.github.utransnet.simulator.externalapi.AssetAmount;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Created by Artem on 06.02.2018.
 */
@Getter
@AllArgsConstructor
public class RouteNode {
    String id;
    int distance;
    AssetAmount fee;
    AssetAmount railCarFee;
}
