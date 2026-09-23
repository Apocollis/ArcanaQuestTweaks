package com.apocollis.aqtweaks.stamina;

import dynamicswordskills.client.DSSKeyHandler;
import dynamicswordskills.network.PacketDispatcher;
import dynamicswordskills.network.server.OpenGuiPacket;

import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.text.TextComponentString;
import net.minecraftforge.client.ClientCommandHandler;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import org.lwjglx.input.Keyboard;

/**
 * Opens the DSS skills GUI from {@link KeyBinding#isPressed()} so MineMenu
 * (and anything else that only bumps {@code pressTime}) works. Hardware presses
 * still go through DSS {@code KeyInputEvent}; this handler consumes that
 * {@code pressTime} and does not send a second packet.
 * Pack ships Dynamic Sword Skills; this class imports it directly.
 */
public final class DssSkillsGuiClient {

    private static final int GUI_SKILLS = 0;

    private DssSkillsGuiClient() {
    }

    public static void register() {
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.register(new DssSkillsGuiClient());
        ClientCommandHandler.instance.registerCommand(new CommandDssGui());
    }

    public static void openSkillsGui() {
        PacketDispatcher.sendToServer(new OpenGuiPacket(GUI_SKILLS));
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
        KeyBinding key = DSSKeyHandler.keys[DSSKeyHandler.KEY_SKILLS_GUI];
        if (key == null || !key.isPressed()) {
            return;
        }
        int code = key.getKeyCode();
        if (code != Keyboard.KEY_NONE && Keyboard.isKeyDown(code)) {
            return;
        }
        openSkillsGui();
    }

    public static final class CommandDssGui extends CommandBase {
        @Override
        public String getName() {
            return "dssgui";
        }

        @Override
        public String getUsage(ICommandSender sender) {
            return "/dssgui";
        }

        @Override
        public int getRequiredPermissionLevel() {
            return 0;
        }

        @Override
        public boolean checkPermission(MinecraftServer server, ICommandSender sender) {
            return true;
        }

        @Override
        public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
            if (Minecraft.getMinecraft().player == null) {
                sender.sendMessage(new TextComponentString("Join a world before opening the skills GUI."));
                return;
            }
            openSkillsGui();
        }
    }
}
