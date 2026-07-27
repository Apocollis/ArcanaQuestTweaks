package com.apocollis.aqtweaks;

import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import java.util.ArrayList;
import java.util.List;

public class CommandWhereAmI extends CommandBase {

    @Override
    public String getName() {
        return "whereami";
    }

    @Override
    public String getUsage(ICommandSender sender) {
        return "/whereami";
    }

    @Override
    public int getRequiredPermissionLevel() {
        return 0; // Allow all players to check their location info
    }

    @Override
    public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
        EntityPlayer player = getCommandSenderAsPlayer(sender);
        World world = player.getEntityWorld();
        BlockPos pos = player.getPosition();

        // 1. Dimension ID and Name
        int dimId = world.provider.getDimension();
        String dimName = world.provider.getDimensionType().getName();

        // 2. Biome registry name
        String biomeName = world.getBiome(pos).getRegistryName().toString();

        // 3. Scan structures using the chunk provider
        String[] structureNames = {"Stronghold", "Mineshaft", "Village", "Temple", "Monument", "Mansion", "RoguelikeDungeon"};
        List<String> inside = new ArrayList<>();
        if (world.getChunkProvider() instanceof net.minecraft.world.gen.ChunkProviderServer) {
            net.minecraft.world.gen.ChunkProviderServer provider = (net.minecraft.world.gen.ChunkProviderServer) world.getChunkProvider();
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

        // Format and send output to player
        sender.sendMessage(new TextComponentString(TextFormatting.GOLD + "=== Location Info ==="));
        sender.sendMessage(new TextComponentString(TextFormatting.YELLOW + "Coordinates: " + TextFormatting.WHITE + String.format("X: %d, Y: %d, Z: %d", pos.getX(), pos.getY(), pos.getZ())));
        sender.sendMessage(new TextComponentString(TextFormatting.YELLOW + "Dimension: " + TextFormatting.WHITE + String.format("%d (%s)", dimId, dimName)));
        sender.sendMessage(new TextComponentString(TextFormatting.YELLOW + "Biome: " + TextFormatting.WHITE + biomeName));
        
        if (inside.isEmpty()) {
            sender.sendMessage(new TextComponentString(TextFormatting.YELLOW + "Structure: " + TextFormatting.GRAY + "None"));
        } else {
            sender.sendMessage(new TextComponentString(TextFormatting.YELLOW + "Structure: " + TextFormatting.GREEN + String.join(", ", inside)));
        }
    }
}
