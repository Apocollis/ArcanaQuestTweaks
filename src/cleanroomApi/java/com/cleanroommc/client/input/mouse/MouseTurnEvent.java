package com.cleanroommc.client.input.mouse;

import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraftforge.fml.common.eventhandler.Cancelable;
import net.minecraftforge.fml.common.eventhandler.Event;

/**
 * Cleanroom 0.7+ client event posted by {@code com.cleanroommc.client.input.InputHooks#onMouseTurn}
 * before camera look is applied.
 *
 * <p>Compile-only stub so Arcana Quest Tweaks can compile against Cleanroom 0.7+ APIs without
 * requiring the full Cleanroom 0.7 runtime jar on the Gradle compile classpath.
 */
@Cancelable
public class MouseTurnEvent extends Event {

    private final EntityPlayerSP player;
    private float yaw;
    private float pitch;

    public MouseTurnEvent(EntityPlayerSP player, float yaw, float pitch) {
        this.player = player;
        this.yaw = yaw;
        this.pitch = pitch;
    }

    public EntityPlayerSP getPlayer() {
        return player;
    }

    public float getYaw() {
        return yaw;
    }

    public void setYaw(float yaw) {
        this.yaw = yaw;
    }

    public float getPitch() {
        return pitch;
    }

    public void setPitch(float pitch) {
        this.pitch = pitch;
    }

    public double getFrameDeltaSeconds() {
        return 0.0;
    }
}
