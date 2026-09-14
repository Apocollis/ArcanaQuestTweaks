package com.apocollis.aqtweaks.gaia;

import com.apocollis.aqtweaks.ArcanaQuestTweaksConfig;
import com.apocollis.aqtweaks.ArcanaQuestTweaksConfig.GrimoireOfGaiaConfig;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityList;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.attributes.IAttribute;
import net.minecraft.entity.ai.attributes.IAttributeInstance;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.event.entity.living.PotionEvent;
import net.minecraftforge.fml.common.eventhandler.Event;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

/**
 * JSON {@code ATTACK_DAMAGE} / max health / armor bases on join. Weapons and Strength still stack.
 * Potion deny is a backup if the melee mixin redirect does not skip Gaia pierce.
 * Deep Dwarf attack uses the Gaia cfg knob; HP/armor use JSON. HP/armor are not gated on pierce.
 */
public class GaiaDamageHandler {

    @SubscribeEvent
    public void onJoin(EntityJoinWorldEvent event) {
        if (event.getWorld() == null || event.getWorld().isRemote) {
            return;
        }
        Entity entity = event.getEntity();
        if (!(entity instanceof EntityLivingBase living)) {
            return;
        }
        ResourceLocation id = EntityList.getKey(entity);
        if (id == null) {
            return;
        }
        String key = id.toString();
        applyHealth(living, GaiaDamageConfig.get().healthFor(key));
        applyBase(living, SharedMonsterAttributes.ARMOR, GaiaDamageConfig.get().armorFor(key));
        applyAttack(living, key);
    }

    private static void applyAttack(EntityLivingBase living, String key) {
        IAttributeInstance attr = living.getEntityAttribute(SharedMonsterAttributes.ATTACK_DAMAGE);
        if (attr == null) {
            return;
        }
        if (GaiaDeepDwarfRegistry.ID_STRING.equals(key)) {
            attr.setBaseValue(GrimoireOfGaiaConfig.deepDwarfAttackDamage);
            return;
        }
        if (!ArcanaQuestTweaksConfig.GrimoireOfGaiaConfig.disablePiercingDamage) {
            return;
        }
        Float listed = GaiaDamageConfig.get().damageFor(key);
        if (listed == null) {
            return;
        }
        attr.setBaseValue(listed.doubleValue());
    }

    private static void applyHealth(EntityLivingBase living, Float listed) {
        if (listed == null) {
            return;
        }
        IAttributeInstance attr = living.getEntityAttribute(SharedMonsterAttributes.MAX_HEALTH);
        if (attr == null) {
            return;
        }
        float oldMax = (float) attr.getAttributeValue();
        float current = living.getHealth();
        attr.setBaseValue(listed.doubleValue());
        float newMax = (float) attr.getAttributeValue();
        if (current >= oldMax - 0.5f) {
            living.setHealth(newMax);
        } else {
            living.setHealth(Math.min(current, newMax));
        }
    }

    private static void applyBase(EntityLivingBase living, IAttribute attribute, Float listed) {
        if (listed == null) {
            return;
        }
        IAttributeInstance attr = living.getEntityAttribute(attribute);
        if (attr == null) {
            return;
        }
        attr.setBaseValue(listed.doubleValue());
    }

    @SubscribeEvent
    public void onPotionApplicable(PotionEvent.PotionApplicableEvent event) {
        if (GaiaPierce.shouldDenyMeleePiercePotion(event.getEntityLiving(), event.getPotionEffect())) {
            event.setResult(Event.Result.DENY);
        }
    }
}
