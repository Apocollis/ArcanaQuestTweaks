package com.apocollis.aqtweaks.client;

import com.apocollis.aqtweaks.ArcanaQuestTweaksConfig;
import com.apocollis.aqtweaks.hazard.HazardChannel;
import com.apocollis.aqtweaks.hazard.HazardChannels;
import com.apocollis.aqtweaks.hazard.HazardState;
import com.apocollis.aqtweaks.hazard.Hazards;

import net.minecraft.client.Minecraft;
import net.minecraft.world.World;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/** Optional HUD line per hazard channel (config {@code Hazard Debug}): what this client sees of each event. */
@SideOnly(Side.CLIENT)
public class HazardDebugHud {

    @SubscribeEvent
    public void onOverlay(RenderGameOverlayEvent.Post event) {
        if (event.getType() != RenderGameOverlayEvent.ElementType.ALL) return;
        if (!ArcanaQuestTweaksConfig.AurorianModuleConfig.hazardDebug) return;
        Minecraft mc = Minecraft.getMinecraft();
        World world = mc.world;
        if (world == null || mc.gameSettings.showDebugInfo) return;
        int y = 4;
        for (HazardChannel channel : HazardChannels.forWorld(world)) {
            HazardState s = Hazards.state(world, channel);
            String text;
            if (s.event == null) {
                text = channel.name + ": no event received";
            } else {
                text = String.format("%s: %s warn %.2f str %.2f start in %ds end in %ds", channel.name,
                        s.phase.name().toLowerCase(), s.warning, s.strength,
                        s.event.ticksToStart(s.tick) / 20L, s.ticksToEnd / 20L);
            }
            mc.fontRenderer.drawStringWithShadow(text, 4, y, 0xFFFFFF);
            y += 10;
        }
    }
}
