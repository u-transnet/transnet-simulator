package com.github.utransnet.simulator.route;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import org.springframework.context.ApplicationContext;

import java.io.IOException;

/**
 * Created by Artem on 06.02.2018.
 */
public class RouteMapFactory {
    private final ApplicationContext applicationContext;

    public RouteMapFactory(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    @SneakyThrows({IOException.class})
    public RouteMap fromJson(String json) {
        RouteMap routeMap = applicationContext.getBean(RouteMap.class);
        RouteMapContainer routeMapContainer = new ObjectMapper().readValue(json, RouteMapContainer.class);
        routeMap.setRoute(routeMapContainer.route);
        routeMap.setId(routeMap.getId());
        return routeMap;
    }

    @SneakyThrows
    public String toJson(RouteMap routeMap) {
        return new ObjectMapper().writeValueAsString(routeMap);
    }
}
