package com.github.utransnet.simulator.externalapi.h2impl;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

/**
 * Created by Artem on 28.02.2018.
 */
public class AssetH2Test {
    @Test
    public void equals() throws Exception {
        AssetH2 a1 = new AssetH2("a");
        AssetH2 a2 = new AssetH2("a");
        AssetH2 b = new AssetH2("b");
        assertEquals(a1, a2);
        assertNotEquals(a1, b);
    }

}