package com.apocollis.aqtweaks.stamina;

import com.apocollis.aqtweaks.util.Reflect;

import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

public class PacketSyncGrappleInput implements IMessage {
    public static final int MODE_NEUTRAL = 0;
    public static final int MODE_CLIMB = 1;
    public static final int MODE_DESCEND = 2;
    private byte mode;
    private boolean motorActive;

    public PacketSyncGrappleInput() {}

    public PacketSyncGrappleInput(int mode, boolean motorActive) {
        this.mode = (byte) mode;
        this.motorActive = motorActive;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        this.mode = buf.readByte();
        this.motorActive = buf.readBoolean();
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeByte(mode);
        buf.writeBoolean(motorActive);
    }

    public static class Handler implements IMessageHandler<PacketSyncGrappleInput, IMessage> {
        @Override
        public IMessage onMessage(PacketSyncGrappleInput message, MessageContext ctx) {
            EntityPlayerMP player = Reflect.getServerPlayer(ctx);
            net.minecraft.server.MinecraftServer server = Reflect.getServer(player);
            if (player != null && server != null) {
                byte mode = message.mode;
                boolean motorActive = message.motorActive;
                server.addScheduledTask(() -> {
                    net.minecraft.nbt.NBTTagCompound data = Reflect.getEntityData(player);
                    Reflect.setInteger(data, "StaminaTweaksGrappleMode", mode);
                    Reflect.setBoolean(data, "StaminaTweaksGrappleMotor", motorActive);
                });
            }
            return null;
        }
    }
}
