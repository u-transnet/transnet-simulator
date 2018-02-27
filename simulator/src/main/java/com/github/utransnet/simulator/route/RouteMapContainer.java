package com.github.utransnet.simulator.route;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * Created by Artem on 07.02.2018.
 */
@Getter
@Setter
class RouteMapContainer {
    String id;

    List<RouteNode> route;

    RouteMapContainer() {
    }

    RouteMapContainer(RouteMap routeMap) {
        id = routeMap.getId();
        route = routeMap.getRoute();
    }
}
