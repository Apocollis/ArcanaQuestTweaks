package com.apocollis.aqtweaks;

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
            ctx.getServerHandler().player.getServerWorld().addScheduledTask(() -> {
                EntityPlayerMP player = ctx.getServerHandler().player;
                if (player != null) {
                    int cost = ArcanaQuestTweaksConfig.staminaModule.ledgeClimb.ledgeClimbCost;
                    if (Reflect.hasEnoughStamina(player, cost)) {
                        FeathersHelper.decreaseFeathers(player, cost);

                        // Play a scraping step sound of the block the player is climbing
                        World world = player.world;
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
            return null;
        }
    }
}
