package com.apocollis.aqtweaks.gaia;

import net.minecraft.entity.Entity;
import net.minecraft.util.EntityDamageSourceIndirect;

public class GaiaDamageSources {

    /**
     * Attributed blast: armor + Blast Protection, not magic-bypass.
     * Vanilla shield facing-check can zero the hit ({@code explosion.player} + bomb location).
     */
    public static class Bomb extends EntityDamageSourceIndirect {
        public Bomb(Entity bomb, Entity thrower) {
            super("explosion.player", bomb, thrower);
            setExplosion();
        }
    }
}
