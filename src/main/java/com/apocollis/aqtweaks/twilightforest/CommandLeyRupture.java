package com.apocollis.aqtweaks.twilightforest;

import com.apocollis.aqtweaks.hazard.HazardState;
import com.apocollis.aqtweaks.hazard.Hazards;

import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.DimensionManager;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javax.annotation.Nullable;

/**
 * OP {@code /leyrupture <surge|clear|status|item|biome> [seconds] [skipWarning]}. {@code surge} starts a rupture
 * through the hazard engine (it warns like a natural one unless {@code skipWarning}); {@code item} reports how the
 * held item is classified; {@code biome} prints the biome name and intensity read at the caller.
 */
public final class CommandLeyRupture extends CommandBase {

    private static final List<String> WORDS = Arrays.asList("surge", "clear", "status", "item", "biome");

    @Override
    public String getName() {
        return "leyrupture";
    }

    @Override
    public String getUsage(ICommandSender sender) {
        return "/leyrupture <surge|clear|status|item|biome> [seconds] [skipWarning]";
    }

    @Override
    public int getRequiredPermissionLevel() {
        return 2;
    }

    @Override
    public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
        if (args.length < 1 || args.length > 3 || !WORDS.contains(args[0].toLowerCase())) {
            throw new CommandException(getUsage(sender));
        }
        World found = null;
        for (World w : DimensionManager.getWorlds()) {
            if (LeyChannels.isTf(w)) found = w;
        }
        if (found == null) throw new CommandException("The Twilight Forest is not loaded. Enter it first.");
        WorldServer world = (WorldServer) found;
        int seconds = 0;
        boolean skip = false;
        for (int i = 1; i < args.length; i++) {
            if ("skipwarning".equalsIgnoreCase(args[i])) skip = true;
            else seconds = parseInt(args[i], 1, 600);
        }
        EntityPlayer caller = sender.getCommandSenderEntity() instanceof EntityPlayer
                ? (EntityPlayer) sender.getCommandSenderEntity() : null;
        switch (args[0].toLowerCase()) {
            case "surge":
                Hazards.trigger(world, LeyChannels.RUPTURE, seconds, skip);
                reply(sender, TextFormatting.LIGHT_PURPLE, "Ley-Rupture scheduled"
                        + (skip ? " (no warning). " : " with its warning. ") + describe(world));
                break;
            case "clear":
                Hazards.clear(world, LeyChannels.RUPTURE);
                LeyChannels.handler.reset(world);
                reply(sender, TextFormatting.AQUA, "Ley-Rupture cleared. " + describe(world));
                break;
            case "item": {
                if (caller == null) throw new CommandException("Run this as a player.");
                String reason = ArcaneItemClassifier.reason(caller.getHeldItemMainhand());
                reply(sender, TextFormatting.GOLD, caller.getHeldItemMainhand().isEmpty() ? "Hold an item."
                        : (reason == null ? "Not classified as a magic item." : "Magic item (" + reason + ")."));
                break;
            }
            case "biome": {
                if (caller == null) throw new CommandException("Run this as a player.");
                String biome = LeyTerrain.biomeName(world, caller.posX, caller.posZ);
                reply(sender, TextFormatting.GOLD, biome + ": intensity " + LeyTerrain.intensity(biome) + ", extra "
                        + (LeyTerrain.extra(biome) == null ? "none" : LeyTerrain.extra(biome).effect));
                break;
            }
            default: {
                StringBuilder sb = new StringBuilder(describe(world));
                sb.append(" | ley mobs ").append(LeyChannels.handler.leyMobCount());
                if (LeyChannels.handler.visBatteries != null) {
                    sb.append(" | tracked batteries ").append(LeyChannels.handler.visBatteries.count());
                }
                if (caller != null) {
                    sb.append(String.format(" | your exposure %.2f", LeyChannels.handler.exposureOf(caller)));
                }
                reply(sender, TextFormatting.GOLD, sb.toString());
                break;
            }
        }
    }

    private static String describe(World world) {
        HazardState s = Hazards.state(world, LeyChannels.RUPTURE);
        if (s.event == null) return "nothing scheduled";
        return s.phase.name().toLowerCase() + String.format(" (starts in %d s, ends in %d s)",
                s.event.ticksToStart(s.tick) / 20L, s.ticksToEnd / 20L);
    }

    private static void reply(ICommandSender sender, TextFormatting color, String text) {
        TextComponentString message = new TextComponentString(text);
        message.getStyle().setColor(color);
        sender.sendMessage(message);
    }

    @Override
    public List<String> getTabCompletions(MinecraftServer server, ICommandSender sender, String[] args,
                                          @Nullable BlockPos targetPos) {
        if (args.length == 1) return getListOfStringsMatchingLastWord(args, WORDS);
        return Collections.emptyList();
    }
}
