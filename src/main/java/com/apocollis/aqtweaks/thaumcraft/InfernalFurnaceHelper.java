package com.apocollis.aqtweaks.thaumcraft;

import com.apocollis.aqtweaks.ArcanaQuestTweaksConfig;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.FurnaceRecipes;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.registry.ForgeRegistries;
import net.minecraftforge.oredict.OreDictionary;
import thaumcraft.api.ThaumcraftApi.SmeltBonus;
import thaumcraft.api.internal.CommonInternals;
import thaumcraft.api.items.ItemsTC;

import java.util.ArrayList;
import java.util.List;

/**
 * Furnace recipes CraftTweaker removed for gated metals, restored on the Infernal Furnace only.
 * One ingot per cook. Nugget and Rare Earth rolls apply to ores, and only when that ore does not
 * already have the same bonus in {@code CommonInternals.smeltingBonus}.
 */
public final class InfernalFurnaceHelper {

    private static final float NUGGET_CHANCE = 0.33F;
    private static final float RARE_EARTH_CHANCE = 0.02F;
    private static final int RARE_EARTH_META = 10;
    private static final int TC_SILVER_NUGGET_META = 3;

    private static final GatedMetal[] METALS = {
            metal("oreGold", "dustGold", "nuggetGold", "ingotGold",
                    "minecraft:gold_ingot", "minecraft:gold_nugget", 1.0F, false, false,
                    new String[]{"minecraft:gold_ore", "gravelores:gold_gravel_ore"},
                    new String[]{"metallurgy:gold_dust"}),
            metal("oreSilver", "dustSilver", "nuggetSilver", "ingotSilver",
                    "metallurgy:silver_ingot", "metallurgy:silver_nugget", 0.7F, false, true,
                    new String[]{"metallurgy:silver_ore", "embers:ore_silver", "gravelores:silver_gravel_ore"},
                    new String[]{"metallurgy:silver_dust", "embers:dust_silver"}),
            metal("orePlatinum", "dustPlatinum", "nuggetPlatinum", "ingotPlatinum",
                    "metallurgy:platinum_ingot", "metallurgy:platinum_nugget", 0.7F, false, false,
                    new String[]{"metallurgy:platinum_ore"},
                    new String[]{"metallurgy:platinum_dust"}),
            metal("oreDeepIron", "dustDeepIron", "nuggetDeepIron", "ingotDeepIron",
                    "metallurgy:deep_iron_ingot", "metallurgy:deep_iron_nugget", 0.7F, false, false,
                    new String[]{"metallurgy:deep_iron_ore"},
                    new String[]{"metallurgy:deep_iron_dust"}),
            metal("oreShadowIron", "dustShadowIron", "nuggetShadowIron", "ingotShadowIron",
                    "metallurgy:shadow_iron_ingot", "metallurgy:shadow_iron_nugget", 0.7F, false, false,
                    new String[]{"metallurgy:shadow_iron_ore"},
                    new String[]{"metallurgy:shadow_iron_dust"}),
            metal("oreIgnatius", "dustIgnatius", "nuggetIgnatius", "ingotIgnatius",
                    "metallurgy:ignatius_ingot", "metallurgy:ignatius_nugget", 0.7F, false, false,
                    new String[]{"metallurgy:ignatius_ore"},
                    new String[]{"metallurgy:ignatius_dust"}),
            metal("oreVulcanite", "dustVulcanite", "nuggetVulcanite", "ingotVulcanite",
                    "metallurgy:vulcanite_ingot", "metallurgy:vulcanite_nugget", 0.7F, false, false,
                    new String[]{"metallurgy:vulcanite_ore"},
                    new String[]{"metallurgy:vulcanite_dust"}),
            metal("oreKalendrite", "dustKalendrite", "nuggetKalendrite", "ingotKalendrite",
                    "metallurgy:kalendrite_ingot", "metallurgy:kalendrite_nugget", 0.7F, false, false,
                    new String[]{"metallurgy:kalendrite_ore"},
                    new String[]{"metallurgy:kalendrite_dust"}),
            metal("oreMithril", "dustMithril", "nuggetMithril", "ingotMithril",
                    "metallurgy:mithril_ingot", "metallurgy:mithril_nugget", 0.7F, false, false,
                    new String[]{"metallurgy:mithril_ore"},
                    new String[]{"metallurgy:mithril_dust"}),
            metal("oreOrichalcum", "dustOrichalcum", "nuggetOrichalcum", "ingotOrichalcum",
                    "metallurgy:orichalcum_ingot", "metallurgy:orichalcum_nugget", 0.7F, false, false,
                    new String[]{"metallurgy:orichalcum_ore"},
                    new String[]{"metallurgy:orichalcum_dust"}),
            metal("oreSanguinite", "dustSanguinite", "nuggetSanguinite", "ingotSanguinite",
                    "metallurgy:sanguinite_ingot", "metallurgy:sanguinite_nugget", 0.7F, false, false,
                    new String[]{"metallurgy:sanguinite_ore"},
                    new String[]{"metallurgy:sanguinite_dust"}),
            metal("oreVyroxeres", "dustVyroxeres", "nuggetVyroxeres", "ingotVyroxeres",
                    "metallurgy:vyroxeres_ingot", "metallurgy:vyroxeres_nugget", 0.7F, false, false,
                    new String[]{"metallurgy:vyroxeres_ore"},
                    new String[]{"metallurgy:vyroxeres_dust"}),
            metal("oreCeruclase", "dustCeruclase", "nuggetCeruclase", "ingotCeruclase",
                    "metallurgy:ceruclase_ingot", "metallurgy:ceruclase_nugget", 0.7F, false, false,
                    new String[]{"metallurgy:ceruclase_ore"},
                    new String[]{"metallurgy:ceruclase_dust"}),
            metal("oreAtlarus", "dustAtlarus", "nuggetAtlarus", "ingotAtlarus",
                    "metallurgy:atlarus_ingot", "metallurgy:atlarus_nugget", 0.7F, false, false,
                    new String[]{"metallurgy:atlarus_ore"},
                    new String[]{"metallurgy:atlarus_dust"}),
            metal("oreManganese", "dustManganese", "nuggetManganese", "ingotManganese",
                    "metallurgy:manganese_ingot", "metallurgy:manganese_nugget", 0.7F, false, false,
                    new String[]{"metallurgy:manganese_ore"},
                    new String[]{"metallurgy:manganese_dust"}),
            metal("oreEximite", "dustEximite", "nuggetEximite", "ingotEximite",
                    "metallurgy:eximite_ingot", "metallurgy:eximite_nugget", 0.7F, false, false,
                    new String[]{"metallurgy:eximite_ore"},
                    new String[]{"metallurgy:eximite_dust"}),
            metal("oreMeutoite", "dustMeutoite", "nuggetMeutoite", "ingotMeutoite",
                    "metallurgy:meutoite_ingot", "metallurgy:meutoite_nugget", 0.7F, false, false,
                    new String[]{"metallurgy:meutoite_ore"},
                    new String[]{"metallurgy:meutoite_dust"}),
            metal("oreMoonstone", "dustMoonstone", "nuggetMoonstone", "ingotMoonstone",
                    "theaurorian:moonstoneingot", "theaurorian:moonstonenugget", 0.7F, false, false,
                    new String[]{"theaurorian:moonstoneore"},
                    new String[0]),
            metal("oreCerulean", "dustCerulean", "nuggetCerulean", "ingotCerulean",
                    "theaurorian:ceruleaningot", "theaurorian:ceruleannugget", 0.7F, false, false,
                    new String[]{"theaurorian:ceruleanore"},
                    new String[0]),
            metal("oreAstralStarmetal", null, "nuggetAstralStarmetal", "ingotAstralStarmetal",
                    "astralsorcery:itemcraftingcomponent:1", null, 0.7F, true, false,
                    new String[]{"astralsorcery:blockcustomore:1"},
                    new String[0])
    };

    private InfernalFurnaceHelper() {}

    public static ItemStack getSmeltingResult(FurnaceRecipes recipes, ItemStack input) {
        if (input == null || input.isEmpty()) {
            return ItemStack.EMPTY;
        }
        ItemStack vanilla = recipes.getSmeltingResult(input);
        if (vanilla != null && !vanilla.isEmpty()) {
            return vanilla;
        }
        if (!enabled()) {
            return ItemStack.EMPTY;
        }
        GatedMetal metal = findInput(input);
        if (metal == null) {
            return ItemStack.EMPTY;
        }
        return metal.resolveIngot();
    }

    public static float getSmeltingExperience(FurnaceRecipes recipes, ItemStack output) {
        float xp = output == null ? 0.0F : recipes.getSmeltingExperience(output);
        if (xp > 0.0F || output == null || output.isEmpty() || !enabled()) {
            return xp;
        }
        GatedMetal metal = findOutput(output);
        return metal == null ? xp : metal.xp;
    }

    public static ItemStack[] appendGatedSmeltingBonus(ItemStack input, World world, ItemStack[] baseBonuses) {
        if (input == null || input.isEmpty() || world == null || world.isRemote || !enabled()) {
            return baseBonuses;
        }
        GatedMetal metal = findOre(input);
        if (metal == null) {
            return baseBonuses;
        }
        boolean hasNugget = false;
        boolean hasRareEarth = false;
        List<SmeltBonus> bonuses = CommonInternals.smeltingBonus;
        if (bonuses != null) {
            for (int i = 0, n = bonuses.size(); i < n; i++) {
                SmeltBonus bonus = bonuses.get(i);
                if (bonus == null || !matchesBonus(bonus, input)) {
                    continue;
                }
                if (isRareEarth(bonus.out)) {
                    hasRareEarth = true;
                } else if (outputsNugget(metal, bonus.out)) {
                    hasNugget = true;
                }
            }
        }
        List<ItemStack> extra = new ArrayList<ItemStack>(2);
        if (!hasNugget && world.rand.nextFloat() <= NUGGET_CHANCE) {
            ItemStack nugget = metal.resolveNugget();
            if (!nugget.isEmpty()) {
                extra.add(nugget);
            }
        }
        if (!hasRareEarth && world.rand.nextFloat() <= RARE_EARTH_CHANCE) {
            ItemStack rare = rareEarth();
            if (!rare.isEmpty()) {
                extra.add(rare);
            }
        }
        if (extra.isEmpty()) {
            return baseBonuses;
        }
        ItemStack[] base = baseBonuses == null ? new ItemStack[0] : baseBonuses;
        ItemStack[] combined = new ItemStack[base.length + extra.size()];
        System.arraycopy(base, 0, combined, 0, base.length);
        for (int i = 0; i < extra.size(); i++) {
            combined[base.length + i] = extra.get(i);
        }
        return combined;
    }

    private static boolean enabled() {
        return ArcanaQuestTweaksConfig.ThaumcraftConfig.enableInfernalFurnaceGatedMetals;
    }

    private static GatedMetal findInput(ItemStack stack) {
        for (GatedMetal metal : METALS) {
            if (metal.isInput(stack)) {
                return metal;
            }
        }
        return null;
    }

    private static GatedMetal findOre(ItemStack stack) {
        for (GatedMetal metal : METALS) {
            if (metal.isOre(stack)) {
                return metal;
            }
        }
        return null;
    }

    private static GatedMetal findOutput(ItemStack stack) {
        for (GatedMetal metal : METALS) {
            if (metal.isOutput(stack)) {
                return metal;
            }
        }
        return null;
    }

    private static boolean matchesBonus(SmeltBonus bonus, ItemStack input) {
        Object key = bonus.in;
        if (key instanceof ItemStack) {
            return sameItem((ItemStack) key, input);
        }
        if (key instanceof String) {
            return inOreDict(input, (String) key);
        }
        return false;
    }

    private static boolean outputsNugget(GatedMetal metal, ItemStack bonusOut) {
        if (bonusOut == null || bonusOut.isEmpty()) {
            return false;
        }
        if (sameItem(bonusOut, metal.resolveNugget())) {
            return true;
        }
        if (metal.nuggetDict != null && inOreDict(bonusOut, metal.nuggetDict)) {
            return true;
        }
        return metal.tcSilverNugget && isTcNugget(bonusOut, TC_SILVER_NUGGET_META);
    }

    private static boolean isRareEarth(ItemStack stack) {
        return isTcNugget(stack, RARE_EARTH_META);
    }

    private static boolean isTcNugget(ItemStack stack, int meta) {
        return stack != null && !stack.isEmpty() && ItemsTC.nuggets != null
                && stack.getItem() == ItemsTC.nuggets && stack.getMetadata() == meta;
    }

    private static ItemStack rareEarth() {
        if (ItemsTC.nuggets == null) {
            return ItemStack.EMPTY;
        }
        return new ItemStack(ItemsTC.nuggets, 1, RARE_EARTH_META);
    }

    private static boolean sameItem(ItemStack a, ItemStack b) {
        if (a == null || b == null || a.isEmpty() || b.isEmpty() || a.getItem() != b.getItem()) {
            return false;
        }
        int ma = a.getMetadata();
        int mb = b.getMetadata();
        return ma == OreDictionary.WILDCARD_VALUE || mb == OreDictionary.WILDCARD_VALUE || ma == mb;
    }

    private static boolean inOreDict(ItemStack stack, String name) {
        if (stack == null || stack.isEmpty() || name == null) {
            return false;
        }
        int[] ids = OreDictionary.getOreIDs(stack);
        for (int id : ids) {
            if (name.equals(OreDictionary.getOreName(id))) {
                return true;
            }
        }
        return false;
    }

    private static boolean matchesId(ItemStack stack, String spec) {
        if (stack == null || stack.isEmpty() || spec == null) {
            return false;
        }
        ResourceLocation name = stack.getItem().getRegistryName();
        if (name == null) {
            return false;
        }
        int split = spec.lastIndexOf(':');
        int first = spec.indexOf(':');
        String id = spec;
        int meta = 0;
        if (split > first) {
            id = spec.substring(0, split);
            try {
                meta = Integer.parseInt(spec.substring(split + 1));
            } catch (NumberFormatException ignored) {
                return false;
            }
        }
        return name.toString().equals(id) && stack.getMetadata() == meta;
    }

    private static ItemStack itemStack(String spec) {
        if (spec == null) {
            return ItemStack.EMPTY;
        }
        int split = spec.lastIndexOf(':');
        int first = spec.indexOf(':');
        String id = spec;
        int meta = 0;
        if (split > first) {
            id = spec.substring(0, split);
            try {
                meta = Integer.parseInt(spec.substring(split + 1));
            } catch (NumberFormatException ignored) {
                return ItemStack.EMPTY;
            }
        }
        int colon = id.indexOf(':');
        if (colon <= 0) {
            return ItemStack.EMPTY;
        }
        Item item = ForgeRegistries.ITEMS.getValue(new ResourceLocation(id.substring(0, colon), id.substring(colon + 1)));
        if (item == null || item == Items.AIR) {
            return ItemStack.EMPTY;
        }
        return new ItemStack(item, 1, meta);
    }

    private static ItemStack firstOre(String name) {
        if (name == null || !OreDictionary.doesOreNameExist(name)) {
            return ItemStack.EMPTY;
        }
        List<ItemStack> ores = OreDictionary.getOres(name, false);
        if (ores == null || ores.isEmpty()) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = ores.get(0);
        if (stack == null || stack.isEmpty()) {
            return ItemStack.EMPTY;
        }
        ItemStack copy = stack.copy();
        if (copy.getMetadata() == OreDictionary.WILDCARD_VALUE) {
            copy.setItemDamage(0);
        }
        copy.setCount(1);
        return copy;
    }

    private static GatedMetal metal(String oreDict, String dustDict, String nuggetDict, String ingotDict,
                                    String ingotId, String nuggetId, float xp, boolean stardustFallback,
                                    boolean tcSilverNugget, String[] explicitOres, String[] explicitDusts) {
        return new GatedMetal(oreDict, dustDict, nuggetDict, ingotDict, ingotId, nuggetId, xp,
                stardustFallback, tcSilverNugget, explicitOres, explicitDusts);
    }

    private static final class GatedMetal {
        final String oreDict;
        final String dustDict;
        final String nuggetDict;
        final String ingotDict;
        final String ingotId;
        final String nuggetId;
        final float xp;
        final boolean stardustFallback;
        final boolean tcSilverNugget;
        final String[] explicitOres;
        final String[] explicitDusts;

        GatedMetal(String oreDict, String dustDict, String nuggetDict, String ingotDict,
                   String ingotId, String nuggetId, float xp, boolean stardustFallback,
                   boolean tcSilverNugget, String[] explicitOres, String[] explicitDusts) {
            this.oreDict = oreDict;
            this.dustDict = dustDict;
            this.nuggetDict = nuggetDict;
            this.ingotDict = ingotDict;
            this.ingotId = ingotId;
            this.nuggetId = nuggetId;
            this.xp = xp;
            this.stardustFallback = stardustFallback;
            this.tcSilverNugget = tcSilverNugget;
            this.explicitOres = explicitOres;
            this.explicitDusts = explicitDusts;
        }

        boolean isOre(ItemStack stack) {
            if (inOreDict(stack, oreDict)) {
                return true;
            }
            for (String id : explicitOres) {
                if (matchesId(stack, id)) {
                    return true;
                }
            }
            return false;
        }

        boolean isInput(ItemStack stack) {
            if (isOre(stack)) {
                return true;
            }
            if (dustDict != null && inOreDict(stack, dustDict)) {
                return true;
            }
            for (String id : explicitDusts) {
                if (matchesId(stack, id)) {
                    return true;
                }
            }
            return false;
        }

        boolean isOutput(ItemStack stack) {
            if (sameItem(stack, resolveIngot())) {
                return true;
            }
            return inOreDict(stack, ingotDict);
        }

        ItemStack resolveIngot() {
            ItemStack explicit = itemStack(ingotId);
            if (!explicit.isEmpty()) {
                return explicit;
            }
            return firstOre(ingotDict);
        }

        ItemStack resolveNugget() {
            ItemStack explicit = itemStack(nuggetId);
            if (!explicit.isEmpty()) {
                return explicit;
            }
            ItemStack fromDict = firstOre(nuggetDict);
            if (!fromDict.isEmpty()) {
                return fromDict;
            }
            if (stardustFallback) {
                return itemStack("astralsorcery:itemcraftingcomponent:2");
            }
            return ItemStack.EMPTY;
        }
    }
}
