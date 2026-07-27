package com.apocollis.aqtweaks;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

public class PotionHomestead extends Potion {

    public static final PotionHomestead INSTANCE = new PotionHomestead();
    private static final ResourceLocation ICON_TEXTURE = new ResourceLocation("aqtweaks", "textures/gui/homestead_icon.png");

    @Mod.EventBusSubscriber(modid = ArcanaQuestTweaks.MODID)
    public static class RegistrationHandler {
        @SubscribeEvent
        public static void registerPotions(RegistryEvent.Register<Potion> event) {
            event.getRegistry().register(INSTANCE);
        }
    }

    public PotionHomestead() {
        super(false, 0xE69D00); // Beneficial, warm hearth amber color
        setRegistryName("homestead");
        setPotionName("effect.aqtweaks.homestead");
    }

    @Override
    @SideOnly(Side.CLIENT)
    public boolean hasStatusIcon() {
        return true;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void renderInventoryEffect(int x, int y, PotionEffect effect, Minecraft mc) {
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        mc.getTextureManager().bindTexture(ICON_TEXTURE);
        Gui.drawModalRectWithCustomSizedTexture(x + 6, y + 7, 0, 0, 18, 18, 18, 18);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void renderHUDEffect(int x, int y, PotionEffect effect, Minecraft mc, float alpha) {
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        mc.getTextureManager().bindTexture(ICON_TEXTURE);
        Gui.drawModalRectWithCustomSizedTexture(x + 3, y + 3, 0, 0, 18, 18, 18, 18);
    }
}
