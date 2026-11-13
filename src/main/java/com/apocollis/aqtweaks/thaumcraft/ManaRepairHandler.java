package com.apocollis.aqtweaks.thaumcraft;

import com.apocollis.aqtweaks.ArcanaQuestTweaksConfig;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

/** Registered only when Thaumcraft and Botania are both loaded. Repairs Mana-Weave items every tick, as Botania does. */
public class ManaRepairHandler {
    @SubscribeEvent
    public void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.side.isClient()) return;
        EntityPlayer player = event.player;
        if (player.world.isRemote || player instanceof FakePlayer || player.isDead) return;
        if (!ArcanaQuestTweaksConfig.ThaumcraftConfig.enableManaRepair) return;
        ManaRepairBotania.repairInventory(player);
    }
}
