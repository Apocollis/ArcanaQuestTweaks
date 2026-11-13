package com.apocollis.aqtweaks.paragon;

import com.apocollis.aqtweaks.ArcanaQuestTweaksConfig;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraftforge.event.entity.living.LivingHurtEvent;

/** Octine: a hit sometimes sets the target on fire (The Betweenlands: 5 seconds, a 25 percent chance). */
public class OctineParagonTrait extends ParagonTrait {
    public OctineParagonTrait() {
        super("octine", "blockOctine", "thebetweenlands", TartariteKind.weaponsAndTools());
    }

    @Override
    public void onMeleeHit(LivingHurtEvent event, EntityPlayer player, EntityLivingBase target, ItemStack stack,
                           TartariteKind kind) {
        ArcanaQuestTweaksConfig.ParagonTraits cfg = ArcanaQuestTweaksConfig.ParagonModuleConfig.traits;
        if (cfg.octineBurnSeconds > 0 && !target.isImmuneToFire() && target.world.rand.nextDouble() < cfg.octineBurnChance) {
            target.setFire(cfg.octineBurnSeconds);
        }
    }
}
