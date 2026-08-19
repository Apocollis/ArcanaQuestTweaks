package com.apocollis.aqtweaks.recipe;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Recipe paths Forge should not parse. Add more Metallurgy dead JSON here later.
 */
public final class RecipeJsonSkip {

    private static final Logger LOGGER = LogManager.getLogger("AQTweaks-Recipes");
    private static final String[] SKIP_CONTAINS = {
            "generated/item/spartanweaponry"
    };
    private static final AtomicBoolean LOGGED = new AtomicBoolean(false);

    private RecipeJsonSkip() {}

    public static boolean shouldSkip(Path file) {
        if (file == null) return false;
        String path = file.toString().replace('\\', '/');
        for (String needle : SKIP_CONTAINS) {
            if (path.contains(needle)) {
                if (LOGGED.compareAndSet(false, true)) {
                    LOGGER.info("Skipping Metallurgy recipe JSON under {}", needle);
                }
                return true;
            }
        }
        return false;
    }
}
