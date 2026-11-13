package com.apocollis.aqtweaks.paragon;

import java.util.EnumSet;

import com.apocollis.aqtweaks.ArcanaQuestTweaksConfig;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraftforge.event.entity.living.LivingHurtEvent;

/**
 * Knightmetal, as Twilight Forest's sword handler has it: the sword and the pickaxe do extra damage to an armored target,
 * the axe to an unarmored one.
 */
public class KnightmetalParagonTrait extends ParagonTrait {
    public KnightmetalParagonTrait() {
        super("knightmetal", "blockKnightmetal", "twilightforest",
                EnumSet.of(TartariteKind.SWORD, TartariteKind.PICKAXE, TartariteKind.AXE));
    }

    @Override
    public void onMeleeHit(LivingHurtEvent event, EntityPlayer player, EntityLivingBase target, ItemStack stack,
                           TartariteKind kind) {
        boolean armored = target.getTotalArmorValue() > 0;
        boolean applies = kind == TartariteKind.AXE ? !armored : armored;
        if (applies) {
            event.setAmount(event.getAmount() + (float) ArcanaQuestTweaksConfig.ParagonModuleConfig.traits.knightmetalBonusDamage);
        }
    }
}
