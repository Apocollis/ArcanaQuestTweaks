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
            "generated/item/spartanweaponry",
            "draugr_ingot_from_block"
    };

    private static final String[] SKIP_ITEMS = {
            "autooredictconv:auto_converter",
            "betterwithmods:steel_block",
            "bibliocraft:atlasbook",
            "bibliocraft:bell",
            "bibliocraft:biblioclipboard",
            "bibliocraft:bibliodrill",
            "bibliocraft:compass",
            "bibliocraft:cookiejar",
            "bibliocraft:dinnerplate",
            "bibliocraft:discrack",
            "bibliocraft:fancyworkbench",
            "bibliocraft:framedchest",
            "bibliocraft:framingboard",
            "bibliocraft:framingsaw",
            "bibliocraft:framingsheet",
            "bibliocraft:handdrill",
            "bibliocraft:lampgold",
            "bibliocraft:lampiron",
            "bibliocraft:lanterngold",
            "bibliocraft:lanterniron",
            "bibliocraft:plumbline",
            "bibliocraft:stockroomcatalog",
            "bibliocraft:tape",
            "bibliocraft:typewriter",
            "da:lightning_smithing_stone",
            "depthsupdate:amethyst_shard",
            "depthsupdate:raw_copper_block",
            "depthsupdate:raw_gold_block",
            "depthsupdate:raw_iron_block",
            "futuremc:campfire",
            "lycanitesmobs:belpharm",
            "xreliquary:apothecary_cauldron",
            "xreliquary:apothecary_mortar",
            "xreliquary:attraction_potion",
            "xreliquary:bullet",
            "xreliquary:fertile_potion",
            "xreliquary:gun_part",
            "xreliquary:magazine",
            "xreliquary:potion",
            "xreliquary:tipped_arrow"
    };

    private static final String[] SKIP_ITEM_NEEDLES = new String[SKIP_ITEMS.length];
    static {
        for (int i = 0; i < SKIP_ITEMS.length; i++) {
            SKIP_ITEM_NEEDLES[i] = "\"" + SKIP_ITEMS[i] + "\"";
        }
    }

    private static final AtomicBoolean LOGGED = new AtomicBoolean(false);
    private static final java.util.Set<String> LOGGED_ITEMS = java.util.Collections.newSetFromMap(new java.util.concurrent.ConcurrentHashMap<>());

    private RecipeJsonSkip() {}

    public static boolean shouldSkip(Path file) {
        if (file == null) return false;
        String path = file.toString().replace('\\', '/');
        for (String needle : SKIP_CONTAINS) {
            if (path.contains(needle)) {
                if (needle.contains("spartanweaponry") && LOGGED.compareAndSet(false, true)) {
                    LOGGER.info("Skipping Metallurgy recipe JSON under {}", needle);
                }
                return true;
            }
        }

        if (path.endsWith(".json")) {
            try {
                String content = java.nio.file.Files.readString(file, java.nio.charset.StandardCharsets.UTF_8);
                for (int i = 0; i < SKIP_ITEM_NEEDLES.length; i++) {
                    if (content.contains(SKIP_ITEM_NEEDLES[i])) {
                        String itemId = SKIP_ITEMS[i];
                        if (LOGGED_ITEMS.add(itemId)) {
                            LOGGER.info("Skipping recipe JSON with known-missing item {}", itemId);
                        }
                        return true;
                    }
                }
            } catch (Exception ignored) {
                // IO failure -> do not skip, Forge handles it
                return false;
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
