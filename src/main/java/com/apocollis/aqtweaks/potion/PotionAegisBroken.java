package com.apocollis.aqtweaks.potion;

import com.apocollis.aqtweaks.ArcanaQuestTweaks;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * Shield Broken: while it lasts, no focus that contains Aegis can be cast (other foci work). It has no other
 * effect; it exists so the lockout syncs to the client and shows a timer.
 */
public class PotionAegisBroken extends Potion {

    public static final PotionAegisBroken BROKEN = new PotionAegisBroken();

    private static final ResourceLocation ICON = new ResourceLocation("aqtweaks", "textures/gui/aegis_broken.png");

    @Mod.EventBusSubscriber(modid = ArcanaQuestTweaks.MODID)
    public static class RegistrationHandler {
        @SubscribeEvent
        public static void registerPotions(RegistryEvent.Register<Potion> event) {
            event.getRegistry().register(BROKEN);
        }
    }

    private PotionAegisBroken() {
        super(true, 0x00C0C0);
        setRegistryName("aegis_broken");
    }

    public static boolean isBroken(EntityLivingBase entity) {
        return entity != null && entity.isPotionActive(BROKEN);
    }

    @Override
    public String getName() {
        return "effect.aqtweaks.aegis_broken";
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
