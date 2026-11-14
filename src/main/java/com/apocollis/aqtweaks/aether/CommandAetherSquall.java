package com.apocollis.aqtweaks.aether;

import com.apocollis.aqtweaks.hazard.HazardState;
import com.apocollis.aqtweaks.hazard.Hazards;

import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraftforge.common.DimensionManager;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javax.annotation.Nullable;

/**
 * OP {@code /aethersquall <gust|lightning|clear|status> [seconds] [skipWarning]} (alias {@code /aetherwind}). Works
 * on the loaded Aether through the hazard engine, so a forced gust warns like a natural one unless
 * {@code skipWarning}; {@code lightning} starts a storm of strike salvos that lock onto exposed players, after a storm warning.
 */
public final class CommandAetherSquall extends CommandBase {

    private static final List<String> WORDS = Arrays.asList("gust", "lightning", "clear", "status");

    @Override
    public String getName() {
        return "aethersquall";
    }

    @Override
    public List<String> getAliases() {
        return Collections.singletonList("aetherwind");
    }

    @Override
    public String getUsage(ICommandSender sender) {
        return "/aethersquall <gust|lightning|clear|status> [seconds] [skipWarning]";
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
        World world = findAether();
        if (world == null) {
            throw new CommandException("The Aether is not loaded. Enter it first.");
        }
        int seconds = 0;
        boolean skip = false;
        for (int i = 1; i < args.length; i++) {
            if ("skipwarning".equalsIgnoreCase(args[i])) {
                skip = true;
            } else {
                seconds = parseInt(args[i], 1, 600);
            }
        }
        switch (args[0].toLowerCase()) {
            case "gust":
                Hazards.trigger(world, AetherChannels.GUST, seconds, skip);
                reply(sender, TextFormatting.AQUA, "Gust scheduled" + (skip ? " (no warning). " : " with its warning. ")
                        + describe(world));
                break;
            case "lightning":
                Hazards.trigger(world, AetherChannels.LIGHTNING, seconds, skip);
                reply(sender, TextFormatting.YELLOW, "Lightning storm scheduled" + (skip ? " (no warning). " : " with its warning. ")
                        + describe(world));
                break;
            case "clear":
                Hazards.clear(world, AetherChannels.GUST);
                Hazards.clear(world, AetherChannels.LIGHTNING);
                if (AetherChannels.handler != null) AetherChannels.handler.reset();
                reply(sender, TextFormatting.AQUA, "Aether squalls cleared. " + describe(world));
                break;
            default:
                reply(sender, TextFormatting.GOLD, describe(world));
                break;
        }
    }

    private static String describe(World world) {
        return "gust: " + one(Hazards.state(world, AetherChannels.GUST)) + "; lightning: "
                + one(Hazards.state(world, AetherChannels.LIGHTNING));
    }

    private static String one(HazardState s) {
        if (s.event == null) return "none";
        return s.phase.name().toLowerCase() + String.format(" (str %.2f, starts in %d s, ends in %d s)", s.strength,
                s.event.ticksToStart(s.tick) / 20L, s.ticksToEnd / 20L);
    }

    private static void reply(ICommandSender sender, TextFormatting color, String text) {
        TextComponentString message = new TextComponentString(text);
        message.getStyle().setColor(color);
        sender.sendMessage(message);
    }

    @Nullable
    private static World findAether() {
        for (World world : DimensionManager.getWorlds()) {
            if (AetherChannels.isAether(world)) return world;
        }
        return null;
    }

    @Override
    public List<String> getTabCompletions(MinecraftServer server, ICommandSender sender, String[] args,
                                          @Nullable BlockPos targetPos) {
        if (args.length == 1) {
            return getListOfStringsMatchingLastWord(args, WORDS);
        }
        return Collections.emptyList();
    }
}
