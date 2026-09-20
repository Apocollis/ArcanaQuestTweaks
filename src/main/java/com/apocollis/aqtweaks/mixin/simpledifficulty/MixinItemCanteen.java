package com.apocollis.aqtweaks.mixin.simpledifficulty;

import com.apocollis.aqtweaks.ArcanaQuestTweaksConfig;
import com.apocollis.aqtweaks.util.Reflect;
import com.charles445.simpledifficulty.api.thirst.ThirstEnum;
import com.charles445.simpledifficulty.item.ItemCanteen;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraftforge.common.util.FakePlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(value = ItemCanteen.class, remap = false)
public abstract class MixinItemCanteen {

    @ModifyVariable(method = "tryAddDose", at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private ThirstEnum aqtweaks$purifyFill(ThirstEnum type, ItemStack stack, ThirstEnum ignored) {
        if (type != ThirstEnum.NORMAL && type != ThirstEnum.RAIN) return type;
        EntityPlayer player = currentPlayer();
        if (player == null || player instanceof FakePlayer) return type;
        if (!ArcanaQuestTweaksConfig.ReskillableModuleConfig.perks.waterCollector.enable) return type;
        if (!Reflect.hasUnlockable(player, "aqtweaks:water_collector")) return type;
        return ThirstEnum.PURIFIED;
    }

    private static EntityPlayer currentPlayer() {
        return SimpleDifficultyFillContext.PLAYER.get();
    }
}
