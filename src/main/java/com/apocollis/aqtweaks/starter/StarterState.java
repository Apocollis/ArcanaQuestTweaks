package com.apocollis.aqtweaks.starter;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;

/** The starter flags kept in the player's persisted data (survive death and respawn). */
public final class StarterState {

    public static final String PENDING = "AQTStarterClassPending";
    public static final String CHOSEN = "AQTStarterClassChosen";
    public static final String CLASS_ID = "AQTStarterClassId";
    /** Set while a Tabula Rasa reawakening is in progress: the next class gives no kit and no one-time commands. */
    public static final String NO_KIT = "AQTStarterClassNoKit";
    /** Flag CustomStartingGear sets after it hands out the default inventory. */
    public static final String CSG_RECEIVED = "csg:receivedInventory";

    private StarterState() {}

    public static NBTTagCompound persisted(EntityPlayer player) {
        NBTTagCompound data = player.getEntityData();
        if (!data.hasKey(EntityPlayer.PERSISTED_NBT_TAG, 10)) {
            data.setTag(EntityPlayer.PERSISTED_NBT_TAG, new NBTTagCompound());
        }
        return data.getCompoundTag(EntityPlayer.PERSISTED_NBT_TAG);
    }

    public static boolean isPending(EntityPlayer player) {
        return persisted(player).getBoolean(PENDING);
    }

    public static boolean isChosen(EntityPlayer player) {
        return persisted(player).getBoolean(CHOSEN);
    }

    public static String classId(EntityPlayer player) {
        return persisted(player).getString(CLASS_ID);
    }

    public static boolean isNoKit(EntityPlayer player) {
        return persisted(player).getBoolean(NO_KIT);
    }

    public static void setNoKit(EntityPlayer player, boolean noKit) {
        if (noKit) {
            persisted(player).setBoolean(NO_KIT, true);
        } else {
            persisted(player).removeTag(NO_KIT);
        }
    }

    /** Back to the menu after a Tabula Rasa draught: pending again, and the next class gives no kit. */
    public static void resetForReawakening(EntityPlayer player) {
        NBTTagCompound tag = persisted(player);
        tag.removeTag(CHOSEN);
        tag.removeTag(CLASS_ID);
        tag.setBoolean(PENDING, true);
        tag.setBoolean(NO_KIT, true);
    }

    public static boolean csgReceived(EntityPlayer player) {
        return persisted(player).getBoolean(CSG_RECEIVED);
    }

    public static void setPending(EntityPlayer player, boolean pending) {
        persisted(player).setBoolean(PENDING, pending);
    }

    public static void setChosen(EntityPlayer player, String classId) {
        NBTTagCompound tag = persisted(player);
        tag.setBoolean(CHOSEN, true);
        tag.setString(CLASS_ID, classId);
        tag.setBoolean(PENDING, false);
    }

    /** Back to a new character for testing or recovery ({@code /aqclass reset}). */
    public static void reset(EntityPlayer player) {
        NBTTagCompound tag = persisted(player);
        tag.removeTag(CHOSEN);
        tag.removeTag(CLASS_ID);
        tag.removeTag(NO_KIT);
        tag.setBoolean(PENDING, true);
    }
}
