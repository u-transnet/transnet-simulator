package com.github.utransnet.simulator.route;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import com.github.utransnet.simulator.externalapi.AssetAmount;

import java.io.IOException;

/**
 * Created by Artem on 27.02.2018.
 */
public class AssetAmountSerializer extends StdSerializer<AssetAmount> {

    public AssetAmountSerializer() {
        this(AssetAmount.class);
    }

    protected AssetAmountSerializer(Class<AssetAmount> t) {
        super(t);
    }

    @Override
    public void serialize(AssetAmount value, JsonGenerator gen, SerializerProvider provider) throws IOException {
        gen.writeString(value.getAmount() + " " + value.getAsset().getId());
    }
}
