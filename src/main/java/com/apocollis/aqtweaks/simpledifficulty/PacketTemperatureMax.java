package com.apocollis.aqtweaks.simpledifficulty;

import com.elenai.elenaidodge2.util.ClientStorage;
import io.netty.buffer.ByteBuf;
import net.minecraft.client.Minecraft;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

/** Server-to-client Elenai feather cap. Does not send the weights config packet. */
public class PacketTemperatureMax implements IMessage {

    private int max;

    public PacketTemperatureMax() {}

    public PacketTemperatureMax(int max) {
        this.max = max;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        max = buf.readInt();
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeInt(max);
    }

    public static class Handler implements IMessageHandler<PacketTemperatureMax, IMessage> {
        @Override
        public IMessage onMessage(PacketTemperatureMax message, MessageContext ctx) {
            if (!ctx.side.isClient()) return null;
            int cap = Math.max(0, message.max);
            Minecraft.getMinecraft().addScheduledTask(() -> ClientStorage.maxDodges = cap);
            return null;
        }
    }
}
