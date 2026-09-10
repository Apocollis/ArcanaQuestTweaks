package com.apocollis.aqtweaks.stamina;

import com.apocollis.aqtweaks.ArcanaQuestTweaksConfig;

import com.apocollis.aqtweaks.util.Reflect;

import io.netty.buffer.ByteBuf;
import net.minecraft.block.Block;
import net.minecraft.block.SoundType;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
public class PacketLedgeClimb implements IMessage {
    public PacketLedgeClimb() {}

    @Override
    public void fromBytes(ByteBuf buf) {}

    @Override
    public void toBytes(ByteBuf buf) {}

    public static class Handler implements IMessageHandler<PacketLedgeClimb, IMessage> {
        @Override
        public IMessage onMessage(PacketLedgeClimb message, MessageContext ctx) {
            EntityPlayerMP player = ctx.getServerHandler().player;
            if (player == null) return null;
            net.minecraft.server.MinecraftServer server = player.getServer();
            if (player != null && server != null) {
                server.addScheduledTask(() -> {
                    // The client FSM already honours these, but a modified client must not be able
                    // to spend feathers through a disabled feature or in a stamina-exempt gamemode.
                    if (!ArcanaQuestTweaksConfig.StaminaModuleConfig.ledgeClimb.enableLedgeClimb) return;
                    if (player.capabilities.isCreativeMode || player.isSpectator()) return;

                    int cost = StaminaPerks.climbCost(player,
                            ArcanaQuestTweaksConfig.StaminaModuleConfig.ledgeClimb.ledgeClimbCost);
                    if (cost <= 0 || Reflect.hasEnoughStamina(player, cost)) {
                        if (cost > 0) {
                            Reflect.decreaseFeathers(player, cost);
                        }
                        NBTTagCompound pData = player.getEntityData();
                        pData.setInteger("StaminaTweaksLedgeClimbState", 1);
                        pData.setInteger("StaminaTweaksLedgeClimbGrace", player.ticksExisted + 60);
                        pData.setInteger("StaminaTweaksLedgeMantleTicks", 0);
                        pData.setInteger("StaminaTweaksLedgeExtraSpends", 0);
                        player.fallDistance = 0.0F;

                        // Play a scraping step sound of the block the player is climbing
                        World world = player.world;
                        if (world != null) {
                            double yawRad = Math.toRadians(player.rotationYaw);
                            double dx = -Math.sin(yawRad);
                            double dz = Math.cos(yawRad);
                            
                            // Look for the block we just climbed (slightly below eye level, in front of the player)
                            int x = MathHelper.floor(player.posX + dx * 0.4D);
                            int y = MathHelper.floor(player.posY + 0.5D);
                            int z = MathHelper.floor(player.posZ + dz * 0.4D);
                            BlockPos pos = new BlockPos(x, y, z);
                            IBlockState state = world.getBlockState(pos);
                            Block block = state.getBlock();
                            
                            if (!block.isAir(state, world, pos)) {
                                SoundType soundType = block.getSoundType(state, world, pos, player);
                                world.playSound(null, player.posX, player.posY, player.posZ, 
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
