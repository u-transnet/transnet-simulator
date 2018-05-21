package com.github.utransnet.simulator.externalapi.h2impl;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

/**
 * Created by Artem on 28.02.2018.
 */
public class AssetAmountH2Test {
    @Test
    public void equals() throws Exception {
        AssetH2 a1 = new AssetH2("a1");
        AssetH2 a2 = new AssetH2("a2");
        AssetAmountH2 a1v10 = new AssetAmountH2(a1, 10);
        AssetAmountH2 a1v20 = new AssetAmountH2(a1, 20);
        AssetAmountH2 a2v10 = new AssetAmountH2(a2, 10);

        assertEquals(a1v10, new AssetAmountH2(a1, 10));
        assertNotEquals(a1v10, a1v20);
        assertNotEquals(a1v10, a2v10);
    }

}