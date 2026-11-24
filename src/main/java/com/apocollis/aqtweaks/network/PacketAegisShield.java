package com.apocollis.aqtweaks.network;

import com.apocollis.aqtweaks.thaumadditions.client.AegisClientState;

import io.netty.buffer.ByteBuf;

import net.minecraft.client.Minecraft;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

/** Server to client: a player's Aegis shield state, so every nearby client can draw the shield and its runes. */
public class PacketAegisShield implements IMessage {
    public static final byte STATE = 0;
    public static final byte ENDED = 1;
    public static final byte BROKE = 2;
    public static final byte HIT = 3;

    public int entityId;
    public byte mode;
    public float pool;
    public float maxPool;

    public PacketAegisShield() {}

    public PacketAegisShield(int entityId, byte mode, float pool, float maxPool) {
        this.entityId = entityId;
        this.mode = mode;
        this.pool = pool;
        this.maxPool = maxPool;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        entityId = buf.readInt();
        mode = buf.readByte();
        pool = buf.readFloat();
        maxPool = buf.readFloat();
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeInt(entityId);
        buf.writeByte(mode);
        buf.writeFloat(pool);
        buf.writeFloat(maxPool);
    }

    public static class Handler implements IMessageHandler<PacketAegisShield, IMessage> {
        @Override
        public IMessage onMessage(PacketAegisShield message, MessageContext ctx) {
            if (!ctx.side.isClient()) return null;
            final PacketAegisShield m = message;
            Minecraft.getMinecraft().addScheduledTask(() -> AegisClientState.apply(m.entityId, m.mode, m.pool, m.maxPool));
            return null;
        }
    }
}
