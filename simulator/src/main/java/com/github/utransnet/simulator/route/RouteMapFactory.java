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
    private final ObjectMapper objectMapper;

    public RouteMapFactory(ApplicationContext applicationContext, ObjectMapper objectMapper) {
        this.applicationContext = applicationContext;
        this.objectMapper = objectMapper;
    }

    @SneakyThrows({IOException.class})
    public RouteMap fromJson(String json) {
        RouteMap routeMap = applicationContext.getBean(RouteMap.class);
        RouteMapContainer routeMapContainer = objectMapper.readValue(json, RouteMapContainer.class);
        routeMap.setRoute(routeMapContainer.route);
        routeMap.setId(routeMapContainer.id);
        return routeMap;
    }

    @SneakyThrows
    public String toJson(RouteMap routeMap) {
        return objectMapper.writeValueAsString(new RouteMapContainer(routeMap));
    }
}
