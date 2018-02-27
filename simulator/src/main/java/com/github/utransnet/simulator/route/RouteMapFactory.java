package com.github.utransnet.simulator.route;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.github.utransnet.simulator.externalapi.AssetAmount;
import lombok.SneakyThrows;
import org.springframework.context.ApplicationContext;

import javax.annotation.PostConstruct;
import java.io.IOException;

/**
 * Created by Artem on 06.02.2018.
 */
public class RouteMapFactory {
    private final ApplicationContext applicationContext;
    private final AssetAmountDeserializer assetAmountDeserializer;
    private final AssetAmountSerializer assetAmountSerializer;
    private ObjectMapper objectMapper;

    public RouteMapFactory(ApplicationContext applicationContext, AssetAmountDeserializer assetAmountDeserializer, AssetAmountSerializer assetAmountSerializer) {
        this.applicationContext = applicationContext;
        this.assetAmountDeserializer = assetAmountDeserializer;
        this.assetAmountSerializer = assetAmountSerializer;
    }


    @PostConstruct
    private void init() {
        objectMapper = new ObjectMapper();
        SimpleModule module = new SimpleModule();
        module.addDeserializer(AssetAmount.class, assetAmountDeserializer);
        module.addSerializer(AssetAmount.class, assetAmountSerializer);
        objectMapper.registerModule(module);
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
