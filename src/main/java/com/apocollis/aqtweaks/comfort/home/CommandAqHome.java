package com.apocollis.aqtweaks.comfort.home;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;

/** OP {@code /aqhome info|rescan|list}: inspect and rebuild the cached home regions. */
public final class CommandAqHome extends CommandBase {

    @Override
    public String getName() {
        return "aqhome";
    }

    @Override
    public String getUsage(ICommandSender sender) {
        return "/aqhome <info|rescan|list>";
    }

    @Override
    public int getRequiredPermissionLevel() {
        return 2;
    }

    @Override
    public List<String> getTabCompletions(MinecraftServer server, ICommandSender sender, String[] args, BlockPos pos) {
        return args.length == 1 ? getListOfStringsMatchingLastWord(args, Arrays.asList("info", "rescan", "list"))
                : Collections.emptyList();
    }

    @Override
    public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
        if (args.length != 1) {
            throw new CommandException(getUsage(sender));
        }
        if ("list".equalsIgnoreCase(args[0])) {
            list(sender);
            return;
        }
        if (!(sender instanceof EntityPlayerMP player)) {
            throw new CommandException("Only a player can run /aqhome " + args[0]);
        }
        HomeArea area = HomeRegistry.areaAtPlayer(player);
        if (area == null) {
            HomeRecord own = HomeRegistry.recordOf(player.getUniqueID());
            area = own == null ? null : own.area();
        }
        if ("rescan".equalsIgnoreCase(args[0])) {
            if (area == null) {
                send(sender, TextFormatting.RED, "No home here and none registered for you (sleep in a bed first).");
            } else if (HomeRegistry.rescan(player.world, area)) {
                send(sender, TextFormatting.GOLD, "Rebuilding the home of " + area.primary().ownerName
                        + " (fill runs over several ticks; /aqhome info to check).");
            } else {
                send(sender, TextFormatting.RED, "Could not start a rebuild (other dimension, already filling or bed not loaded).");
            }
            return;
        }
        if ("info".equalsIgnoreCase(args[0])) {
            info(sender, area, player);
            return;
        }
        throw new CommandException(getUsage(sender));
    }

    private static void info(ICommandSender sender, HomeArea area, EntityPlayerMP player) {
        if (area == null || area.primary() == null) {
            send(sender, TextFormatting.RED, "No home here and none registered for you.");
            return;
        }
        HomeRecord primary = area.primary();
        HomeRegion region = area.region();
        send(sender, TextFormatting.GOLD, HomeNames.format(HomeConfig.nameFormat, primary.ownerName)
                + "  [" + (area.isReady() ? (region.isBox() ? "box" : "enclosed") : "not built")
                + (area.isBuilding() ? ", filling" : "") + (area.isDirty() ? ", dirty" : "") + "]");
        send(sender, TextFormatting.GRAY, "owners: " + owners(area) + "  dimension " + area.dimension()
                + "  bed " + primary.bed.getX() + " " + primary.bed.getY() + " " + primary.bed.getZ()
                + "  hearthkeeper: " + area.anyHearthkeeper());
        if (region != null) {
            long age = player.world.getTotalWorldTime() - area.builtAt();
            send(sender, TextFormatting.GRAY, "cells " + region.cellCount() + (region.isBox() ? "" : ", boundary " + region.boundaryCount())
                    + "  bounds " + region.minX() + "," + region.minY() + "," + region.minZ() + " to "
                    + region.maxX() + "," + region.maxY() + "," + region.maxZ()
                    + "  version " + area.version() + "  built " + (age / 20) + "s ago  (" + region.reason() + ")");
            send(sender, TextFormatting.GRAY, region.portals().summary());
            send(sender, TextFormatting.GRAY, "you are " + (HomeRegistry.areaAtPlayer(player) == area ? "inside" : "outside")
                    + " this home; run /aqcomfort for the score");
        }
    }

    private static void list(ICommandSender sender) {
        if (HomeRegistry.areas().isEmpty()) {
            send(sender, TextFormatting.GRAY, "No homes registered.");
            return;
        }
        for (HomeArea area : HomeRegistry.areas()) {
            if (area.primary() == null) {
                continue;
            }
            HomeRegion region = area.region();
            send(sender, TextFormatting.GRAY, owners(area) + " dim " + area.dimension() + " bed "
                    + area.primary().bed.getX() + " " + area.primary().bed.getY() + " " + area.primary().bed.getZ()
                    + " " + (region == null ? "not built" : (region.isBox() ? "box" : region.cellCount() + " cells")));
        }
    }

    private static String owners(HomeArea area) {
        StringBuilder sb = new StringBuilder();
        for (HomeRecord r : area.records()) {
            if (sb.length() > 0) {
                sb.append(", ");
            }
            sb.append(r.ownerName);
        }
        return sb.toString();
    }

    private static void send(ICommandSender sender, TextFormatting color, String text) {
        TextComponentString line = new TextComponentString(text);
        line.getStyle().setColor(color);
        sender.sendMessage(line);
    }
}
