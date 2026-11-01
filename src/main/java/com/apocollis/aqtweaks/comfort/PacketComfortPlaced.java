package com.apocollis.aqtweaks.comfort;

import io.netty.buffer.ByteBuf;
import net.minecraft.client.Minecraft;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

/** One placed comfort block, for this player only. */
public class PacketComfortPlaced implements IMessage {

    public BlockPos pos = BlockPos.ORIGIN;
    public boolean addsScore;

    public PacketComfortPlaced() {}

    public PacketComfortPlaced(BlockPos pos, boolean addsScore) {
        this.pos = pos;
        this.addsScore = addsScore;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        pos = new BlockPos(buf.readInt(), buf.readInt(), buf.readInt());
        addsScore = buf.readBoolean();
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeInt(pos.getX());
        buf.writeInt(pos.getY());
        buf.writeInt(pos.getZ());
        buf.writeBoolean(addsScore);
    }

    public static class Handler implements IMessageHandler<PacketComfortPlaced, IMessage> {
        @Override
        public IMessage onMessage(PacketComfortPlaced message, MessageContext ctx) {
            if (!ctx.side.isClient()) return null;
            Minecraft.getMinecraft().addScheduledTask(() -> ComfortClientState.acceptPlaced(message));
            return null;
        }
    }
}
