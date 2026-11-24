package com.apocollis.aqtweaks.reskillable;

import com.apocollis.aqtweaks.ArcanaQuestTweaksConfig;
import com.apocollis.aqtweaks.potion.Stun;
import com.apocollis.aqtweaks.stamina.StaminaModule;
import com.apocollis.aqtweaks.thaumadditions.AegisBlockEvent;
import com.apocollis.aqtweaks.thaumadditions.AegisChannels;
import com.apocollis.aqtweaks.thaumadditions.ShieldState;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.entity.ai.attributes.IAttributeInstance;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.projectile.EntityArrow;
import net.minecraft.entity.projectile.EntityTippedArrow;
import net.minecraft.init.Items;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.ItemStack;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.event.entity.ProjectileImpactEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.world.BlockEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent.PlayerLoggedOutEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

/**
 * Event wiring for the starter class perks (plan 13): Riposte (Warrior), Steady Draw (Archer), Fleetfoot (Scout) and
 * Vein Rhythm (Miner). The rules live in {@link StarterPerkMath}; the stamina side of Steady Draw and Fleetfoot's free
 * sprint start is in {@code StaminaModule}. Registered with Reskillable in {@code CommonProxy}.
 */
public class StarterClassPerks {

    private static final String RIPOSTE = "aqtweaks:riposte";
    private static final String STEADY_DRAW = "aqtweaks:steady_draw";
    private static final String FLEETFOOT = "aqtweaks:fleetfoot";
    private static final String VEIN_RHYTHM = "aqtweaks:vein_rhythm";

    private static final String NBT_RIPOSTE_AT = "AqtweaksRiposteAt";
    /** The shield hold timer used by {@code StaminaModule.handleServerShieldBlocking}. */
    private static final String NBT_SHIELD_TICKS = "StaminaTweaksShieldTicks";
    /** Anti-repeat: one parry per this many ticks. */
    private static final int RIPOSTE_REPEAT_TICKS = 10;

    private static final UUID SNEAK_UUID = UUID.fromString("5f0e2a64-8c31-4b7d-a9d2-3c6e1b7f4a01");
    private static final UUID SPRINT_UUID = UUID.fromString("5f0e2a64-8c31-4b7d-a9d2-3c6e1b7f4a02");

    private static final Map<UUID, int[]> VEIN_STACKS = new HashMap<>();
    private static final Map<UUID, long[]> VEIN_LAST = new HashMap<>();

    private static ArcanaQuestTweaksConfig.ReskillableStarterCombatPerks cfg() {
        return ArcanaQuestTweaksConfig.ReskillableModuleConfig.starterCombat;
    }

    private static ArcanaQuestTweaksConfig.ReskillablePerks layouts() {
        return ArcanaQuestTweaksConfig.ReskillableModuleConfig.perks;
    }

    // ---- Riposte -------------------------------------------------------------------------------------------

    /** Vanilla shields and any item that blocks (Spartan shields, modded shields). Aegis takes the other path. */
    @SubscribeEvent(priority = EventPriority.HIGH)
    public void onAttack(LivingAttackEvent event) {
        if (!(event.getEntityLiving() instanceof EntityPlayer)) return;
        EntityPlayer player = (EntityPlayer) event.getEntityLiving();
        if (player.world.isRemote || event.getAmount() <= 0.0F) return;
        if (!PerkAccess.on(player, RIPOSTE, layouts().riposte.enable)) return;
        if (AegisChannels.isRaised(player)) return;
        DamageSource source = event.getSource();
        if (!shieldFaces(player, source)) return;
        int raised = ShieldState.ticksRaised(player);
        if (!StarterPerkMath.isParry(raised, cfg().riposteWindowTicks)) return;
        Entity immediate = source.getImmediateSource();
        parry(player, immediate instanceof EntityLivingBase ? (EntityLivingBase) immediate : null);
    }

    /** A raised Aegis absorbed a hit; same timing rule, the stamina part does not apply (Aegis is never billed). */
    @SubscribeEvent
    public void onAegisBlock(AegisBlockEvent event) {
        EntityPlayer player = event.getPlayer();
        if (player == null || player.world.isRemote) return;
        if (!PerkAccess.on(player, RIPOSTE, layouts().riposte.enable)) return;
        if (!StarterPerkMath.isParry(event.getTicksRaised(), cfg().riposteWindowTicks)) return;
        Entity attacker = event.getAttacker();
        parry(player, attacker instanceof EntityLivingBase ? (EntityLivingBase) attacker : null);
    }

    private static void parry(EntityPlayer player, EntityLivingBase attacker) {
        long now = player.world.getTotalWorldTime();
        long last = player.getEntityData().getLong(NBT_RIPOSTE_AT);
        if (last > 0L && now - last < RIPOSTE_REPEAT_TICKS) return;
        player.getEntityData().setLong(NBT_RIPOSTE_AT, now);

        if (cfg().riposteFreeBlock) {
            // Shield stamina is billed on a hold timer; a parry restarts it, so the next charge is a full interval away.
            player.getEntityData().setInteger(NBT_SHIELD_TICKS, 0);
        }
        player.world.playSound(null, player.posX, player.posY, player.posZ, SoundEvents.ITEM_SHIELD_BLOCK,
                SoundCategory.PLAYERS, 1.0F, 1.5F);
        if (attacker != null && attacker != player && attacker.isEntityAlive()) {
            int ticks = cfg().riposteStaggerTicks;
            if (ticks > 0) {
                Stun.apply(attacker, ticks);
            }
            if (player.world instanceof WorldServer) {
                ((WorldServer) player.world).spawnParticle(EnumParticleTypes.CRIT_MAGIC,
                        attacker.posX, attacker.posY + attacker.height * 0.6D, attacker.posZ,
                        10, 0.3D, 0.3D, 0.3D, 0.1D);
            }
        }
    }

    /**
     * Same test vanilla's protected {@code canBlockDamageSource} applies: not unblockable, a blocking item in use, and
     * the damage comes from in front of the player.
     */
    static boolean shieldFaces(EntityPlayer player, DamageSource source) {
        if (source.isUnblockable() || !player.isActiveItemStackBlocking()) return false;
        Vec3d at = source.getDamageLocation();
        if (at == null) return false;
        Vec3d look = player.getLook(1.0F);
        Vec3d toSource = at.subtractReverse(new Vec3d(player.posX, player.posY, player.posZ)).normalize();
        toSource = new Vec3d(toSource.x, 0.0D, toSource.z);
        return toSource.dotProduct(look) < 0.0D;
    }

    // ---- Steady Draw ---------------------------------------------------------------------------------------

    /** 75% (config) of the arrows that hit an entity come back at its feet. */
    @SubscribeEvent
    public void onArrowImpact(ProjectileImpactEvent.Arrow event) {
        EntityArrow arrow = event.getArrow();
        if (arrow.world.isRemote) return;
        RayTraceResult ray = event.getRayTraceResult();
        if (ray == null || ray.typeOfHit != RayTraceResult.Type.ENTITY || !(ray.entityHit instanceof EntityLivingBase)) {
            return;
        }
        if (!(arrow.shootingEntity instanceof EntityPlayer)) return;
        EntityPlayer shooter = (EntityPlayer) arrow.shootingEntity;
        if (ray.entityHit == shooter) return;
        if (!PerkAccess.on(shooter, STEADY_DRAW, layouts().steadyDraw.enable)) return;
        boolean pickup = arrow.pickupStatus == EntityArrow.PickupStatus.ALLOWED;
        boolean vanilla = arrow.getClass() == EntityTippedArrow.class;
        if (!StarterPerkMath.returnsArrow(pickup, vanilla, arrow.world.rand.nextDouble(), cfg().steadyDrawReturnChance)) {
            return;
        }
        Entity target = ray.entityHit;
        EntityItem drop = new EntityItem(arrow.world, target.posX, target.posY, target.posZ, new ItemStack(Items.ARROW));
        drop.setDefaultPickupDelay();
        arrow.world.spawnEntity(drop);
    }

    // ---- Fleetfoot -----------------------------------------------------------------------------------------

    /** Sneak speed restore and the light-weapon sprint bonus, as movement attribute modifiers. */
    @SubscribeEvent
    public void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.player.world.isRemote) return;
        EntityPlayer player = event.player;
        if (player instanceof FakePlayer || player.ticksExisted % 5 != 0) return;
        IAttributeInstance speed = player.getEntityAttribute(SharedMonsterAttributes.MOVEMENT_SPEED);
        if (speed == null) return;
        boolean perk = PerkAccess.on(player, FLEETFOOT, layouts().fleetfoot.enable);
        boolean sneak = perk && player.isSneaking();
        boolean sprint = perk && player.isSprinting()
                && StaminaModule.getWeaponType(player.getHeldItemMainhand()) == StaminaModule.WeaponType.LIGHT;
        sync(speed, SNEAK_UUID, "aqtweaks.fleetfoot.sneak", sneak, StarterPerkMath.sneakModifierAmount(cfg().fleetfootSneakRestore));
        sync(speed, SPRINT_UUID, "aqtweaks.fleetfoot.sprint", sprint, cfg().fleetfootSprintBonus);
    }

    private static void sync(IAttributeInstance attribute, UUID id, String name, boolean want, double amount) {
        AttributeModifier existing = attribute.getModifier(id);
        if (want && amount != 0.0D) {
            if (existing != null && existing.getAmount() == amount) return;
            if (existing != null) attribute.removeModifier(id);
            attribute.applyModifier(new AttributeModifier(id, name, amount, 2).setSaved(false));
        } else if (existing != null) {
            attribute.removeModifier(id);
        }
    }

    // ---- Vein Rhythm ---------------------------------------------------------------------------------------

    /** A pickaxe-class break of stone or ore starts or extends the chain. */
    @SubscribeEvent
    public void onBlockBroken(BlockEvent.BreakEvent event) {
        EntityPlayer player = event.getPlayer();
        if (player == null || player.world.isRemote || player instanceof FakePlayer) return;
        if (!PerkAccess.on(player, VEIN_RHYTHM, layouts().veinRhythm.enable)) return;
        if (!qualifies(player.getHeldItemMainhand(), event.getState())) return;
        long now = player.world.getTotalWorldTime();
        int[] stacks = VEIN_STACKS.computeIfAbsent(player.getUniqueID(), k -> new int[1]);
        long[] last = VEIN_LAST.computeIfAbsent(player.getUniqueID(), k -> new long[1]);
        stacks[0] = StarterPerkMath.stacksAfterBreak(stacks[0], last[0], now, cfg().veinRhythmWindowTicks,
                cfg().veinRhythmMaxStacks);
        last[0] = now;
    }

    @SubscribeEvent
    public void onBreakSpeed(PlayerEvent.BreakSpeed event) {
        EntityPlayer player = event.getEntityPlayer();
        if (player == null || player.world.isRemote || player instanceof FakePlayer) return;
        int[] stacks = VEIN_STACKS.get(player.getUniqueID());
        if (stacks == null) return;
        if (!PerkAccess.on(player, VEIN_RHYTHM, layouts().veinRhythm.enable)) return;
        if (!qualifies(player.getHeldItemMainhand(), event.getState())) return;
        long[] last = VEIN_LAST.get(player.getUniqueID());
        int active = StarterPerkMath.activeStacks(stacks[0], last[0], player.world.getTotalWorldTime(),
                cfg().veinRhythmWindowTicks);
        if (active > 0) {
            event.setNewSpeed(event.getNewSpeed() * StarterPerkMath.breakSpeedMultiplier(active, cfg().veinRhythmBonusPerStack));
        }
    }

    /** Pickaxe-class tool on a block whose harvest tool is the pickaxe (stone and ores). */
    private static boolean qualifies(ItemStack held, IBlockState state) {
        if (held.isEmpty() || state == null) return false;
        if (!held.getItem().getToolClasses(held).contains("pickaxe")) return false;
        return "pickaxe".equals(state.getBlock().getHarvestTool(state));
    }

    @SubscribeEvent
    public void onLogout(PlayerLoggedOutEvent event) {
        UUID id = event.player.getUniqueID();
        VEIN_STACKS.remove(id);
        VEIN_LAST.remove(id);
    }
}
