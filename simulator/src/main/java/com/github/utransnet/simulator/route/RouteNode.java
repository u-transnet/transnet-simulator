package com.github.utransnet.simulator.route;

import com.github.utransnet.simulator.externalapi.AssetAmount;
import lombok.Getter;

/**
 * Created by Artem on 06.02.2018.
 */
@Getter
public class RouteNode {
    String name;
    long distance;
    AssetAmount fee;
    AssetAmount railCarFee;
}
