package com.apocollis.aqtweaks.stamina;


import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

public class PacketSyncGrappleInput implements IMessage {
    public static final int MODE_NEUTRAL = 0;
    public static final int MODE_CLIMB = 1;
    public static final int MODE_DESCEND = 2;
    public static final int MODE_SWING = 3;
    private byte mode;
    private boolean motorActive;
    private boolean grounded;

    public PacketSyncGrappleInput() {}

    public PacketSyncGrappleInput(int mode, boolean motorActive, boolean grounded) {
        this.mode = (byte) mode;
        this.motorActive = motorActive;
        this.grounded = grounded;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        this.mode = buf.readByte();
        this.motorActive = buf.readBoolean();
        this.grounded = buf.readBoolean();
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeByte(mode);
        buf.writeBoolean(motorActive);
        buf.writeBoolean(grounded);
    }

    public static class Handler implements IMessageHandler<PacketSyncGrappleInput, IMessage> {
        @Override
        public IMessage onMessage(PacketSyncGrappleInput message, MessageContext ctx) {
            EntityPlayerMP player = ctx.getServerHandler().player;
            if (player == null) return null;
            net.minecraft.server.MinecraftServer server = player.getServer();
            if (player != null && server != null) {
                byte mode = message.mode;
                boolean motorActive = message.motorActive;
                boolean grounded = message.grounded;
                server.addScheduledTask(() -> {
                    if (player.capabilities.isCreativeMode || player.isSpectator()) return;
                    net.minecraft.nbt.NBTTagCompound data = player.getEntityData();
                    data.setInteger("StaminaTweaksGrappleMode", mode);
                    data.setBoolean("StaminaTweaksGrappleMotor", motorActive);
                    data.setBoolean("StaminaTweaksGrappleGrounded", grounded);
                });
            }
            return null;
        }
    }
}
