package com.apocollis.aqtweaks.thaumadditions;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.fml.common.eventhandler.Event;

/**
 * Posted on the Forge bus each time a raised Aegis absorbs a hit (not cancelable). A parry or timing perk listens
 * here, next to whatever it uses for ordinary shields. {@link #ticksRaised} is the shield's age at the hit.
 */
public class AegisBlockEvent extends Event {
    private final EntityPlayer player;
    private final Entity attacker;
    private final float damage;
    private final float poolRemaining;
    private final int ticksRaised;
    private final boolean broke;

    public AegisBlockEvent(EntityPlayer player, Entity attacker, float damage, float poolRemaining, int ticksRaised,
                           boolean broke) {
        this.player = player;
        this.attacker = attacker;
        this.damage = damage;
        this.poolRemaining = poolRemaining;
        this.ticksRaised = ticksRaised;
        this.broke = broke;
    }

    public EntityPlayer getPlayer() {
        return player;
    }

    /** The entity that dealt the hit (the true source when there is one), or null. */
    public Entity getAttacker() {
        return attacker;
    }

    public float getDamage() {
        return damage;
    }

    public float getPoolRemaining() {
        return poolRemaining;
    }

    public int getTicksRaised() {
        return ticksRaised;
    }

    /** True when this hit emptied the pool. */
    public boolean broke() {
        return broke;
    }
}
