package com.apocollis.aqtweaks.qualitytools;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import com.google.common.collect.HashMultimap;
import com.tmtravlr.qualitytools.QualityToolsHelper;
import com.tmtravlr.qualitytools.config.ConfigLoader;
import com.tmtravlr.qualitytools.config.QualityEntry;
import com.tmtravlr.qualitytools.config.QualityType;

import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTTagString;
import net.minecraft.util.text.TextFormatting;

/**
 * Live QT {@code Quality} plus Tweaks {@code aqtweaks} stash. Compile-hard Quality Tools.
 */
public final class QualityNbt {
    public static final String QUALITY = "Quality";
    public static final String AQT = "aqtweaks";
    public static final String QUALITY_BASE = "QualityBase";
    public static final String WEAR_FLAG = "Wear";
    public static final String LAST_WEAR_CHECK = "LastWearCheck";

    public static final String COLOR_DARK_GRAY = "dark_gray";
    public static final String COLOR_GRAY = "gray";
    public static final String COLOR_RED = "red";
    public static final String COLOR_YELLOW = "yellow";
    public static final String COLOR_GREEN = "green";
    public static final String COLOR_BLUE = "blue";
    public static final String COLOR_GOLD = "gold";
    public static final String COLOR_NORMAL = "normal";

    private static final Map<Item, Boolean> QUALITY_ITEM_CACHE = new ConcurrentHashMap<>();

    private QualityNbt() {}

    public static boolean isQualityItem(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return false;
        }
        if (QualityToolsHelper.hasQualityTag(stack)) {
            return true;
        }
        if (ConfigLoader.qualityTypes == null || ConfigLoader.qualityTypes.isEmpty()) {
            return false;
        }
        Item item = stack.getItem();
        Boolean cached = QUALITY_ITEM_CACHE.get(item);
        if (Boolean.TRUE.equals(cached)) {
            return true;
        }
        if (matchingType(stack) == null) {
            return false;
        }
        QUALITY_ITEM_CACHE.put(item, Boolean.TRUE);
        return true;
    }

    public static QualityType matchingType(ItemStack stack) {
        if (stack == null || stack.isEmpty() || ConfigLoader.qualityTypes == null) {
            return null;
        }
        for (QualityType type : ConfigLoader.qualityTypes.values()) {
            if (type != null && type.itemMatches(stack)) {
                return type;
            }
        }
        return null;
    }

    public static String liveColor(ItemStack stack) {
        if (!QualityToolsHelper.hasQualityTag(stack)) {
            return COLOR_NORMAL;
        }
        NBTTagCompound quality = QualityToolsHelper.getQualityTag(stack);
        if (quality == null || quality.isEmpty()) {
            return COLOR_NORMAL;
        }
        String color = quality.getString("Color");
        return color == null || color.isEmpty() ? COLOR_NORMAL : color.toLowerCase();
    }

    public static String keptColor(ItemStack stack) {
        NBTTagCompound base = getQualityBase(stack);
        if (base != null && !base.isEmpty()) {
            String color = base.getString("Color");
            if (color != null && !color.isEmpty()) {
                return color.toLowerCase();
            }
            return COLOR_NORMAL;
        }
        return liveColor(stack);
    }

    public static boolean isDarkGray(ItemStack stack) {
        return COLOR_DARK_GRAY.equals(liveColor(stack));
    }

    public static boolean isGray(ItemStack stack) {
        return COLOR_GRAY.equals(liveColor(stack));
    }

    public static boolean isKeptColor(String color) {
        return COLOR_RED.equals(color)
                || COLOR_YELLOW.equals(color)
                || COLOR_GREEN.equals(color)
                || COLOR_BLUE.equals(color)
                || COLOR_GOLD.equals(color);
    }

    public static NBTTagCompound aqt(ItemStack stack, boolean create) {
        if (stack == null || stack.isEmpty()) {
            return create ? new NBTTagCompound() : null;
        }
        if (!stack.hasTagCompound()) {
            if (!create) {
                return null;
            }
            stack.setTagCompound(new NBTTagCompound());
        }
        NBTTagCompound root = stack.getTagCompound();
        if (!root.hasKey(AQT, 10)) {
            if (!create) {
                return null;
            }
            root.setTag(AQT, new NBTTagCompound());
        }
        return root.getCompoundTag(AQT);
    }

    public static NBTTagCompound getQualityBase(ItemStack stack) {
        NBTTagCompound aqt = aqt(stack, false);
        if (aqt == null || !aqt.hasKey(QUALITY_BASE, 10)) {
            return null;
        }
        return aqt.getCompoundTag(QUALITY_BASE);
    }

    public static void copyLiveToQualityBaseIfKept(ItemStack stack) {
        String color = liveColor(stack);
        if (!isKeptColor(color)) {
            return;
        }
        NBTTagCompound quality = QualityToolsHelper.getQualityTag(stack);
        if (quality == null || quality.isEmpty()) {
            return;
        }
        aqt(stack, true).setTag(QUALITY_BASE, quality.copy());
    }

    public static void setQualityBaseFromLive(ItemStack stack) {
        if (!QualityToolsHelper.hasQualityTag(stack)) {
            clearQualityBase(stack);
            return;
        }
        NBTTagCompound quality = QualityToolsHelper.getQualityTag(stack);
        aqt(stack, true).setTag(QUALITY_BASE, quality.copy());
    }

    public static void clearQualityBase(ItemStack stack) {
        NBTTagCompound aqt = aqt(stack, false);
        if (aqt != null) {
            aqt.removeTag(QUALITY_BASE);
        }
    }

    public static boolean hasWearFlag(ItemStack stack) {
        NBTTagCompound aqt = aqt(stack, false);
        return aqt != null && aqt.getBoolean(WEAR_FLAG);
    }

    public static void setWearFlag(ItemStack stack, boolean value) {
        if (value) {
            aqt(stack, true).setBoolean(WEAR_FLAG, true);
        } else {
            NBTTagCompound aqt = aqt(stack, false);
            if (aqt != null) {
                aqt.removeTag(WEAR_FLAG);
            }
        }
    }

    public static long lastWearCheck(ItemStack stack) {
        NBTTagCompound aqt = aqt(stack, false);
        return aqt == null ? 0L : aqt.getLong(LAST_WEAR_CHECK);
    }

    public static void setLastWearCheck(ItemStack stack, long time) {
        aqt(stack, true).setLong(LAST_WEAR_CHECK, time);
    }

    public static void stripLiveQuality(ItemStack stack) {
        if (stack.hasTagCompound()) {
            stack.getTagCompound().removeTag(QUALITY);
        }
    }

    public static void restoreFromBaseOrStrip(ItemStack stack) {
        NBTTagCompound base = getQualityBase(stack);
        setWearFlag(stack, false);
        if (base != null && !base.isEmpty()) {
            if (!stack.hasTagCompound()) {
                stack.setTagCompound(new NBTTagCompound());
            }
            stack.getTagCompound().setTag(QUALITY, base.copy());
        } else {
            stripLiveQuality(stack);
        }
    }

    public static void syncBaseAfterReforge(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return;
        }
        String color = liveColor(stack);
        if (isKeptColor(color)) {
            setQualityBaseFromLive(stack);
        } else {
            clearQualityBase(stack);
        }
        setWearFlag(stack, false);
    }

    public static boolean applyColorWouldSucceed(ItemStack stack, String color) {
        QualityType type = matchingType(stack);
        if (type == null || type.qualities == null) {
            return false;
        }
        for (QualityEntry entry : type.qualities) {
            if (entry == null || entry.color == null) {
                continue;
            }
            if (color.equalsIgnoreCase(entry.color.getFriendlyName())) {
                return true;
            }
        }
        return false;
    }

    public static boolean applyColor(ItemStack stack, String color) {
        QualityType type = matchingType(stack);
        if (type == null || type.qualities == null) {
            return false;
        }
        List<QualityEntry> matches = new ArrayList<>();
        for (QualityEntry entry : type.qualities) {
            if (entry == null || entry.color == null) {
                continue;
            }
            if (color.equalsIgnoreCase(entry.color.getFriendlyName())) {
                matches.add(entry);
            }
        }
        if (matches.isEmpty()) {
            return false;
        }
        QualityEntry pick = matches.get(QualityType.RAND.nextInt(matches.size()));
        applyEntry(stack, type, pick);
        return true;
    }

    public static boolean applyUniqueColor(ItemStack stack, String color) {
        QualityType type = matchingType(stack);
        QualityEntry entry = uniqueEntry(type, color);
        if (entry == null) {
            return false;
        }
        applyEntry(stack, type, entry);
        return true;
    }

    public static QualityEntry uniqueEntry(QualityType type, String color) {
        if (type == null || type.qualities == null) {
            return null;
        }
        QualityEntry found = null;
        for (QualityEntry entry : type.qualities) {
            if (entry == null || entry.color == null) {
                continue;
            }
            if (color.equalsIgnoreCase(entry.color.getFriendlyName())) {
                if (found != null) {
                    return null;
                }
                found = entry;
            }
        }
        return found;
    }

    public static void applyEntry(ItemStack stack, QualityType type, QualityEntry entry) {
        if (!stack.hasTagCompound()) {
            stack.setTagCompound(new NBTTagCompound());
        }
        stack.getTagCompound().removeTag(QUALITY);
        if (entry.name != null && entry.name.equalsIgnoreCase("normal")) {
            return;
        }
        NBTTagCompound quality = new NBTTagCompound();
        quality.setString("Name", entry.name == null ? "" : entry.name);
        TextFormatting formatting = entry.color;
        if (formatting != null) {
            quality.setString("Color", formatting.getFriendlyName());
        }
        NBTTagList slots = new NBTTagList();
        if (type.slots != null) {
            for (String slot : type.slots) {
                slots.appendTag(new NBTTagString(slot));
            }
        }
        quality.setTag("Slots", slots);
        NBTTagList modifiers = new NBTTagList();
        HashMultimap<String, AttributeModifier> map = entry.attributeMap;
        if (map != null) {
            for (String attr : map.keySet()) {
                for (AttributeModifier modifier : map.get(attr)) {
                    AttributeModifier copy = new AttributeModifier(
                            UUID.randomUUID(),
                            modifier.getName(),
                            modifier.getAmount(),
                            modifier.getOperation());
                    NBTTagCompound tag = SharedMonsterAttributes.writeAttributeModifierToNBT(copy);
                    tag.setString("AttributeName", attr);
                    modifiers.appendTag(tag);
                }
            }
        }
        quality.setTag("AttributeModifiers", modifiers);
        stack.getTagCompound().setTag(QUALITY, quality);
    }
}
