package com.github.utransnet.simulator.route;

import com.github.utransnet.simulator.externalapi.Asset;
import com.github.utransnet.simulator.externalapi.AssetAmount;
import com.github.utransnet.simulator.externalapi.ExternalAPI;
import com.github.utransnet.simulator.externalapi.UserAccount;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

/**
 * Created by Artem on 31.01.2018.
 */

@EqualsAndHashCode(of = {"id"})
public class RouteMap {
    @Getter
    @Setter(AccessLevel.PACKAGE)
    private String id;

    @Getter
    private int step = 0;

    @Getter
    @Setter(AccessLevel.PACKAGE)
    private List<RouteNode> route;

    private final ExternalAPI externalAPI;


    public RouteMap(ExternalAPI externalAPI) {
        this.externalAPI = externalAPI;
    }

    public UserAccount getStart() {
        return externalAPI.getAccountById(route.get(0).id);
    }

    public UserAccount getNextAccount(){
        return externalAPI.getAccountById(route.get(step).id);
    }

    public AssetAmount getNextFee() {
        return route.get(step).fee;
    }

    public AssetAmount getNextRailCarFee() {
        return route.get(step).railCarFee;
    }

    public int getNextDistance() {
        return route.get(step).distance;
    }

    public Map<Asset, Long> getTotalFee() {
        Stream<AssetAmount> assetAmountStream = route.stream().map(RouteNode::getFee);
        return collectFee(assetAmountStream);
    }

    public Map<Asset, Long> getTotalRailCarFee() {
        Stream<AssetAmount> assetAmountStream = route.stream().map(RouteNode::getRailCarFee);
        return collectFee(assetAmountStream);
    }

    public Map<Asset, Long> getFeeSum() {
        Stream<AssetAmount> feeStream = route.stream().map(RouteNode::getRailCarFee);
        Stream<AssetAmount> railCarFeeStream = route.stream().map(RouteNode::getFee);
        return collectFee(Stream.concat(feeStream, railCarFeeStream));
    }

    private HashMap<Asset, Long> collectFee(Stream<AssetAmount> assetAmountStream) {
        return assetAmountStream.sequential().collect(
                HashMap::new,
                (map, fee) -> map.compute(
                        fee.getAsset(),
                        (asset, aLong) -> aLong != null ? aLong + fee.getAmount() : fee.getAmount()
                ),
                (map1, map2) -> {} // no parallel work
        );
    }

    public int getTotalDistance() {
        return route.stream().mapToInt(RouteNode::getDistance).sum();
    }

    public boolean goNext() {
        if (step < route.size() - 1) {
            step++;
            return true;
        }
        return false;
    }

}
