package com.apocollis.aqtweaks.mixin.gaia;

import gaia.entity.EntityMobHostileBase;
import gaia.entity.ai.EntityAIGaiaStrafe;
import gaia.entity.ai.Ranged;
import gaia.entity.monster.EntityGaiaDeathword;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.IRangedAttackMob;
import net.minecraft.entity.ai.EntityAIAttackMelee;
import net.minecraft.entity.ai.EntityAIAttackRanged;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = EntityGaiaDeathword.class, remap = false)
public abstract class MixinEntityGaiaDeathword extends EntityMobHostileBase implements IRangedAttackMob {

    @Shadow
    private EntityAIAttackMelee aiMeleeAttack;

    @Shadow
    private EntityAIGaiaStrafe aiStrafe;

    @Unique
    private EntityAIAttackRanged aqtweaks$rangedMagic;

    public MixinEntityGaiaDeathword(World worldIn) {
        super(worldIn);
    }

    @Override
    public void attackEntityWithRangedAttack(EntityLivingBase target, float distanceFactor) {
        Ranged.magic(target, this, distanceFactor);
    }

    @Override
    public void setSwingingArms(boolean swingingArms) {
    }

    @Inject(method = "func_184651_r", at = @At("RETURN"))
    private void aqtweaks$addRangedAi(CallbackInfo ci) {
        this.tasks.addTask(1, aqtweaks$ranged());
    }

    @Inject(method = "setCombatTask", at = @At("HEAD"), cancellable = true)
    private void aqtweaks$rangedCombatTask(CallbackInfo ci) {
        aqtweaks$installRanged();
        ci.cancel();
    }

    @Inject(method = "setAI", at = @At("HEAD"), cancellable = true)
    private void aqtweaks$rangedSetAi(byte id, CallbackInfo ci) {
        aqtweaks$installRanged();
        ci.cancel();
    }

    @Inject(method = "func_70652_k", at = @At("HEAD"), cancellable = true)
    private void aqtweaks$noMelee(Entity entity, CallbackInfoReturnable<Boolean> cir) {
        cir.setReturnValue(false);
    }

    @Unique
    private void aqtweaks$installRanged() {
        EntityAIAttackRanged ranged = aqtweaks$ranged();
        if (this.aiMeleeAttack != null) {
            this.tasks.removeTask(this.aiMeleeAttack);
        }
        if (this.aiStrafe != null) {
            this.tasks.removeTask(this.aiStrafe);
        }
        this.tasks.removeTask(ranged);
        this.tasks.addTask(1, ranged);
    }

    @Unique
    private EntityAIAttackRanged aqtweaks$ranged() {
        if (this.aqtweaks$rangedMagic == null) {
            this.aqtweaks$rangedMagic = new EntityAIAttackRanged(this, 1.25D, 20, 60, 15.0F);
        }
        return this.aqtweaks$rangedMagic;
    }
}
