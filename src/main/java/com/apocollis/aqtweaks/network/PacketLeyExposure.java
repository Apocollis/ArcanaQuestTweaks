package com.apocollis.aqtweaks.network;

import com.apocollis.aqtweaks.twilightforest.ClientLey;

import io.netty.buffer.ByteBuf;

import net.minecraft.client.Minecraft;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

/** Server to client: the player's current Ley-Rupture exposure factor (0 when sheltered, safe or not active). */
public class PacketLeyExposure implements IMessage {
    public float exposure;

    public PacketLeyExposure() {}

    public PacketLeyExposure(float exposure) {
        this.exposure = exposure;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        exposure = buf.readFloat();
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeFloat(exposure);
    }

    public static class Handler implements IMessageHandler<PacketLeyExposure, IMessage> {
        @Override
        public IMessage onMessage(PacketLeyExposure m, MessageContext ctx) {
            if (!ctx.side.isClient()) return null;
            final float value = m.exposure;
            Minecraft.getMinecraft().addScheduledTask(() -> ClientLey.exposure = value);
            return null;
        }
    }
}
