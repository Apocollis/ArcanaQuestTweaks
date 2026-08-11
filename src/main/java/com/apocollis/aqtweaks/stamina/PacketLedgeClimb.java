package com.apocollis.aqtweaks.stamina;

import com.apocollis.aqtweaks.ArcanaQuestTweaks;

import com.apocollis.aqtweaks.ArcanaQuestTweaksConfig;

import com.apocollis.aqtweaks.util.Reflect;

import io.netty.buffer.ByteBuf;
import net.minecraft.block.Block;
import net.minecraft.block.SoundType;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import com.elenai.elenaidodge2.api.FeathersHelper;

public class PacketLedgeClimb implements IMessage {
    public PacketLedgeClimb() {}

    @Override
    public void fromBytes(ByteBuf buf) {}

    @Override
    public void toBytes(ByteBuf buf) {}

    public static class Handler implements IMessageHandler<PacketLedgeClimb, IMessage> {
        @Override
        public IMessage onMessage(PacketLedgeClimb message, MessageContext ctx) {
            EntityPlayerMP player = Reflect.getServerPlayer(ctx);
            net.minecraft.server.MinecraftServer server = Reflect.getServer(player);
            if (player != null && server != null) {
                server.addScheduledTask(() -> {
                    int cost = ArcanaQuestTweaksConfig.StaminaModuleConfig.ledgeClimb.ledgeClimbCost;
                    if (Reflect.hasEnoughStamina(player, cost)) {
                        FeathersHelper.decreaseFeathers(player, cost);

                        // Play a scraping step sound of the block the player is climbing
                        World world = Reflect.getWorld(player);
                        if (world != null) {
                            double yawRad = Math.toRadians(Reflect.getRotationYaw(player));
                            double dx = -Math.sin(yawRad);
                            double dz = Math.cos(yawRad);
                            
                            // Look for the block we just climbed (slightly below eye level, in front of the player)
                            int x = MathHelper.floor(Reflect.getPosX(player) + dx * 0.4D);
                            int y = MathHelper.floor(Reflect.getPosY(player) + 0.5D);
                            int z = MathHelper.floor(Reflect.getPosZ(player) + dz * 0.4D);
                            BlockPos pos = new BlockPos(x, y, z);
                            IBlockState state = Reflect.getBlockState(world, pos);
                            Block block = Reflect.getBlock(state);
                            
                            if (!Reflect.isAir(block, state, world, pos)) {
                                SoundType soundType = Reflect.getSoundType(block, state, world, pos, player);
                                Reflect.playSound(world, null, Reflect.getPosX(player), Reflect.getPosY(player), Reflect.getPosZ(player), 
                                    soundType.getStepSound(), SoundCategory.PLAYERS, 
                                    soundType.getVolume() * 0.5F, soundType.getPitch() * 0.8F);
                            }
                        }
                    }
                });
            }
            return null;
        }
    }
}
