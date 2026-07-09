package com.apocollis.aqtweaks;

import net.minecraft.entity.Entity;
import net.minecraft.util.EntityDamageSource;
import net.minecraft.util.EntityDamageSourceIndirect;

public class GaiaDamageSources {

    public static class Melee extends EntityDamageSource {
        public Melee(Entity source) {
            super("mob", source);
        }

        @Override
        public boolean isMagicDamage() {
            return true;
        }
    }

    public static class Projectile extends EntityDamageSourceIndirect {
        public Projectile(Entity source, Entity indirectEntityIn) {
            super("indirectMagic", source, indirectEntityIn);
        }

        @Override
        public boolean isMagicDamage() {
            return true;
        }
    }
}
