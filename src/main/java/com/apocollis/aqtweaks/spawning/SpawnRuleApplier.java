package com.apocollis.aqtweaks.spawning;

import com.apocollis.aqtweaks.spawning.rules.GearTable;
import com.apocollis.aqtweaks.spawning.rules.ModifierPlan;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import net.minecraft.block.Block;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.attributes.IAttribute;
import net.minecraft.entity.ai.attributes.IAttributeInstance;
import net.minecraft.entity.monster.EntityEnderman;
import net.minecraft.entity.monster.EntityPigZombie;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.ResourceLocation;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Gives a mob what the modifiers and gear tables decided, and reads back what a mob has so the comparison with In
 * Control can tell what In Control did. Stats follow In Control: {@code base * multiply + add} on the base value, and
 * health also fills the mob to its new maximum.
 */
final class SpawnRuleApplier {
    private static final Logger LOGGER = LogManager.getLogger("AQTweaks-Spawning");
    private static final double ANGRY_RANGE = 50.0;

    static final Map<String, IAttribute> ATTRIBUTES = new LinkedHashMap<String, IAttribute>();
    static final Map<String, EntityEquipmentSlot> SLOTS = new LinkedHashMap<String, EntityEquipmentSlot>();
    private static final Set<String> WARNED = new HashSet<String>();

    static {
        ATTRIBUTES.put("health", SharedMonsterAttributes.MAX_HEALTH);
        ATTRIBUTES.put("damage", SharedMonsterAttributes.ATTACK_DAMAGE);
        ATTRIBUTES.put("speed", SharedMonsterAttributes.MOVEMENT_SPEED);
        ATTRIBUTES.put("armor", SharedMonsterAttributes.ARMOR);
        ATTRIBUTES.put("armortoughness", SharedMonsterAttributes.ARMOR_TOUGHNESS);
        ATTRIBUTES.put("knockbackresistance", SharedMonsterAttributes.KNOCKBACK_RESISTANCE);
        ATTRIBUTES.put("followrange", SharedMonsterAttributes.FOLLOW_RANGE);
        ATTRIBUTES.put("flyingspeed", SharedMonsterAttributes.FLYING_SPEED);
        SLOTS.put("hand", EntityEquipmentSlot.MAINHAND);
        SLOTS.put("offhand", EntityEquipmentSlot.OFFHAND);
        SLOTS.put("head", EntityEquipmentSlot.HEAD);
        SLOTS.put("chest", EntityEquipmentSlot.CHEST);
        SLOTS.put("legs", EntityEquipmentSlot.LEGS);
        SLOTS.put("feet", EntityEquipmentSlot.FEET);
    }

    private SpawnRuleApplier() {
    }

    /** What a mob had before any rule touched it. */
    static final class Snapshot {
        final Map<String, Double> stats = new HashMap<String, Double>();
        final Map<String, Integer> potions = new HashMap<String, Integer>();
        final Map<String, String> items = new HashMap<String, String>();
    }

    static Snapshot snapshot(EntityLiving mob) {
        Snapshot snapshot = new Snapshot();
        for (Map.Entry<String, IAttribute> entry : ATTRIBUTES.entrySet()) {
            IAttributeInstance attribute = mob.getEntityAttribute(entry.getValue());
            if (attribute != null) {
                snapshot.stats.put(entry.getKey(), attribute.getBaseValue());
            }
        }
        for (PotionEffect effect : mob.getActivePotionEffects()) {
            snapshot.potions.put(potionId(effect.getPotion()), effect.getAmplifier());
        }
        for (String slot : SLOTS.keySet()) {
            snapshot.items.put(slot, heldId(mob, slot));
        }
        return snapshot;
    }

    static String potionId(Potion potion) {
        ResourceLocation name = potion.getRegistryName();
        return name == null ? "?" : name.toString();
    }

    /** The registry id of what a mob holds or wears in a slot, "" when empty. An Enderman's hand is its carried block. */
    static String heldId(EntityLiving mob, String slot) {
        if ("hand".equals(slot) && mob instanceof EntityEnderman) {
            net.minecraft.block.state.IBlockState held = ((EntityEnderman) mob).getHeldBlockState();
            if (held == null) {
                return "";
            }
            ResourceLocation name = held.getBlock().getRegistryName();
            return name == null ? "" : name.toString();
        }
        ItemStack stack = mob.getItemStackFromSlot(SLOTS.get(slot));
        if (stack.isEmpty()) {
            return "";
        }
        ResourceLocation name = stack.getItem().getRegistryName();
        return name == null ? "" : name.toString();
    }

    /** The registry id an item spec (id or id/meta) names. */
    static String specId(String spec) {
        int slash = spec.indexOf('/');
        return slash < 0 ? spec : spec.substring(0, slash);
    }

    static void apply(EntityLiving mob, ModifierPlan plan, GearTable.Loadout loadout) {
        for (String stat : plan.stats()) {
            IAttributeInstance attribute = mob.getEntityAttribute(ATTRIBUTES.get(stat));
            if (attribute == null) {
                continue;
            }
            double changed = plan.expected(stat, attribute.getBaseValue());
            attribute.setBaseValue(changed);
            if ("health".equals(stat)) {
                mob.setHealth((float) changed);
            }
        }
        for (String potion : plan.potions()) {
            addPotion(mob, potion);
        }
        if (plan.angry()) {
            makeAngry(mob);
        }
        for (Map.Entry<String, String> slot : loadout.slots().entrySet()) {
            equip(mob, slot.getKey(), slot.getValue());
        }
    }

    private static void addPotion(EntityLiving mob, String spec) {
        String[] parts = spec.split(",");
        Potion potion = Potion.getPotionFromResourceLocation(parts[0]);
        if (potion == null) {
            warnOnce("potion " + parts[0], "unknown potion '" + parts[0] + "' in the modifiers file");
            return;
        }
        mob.addPotionEffect(new PotionEffect(potion, Integer.parseInt(parts[1]), Integer.parseInt(parts[2])));
    }

    private static void makeAngry(EntityLiving mob) {
        EntityPlayer target = mob.world.getClosestPlayerToEntity(mob, ANGRY_RANGE);
        if (target == null) {
            return;
        }
        if (mob instanceof EntityPigZombie) {
            mob.setRevengeTarget(target);
        }
        mob.setAttackTarget(target);
    }

    private static void equip(EntityLiving mob, String slot, String spec) {
        String id = specId(spec);
        Item item = Item.REGISTRY.getObject(new ResourceLocation(id));
        if (item == null || item == Items.AIR) {
            warnOnce("item " + id, "unknown item '" + id + "' in the gear file");
            return;
        }
        int meta = 0;
        int slash = spec.indexOf('/');
        if (slash >= 0) {
            try {
                meta = Integer.parseInt(spec.substring(slash + 1));
            } catch (NumberFormatException ignored) {
                warnOnce("meta " + spec, "item '" + spec + "' in the gear file has a damage value that is not a number");
            }
        }
        ItemStack stack = new ItemStack(item, 1, meta);
        if ("hand".equals(slot) && mob instanceof EntityEnderman) {
            if (item instanceof ItemBlock) {
                Block block = ((ItemBlock) item).getBlock();
                ((EntityEnderman) mob).setHeldBlockState(block.getStateFromMeta(item.getMetadata(meta)));
            }
            return;
        }
        mob.setItemStackToSlot(SLOTS.get(slot), stack);
    }

    private static void warnOnce(String key, String message) {
        if (WARNED.add(key)) {
            LOGGER.warn("[AQTweaks] {}", message);
        }
    }

    static boolean isLiving(Object entity) {
        return entity instanceof EntityLivingBase;
    }
}
