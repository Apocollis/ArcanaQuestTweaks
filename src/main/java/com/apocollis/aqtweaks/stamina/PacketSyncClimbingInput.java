package com.apocollis.aqtweaks.stamina;

import com.apocollis.aqtweaks.ArcanaQuestTweaks;

import com.apocollis.aqtweaks.util.Reflect;

import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

public class PacketSyncClimbingInput implements IMessage {
    private boolean jumpPressed;

    public PacketSyncClimbingInput() {}

    public PacketSyncClimbingInput(boolean jumpPressed) {
        this.jumpPressed = jumpPressed;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        this.jumpPressed = buf.readBoolean();
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeBoolean(this.jumpPressed);
    }

    public static class Handler implements IMessageHandler<PacketSyncClimbingInput, IMessage> {
        @Override
        public IMessage onMessage(PacketSyncClimbingInput message, MessageContext ctx) {
            EntityPlayerMP player = Reflect.getServerPlayer(ctx);
            net.minecraft.server.MinecraftServer server = Reflect.getServer(player);
            if (player != null && server != null) {
                server.addScheduledTask(() -> {
                    Reflect.setBoolean(Reflect.getEntityData(player), "StaminaTweaksClimbJumpInput", message.jumpPressed);
                });
            }
            return null;
        }
    }
}
