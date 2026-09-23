package com.apocollis.aqtweaks.client;

import baubles.common.config.KeyBindings;
import baubles.common.network.PacketHandler;
import baubles.common.network.PacketOpen;

import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.KeyBinding;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

/**
 * Opens the expanded Baubles GUI from {@link KeyBinding#isPressed()} so MineMenu
 * works when the bind is unset. A press that already sent {@code PacketOpen}
 * is marked by {@link #noteHardwareOpen()} and does not send a second packet.
 * Pack ships BaublesEX; this class imports it directly.
 */
public final class BaublesMenuClient {

    private static boolean hardwareOpen;

    private BaublesMenuClient() {
    }

    public static void register() {
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.register(new BaublesMenuClient());
    }

    /** Baubles {@code onKeyInput} already sent {@code PacketOpen}. */
    public static void noteHardwareOpen() {
        hardwareOpen = true;
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        boolean hardware = hardwareOpen;
        hardwareOpen = false;
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.player == null) {
            return;
        }
        KeyBinding key = KeyBindings.KEY_BAUBLES;
        if (key == null || !key.isPressed() || hardware) {
            return;
        }
        PacketHandler.INSTANCE.sendToServer(new PacketOpen(PacketOpen.Option.EXPANSION));
    }
}
