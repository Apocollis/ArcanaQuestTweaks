package com.apocollis.aqtweaks.potion;

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

public class PotionPerkCooldown extends Potion {

    public static final PotionPerkCooldown ADRENALINE = new PotionPerkCooldown("adrenaline_cooldown", "adrenaline", 0x8B1A1A);
    public static final PotionPerkCooldown EVASION = new PotionPerkCooldown("evasion_cooldown", "evasion", 0x3D6B4F);
    public static final PotionPerkCooldown RESPITE = new PotionPerkCooldown("respite_cooldown", "respite", 0x5C4A8A);

    private final ResourceLocation icon;
    private final String effectName;

    @Mod.EventBusSubscriber(modid = ArcanaQuestTweaks.MODID)
    public static class RegistrationHandler {
        @SubscribeEvent
        public static void registerPotions(RegistryEvent.Register<Potion> event) {
            event.getRegistry().register(ADRENALINE);
            event.getRegistry().register(EVASION);
            event.getRegistry().register(RESPITE);
        }
    }

    private PotionPerkCooldown(String registryPath, String unlockablePath, int liquidColor) {
        super(true, liquidColor);
        setRegistryName(registryPath);
        this.effectName = "effect.aqtweaks." + registryPath;
        this.icon = new ResourceLocation("aqtweaks", "textures/unlockables/" + unlockablePath + ".png");
    }

    @Override
    public String getName() {
        return effectName;
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
        mc.getTextureManager().bindTexture(icon);
    }
}
