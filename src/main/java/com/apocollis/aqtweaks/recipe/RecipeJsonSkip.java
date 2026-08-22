package com.apocollis.aqtweaks.recipe;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiFunction;

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

    /**
     * Wrap a CraftingHelper {@code findFiles} processor. Skip lives here so the mixin
     * does not inject a lambda into Forge.
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    public static BiFunction wrapProcessor(BiFunction processor, String base) {
        if (processor == null || base == null || !base.contains("/recipes")) {
            return processor;
        }
        return (root, file) -> {
            if (file instanceof Path && shouldSkip((Path) file)) {
                return Boolean.TRUE;
            }
            return processor.apply(root, file);
        };
    }
}
