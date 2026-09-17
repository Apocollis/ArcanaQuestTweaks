package com.apocollis.aqtweaks.portal.client;

import com.apocollis.aqtweaks.ArcanaQuestTweaks;
import com.apocollis.aqtweaks.portal.PortalModule;
import com.apocollis.aqtweaks.portal.RiftLighting;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.item.Item;
import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.client.event.TextureStitchEvent;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.relauncher.Side;

@Mod.EventBusSubscriber(value = Side.CLIENT, modid = ArcanaQuestTweaks.MODID)
public final class PortalClientEvents {

    private PortalClientEvents() {}

    @SubscribeEvent
    public static void registerModels(ModelRegistryEvent event) {
        register(PortalModule.TEAR);
        register(PortalModule.WILD);
    }

    @SubscribeEvent
    public static void onTextureStitch(TextureStitchEvent.Post event) {
        RenderArcaneRift.invalidatePortalSprite();
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.world != null) {
            RiftLighting.drainClient(mc.world);
        }
    }

    private static void register(Item item) {
        ModelLoader.setCustomModelResourceLocation(item, 0,
                new ModelResourceLocation(item.getRegistryName(), "inventory"));
    }
}
