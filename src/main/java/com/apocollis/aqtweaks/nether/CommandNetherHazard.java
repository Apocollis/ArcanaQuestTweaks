package com.apocollis.aqtweaks.nether;

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
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.DimensionManager;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javax.annotation.Nullable;

/**
 * OP {@code /netherhazard <spout|ash|clear|status> [seconds] [skipWarning]}. Works on the loaded Nether through
 * the hazard engine, so a forced event warns (spout tremor) like a natural one unless {@code skipWarning}.
 */
public final class CommandNetherHazard extends CommandBase {

    private static final List<String> WORDS = Arrays.asList("spout", "ash", "clear", "status");

    @Override
    public String getName() {
        return "netherhazard";
    }

    @Override
    public String getUsage(ICommandSender sender) {
        return "/netherhazard <spout|ash|clear|status> [seconds] [skipWarning]";
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
        WorldServer world = DimensionManager.getWorld(-1);
        if (world == null) {
            throw new CommandException("The Nether is not loaded.");
        }
        int seconds = 0;
        boolean skip = false;
        for (int i = 1; i < args.length; i++) {
            if ("skipwarning".equalsIgnoreCase(args[i])) {
                skip = true;
            } else {
                seconds = parseInt(args[i], 1, 3600);
            }
        }
        switch (args[0].toLowerCase()) {
            case "spout":
                Hazards.trigger(world, NetherHazards.SPOUT, seconds, skip);
                reply(sender, TextFormatting.GOLD, "Spout event scheduled" + (skip ? " (no tremor). " : " with its tremor. ")
                        + describe(world));
                break;
            case "ash":
                Hazards.trigger(world, NetherHazards.ASH, seconds, skip);
                reply(sender, TextFormatting.GRAY, "Ash event scheduled. " + describe(world));
                break;
            case "clear": {
                Hazards.clear(world, NetherHazards.SPOUT);
                Hazards.clear(world, NetherHazards.ASH);
                NetherHazards.ash.clearAll(world);
                NetherHazards.spouts.reset();
                SplashQueue.get(world).clearAll(world);
                reply(sender, TextFormatting.AQUA, "Nether hazards cleared. " + describe(world));
                break;
            }
            default: {
                StringBuilder sb = new StringBuilder(describe(world));
                sb.append(" | clouds ").append(NetherHazards.ash.clouds().size());
                for (AshCloud c : NetherHazards.ash.clouds()) {
                    sb.append(" [#").append(c.id).append(" ").append(c.occupied.size()).append(" cells]");
                }
                sb.append(" | thrown lava cells ").append(SplashQueue.get(world).size());
                sb.append(" | active spouts ").append(NetherHazards.spouts.activeSpouts());
                if (sender.getCommandSenderEntity() instanceof EntityPlayer) {
                    EntityPlayer p = (EntityPlayer) sender.getCommandSenderEntity();
                    sb.append(" | your ash dose ").append(String.format("%.1f", NetherHazards.ash.doseOf(p.getUniqueID())));
                }
                reply(sender, TextFormatting.GOLD, sb.toString());
                break;
            }
        }
    }

    private static String describe(WorldServer world) {
        return "spout: " + one(Hazards.state(world, NetherHazards.SPOUT)) + "; ash: "
                + one(Hazards.state(world, NetherHazards.ASH));
    }

    private static String one(HazardState s) {
        if (s.event == null) return "none";
        return s.phase.name().toLowerCase() + " (starts in " + s.event.ticksToStart(s.tick) / 20L + " s, ends in "
                + s.ticksToEnd / 20L + " s)";
    }

    private static void reply(ICommandSender sender, TextFormatting color, String text) {
        TextComponentString message = new TextComponentString(text);
        message.getStyle().setColor(color);
        sender.sendMessage(message);
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
