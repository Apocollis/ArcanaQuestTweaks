package com.apocollis.aqtweaks.aurorian;

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
 * OP {@code /aurorianweather <moonfrost|clear|status> [seconds] [skipWarning]} (alias {@code /aqmoonfrost}), the
 * counterpart of {@code /atumweather}. It works on the loaded Aurorian world through the hazard engine, so a forced
 * storm warns like a natural one unless {@code skipWarning} is given. Works from the console and from a command block.
 */
public final class CommandAurorianWeather extends CommandBase {

    private static final List<String> WORDS = Arrays.asList("moonfrost", "clear", "status");

    @Override
    public String getName() {
        return "aurorianweather";
    }

    @Override
    public List<String> getAliases() {
        return Collections.singletonList("aqmoonfrost");
    }

    @Override
    public String getUsage(ICommandSender sender) {
        return "/aurorianweather <moonfrost|clear|status> [seconds] [skipWarning]";
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
        World world = findAurorian();
        if (world == null) {
            throw new CommandException("The Aurorian dimension is not loaded. Enter it first.");
        }
        if (!MoonfrostState.enabled()) {
            throw new CommandException("Moonfrost storms are disabled in aqtweaks_aurorian.cfg");
        }
        String word = args[0].toLowerCase();
        switch (word) {
            case "moonfrost": {
                int seconds = 0;
                boolean skip = false;
                for (int i = 1; i < args.length; i++) {
                    if ("skipwarning".equalsIgnoreCase(args[i])) {
                        skip = true;
                    } else {
                        seconds = parseInt(args[i], 1, 7200);
                    }
                }
                Hazards.trigger(world, MoonfrostState.CHANNEL, seconds, skip);
                reply(sender, TextFormatting.AQUA, "Moonfrost storm scheduled"
                        + (skip ? " (no warning). " : " with its normal warning. ") + describe(world));
                break;
            }
            case "clear": {
                if (Hazards.clear(world, MoonfrostState.CHANNEL)) {
                    reply(sender, TextFormatting.AQUA, "Moonfrost storm is ending. " + describe(world));
                } else {
                    reply(sender, TextFormatting.GRAY, "No Moonfrost storm to clear. " + describe(world));
                }
                break;
            }
            default:
                reply(sender, TextFormatting.GOLD, describe(world));
                break;
        }
    }

    private static String describe(World world) {
        HazardState s = MoonfrostState.get(world);
        if (s.event == null) return "nothing scheduled";
        return String.format("%s, strength %.2f, warning %.2f, starts in %d s, ends in %d s",
                s.phase.name().toLowerCase(), s.strength, s.warning, s.event.ticksToStart(s.tick) / 20L,
                s.ticksToEnd / 20L);
    }

    private static void reply(ICommandSender sender, TextFormatting color, String text) {
        TextComponentString message = new TextComponentString(text);
        message.getStyle().setColor(color);
        sender.sendMessage(message);
    }

    @Nullable
    private static World findAurorian() {
        for (World world : DimensionManager.getWorlds()) {
            if (MoonfrostState.isAurorian(world)) {
                return world;
            }
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
