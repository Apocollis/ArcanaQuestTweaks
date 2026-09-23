package com.apocollis.aqtweaks.reskillable;

import com.apocollis.aqtweaks.ArcanaQuestTweaksConfig;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import vazkii.botania.api.mana.ManaItemHandler;

public final class MagicSchoolBotania {

    private MagicSchoolBotania() {}

    public static void applyManaVeil(EntityPlayer player, LivingHurtEvent event) {
        var magic = ArcanaQuestTweaksConfig.ReskillableModuleConfig.magic;
        float amount = event.getAmount();
        int wantHp = (int) Math.floor(amount * magic.manaVeilAbsorb);
        if (wantHp < 1) {
            return;
        }
        int per = magic.manaVeilManaPerHp;
        int available = ManaItemHandler.requestMana(ItemStack.EMPTY, player, wantHp * per, false);
        int absorbHp = Math.min(wantHp, available / per);
        if (absorbHp < 1) {
            return;
        }
        if (!ManaItemHandler.requestManaExact(ItemStack.EMPTY, player, absorbHp * per, true)) {
            return;
        }
        event.setAmount(Math.max(0f, amount - absorbHp));
    }
}
