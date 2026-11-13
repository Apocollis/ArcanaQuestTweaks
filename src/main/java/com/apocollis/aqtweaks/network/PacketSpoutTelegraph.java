package com.apocollis.aqtweaks.network;

import com.apocollis.aqtweaks.nether.ClientSpouts;

import io.netty.buffer.ByteBuf;

import net.minecraft.client.Minecraft;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

/** Server to client: a spout at a position starts its telegraph; it erupts after {@code ticks} ticks. */
public class PacketSpoutTelegraph implements IMessage {
    public int x;
    public int y;
    public int z;
    public int ticks;

    public PacketSpoutTelegraph() {}

    public PacketSpoutTelegraph(int x, int y, int z, int ticks) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.ticks = ticks;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        x = buf.readInt();
        y = buf.readInt();
        z = buf.readInt();
        ticks = buf.readInt();
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeInt(x);
        buf.writeInt(y);
        buf.writeInt(z);
        buf.writeInt(ticks);
    }

    public static class Handler implements IMessageHandler<PacketSpoutTelegraph, IMessage> {
        @Override
        public IMessage onMessage(PacketSpoutTelegraph message, MessageContext ctx) {
            if (!ctx.side.isClient()) return null;
            final PacketSpoutTelegraph m = message;
            Minecraft.getMinecraft().addScheduledTask(() -> ClientSpouts.add(m.x, m.y, m.z, m.ticks));
            return null;
        }
    }
}
