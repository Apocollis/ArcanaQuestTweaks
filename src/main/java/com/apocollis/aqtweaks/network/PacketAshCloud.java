package com.apocollis.aqtweaks.network;

import com.apocollis.aqtweaks.nether.ClientAshClouds;

import io.netty.buffer.ByteBuf;

import net.minecraft.client.Minecraft;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

/**
 * Server to client: one ash cloud (centre, velocity, reach, coarse occupancy bitmap). Sent on spawn, about once
 * a second while it lives, on login and dimension change, and once with {@code alive = false} when it ends.
 */
public class PacketAshCloud implements IMessage {
    public int id;
    public boolean alive;
    public double x;
    public double y;
    public double z;
    public float vx;
    public float vz;
    public float rh;
    public float rv;
    public long endTick;
    public int ox;
    public int oy;
    public int oz;
    public int sx;
    public int sy;
    public int sz;
    public byte[] bits = new byte[0];

    public PacketAshCloud() {}

    public static PacketAshCloud removed(int id) {
        PacketAshCloud p = new PacketAshCloud();
        p.id = id;
        p.alive = false;
        return p;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        id = buf.readInt();
        alive = buf.readBoolean();
        if (!alive) return;
        x = buf.readDouble();
        y = buf.readDouble();
        z = buf.readDouble();
        vx = buf.readFloat();
        vz = buf.readFloat();
        rh = buf.readFloat();
        rv = buf.readFloat();
        endTick = buf.readLong();
        ox = buf.readInt();
        oy = buf.readInt();
        oz = buf.readInt();
        sx = buf.readShort();
        sy = buf.readShort();
        sz = buf.readShort();
        int n = buf.readInt();
        bits = new byte[n];
        buf.readBytes(bits);
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeInt(id);
        buf.writeBoolean(alive);
        if (!alive) return;
        buf.writeDouble(x);
        buf.writeDouble(y);
        buf.writeDouble(z);
        buf.writeFloat(vx);
        buf.writeFloat(vz);
        buf.writeFloat(rh);
        buf.writeFloat(rv);
        buf.writeLong(endTick);
        buf.writeInt(ox);
        buf.writeInt(oy);
        buf.writeInt(oz);
        buf.writeShort(sx);
        buf.writeShort(sy);
        buf.writeShort(sz);
        buf.writeInt(bits.length);
        buf.writeBytes(bits);
    }

    public static class Handler implements IMessageHandler<PacketAshCloud, IMessage> {
        @Override
        public IMessage onMessage(PacketAshCloud m, MessageContext ctx) {
            if (!ctx.side.isClient()) return null;
            Minecraft.getMinecraft().addScheduledTask(() -> ClientAshClouds.accept(m.id, m.alive, m.x, m.y, m.z, m.vx,
                    m.vz, m.rh, m.rv, m.endTick, m.ox, m.oy, m.oz, m.sx, m.sy, m.sz, m.bits));
            return null;
        }
    }
}
