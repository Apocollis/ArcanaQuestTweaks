package com.apocollis.aqtweaks.gaia;

import com.apocollis.aqtweaks.ArcanaQuestTweaksConfig;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityList;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.attributes.IAttributeInstance;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.event.entity.living.PotionEvent;
import net.minecraftforge.fml.common.eventhandler.Event;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

/**
 * JSON {@code ATTACK_DAMAGE} base on join. Weapons and buffs still stack.
 * Potion deny is a backup if the melee mixin redirect does not skip Gaia pierce.
 */
public class GaiaDamageHandler {

    @SubscribeEvent
    public void onJoin(EntityJoinWorldEvent event) {
        if (!ArcanaQuestTweaksConfig.GrimoireOfGaiaConfig.disablePiercingDamage) return;
        if (event.getWorld() == null || event.getWorld().isRemote) return;
        Entity entity = event.getEntity();
        if (!(entity instanceof EntityLivingBase)) return;
        ResourceLocation id = EntityList.getKey(entity);
        if (id == null) return;
        Float listed = GaiaDamageConfig.get().damageFor(id.toString());
        if (listed == null) return;
        IAttributeInstance attr = ((EntityLivingBase) entity)
                .getEntityAttribute(SharedMonsterAttributes.ATTACK_DAMAGE);
        if (attr == null) return;
        attr.setBaseValue(listed.doubleValue());
    }

    @SubscribeEvent
    public void onPotionApplicable(PotionEvent.PotionApplicableEvent event) {
        if (GaiaPierce.shouldDenyMeleePiercePotion(event.getEntityLiving(), event.getPotionEffect())) {
            event.setResult(Event.Result.DENY);
        }
    }
}
