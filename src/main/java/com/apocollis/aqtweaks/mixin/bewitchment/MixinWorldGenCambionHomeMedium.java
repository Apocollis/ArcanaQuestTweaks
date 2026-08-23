package com.apocollis.aqtweaks.mixin.bewitchment;

import com.apocollis.aqtweaks.ArcanaQuestTweaksConfig;
import com.apocollis.aqtweaks.rtg.StructureLandSettle;
import com.apocollis.aqtweaks.rtg.StructureVillageOverlap;
import com.apocollis.aqtweaks.rtg.VillageDebug;
import com.bewitchment.common.world.gen.structures.WorldGenCambionHomeMedium;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraft.world.gen.structure.template.PlacementSettings;
import net.minecraft.world.gen.structure.template.Template;
import net.minecraft.world.gen.structure.template.TemplateManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Random;

@Mixin(value = WorldGenCambionHomeMedium.class, remap = false)
public abstract class MixinWorldGenCambionHomeMedium {

    @Unique
    private BlockPos aqtweaks$pastePos;

    @Inject(method = "func_180709_b", at = @At("HEAD"))
    private void aqtweaks$resetPaste(World world, Random rand, BlockPos position,
                                    CallbackInfoReturnable<Boolean> cir) {
        aqtweaks$pastePos = null;
    }

    @Redirect(
            method = "func_180709_b",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/gen/structure/template/Template;func_186253_b(Lnet/minecraft/world/World;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/world/gen/structure/template/PlacementSettings;)V"
            )
    )
    private void aqtweaks$pasteCambion(Template template, World world, BlockPos pos,
                                      PlacementSettings settings) {
        BlockPos placed = pos == null ? null : pos.up();
        aqtweaks$pastePos = placed;
        StructureLandSettle.addBlocksSkippingAir(template, world, placed, settings);
    }

    @Inject(method = "func_180709_b", at = @At("HEAD"), cancellable = true)
    private void aqtweaks$skipVillageOverlap(World world, Random rand, BlockPos position,
                                            CallbackInfoReturnable<Boolean> cir) {
        if (!ArcanaQuestTweaksConfig.RtgModuleConfig.surface.enableCambionHouseSettle) return;
        if (!StructureVillageOverlap.enabled() || world == null || position == null) return;
        if (!(world instanceof WorldServer) || world.getMinecraftServer() == null) return;
        TemplateManager manager = ((WorldServer) world).getStructureTemplateManager();
        Template template = manager.getTemplate(world.getMinecraftServer(),
                new ResourceLocation("bewitchment", "cambionmedium1"));
        if (template == null) return;
        BlockPos placed = position.up();
        if (StructureVillageOverlap.overlapsVillage(world, placed, template.getSize())) {
            VillageDebug.log("cambion medium skip village overlap at=%d,%d,%d",
                    placed.getX(), placed.getY(), placed.getZ());
            cir.setReturnValue(Boolean.FALSE);
        }
    }

    @Inject(method = "func_180709_b", at = @At("RETURN"))
    private void aqtweaks$plateCambion(World world, Random rand, BlockPos position,
                                      CallbackInfoReturnable<Boolean> cir) {
        if (!Boolean.TRUE.equals(cir.getReturnValue())) return;
        if (!ArcanaQuestTweaksConfig.RtgModuleConfig.surface.enableCambionHouseSettle) return;
        if (!StructureLandSettle.enabled() || world == null || position == null) return;
        if (!(world instanceof WorldServer) || world.getMinecraftServer() == null) return;
        TemplateManager manager = ((WorldServer) world).getStructureTemplateManager();
        Template template = manager.getTemplate(world.getMinecraftServer(),
                new ResourceLocation("bewitchment", "cambionmedium1"));
        if (template == null || template.getSize() == null) return;
        BlockPos placed = aqtweaks$pastePos != null ? aqtweaks$pastePos : position.up();
        BlockPos size = template.getSize();
        int pad = Math.max(0, ArcanaQuestTweaksConfig.RtgModuleConfig.surface.cambionHousePad);
        int falloff = Math.max(0, ArcanaQuestTweaksConfig.RtgModuleConfig.surface.cambionHouseFalloff);
        int solidTopY = position.getY();
        StructureLandSettle.fillHolesPadded(world,
                placed.getX(), placed.getX() + Math.max(0, size.getX() - 1),
                placed.getZ(), placed.getZ() + Math.max(0, size.getZ() - 1),
                solidTopY, pad, falloff, true);
        VillageDebug.log("cambion medium plate at=%d,%d,%d pad=%d falloff=%d solidTopY=%d pasteY=%d",
                placed.getX(), placed.getY(), placed.getZ(), pad, falloff, solidTopY, placed.getY());
    }
}
