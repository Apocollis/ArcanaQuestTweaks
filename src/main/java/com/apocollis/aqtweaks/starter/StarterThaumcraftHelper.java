package com.apocollis.aqtweaks.starter;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.init.Items;
import net.minecraft.util.ResourceLocation;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import thaumcraft.api.ThaumcraftApi;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.capabilities.ThaumcraftCapabilities;
import thaumcraft.api.crafting.InfusionRecipe;

/** Thaumcraft side of Tabula Rasa. Only touched when Thaumcraft is loaded. */
public final class StarterThaumcraftHelper {

    private static final Logger LOG = LogManager.getLogger("aqtweaks");
    public static final String RESEARCH = "AQ_TABULA_RASA";

    private StarterThaumcraftHelper() {}

    public static boolean isResearchComplete(EntityPlayer player, String key) {
        return ThaumcraftCapabilities.knowsResearchStrict(player, key);
    }

    private static ItemStack find(String id, List<String> missing) {
        Item item = Item.REGISTRY.getObject(new ResourceLocation(id));
        if (item == null || item == Items.AIR) {
            missing.add(id);
            return ItemStack.EMPTY;
        }
        return new ItemStack(item, 1, 0);
    }

    /** Registers the Infusion recipe; skipped with one warning when an ingredient mod is missing. */
    public static boolean registerTabulaRasaInfusion() {
        if (StarterItemRegistry.TABULA_RASA == null) return false;
        List<String> missing = new ArrayList<>();
        ItemStack catalyst = find("contenttweaker:life_elixir", missing);
        ItemStack[] ring = {
                find("thaumcraft:void_seed", missing), find("botania:enderdagger", missing),
                find("astralsorcery:itemshiftingstar", missing), find("bewitchment:waystone", missing),
                find("embers:intelligent_apparatus", missing), find("da:aegyptia_metal_ingot", missing),
                find("da:draugr_ingot", missing), find("da:incendium_core", missing)};
        if (!missing.isEmpty()) {
            LOG.warn("[starter classes] Tabula Rasa infusion recipe skipped, missing items: {}", missing);
            return false;
        }
        AspectList aspects = new AspectList()
                .add(Aspect.MIND, 40).add(Aspect.EXCHANGE, 40).add(Aspect.VOID, 30)
                .add(Aspect.SOUL, 30).add(Aspect.MAGIC, 30);
        Object[] components = ring;
        ThaumcraftApi.addInfusionCraftingRecipe(new ResourceLocation("aqtweaks", "tabula_rasa"),
                new InfusionRecipe(RESEARCH, new ItemStack(StarterItemRegistry.TABULA_RASA), 4, aspects,
                        catalyst, components));
        return true;
    }
}
