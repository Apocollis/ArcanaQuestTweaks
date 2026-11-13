package com.apocollis.aqtweaks.rarity;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import net.minecraft.enchantment.Enchantment;
import net.minecraft.item.EnumRarity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.ResourceLocation;

/**
 * Resolves the displayed rarity of a stack. Called by FQCN from the early {@code ItemStack.getRarity} and
 * {@code Item.getForgeRarity} mixins. Order: native rarity, enchantment mapping, quality mapping, custom override.
 * The highest of the three traits wins and a trait never lowers the tier.
 */
public final class RarityOverrides {
    private RarityOverrides() {}

    /** Quality Tools stores its roll in this root compound; Color is the TextFormatting friendly name. */
    private static final String QUALITY_TAG = "Quality";
    private static final String QUALITY_COLOR = "Color";

    /** Unenchanted rating per item, used to tell vanilla's enchant bump from a genuinely Rare item. */
    private static final Map<Long, RarityTier> PROBE = new ConcurrentHashMap<>();

    public static void invalidate() {
        PROBE.clear();
    }

    public static EnumRarity resolve(ItemStack stack, EnumRarity nativeRarity) {
        if (stack == null || stack.isEmpty() || nativeRarity == null) return nativeRarity;
        RarityModule.State s = RarityModule.state();
        if (!s.enabled) return nativeRarity;
        RarityTier nativeTier = RarityRegistry.tierOf(nativeRarity);
        if (nativeTier == null) return nativeRarity;

        RarityTier tier = nativeTier;
        NBTTagCompound tag = stack.getTagCompound();
        if (tag != null) {
            RarityTier enchant = s.enchantMapping ? enchantTier(stack, tag, s) : null;
            if (enchant != null) {
                tier = RarityTier.max(RarityEnchantTier.base(nativeTier, probe(stack)), enchant);
            }
            if (s.qualityMapping) {
                tier = RarityTier.max(tier, qualityTier(tag, s));
            }
        }

        if (!s.overrides.isEmpty()) {
            RarityTier forced = override(stack, s);
            if (forced != null) tier = forced;
        }
        return tier == nativeTier ? nativeRarity : RarityRegistry.forTier(tier);
    }

    private static RarityTier enchantTier(ItemStack stack, NBTTagCompound tag, RarityModule.State s) {
        if (!tag.hasKey("ench", 9)) return null;
        NBTTagList list = stack.getEnchantmentTagList();
        if (list == null || list.isEmpty()) return null;
        List<RarityEnchantTier.EnchRank> ranks = new ArrayList<>(list.tagCount());
        for (int i = 0; i < list.tagCount(); i++) {
            Enchantment e = Enchantment.getEnchantmentByID(list.getCompoundTagAt(i).getShort("id"));
            if (e == null || e.isCurse()) continue;
            RarityEnchantTier.EnchRank r = RarityEnchantTier.rank(e.getRarity().name());
            if (r != null) ranks.add(r);
        }
        return RarityEnchantTier.best(s.enchantMap, ranks);
    }

    private static RarityTier qualityTier(NBTTagCompound tag, RarityModule.State s) {
        if (!tag.hasKey(QUALITY_TAG, 10)) return null;
        return RarityQualityTier.tierFor(s.qualityMap, tag.getCompoundTag(QUALITY_TAG).getString(QUALITY_COLOR));
    }

    /** Cache key: item id in the high word, subtype meta in the low word. Damage is not a subtype, so it maps to 0. */
    static long probeKey(int itemId, boolean hasSubtypes, int meta) {
        return ((long) itemId << 32) | ((hasSubtypes ? meta : 0) & 0xFFFFFFFFL);
    }

    private static RarityTier probe(ItemStack stack) {
        Item item = stack.getItem();
        int meta = item.getHasSubtypes() ? stack.getMetadata() : 0;
        long key = probeKey(Item.getIdFromItem(item), item.getHasSubtypes(), meta);
        RarityTier t = PROBE.get(key);
        if (t == null) {
            // Called directly on Item, so the rarity mixins are not re-entered.
            RarityTier r;
            try {
                r = RarityRegistry.tierOf(item.getRarity(new ItemStack(item, 1, meta)));
            } catch (Throwable ex) {
                r = null;
            }
            // An unprobeable item is treated as genuinely Rare, so its native Rare is kept (never lowered).
            t = r == null ? RarityTier.RARE : r;
            PROBE.put(key, t);
        }
        return t;
    }

    private static RarityTier override(ItemStack stack, RarityModule.State s) {
        ResourceLocation id = stack.getItem().getRegistryName();
        if (id == null) return null;
        return s.overrides.lookup(id.getNamespace(), id.getPath(), stack.getMetadata());
    }
}
