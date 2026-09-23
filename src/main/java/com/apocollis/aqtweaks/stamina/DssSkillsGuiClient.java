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

/**
 * Opens the DSS skills GUI from {@link KeyBinding#isPressed()} so MineMenu
 * (and anything else that only bumps {@code pressTime}) works. A real key press
 * is marked by {@link #noteHardwareSkillsKey()} and does not send a second packet.
 * Pack ships Dynamic Sword Skills; this class imports it directly.
 */
public final class DssSkillsGuiClient {

    private static final int GUI_SKILLS = 0;
    private static boolean hardwareSkillsKey;

    private DssSkillsGuiClient() {
    }

    public static void register() {
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.register(new DssSkillsGuiClient());
        ClientCommandHandler.instance.registerCommand(new CommandDssGui());
    }

    public static void openSkillsGui() {
        PacketDispatcher.sendToServer(new OpenGuiPacket(GUI_SKILLS));
    }

    /** DSS {@code onKeyPressed} already sent the packet for this physical key. */
    public static void noteHardwareSkillsKey() {
        hardwareSkillsKey = true;
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        boolean hardware = hardwareSkillsKey;
        hardwareSkillsKey = false;
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.player == null) {
            return;
        }
        KeyBinding key = DSSKeyHandler.keys[DSSKeyHandler.KEY_SKILLS_GUI];
        if (key == null || !key.isPressed() || hardware) {
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
