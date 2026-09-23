package com.apocollis.aqtweaks.client;

import baubles.common.config.KeyBindings;
import baubles.common.network.PacketHandler;
import baubles.common.network.PacketOpen;

import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.KeyBinding;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import org.lwjglx.input.Keyboard;

/**
 * Opens the expanded Baubles GUI from {@link KeyBinding#isPressed()} so MineMenu
 * works. Hardware presses still go through Baubles {@code onKeyInput}; this handler
 * consumes {@code pressTime} and does not send a second packet.
 * Pack ships BaublesEX; this class imports it directly.
 */
public final class BaublesMenuClient {

    private BaublesMenuClient() {
    }

    public static void register() {
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.register(new BaublesMenuClient());
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.player == null) {
            return;
        }
        KeyBinding key = KeyBindings.KEY_BAUBLES;
        if (key == null || !key.isPressed()) {
            return;
        }
        int code = key.getKeyCode();
        if (code != Keyboard.KEY_NONE && Keyboard.isKeyDown(code)) {
            return;
        }
        PacketHandler.INSTANCE.sendToServer(new PacketOpen(PacketOpen.Option.EXPANSION));
    }
}
