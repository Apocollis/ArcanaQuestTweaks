package com.apocollis.aqtweaks.reskillable.client;

import com.apocollis.aqtweaks.reskillable.PerkToggleState;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.network.FMLNetworkEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/** Client side of perk toggling: applies the sync packet and forgets the mirror on disconnect. */
@SideOnly(Side.CLIENT)
public final class PerkToggleClient {

    /** Runs on the client thread (the packet handler hands over from the netty thread). */
    public static void apply(final List<String> disabled) {
        final Minecraft mc = Minecraft.getMinecraft();
        mc.addScheduledTask(() -> {
            if (mc.player != null) PerkToggleState.setClientMirror(mc.player.getUniqueID(), disabled);
        });
    }

    @SubscribeEvent
    public void onDisconnect(FMLNetworkEvent.ClientDisconnectionFromServerEvent event) {
        PerkToggleState.clearClientMirror();
    }
}
