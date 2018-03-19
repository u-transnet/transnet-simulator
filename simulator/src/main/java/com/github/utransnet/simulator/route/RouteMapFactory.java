package com.github.utransnet.simulator.route;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.Nullable;
import org.springframework.context.ApplicationContext;
import org.springframework.util.Assert;

import java.io.IOException;

/**
 * Created by Artem on 06.02.2018.
 */
@Slf4j
public class RouteMapFactory {
    private final ApplicationContext applicationContext;
    private final ObjectMapper objectMapper;

    public RouteMapFactory(ApplicationContext applicationContext, ObjectMapper objectMapper) {
        this.applicationContext = applicationContext;
        this.objectMapper = objectMapper;
    }

    public RouteMap fromJsonForce(String json) throws JsonParseException {
        RouteMap routeMap = applicationContext.getBean(RouteMap.class);
        RouteMapContainer routeMapContainer = null;
        try {
            routeMapContainer = objectMapper.readValue(json, RouteMapContainer.class);
        } catch (JsonParseException e) {
            log.warn("String '" + json + "' doesn't contain valid RouteMap");
            throw e;
        } catch (IOException e) {
            log.error("Unexpected error in RouteMap deserialization", e);
        }
        routeMap.setRoute(routeMapContainer.route);
        routeMap.setId(routeMapContainer.id);
        Assert.notNull(routeMap.getStart(), "Route map must have start");
        return routeMap;
    }

    @Nullable
    public RouteMap fromJson(String json) {
        try {
            return fromJsonForce(json);
        } catch (JsonParseException e) {
            return null;
        }
    }

    @SneakyThrows
    public String toJson(RouteMap routeMap) {
        return objectMapper.writeValueAsString(new RouteMapContainer(routeMap));
    }
}
