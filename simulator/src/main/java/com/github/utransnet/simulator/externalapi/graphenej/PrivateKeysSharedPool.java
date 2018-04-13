package com.github.utransnet.simulator.externalapi.graphenej;

import org.bitcoinj.core.ECKey;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by Artem on 13.04.2018.
 */
public class PrivateKeysSharedPool {
    private final Map<String, ECKey> map = new HashMap<>(32);

    @Nullable
    public ECKey get(String key) {
        return map.get(key);
    }

    public ECKey put(String key, ECKey value) {
        return map.put(key, value);
    }

    public ECKey remove(String key) {
        return map.remove(key);
    }
}
