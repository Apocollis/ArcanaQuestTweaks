package com.apocollis.aqtweaks.additions.client;

import com.apocollis.aqtweaks.ArcanaQuestTweaks;
import com.apocollis.aqtweaks.additions.EntityNamelessVessel;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import software.bernie.geckolib3.core.event.predicate.AnimationEvent;
import software.bernie.geckolib3.core.processor.IBone;
import software.bernie.geckolib3.model.AnimatedGeoModel;
import software.bernie.geckolib3.model.provider.data.EntityModelData;

@SideOnly(Side.CLIENT)
public class NamelessVesselModel extends AnimatedGeoModel<EntityNamelessVessel> {
    private static final ResourceLocation MODEL =
            new ResourceLocation(ArcanaQuestTweaks.MODID, "geo/nameless_vessel.geo.json");
    private static final ResourceLocation TEXTURE =
            new ResourceLocation(ArcanaQuestTweaks.MODID, "textures/entity/nameless_vessel.png");
    private static final ResourceLocation ANIMATION =
            new ResourceLocation(ArcanaQuestTweaks.MODID, "animations/nameless_vessel.animation.json");

    @Override
    public ResourceLocation getModelLocation(EntityNamelessVessel object) {
        return MODEL;
    }

    @Override
    public ResourceLocation getTextureLocation(EntityNamelessVessel object) {
        return TEXTURE;
    }

    @Override
    public ResourceLocation getAnimationFileLocation(EntityNamelessVessel object) {
        return ANIMATION;
    }

    @Override
    public void setLivingAnimations(EntityNamelessVessel entity, Integer uniqueID, AnimationEvent customPredicate) {
        super.setLivingAnimations(entity, uniqueID, customPredicate);
        if (customPredicate == null || entity.isErupting()) {
            return;
        }
        IBone head = this.getAnimationProcessor().getBone("head");
        if (head != null) {
            EntityModelData extraData = (EntityModelData) customPredicate.getExtraDataOfType(EntityModelData.class).get(0);
            if (extraData != null) {
                head.setRotationX(head.getRotationX() + extraData.headPitch * ((float) Math.PI / 180.0F) * 0.65F);
                head.setRotationY(head.getRotationY() + extraData.netHeadYaw * ((float) Math.PI / 180.0F) * 0.65F);
            }
        }
    }
}
