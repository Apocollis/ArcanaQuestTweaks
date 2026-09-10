package com.apocollis.aqtweaks.client;

import com.apocollis.aqtweaks.ArcanaQuestTweaksConfig;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemAxe;
import net.minecraft.item.ItemPickaxe;
import net.minecraft.item.ItemSpade;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemTool;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.ModContainer;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Client overlay/tooltip tweaks. Toughness Bar placement is a mixin; this handler
 * copies Metallurgy 4 Reforged tool-stat lines onto non-Metallurgy mining tools.
 */
@SideOnly(Side.CLIENT)
public class ClientModule {

    private static final String STAR = "\u2B51";
    private static final TextFormatting[] SCALE_COLORS = {
            TextFormatting.DARK_RED,
            TextFormatting.RED,
            TextFormatting.GOLD,
            TextFormatting.YELLOW,
            TextFormatting.DARK_GREEN,
            TextFormatting.AQUA,
            TextFormatting.LIGHT_PURPLE
    };

    /** The loaded mod list is fixed for the session, so this never needs invalidating. */
    private static final Map<String, String> MOD_NAMES = new HashMap<>();

    @SubscribeEvent
    public void onItemTooltip(ItemTooltipEvent event) {
        if (!ArcanaQuestTweaksConfig.ClientModuleConfig.tooltips.metallurgyTooltipCompat) {
            return;
        }
        ItemStack stack = event.getItemStack();
        if (stack == null || stack.isEmpty()) {
            return;
        }
        Item item = stack.getItem();
        ResourceLocation id = item.getRegistryName();
        if (id == null || "metallurgy".equals(id.getNamespace())) {
            return;
        }
        if (!isMiningTool(item, stack)) {
            return;
        }

        List<String> tooltip = event.getToolTip();
        tooltip.add(TextFormatting.BLUE + categoryName(id.getNamespace()) + " Tools");

        int harvest = maxHarvestLevel(item, stack, event.getEntityPlayer());
        if (harvest >= 0) {
            int stars = MathHelper.clamp(harvest + 1, 1, 7);
            tooltip.add(TextFormatting.GRAY + "Harvest Level: " + SCALE_COLORS[stars - 1] + starsString(stars));
        }

        int maxDamage = stack.getMaxDamage();
        if (maxDamage > 0) {
            int remaining = maxDamage - stack.getItemDamage();
            float ratio = remaining / (float) maxDamage;
            TextFormatting useColor;
            if (ratio < 0.33F) {
                useColor = TextFormatting.RED;
            } else if (ratio < 0.66F) {
                useColor = TextFormatting.YELLOW;
            } else {
                useColor = TextFormatting.GREEN;
            }
            tooltip.add(TextFormatting.GRAY + "Durability: " + useColor + remaining + '/' + maxDamage);
        }

        float efficiency = baseEfficiency(item, stack);
        int colorIndex = MathHelper.ceil(MathHelper.clamp(efficiency - 6.0F, 0.0F, 6.0F));
        tooltip.add(TextFormatting.GRAY + "Efficiency: " + SCALE_COLORS[colorIndex] + String.valueOf(efficiency));
    }

    private static boolean isMiningTool(Item item, ItemStack stack) {
        if (item instanceof ItemPickaxe || item instanceof ItemAxe || item instanceof ItemSpade
                || item instanceof ItemTool) {
            return true;
        }
        Set<String> classes = item.getToolClasses(stack);
        return classes != null && (classes.contains("pickaxe") || classes.contains("axe") || classes.contains("shovel"));
    }

    private static String categoryName(String modid) {
        if ("minecraft".equals(modid)) {
            return "Vanilla";
        }
        String cached = MOD_NAMES.get(modid);
        if (cached != null) {
            return cached;
        }
        String name = modid;
        ModContainer container = Loader.instance().getIndexedModList().get(modid);
        if (container != null && container.getName() != null && !container.getName().isEmpty()) {
            name = container.getName();
        }
        MOD_NAMES.put(modid, name);
        return name;
    }

    private static int maxHarvestLevel(Item item, ItemStack stack, EntityPlayer player) {
        int harvest = -1;
        Set<String> classes = item.getToolClasses(stack);
        if (classes != null) {
            for (String toolClass : classes) {
                harvest = Math.max(harvest, item.getHarvestLevel(stack, toolClass, player, null));
            }
        }
        return harvest;
    }

    private static float baseEfficiency(Item item, ItemStack stack) {
        IBlockState state = Blocks.STONE.getDefaultState();
        Set<String> classes = item.getToolClasses(stack);
        if (classes != null) {
            if (classes.contains("axe")) {
                state = Blocks.LOG.getDefaultState();
            } else if (classes.contains("shovel")) {
                state = Blocks.DIRT.getDefaultState();
            }
        } else if (item instanceof ItemAxe) {
            state = Blocks.LOG.getDefaultState();
        } else if (item instanceof ItemSpade) {
            state = Blocks.DIRT.getDefaultState();
        }
        return item.getDestroySpeed(stack, state);
    }

    private static String starsString(int count) {
        StringBuilder sb = new StringBuilder(count * STAR.length());
        for (int i = 0; i < count; i++) {
            sb.append(STAR);
        }
        return sb.toString();
    }
}