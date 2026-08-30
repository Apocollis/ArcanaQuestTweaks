package com.apocollis.aqtweaks.gaia;

import com.apocollis.aqtweaks.ArcanaQuestTweaksConfig;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityList;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.attributes.IAttributeInstance;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

public class GaiaDamageHandler {

    @SubscribeEvent
    public void onJoin(EntityJoinWorldEvent event) {
        if (!ArcanaQuestTweaksConfig.GrimoireOfGaiaConfig.disablePiercingDamage) return;
        if (event.getWorld() == null || event.getWorld().isRemote) return;
        Entity entity = event.getEntity();
        if (!(entity instanceof EntityLivingBase)) return;
        ResourceLocation id = EntityList.getKey(entity);
        if (id == null) return;
        Float base = GaiaDamageConfig.get().damageFor(id.toString());
        if (base == null) return;
        IAttributeInstance attr = ((EntityLivingBase) entity)
                .getEntityAttribute(SharedMonsterAttributes.ATTACK_DAMAGE);
        if (attr == null) return;
        attr.setBaseValue(base.doubleValue());
    }
}
