package com.apocollis.aqtweaks.reskillable;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTTagString;

/**
 * Which toggleable perks a player has switched off. Server: the player's persisted NBT (survives death, relog and
 * dimension change). Client: a mirror of the local player's list, filled by {@code PacketSyncPerkToggles}; it applies
 * to the local player only, so other players' perks read as enabled on this client. No client-only types here, so
 * {@link PerkAccess} can call it from either side.
 */
public final class PerkToggleState {

    public static final String NBT_KEY = "AQTDisabledPerks";

    private static final Set<String> CLIENT_DISABLED = ConcurrentHashMap.newKeySet();
    private static volatile UUID clientPlayer;

    private PerkToggleState() {}

    /** True when {@code id} is toggleable and this player switched it off. Does not check that it is unlocked. */
    public static boolean isDisabled(EntityPlayer player, String id) {
        if (player == null || !ToggleablePerks.isToggleable(id)) return false;
        if (player.world != null && player.world.isRemote) {
            UUID local = clientPlayer;
            return local != null && local.equals(player.getUniqueID()) && CLIENT_DISABLED.contains(id);
        }
        return read(player).contains(id);
    }

    /** Flips the flag and returns the new state (true = now disabled). Server only. */
    public static boolean toggle(EntityPlayer player, String id) {
        List<String> list = read(player);
        boolean nowDisabled = !list.remove(id);
        if (nowDisabled) list.add(id);
        write(player, list);
        return nowDisabled;
    }

    /** Removes the flag (the perk was refunded, so a re-bought perk starts enabled). */
    public static boolean clear(EntityPlayer player, String id) {
        List<String> list = read(player);
        boolean removed = list.remove(id);
        if (removed) write(player, list);
        return removed;
    }

    /** Removes every disabled flag (class reset). Server only. */
    public static void clearAll(EntityPlayer player) {
        if (!read(player).isEmpty()) write(player, new ArrayList<>());
    }

    public static List<String> disabledList(EntityPlayer player) {
        List<String> list = read(player);
        list.removeIf(id -> !ToggleablePerks.isToggleable(id));
        return list;
    }

    private static List<String> read(EntityPlayer player) {
        List<String> out = new ArrayList<>();
        NBTTagCompound persisted = player.getEntityData().getCompoundTag(EntityPlayer.PERSISTED_NBT_TAG);
        NBTTagList tags = persisted.getTagList(NBT_KEY, 8);
        for (int i = 0; i < tags.tagCount(); i++) out.add(tags.getStringTagAt(i));
        return out;
    }

    private static void write(EntityPlayer player, List<String> ids) {
        NBTTagCompound entityData = player.getEntityData();
        NBTTagCompound persisted = entityData.getCompoundTag(EntityPlayer.PERSISTED_NBT_TAG);
        NBTTagList tags = new NBTTagList();
        for (String id : ids) tags.appendTag(new NBTTagString(id));
        persisted.setTag(NBT_KEY, tags);
        entityData.setTag(EntityPlayer.PERSISTED_NBT_TAG, persisted);
    }

    // ---------------------------------------------------------------- client mirror

    /** Called on the client thread by the sync packet. */
    public static void setClientMirror(UUID player, List<String> disabled) {
        clientPlayer = player;
        CLIENT_DISABLED.clear();
        if (disabled != null) CLIENT_DISABLED.addAll(disabled);
    }

    public static void clearClientMirror() {
        clientPlayer = null;
        CLIENT_DISABLED.clear();
    }

    /** Client side check used by the skill screen for the local player. */
    public static boolean isClientDisabled(String id) {
        return CLIENT_DISABLED.contains(id);
    }
}
