package com.apocollis.aqtweaks.mixin;

import net.minecraft.block.BlockCrops;
import net.minecraft.item.Item;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(BlockCrops.class)
public interface MixinBlockCropsSeed {

    @Invoker("getSeed")
    Item aqtweaks$getSeed();
}
