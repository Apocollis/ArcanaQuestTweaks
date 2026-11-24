package com.apocollis.aqtweaks.additions.client;

import com.apocollis.aqtweaks.ArcanaQuestTweaks;
import com.apocollis.aqtweaks.additions.EntityNamelessOne;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import software.bernie.geckolib3.core.event.predicate.AnimationEvent;
import software.bernie.geckolib3.core.processor.IBone;
import software.bernie.geckolib3.model.AnimatedGeoModel;
import software.bernie.geckolib3.model.provider.data.EntityModelData;

@SideOnly(Side.CLIENT)
public class NamelessOneModel extends AnimatedGeoModel<EntityNamelessOne> {
    private static final ResourceLocation MODEL =
            new ResourceLocation(ArcanaQuestTweaks.MODID, "geo/nameless_one.geo.json");
    private static final ResourceLocation TEXTURE =
            new ResourceLocation(ArcanaQuestTweaks.MODID, "textures/entity/nameless_one.png");
    private static final ResourceLocation ANIMATION =
            new ResourceLocation(ArcanaQuestTweaks.MODID, "animations/nameless_one.animation.json");

    @Override
    public ResourceLocation getModelLocation(EntityNamelessOne object) {
        return MODEL;
    }

    @Override
    public ResourceLocation getTextureLocation(EntityNamelessOne object) {
        return TEXTURE;
    }

    @Override
    public ResourceLocation getAnimationFileLocation(EntityNamelessOne object) {
        return ANIMATION;
    }

    @Override
    public void setLivingAnimations(EntityNamelessOne entity, Integer uniqueID, AnimationEvent customPredicate) {
        super.setLivingAnimations(entity, uniqueID, customPredicate);

        // Beat 1 of death sequence: hide halo_ring completely once the shatter finishes at tick 31 (1.55s)
        IBone haloRing = this.getAnimationProcessor().getBone("halo_ring");
        if (haloRing != null) {
            boolean hideHalo = (entity.getAnimState() == EntityNamelessOne.STATE_DEATH || entity.getHealth() <= 0.0F)
                    && entity.getCustomDeathTicks() > 31;
            haloRing.setHidden(hideHalo);
        }

        if (customPredicate == null
                || entity.getAnimState() == EntityNamelessOne.STATE_DEATH
                || entity.getAnimState() == EntityNamelessOne.STATE_SPAWN_ERUPT
                || entity.getHealth() <= 0.0F) {
            return;
        }

        IBone head = this.getAnimationProcessor().getBone("head");
        if (head != null) {
            EntityModelData extraData = (EntityModelData) customPredicate.getExtraDataOfType(EntityModelData.class).get(0);
            if (extraData != null) {
                head.setRotationX(head.getRotationX() + extraData.headPitch * ((float) Math.PI / 180.0F) * 0.55F);
                head.setRotationY(head.getRotationY() + extraData.netHeadYaw * ((float) Math.PI / 180.0F) * 0.55F);
            }
        }
    }
}
