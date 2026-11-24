package com.apocollis.aqtweaks.potion;

import com.apocollis.aqtweaks.ArcanaQuestTweaks;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * Stun: the target cannot attack or use items while it lasts (see {@link StunHandler}) and moves at about
 * Slowness I speed. Shared pack effect: the Shock focus applies it, and other systems can through {@link Stun}.
 */
public class PotionStun extends Potion {

    public static final PotionStun STUN = new PotionStun();

    private static final ResourceLocation ICON = new ResourceLocation("aqtweaks", "textures/gui/stun.png");

    @Mod.EventBusSubscriber(modid = ArcanaQuestTweaks.MODID)
    public static class RegistrationHandler {
        @SubscribeEvent
        public static void registerPotions(RegistryEvent.Register<Potion> event) {
            event.getRegistry().register(STUN);
        }
    }

    private PotionStun() {
        super(true, 0xFFE150);
        setRegistryName("stun");
        // Slowness I: -15% of base speed (operation 2 = multiply total), own modifier id.
        registerPotionAttributeModifier(SharedMonsterAttributes.MOVEMENT_SPEED,
                "9d3b1c58-6a5e-4a37-9a8b-0c2f4b6f3a11", -0.15D, 2);
    }

    @Override
    public String getName() {
        return "effect.aqtweaks.stun";
    }

    @Override
    public boolean isReady(int duration, int amplifier) {
        return false;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public boolean hasStatusIcon() {
        return true;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void renderInventoryEffect(int x, int y, PotionEffect effect, Minecraft mc) {
        bindIcon(mc);
        Gui.drawModalRectWithCustomSizedTexture(x + 6, y + 7, 0, 0, 18, 18, 16, 16);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void renderHUDEffect(int x, int y, PotionEffect effect, Minecraft mc, float alpha) {
        GlStateManager.color(1.0F, 1.0F, 1.0F, alpha);
        bindIcon(mc);
        Gui.drawModalRectWithCustomSizedTexture(x + 3, y + 3, 0, 0, 18, 18, 16, 16);
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
    }

    @SideOnly(Side.CLIENT)
    private void bindIcon(Minecraft mc) {
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        mc.getTextureManager().bindTexture(ICON);
    }
}
