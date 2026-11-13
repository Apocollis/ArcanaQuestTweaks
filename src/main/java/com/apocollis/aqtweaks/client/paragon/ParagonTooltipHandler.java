package com.apocollis.aqtweaks.client.paragon;

import com.apocollis.aqtweaks.paragon.ParagonRegistry;
import com.apocollis.aqtweaks.paragon.ParagonTrait;
import com.apocollis.aqtweaks.paragon.TartariteKind;

import net.minecraft.client.resources.I18n;
import net.minecraft.item.ItemStack;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/** The tooltip lines of an absorbed trait: a header, one line on what it does, and the trait's own lines. */
@SideOnly(Side.CLIENT)
public class ParagonTooltipHandler {
    @SubscribeEvent
    public void onTooltip(ItemTooltipEvent event) {
        ItemStack stack = event.getItemStack();
        TartariteKind kind = ParagonRegistry.kindOf(stack);
        if (kind == null || !stack.hasTagCompound()) return;
        String id = ParagonRegistry.storedId(stack);
        if (id == null) return;
        ParagonTrait trait = ParagonRegistry.get(id);
        if (trait == null) return;
        event.getToolTip().add(I18n.format("aqtweaks.paragon.header", I18n.format("aqtweaks.paragon.trait." + id)));
        event.getToolTip().add("§7" + I18n.format("aqtweaks.paragon.desc." + id));
        if (kind.isArmor()) {
            String set = I18n.format("aqtweaks.paragon.set." + id);
            if (!set.startsWith("aqtweaks.paragon.set.")) event.getToolTip().add("§7" + set);
        }
        net.minecraft.world.World world = net.minecraft.client.Minecraft.getMinecraft().world;
        if (world != null) trait.addTooltip(stack, kind, world, event.getToolTip(), event.getFlags());
    }
}
