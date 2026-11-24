package com.apocollis.aqtweaks.titles;

import com.apocollis.aqtweaks.ArcanaQuestTweaksConfig.ClientModuleConfig;
import com.yungnickyoung.minecraft.travelerstitles.TravelersTitles;
import com.yungnickyoung.minecraft.travelerstitles.config.ConfigBiomes;
import com.yungnickyoung.minecraft.travelerstitles.config.TTConfig;
import com.yungnickyoung.minecraft.travelerstitles.render.TitleRenderer;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.I18n;
import net.minecraft.util.text.TextComponentString;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.common.network.FMLNetworkEvent;

/**
 * Shows structure titles through Traveler's Titles' own {@link TitleRenderer}, built from its biome title
 * settings so the look matches. While one is on screen the biome title is held back
 * ({@code MixinTitleRenderManagerStructure}) and the one already showing is cleared when the structure title
 * starts. A player home also covers the Waystones title ({@code MixinWaystonesCompat}). Compile-hard against the Traveler's Titles jar; registered only when that mod
 * is loaded.
 */
public final class TravelersTitlesStructureHook {

    private static TitleRenderer<String> renderer;
    /** True when the title now showing (or last shown) is a player home. */
    private static boolean homeTitle;

    public static void register() {
        MinecraftForge.EVENT_BUS.register(new TravelersTitlesStructureHook());
    }

    /** True while a structure title is fading in, showing or fading out. */
    public static boolean isActive() {
        TitleRenderer<String> r = renderer;
        return r != null && r.titleTimer > 0;
    }

    /** True while a player home's title is on screen. Only homes cover the Waystones title. */
    public static boolean isHomeActive() {
        return homeTitle && isActive();
    }

    /** Called on the client thread from {@link StructureTitleClient}. */
    static void show(String key, String serverTitle, String subtitle) {
        ConfigBiomes biomes = TTConfig.biomes;
        if (biomes == null) {
            return;
        }
        String plainHome = I18n.format(StructureTitles.langKey(StructureTitles.HOME_KEY));
        String text = StructureTitles.clientTitle(key, serverTitle, I18n.format(StructureTitles.langKey(key)),
                ClientModuleConfig.structureTitles.showOwnerNameOnHomes, plainHome);
        if (text.isEmpty()) {
            return;
        }
        TitleRenderer<String> next = new TitleRenderer<>(
                1,
                true,
                biomes.textFadeInTime.get(),
                biomes.textDisplayTime.get(),
                biomes.textFadeOutTime.get(),
                biomes.textColor.get(),
                biomes.renderShadow.get(),
                biomes.textSize.get(),
                biomes.textXOffset.get(),
                biomes.textYOffset.get() + ClientModuleConfig.structureTitles.yOffset,
                biomes.centerText.get());
        next.displayTitle(new TextComponentString(text), new TextComponentString(""));
        renderer = next;
        homeTitle = StructureTitles.HOME_KEY.equals(key);
        clearOtherTitles(homeTitle);
    }

    /** Clears the biome title, and for a home also the Waystones title, that may already be on screen. */
    private static void clearOtherTitles(boolean home) {
        if (TravelersTitles.titleManager != null && TravelersTitles.titleManager.biomeTitleRenderer != null) {
            TravelersTitles.titleManager.biomeTitleRenderer.clearTimer();
        }
        if (home && Loader.isModLoaded("waystones")) {
            clearWaystoneTitle();
        }
    }

    private static void clearWaystoneTitle() {
        try {
            com.yungnickyoung.minecraft.travelerstitles.compat.WaystonesCompat.reset();
        } catch (Throwable ignored) {
            // Traveler's Titles built without its Waystones part, or Waystones is not usable: nothing to clear.
        }
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        TitleRenderer<String> r = renderer;
        if (event.phase != TickEvent.Phase.START || r == null || Minecraft.getMinecraft().isGamePaused()) {
            return;
        }
        r.tick();
        if (r.titleTimer > 0 && TravelersTitles.titleManager != null
                && TravelersTitles.titleManager.biomeTitleRenderer != null) {
            TravelersTitles.titleManager.biomeTitleRenderer.clearTimer();
        }
    }

    @SubscribeEvent
    public void onRender(RenderGameOverlayEvent.Pre event) {
        TitleRenderer<String> r = renderer;
        if (r == null || event.getType() != RenderGameOverlayEvent.ElementType.ALL
                || Minecraft.getMinecraft().gameSettings.hideGUI) {
            return;
        }
        r.renderText(event.getPartialTicks());
    }

    @SubscribeEvent
    public void onDisconnect(FMLNetworkEvent.ClientDisconnectionFromServerEvent event) {
        renderer = null;
    }
}
