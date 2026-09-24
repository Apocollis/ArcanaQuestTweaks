package com.apocollis.aqtweaks.reskillable.client;

import io.netty.buffer.ByteBuf;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

import java.util.ArrayList;
import java.util.List;

/** Ore positions for the mining player. The client draws the boxes. */
public class PacketProspectorMarks implements IMessage {

    private BlockPos[] positions = new BlockPos[0];

    public PacketProspectorMarks() {}

    public PacketProspectorMarks(List<BlockPos> ores) {
        positions = ores.toArray(new BlockPos[0]);
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        int count = buf.readInt();
        if (count < 0 || count > 8192) {
            positions = new BlockPos[0];
            return;
        }
        positions = new BlockPos[count];
        for (int i = 0; i < count; i++) {
            positions[i] = new BlockPos(buf.readInt(), buf.readInt(), buf.readInt());
        }
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeInt(positions.length);
        for (BlockPos pos : positions) {
            buf.writeInt(pos.getX());
            buf.writeInt(pos.getY());
            buf.writeInt(pos.getZ());
        }
    }

    public static class Handler implements IMessageHandler<PacketProspectorMarks, IMessage> {
        @Override
        public IMessage onMessage(PacketProspectorMarks message, MessageContext ctx) {
            if (!ctx.side.isClient()) return null;
            List<BlockPos> copy = new ArrayList<>();
            for (BlockPos pos : message.positions) {
                if (pos != null) copy.add(pos);
            }
            net.minecraft.client.Minecraft.getMinecraft().addScheduledTask(() -> ProspectorOutline.show(copy));
            return null;
        }
    }
}
