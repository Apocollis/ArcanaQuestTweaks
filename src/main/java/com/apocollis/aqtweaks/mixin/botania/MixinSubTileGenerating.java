package com.apocollis.aqtweaks.mixin.botania;

import com.apocollis.aqtweaks.ArcanaQuestTweaksConfig;
import com.apocollis.aqtweaks.reskillable.MagicSchoolPresence;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import vazkii.botania.api.subtile.SubTileGenerating;

@Mixin(value = SubTileGenerating.class, remap = false)
public abstract class MixinSubTileGenerating {

    @ModifyVariable(method = "addMana", at = @At("HEAD"), argsOnly = true)
    private int aqtweaks$grove(int mana) {
        if (mana <= 0) {
            return mana;
        }
        SubTileGenerating self = (SubTileGenerating) (Object) this;
        World world = self.getWorld();
        BlockPos pos = self.getPos();
        var perks = ArcanaQuestTweaksConfig.ReskillableModuleConfig.perks;
        var magic = ArcanaQuestTweaksConfig.ReskillableModuleConfig.magic;
        if (!MagicSchoolPresence.nearby(world, pos, magic.groveRange, "aqtweaks:druid", perks.druid.enable)) {
            return mana;
        }
        return Math.max(1, (int) Math.floor(mana * magic.groveMana));
    }
}
