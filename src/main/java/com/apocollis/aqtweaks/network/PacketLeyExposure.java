package com.apocollis.aqtweaks.network;

import com.apocollis.aqtweaks.twilightforest.ClientLey;

import io.netty.buffer.ByteBuf;

import net.minecraft.client.Minecraft;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

/**
 * Server to client: the player's current Ley-Rupture exposure factor (0 when sheltered, safe or not active), and
 * optionally the boxes of the sheltered structures near the player (six ints each), sent only when they change.
 */
public class PacketLeyExposure implements IMessage {
    public float exposure;
    /** Null when the packet carries no box update. */
    public int[] boxes;

    public PacketLeyExposure() {}

    public PacketLeyExposure(float exposure) {
        this.exposure = exposure;
    }

    public PacketLeyExposure(float exposure, int[] boxes) {
        this.exposure = exposure;
        this.boxes = boxes;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        exposure = buf.readFloat();
        if (buf.readBoolean()) {
            int count = Math.max(0, Math.min(buf.readUnsignedShort(), 256));
            boxes = new int[count * 6];
            for (int i = 0; i < boxes.length; i++) {
                boxes[i] = buf.readInt();
            }
        }
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeFloat(exposure);
        buf.writeBoolean(boxes != null);
        if (boxes != null) {
            int count = Math.min(boxes.length / 6, 256);
            buf.writeShort(count);
            for (int i = 0; i < count * 6; i++) {
                buf.writeInt(boxes[i]);
            }
        }
    }

    public static class Handler implements IMessageHandler<PacketLeyExposure, IMessage> {
        @Override
        public IMessage onMessage(PacketLeyExposure m, MessageContext ctx) {
            if (!ctx.side.isClient()) return null;
            final float value = m.exposure;
            final int[] boxes = m.boxes;
            Minecraft.getMinecraft().addScheduledTask(() -> {
                ClientLey.exposure = value;
                if (boxes != null) ClientLey.shelterBoxes = boxes;
            });
            return null;
        }
    }
}
