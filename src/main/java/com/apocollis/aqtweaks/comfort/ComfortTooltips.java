package com.apocollis.aqtweaks.comfort;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.input.Keyboard;

/** Cozy block item lines, and the Rested breakdown shown on the inventory Comfort tab. */
@SideOnly(Side.CLIENT)
public final class ComfortTooltips {

    @SubscribeEvent
    public void onItemTooltip(ItemTooltipEvent event) {
        ItemStack stack = event.getItemStack();
        if (stack.isEmpty() || !(stack.getItem() instanceof ItemBlock)) return;
        Block block = ((ItemBlock) stack.getItem()).getBlock();
        ResourceLocation name = block.getRegistryName();
        if (name == null) return;
        PacketComfortCatalog.Row row = ComfortClientState.catalog.get(name.toString());
        if (row == null) return;
        List<String> lines = event.getToolTip();
        String counted = "";
        int[] counts = ComfortClientState.categories.get(row.category);
        if (counts != null) {
            counted = " " + counts[1] + " of " + counts[2] + " counted";
        }
        lines.add(TextFormatting.GOLD + capitalize(row.category) + " " + trim(row.weight) + counted);
        if (Keyboard.isKeyDown(Keyboard.KEY_LSHIFT) || Keyboard.isKeyDown(Keyboard.KEY_RSHIFT)) {
            lines.add(TextFormatting.DARK_GRAY + "Family " + row.family);
        }
    }

    /**
     * Lines for the Comfort tab hover box, below its on/off header. Rested shows whenever the effect is on the
     * player, at home or not. Hearthside shows only while the player is also resting at home.
     */
    public static List<String> tabLines() {
        List<String> text = new ArrayList<>();
        Minecraft mc = Minecraft.getMinecraft();
        PotionEffect rested = mc.player == null ? null : mc.player.getActivePotionEffect(PotionHomestead.INSTANCE);
        if (rested != null) {
            int seconds = Math.max(0, rested.getDuration() / 20);
            text.add(TextFormatting.GOLD + "Rested " + ComfortClientState.roman(rested.getAmplifier() + 1)
                    + TextFormatting.GRAY + String.format(java.util.Locale.ROOT, "  %d:%02d left", seconds / 60, seconds % 60));
            if (ComfortClientState.insight > 0.0f) {
                text.add(TextFormatting.YELLOW
                        + String.format(java.util.Locale.ROOT, "Insight: +%.0f%% XP from orbs", ComfortClientState.insight * 100.0f));
            }
            if (ComfortClientState.vigorInterval > 0 && ComfortClientState.vigorInterval < 30) {
                text.add(TextFormatting.GREEN + "Vigor: restores 10 feathers in "
                        + ComfortClientState.vigorInterval + "s (base 30s)");
            }
            if (ComfortClientState.halfHearts > 0) {
                text.add(TextFormatting.RED + "Fortitude: +" + (ComfortClientState.halfHearts / 2) + " hearts, +"
                        + (ComfortClientState.halfFeathers / 2) + " feathers");
            }
            boolean hasClarity = mc.player != null && mc.player.isPotionActive(PotionClarity.INSTANCE);
            if ((hasClarity || ComfortClientState.state == 2) && ComfortClientState.granted > 0 && Loader.isModLoaded("thaumcraft")) {
                PotionEffect clarity = mc.player == null ? null : mc.player.getActivePotionEffect(PotionClarity.INSTANCE);
                int tier = clarity != null ? clarity.getAmplifier() + 1 : Math.max(1, ComfortClientState.granted);
                int minutes = PotionClarity.secondsPerWarp(tier - 1) / 60;
                String rate = minutes == 1 ? "1 warp per min" : "1 warp per " + minutes + " min";
                text.add(TextFormatting.AQUA + "* Hearthside: At Home (Cleansing warp - " + rate + ")");
            }
        }

        if (ComfortClientState.present) {
            if (rested != null) {
                text.add("");
            }
            String scoreTierName = ComfortClientState.scoreTier > 0
                    ? "Tier " + ComfortClientState.roman(ComfortClientState.scoreTier)
                    : "Below Tier I";
            text.add(TextFormatting.GOLD + "Comfort Score: " + TextFormatting.WHITE
                    + trim(ComfortClientState.effective) + TextFormatting.GRAY + " (" + scoreTierName + ")");

            StringBuilder formula = new StringBuilder();
            formula.append(TextFormatting.DARK_GRAY).append("Cozy ").append(TextFormatting.GRAY).append(trim(ComfortClientState.cozy));
            if (ComfortClientState.bonus > 0.0f) {
                formula.append(TextFormatting.DARK_GRAY).append(" | ").append(TextFormatting.GREEN).append("+").append(trim(ComfortClientState.bonus)).append(" bonus");
            }
            if (ComfortClientState.penalty > 0.0f) {
                formula.append(TextFormatting.DARK_GRAY).append(" | ").append(TextFormatting.RED).append("-").append(trim(ComfortClientState.penalty)).append(" penalty");
            }
            text.add(formula.toString());

            text.add(TextFormatting.GRAY + ComfortHud.nextLine());

            if (ComfortClientState.missingFurniture) {
                text.add(TextFormatting.RED + "Held back by: Missing required furniture");
            } else if (ComfortClientState.hurtTicks > 0) {
                text.add(TextFormatting.RED + "Held back by: Hurt cooldown (" + (ComfortClientState.hurtTicks / 20) + "s)");
            } else if (ComfortClientState.attackTicks > 0) {
                text.add(TextFormatting.RED + "Held back by: Attack cooldown (" + (ComfortClientState.attackTicks / 20) + "s)");
            } else if (ComfortClientState.penalty > 0.0f) {
                text.add(TextFormatting.RED + ComfortHud.heldBack());
            }

            if (!ComfortClientState.categoryScores.isEmpty()) {
                text.add(TextFormatting.YELLOW + "Cozy Furnishings (" + trim(ComfortClientState.cozy) + "):");
                for (ComfortClientState.CategoryScore cat : ComfortClientState.categoryScores) {
                    if (cat.amount <= 0.0f && cat.counted <= 0) continue;
                    text.add("  " + TextFormatting.GRAY + capitalize(cat.name) + ": "
                            + TextFormatting.WHITE + trim(cat.amount)
                            + TextFormatting.DARK_GRAY + " (" + cat.counted + "/" + cat.limit + ")");
                }
            }

            if (ComfortClientState.bonus > 0.0f) {
                text.add(TextFormatting.GREEN + "Active Bonuses (+" + trim(ComfortClientState.bonus) + "):");
                for (PacketComfortStatus.Line line : ComfortClientState.lines) {
                    if ((line.kind == 6 || line.kind == 7) && line.amount > 0.0f) {
                        text.add("  " + TextFormatting.GRAY + ComfortClientState.lineLabel(line) + ": "
                                + TextFormatting.GREEN + "+" + trim(line.amount));
                    }
                }
            }

            if (ComfortClientState.penalty > 0.0f) {
                text.add(TextFormatting.RED + "Active Penalties (-" + trim(ComfortClientState.penalty) + "):");
                for (PacketComfortStatus.Line line : ComfortClientState.lines) {
                    if (line.kind != 6 && line.kind != 7 && line.amount > 0.0f) {
                        text.add("  " + TextFormatting.GRAY + ComfortClientState.lineLabel(line) + ": "
                                + TextFormatting.RED + "-" + trim(line.amount));
                    }
                }
            }

            if (ComfortClientState.state == 1 && ComfortClientState.scoreTier >= 1
                    && !ComfortClientState.missingFurniture
                    && ComfortClientState.hurtTicks <= 0 && ComfortClientState.attackTicks <= 0) {
                text.add(TextFormatting.DARK_GRAY + "Sit, sneak, or lie down to rest");
            }
        } else {
            if (rested != null) {
                text.add(TextFormatting.DARK_GRAY + "(Away from home - no comfort area nearby)");
            } else {
                text.add(TextFormatting.DARK_GRAY + "No comfort area nearby");
            }
        }
        return text;
    }

    private static String capitalize(String category) {
        if (category == null || category.isEmpty()) return "";
        return Character.toUpperCase(category.charAt(0)) + category.substring(1);
    }

    private static String trim(float value) {
        if (value == Math.rint(value)) return Integer.toString((int) value);
        return String.format(java.util.Locale.ROOT, "%.1f", value);
    }
}
