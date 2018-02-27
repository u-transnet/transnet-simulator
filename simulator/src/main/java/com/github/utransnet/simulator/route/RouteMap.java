package com.github.utransnet.simulator.route;

import com.github.utransnet.simulator.externalapi.*;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by Artem on 31.01.2018.
 */

@EqualsAndHashCode(of = {"id"})
public class RouteMap {
    @Getter
    @Setter(AccessLevel.PACKAGE)
    private String id;

    private int step = 0;

    @Getter(AccessLevel.PACKAGE)
    @Setter(AccessLevel.PACKAGE)
    private List<RouteNode> route;

    private final ExternalAPI externalAPI;
    private final APIObjectFactory apiObjectFactory;


    public RouteMap(ExternalAPI externalAPI, APIObjectFactory apiObjectFactory) {
        this.externalAPI = externalAPI;
        this.apiObjectFactory = apiObjectFactory;
    }

    public UserAccount getStart() {
        return externalAPI.getAccountByName(route.get(0).name);
    }

    public UserAccount getNextAccount(){
        return externalAPI.getAccountByName(route.get(step).name);
    }

    public AssetAmount getNextFee() {
        return route.get(step).fee;
    }

    public AssetAmount getNextRailCarFee() {
        return route.get(step).railCarFee;
    }

    public long getNextDistance() {
        return route.get(step).distance;
    }

    public Map<Asset, Long> getTotalFee() {
        return route.stream().map(RouteNode::getFee).sequential().collect(
                HashMap::new,
                (map, fee) -> map.compute(
                        fee.getAsset(),
                        (asset, aLong) -> aLong != null ? aLong + fee.getAmount() : fee.getAmount()
                ),
                (map1, map2) -> {} // no parallel work
        );
    }

    public long getTotalDistance() {
        return route.stream().mapToLong(RouteNode::getDistance).sum();
    }

    public boolean goNext() {
        if(step < route.size()) {
            step++;
            return true;
        }
        return false;
    }

}
