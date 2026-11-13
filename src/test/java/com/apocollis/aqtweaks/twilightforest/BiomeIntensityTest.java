package com.apocollis.aqtweaks.twilightforest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;
import org.junit.jupiter.api.Test;

class BiomeIntensityTest {
    @Test
    void parsesIntensityAndSkipsMalformed() {
        Map<String, Double> t = BiomeIntensity.parseIntensity(new String[] {"clearing=0.6", "bad", "x=notanumber", "darkForest=1.3"});
        assertEquals(2, t.size());
        assertEquals(0.6D, BiomeIntensity.intensityOf(t, "clearing", 1.6D), 1e-9);
        assertEquals(1.0D, BiomeIntensity.intensityOf(t, "twilightForest", 1.6D), 1e-9);
    }

    @Test
    void intensityIsCapped() {
        Map<String, Double> t = BiomeIntensity.parseIntensity(new String[] {"highlandsCenter=3.0"});
        assertEquals(1.6D, BiomeIntensity.intensityOf(t, "highlandsCenter", 1.6D), 1e-9);
    }

    @Test
    void parsesExtras() {
        Map<String, BiomeIntensity.Extra> t = BiomeIntensity.parseExtras(
                new String[] {"darkForest=minecraft:blindness,40", "tfSwamp=minecraft:weakness,80,1", "bad=oops"});
        assertEquals(2, t.size());
        assertEquals("minecraft:blindness", t.get("darkForest").effect);
        assertEquals(40, t.get("darkForest").ticks);
        assertEquals(1, t.get("tfSwamp").amplifier);
        assertNull(t.get("bad"));
        assertTrue(t.containsKey("tfSwamp"));
    }
}
