package com.github.utransnet.simulator.route;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.github.utransnet.simulator.externalapi.APIObjectFactory;
import com.github.utransnet.simulator.externalapi.AssetAmount;
import org.apache.commons.lang3.math.NumberUtils;

import java.io.IOException;

/**
 * Created by Artem on 27.02.2018.
 */
public class AssetAmountDeserializer extends StdDeserializer<AssetAmount> {
    private final APIObjectFactory apiObjectFactory;

    public AssetAmountDeserializer(APIObjectFactory apiObjectFactory) {
        this(AssetAmount.class, apiObjectFactory);
    }

    protected AssetAmountDeserializer(Class<?> vc, APIObjectFactory apiObjectFactory) {
        super(vc);
        this.apiObjectFactory = apiObjectFactory;
    }

    @Override
    public AssetAmount deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        JsonNode node = p.getCodec().readTree(p);
        String s = node.asText();
        String[] split = s.split(" ");
        if (split.length != 2) {
            throw new RuntimeException("Wrong AssetAmount string: " + s);
        }
        if (!NumberUtils.isParsable(split[0])) {
            throw new RuntimeException("Wrong AssetAmount amount: " + split[0]);
        }
        return apiObjectFactory.getAssetAmount(split[1], Long.parseLong(split[0]));
    }
}
