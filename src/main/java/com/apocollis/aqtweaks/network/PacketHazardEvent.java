package com.apocollis.aqtweaks.network;

import com.apocollis.aqtweaks.hazard.ClientHazards;
import com.apocollis.aqtweaks.hazard.HazardEvent;

import io.netty.buffer.ByteBuf;

import net.minecraft.client.Minecraft;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

/**
 * Server to client: one whole hazard event (warning, start, end, ramps, seed, intensity) for a dimension.
 * Sent once when the event is created, again on login, dimension change and respawn. A new event for the
 * same channel replaces the old one.
 */
public class PacketHazardEvent implements IMessage {
    public int dimension;
    public HazardEvent event;

    public PacketHazardEvent() {}

    public PacketHazardEvent(int dimension, HazardEvent event) {
        this.dimension = dimension;
        this.event = event;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        dimension = buf.readInt();
        int channel = buf.readInt();
        long id = buf.readLong();
        long warningStart = buf.readLong();
        long start = buf.readLong();
        long end = buf.readLong();
        int rampUp = buf.readInt();
        int rampDown = buf.readInt();
        long seed = buf.readLong();
        float intensity = buf.readFloat();
        int flags = buf.readInt();
        event = new HazardEvent(channel, id, warningStart, start, end, rampUp, rampDown, seed, intensity, flags);
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeInt(dimension);
        buf.writeInt(event.channel);
        buf.writeLong(event.id);
        buf.writeLong(event.warningStart);
        buf.writeLong(event.start);
        buf.writeLong(event.end);
        buf.writeInt(event.rampUp);
        buf.writeInt(event.rampDown);
        buf.writeLong(event.seed);
        buf.writeFloat(event.intensity);
        buf.writeInt(event.flags);
    }

    public static class Handler implements IMessageHandler<PacketHazardEvent, IMessage> {
        @Override
        public IMessage onMessage(PacketHazardEvent message, MessageContext ctx) {
            if (!ctx.side.isClient()) return null;
            final int dim = message.dimension;
            final HazardEvent event = message.event;
            Minecraft.getMinecraft().addScheduledTask(() -> ClientHazards.accept(dim, event));
            return null;
        }
    }
}
