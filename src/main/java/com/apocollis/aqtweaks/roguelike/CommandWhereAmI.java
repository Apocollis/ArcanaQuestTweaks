package com.apocollis.aqtweaks.roguelike;

import com.apocollis.aqtweaks.util.Reflect;

import net.minecraft.command.CommandException;
import net.minecraft.command.ICommand;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.annotation.Nullable;

public class CommandWhereAmI implements ICommand {

    @Override
    public String getName() {
        return "whereami";
    }

    @Override
    public String getUsage(ICommandSender sender) {
        return "/whereami";
    }

    @Override
    public List<String> getAliases() {
        return Collections.emptyList();
    }

    public int getRequiredPermissionLevel() {
        return 0; // Allow all players to check their location info
    }

    @Override
    public boolean checkPermission(MinecraftServer server, ICommandSender sender) {
        return true;
    }

    @Override
    public List<String> getTabCompletions(MinecraftServer server, ICommandSender sender, String[] args, @Nullable BlockPos targetPos) {
        return Collections.emptyList();
    }

    @Override
    public boolean isUsernameIndex(String[] args, int index) {
        return false;
    }

    @Override
    public int compareTo(ICommand o) {
        return this.getName().compareTo(o.getName());
    }

    @Override
    public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
        EntityPlayer player = null;
        if (sender instanceof EntityPlayer) {
            player = (EntityPlayer) sender;
        } else if (sender.getCommandSenderEntity() instanceof EntityPlayer) {
            player = (EntityPlayer) sender.getCommandSenderEntity();
        }

        World world = sender.getEntityWorld();
        BlockPos pos = sender.getPosition();

        // 1. Dimension ID and Name
        int dimId = (player != null) ? Reflect.getDimension(player) : (world != null && world.provider != null ? world.provider.getDimension() : 0);
        String dimName = String.valueOf(dimId);
        try {
            if (world != null && world.provider != null && world.provider.getDimensionType() != null) {
                dimName = world.provider.getDimensionType().getName();
            } else {
                net.minecraft.world.DimensionType dt = net.minecraft.world.DimensionType.getById(dimId);
                if (dt != null) dimName = dt.getName();
            }
        } catch (Throwable ignored) {}

        // 2. Biome registry name
        String biomeName = "Unknown";
        if (world != null && pos != null) {
            net.minecraft.world.biome.Biome b = Reflect.getBiome(world, pos);
            if (b != null && b.getRegistryName() != null) {
                biomeName = b.getRegistryName().toString();
            }
        }

        // 3. Scan structures using the chunk provider
        String[] structureNames = {"Stronghold", "Mineshaft", "Village", "Temple", "Monument", "Mansion", "RoguelikeDungeon"};
        List<String> inside = new ArrayList<>();
        if (world instanceof net.minecraft.world.WorldServer) {
            net.minecraft.world.gen.ChunkProviderServer provider = ((net.minecraft.world.WorldServer) world).getChunkProvider();
            if (provider != null) {
                for (String struct : structureNames) {
                    if (provider.isInsideStructure(world, struct, pos)) {
                        if ("RoguelikeDungeon".equals(struct)) {
                            int level = RoguelikeDungeonSavedData.get(world).getDungeonLevel(pos);
                            if (level == -1) {
                                inside.add("RoguelikeDungeon (Tower)");
                            } else if (level >= 0) {
                                inside.add("RoguelikeDungeon (Floor " + (level + 1) + ")");
                            } else {
                                inside.add("RoguelikeDungeon");
                            }
                        } else {
                            inside.add(struct);
                        }
                    }
                }
            }
        }

        // Format and send output to player
        Reflect.sendMessage(sender, new TextComponentString(TextFormatting.GOLD + "=== Location Info ==="));
        if (pos != null) {
            Reflect.sendMessage(sender, new TextComponentString(TextFormatting.YELLOW + "Coordinates: " + TextFormatting.WHITE + String.format("X: %d, Y: %d, Z: %d", Reflect.getX(pos), Reflect.getY(pos), Reflect.getZ(pos))));
        }
        Reflect.sendMessage(sender, new TextComponentString(TextFormatting.YELLOW + "Dimension: " + TextFormatting.WHITE + String.format("%d (%s)", dimId, dimName)));
        Reflect.sendMessage(sender, new TextComponentString(TextFormatting.YELLOW + "Biome: " + TextFormatting.WHITE + biomeName));
        
        if (inside.isEmpty()) {
            Reflect.sendMessage(sender, new TextComponentString(TextFormatting.YELLOW + "Structure: " + TextFormatting.GRAY + "None"));
        } else {
            Reflect.sendMessage(sender, new TextComponentString(TextFormatting.YELLOW + "Structure: " + TextFormatting.GREEN + String.join(", ", inside)));
        }
    }
}
