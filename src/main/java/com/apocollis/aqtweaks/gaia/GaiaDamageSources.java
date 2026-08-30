package com.apocollis.aqtweaks.gaia;

import net.minecraft.entity.Entity;
import net.minecraft.util.EntityDamageSourceIndirect;

public class GaiaDamageSources {

    /** Attributed blast hit: armor + Blast Protection, not magic-bypass. */
    public static class Bomb extends EntityDamageSourceIndirect {
        public Bomb(Entity bomb, Entity thrower) {
            super("explosion.player", bomb, thrower);
            setExplosion();
        }
    }
}
