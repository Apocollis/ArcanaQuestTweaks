package com.apocollis.aqtweaks.mixin.bewitchment;

import com.apocollis.aqtweaks.ArcanaQuestTweaksConfig;
import com.apocollis.aqtweaks.rtg.StructureVillageOverlap;
import com.apocollis.aqtweaks.rtg.VillageDebug;
import com.bewitchment.common.world.gen.structures.WorldGenCambionHomeMedium;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraft.world.gen.structure.template.Template;
import net.minecraft.world.gen.structure.template.TemplateManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Random;

@Mixin(value = WorldGenCambionHomeMedium.class)
public abstract class MixinWorldGenCambionHomeMedium {

    @ModifyArg(
            method = "generate",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/gen/structure/template/Template;addBlocksToWorld(Lnet/minecraft/world/World;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/world/gen/structure/template/PlacementSettings;)V"
            ),
            index = 1
    )
    private BlockPos aqtweaks$raiseCambion(BlockPos pos) {
        return pos == null ? null : pos.up();
    }

    @Inject(method = "generate", at = @At("HEAD"), cancellable = true)
    private void aqtweaks$skipVillageOverlap(World world, Random rand, BlockPos position,
                                            CallbackInfoReturnable<Boolean> cir) {
        if (!ArcanaQuestTweaksConfig.RtgModuleConfig.surface.enableCambionHouseSettle) return;
        if (!StructureVillageOverlap.enabled() || world == null || position == null) return;
        if (!(world instanceof WorldServer) || world.getMinecraftServer() == null) return;
        TemplateManager manager = ((WorldServer) world).getStructureTemplateManager();
        Template template = manager.getTemplate(world.getMinecraftServer(),
                new net.minecraft.util.ResourceLocation("bewitchment", "cambionmedium1"));
        if (template == null) return;
        BlockPos placed = position.up();
        if (StructureVillageOverlap.overlapsVillage(world, placed, template.getSize())) {
            VillageDebug.log("cambion medium skip village overlap at=%d,%d,%d",
                    placed.getX(), placed.getY(), placed.getZ());
            cir.setReturnValue(Boolean.FALSE);
        }
    }
}
