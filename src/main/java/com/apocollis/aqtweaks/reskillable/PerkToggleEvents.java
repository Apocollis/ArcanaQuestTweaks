package com.apocollis.aqtweaks.reskillable;

import codersafterdark.reskillable.api.event.LockUnlockableEvent;
import com.apocollis.aqtweaks.ArcanaQuestTweaks;
import com.apocollis.aqtweaks.network.PacketSyncPerkToggles;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent.PlayerChangedDimensionEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent.PlayerLoggedInEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent.PlayerRespawnEvent;

/** Server side of perk toggling: the toggle request, the sync triggers, and clearing the flag on a refund. */
public final class PerkToggleEvents {

    /** Validates and flips one perk for the player, then syncs and refreshes attribute perks. */
    public static void handleToggle(EntityPlayerMP player, String perkId) {
        if (player == null || player.isSpectator() || player instanceof FakePlayer) return;
        if (!ToggleablePerks.isToggleable(perkId) || !PerkAccess.unlocked(player, perkId)) return;
        PerkToggleState.toggle(player, perkId);
        sync(player);
        ReskillableModule.applyAttributes(player);
    }

    public static void sync(EntityPlayerMP player) {
        if (player == null || player instanceof FakePlayer) return;
        ArcanaQuestTweaks.NETWORK.sendTo(new PacketSyncPerkToggles(PerkToggleState.disabledList(player)), player);
    }

    @SubscribeEvent
    public void onLogin(PlayerLoggedInEvent event) {
        if (event.player instanceof EntityPlayerMP mp) sync(mp);
    }

    @SubscribeEvent
    public void onRespawn(PlayerRespawnEvent event) {
        if (event.player instanceof EntityPlayerMP mp) sync(mp);
    }

    @SubscribeEvent
    public void onDimension(PlayerChangedDimensionEvent event) {
        if (event.player instanceof EntityPlayerMP mp) sync(mp);
    }

    /** A refunded perk loses its disabled flag, so buying it again starts enabled. */
    @SubscribeEvent
    public void onLocked(LockUnlockableEvent.Post event) {
        EntityPlayer player = event.getEntityPlayer();
        if (!(player instanceof EntityPlayerMP mp) || mp.world.isRemote) return;
        ResourceLocation key = event.getUnlockable().getRegistryName();
        if (key != null && PerkToggleState.clear(mp, key.toString())) {
            sync(mp);
            ReskillableModule.applyAttributes(mp);
        }
    }
}
