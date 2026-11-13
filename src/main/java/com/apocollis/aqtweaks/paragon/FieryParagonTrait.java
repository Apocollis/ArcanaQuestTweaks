package com.apocollis.aqtweaks.paragon;

import java.util.EnumSet;

import com.apocollis.aqtweaks.ArcanaQuestTweaksConfig;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.world.WorldServer;
import net.minecraftforge.event.entity.living.LivingHurtEvent;

/** Fiery: a hit sets the target on fire, as Twilight Forest's Fiery pickaxe does (15 seconds there). */
public class FieryParagonTrait extends ParagonTrait {
    public FieryParagonTrait() {
        super("fiery", "blockFiery", "twilightforest", TartariteKind.weaponsAndTools());
    }

    @Override
    public void onMeleeHit(LivingHurtEvent event, EntityPlayer player, EntityLivingBase target, ItemStack stack,
                           TartariteKind kind) {
        int seconds = ArcanaQuestTweaksConfig.ParagonModuleConfig.traits.fieryBurnSeconds;
        if (seconds <= 0 || target.isImmuneToFire()) return;
        target.setFire(seconds);
        if (target.world instanceof WorldServer) {
            ((WorldServer) target.world).spawnParticle(EnumParticleTypes.FLAME, target.posX,
                    target.posY + target.height * 0.5D, target.posZ, 20, 0.3D, 0.3D, 0.3D, 0.02D);
        }
    }
}
