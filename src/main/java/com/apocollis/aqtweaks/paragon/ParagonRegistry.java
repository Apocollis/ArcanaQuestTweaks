package com.apocollis.aqtweaks.paragon;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.apocollis.aqtweaks.ArcanaQuestTweaksConfig;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ResourceLocation;

/** The traits, the nine-item rule and the reads the dispatcher makes of a stack. */
public final class ParagonRegistry {
    private ParagonRegistry() {}

    /** Our trait id on the stack. */
    public static final String NBT_KEY = "arcana_paragon";
    /** Metallurgy's own Paragon marker; an item with it takes no trait of ours. */
    public static final String NBT_METALLURGY_KEY = "paragon";

    private static final Map<String, ParagonTrait> TRAITS = new LinkedHashMap<>();
    private static final List<ParagonTrait> WEARER_TRAITS = new ArrayList<>();
    /** Item to kind; a missing key is "not looked up yet", {@link #NOT_TARTARITE} marks a checked plain item. */
    private static final Map<Item, Object> KIND_CACHE = new IdentityHashMap<>();
    private static final Object NOT_TARTARITE = new Object();

    private static String[] disabledSource;
    private static Set<String> disabledIds = Collections.emptySet();

    public static void register(ParagonTrait trait) {
        TRAITS.put(trait.getId(), trait);
        if (trait.tracksWearer()) WEARER_TRAITS.add(trait);
    }

    public static ParagonTrait get(String id) {
        return id == null ? null : TRAITS.get(id);
    }

    public static Iterable<ParagonTrait> all() {
        return TRAITS.values();
    }

    public static List<ParagonTrait> wearerTraits() {
        return WEARER_TRAITS;
    }

    /** True when absorbing is on and this trait is not in the disabled list. */
    public static boolean isEnabled(ParagonTrait trait) {
        if (trait == null || !ArcanaQuestTweaksConfig.ParagonModuleConfig.general.enable) return false;
        String[] source = ArcanaQuestTweaksConfig.ParagonModuleConfig.general.disabledTraits;
        if (source != disabledSource) {
            Set<String> ids = new HashSet<>();
            for (String raw : source) {
                if (raw != null && !raw.trim().isEmpty()) ids.add(raw.trim());
            }
            disabledIds = ids;
            disabledSource = source;
        }
        return !disabledIds.contains(trait.getId());
    }

    /** The Tartarite kind of a stack, or {@code null} for any other item. Read once per item type. */
    public static TartariteKind kindOf(ItemStack stack) {
        if (stack.isEmpty()) return null;
        Item item = stack.getItem();
        Object cached = KIND_CACHE.get(item);
        if (cached == null) {
            ResourceLocation name = item.getRegistryName();
            TartariteKind kind = name == null ? null : TartariteKind.fromPath(name.getNamespace(), name.getPath());
            cached = kind == null ? NOT_TARTARITE : kind;
            KIND_CACHE.put(item, cached);
        }
        return cached == NOT_TARTARITE ? null : (TartariteKind) cached;
    }

    /** The id stored on a stack, or {@code null}; no registry lookup. */
    public static String storedId(ItemStack stack) {
        NBTTagCompound tag = stack.getTagCompound();
        return tag != null && tag.hasKey(NBT_KEY, 8) ? tag.getString(NBT_KEY) : null;
    }

    public static boolean hasMetallurgyParagon(ItemStack stack) {
        NBTTagCompound tag = stack.getTagCompound();
        return tag != null && tag.hasKey(NBT_METALLURGY_KEY);
    }

    /** The enabled trait the stack carries, or {@code null}. Cheap enough for per-event use. */
    public static ParagonTrait traitOf(ItemStack stack) {
        if (stack.isEmpty() || !stack.hasTagCompound() || kindOf(stack) == null) return null;
        String id = storedId(stack);
        if (id == null) return null;
        ParagonTrait trait = TRAITS.get(id);
        return isEnabled(trait) ? trait : null;
    }

    /** The trait all four worn pieces carry, or {@code null}. */
    public static ParagonTrait fullSetTrait(EntityPlayer player) {
        String helmet = wornId(player, EntityEquipmentSlot.HEAD);
        if (helmet == null) return null;
        String id = ParagonRules.fullSet(helmet, wornId(player, EntityEquipmentSlot.CHEST),
                wornId(player, EntityEquipmentSlot.LEGS), wornId(player, EntityEquipmentSlot.FEET));
        ParagonTrait trait = id == null ? null : TRAITS.get(id);
        return isEnabled(trait) ? trait : null;
    }

    private static String wornId(EntityPlayer player, EntityEquipmentSlot slot) {
        ItemStack stack = player.getItemStackFromSlot(slot);
        if (stack.isEmpty() || !stack.hasTagCompound()) return null;
        TartariteKind kind = kindOf(stack);
        return kind != null && kind.isArmor() ? storedId(stack) : null;
    }
}
