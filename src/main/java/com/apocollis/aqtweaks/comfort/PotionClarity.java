package com.apocollis.aqtweaks.comfort;

import com.apocollis.aqtweaks.ArcanaQuestTweaks;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/** Visible only while resting at home with Thaumcraft loaded. It does not linger. */
public class PotionClarity extends Potion {

    public static final PotionClarity INSTANCE = new PotionClarity();
    private static final ResourceLocation ICON =
            new ResourceLocation("aqtweaks", "textures/gui/clarity_icon.png");

    @Mod.EventBusSubscriber(modid = ArcanaQuestTweaks.MODID)
    public static class RegistrationHandler {
        @SubscribeEvent
        public static void registerPotions(RegistryEvent.Register<Potion> event) {
            event.getRegistry().register(INSTANCE);
        }
    }

    public PotionClarity() {
        super(false, 0xC9E4FF);
        setRegistryName("clarity");
    }

    @Override
    public String getName() {
        return "effect.aqtweaks.hearthside";
    }

    /** Seconds between temporary-warp clears at this amplifier (tier I / II / III). */
    public static int secondsPerWarp(int amplifier) {
        if (amplifier >= 2) return 60;
        if (amplifier == 1) return 120;
        return 180;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public boolean hasStatusIcon() {
        return true;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void renderInventoryEffect(int x, int y, PotionEffect effect, Minecraft mc) {
        drawIcon(x + 6, y + 7, mc);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void renderHUDEffect(int x, int y, PotionEffect effect, Minecraft mc, float alpha) {
        drawIcon(x + 3, y + 3, mc);
    }

    @SideOnly(Side.CLIENT)
    private static void drawIcon(int x, int y, Minecraft mc) {
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        mc.getTextureManager().bindTexture(ICON);
        Gui.drawModalRectWithCustomSizedTexture(x, y, 0, 0, 18, 18, 18, 18);
    }
}
