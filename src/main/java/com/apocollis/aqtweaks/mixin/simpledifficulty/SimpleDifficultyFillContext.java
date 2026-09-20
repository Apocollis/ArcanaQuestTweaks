package com.apocollis.aqtweaks.mixin.simpledifficulty;

import net.minecraft.entity.player.EntityPlayer;

final class SimpleDifficultyFillContext {

    static final ThreadLocal<EntityPlayer> PLAYER = new ThreadLocal<>();

    private SimpleDifficultyFillContext() {}
}
