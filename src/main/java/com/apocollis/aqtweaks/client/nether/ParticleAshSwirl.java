package com.apocollis.aqtweaks.client.nether;

import com.apocollis.aqtweaks.nether.AshSwirlShape;
import com.apocollis.aqtweaks.nether.AshSwirlShape.Kind;
import com.apocollis.aqtweaks.nether.ClientAshClouds;

import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.Particle;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * One mote of a Nether ash cloud. A cloud particle keeps its offset from the cloud centre and is re-placed at
 * {@code centre + rotated offset} every tick, so the cloud's drift carries every mote with it and the mass turns
 * as one rounded body (spin about the vertical axis, core rising and rim sinking). A warning wisp ({@code cloudId < 0})
 * simply drifts downwind. Spawned straight into the particle manager (never distance culled); counted against the
 * ash particle caps; expires on the first solid block, outside the cloud ellipsoid, or when its cloud is gone.
 */
@SideOnly(Side.CLIENT)
public class ParticleAshSwirl extends Particle {

    public static final AtomicInteger ACTIVE_COUNT = new AtomicInteger(0);
    private static final Map<Integer, AtomicInteger> PER_CLOUD = new ConcurrentHashMap<>();

    public static int active() {
        return ACTIVE_COUNT.get();
    }

    public static int activeFor(int cloudId) {
        AtomicInteger n = PER_CLOUD.get(cloudId);
        return n == null ? 0 : n.get();
    }

    /** The particle manager clears particles without calling {@code setExpired}, so the counters are reset by hand. */
    public static void resetCounts() {
        ACTIVE_COUNT.set(0);
        PER_CLOUD.clear();
    }

    private final Kind kind;
    private final int cloudId;
    private double ox;
    private double oy;
    private double oz;
    private final double spin;
    private final float baseAlpha;
    private final double driftX;
    private final double driftZ;
    private boolean counted = true;
    private final BlockPos.MutableBlockPos posCheck = new BlockPos.MutableBlockPos();

    /** A cloud mote at the given offset from the cloud centre. */
    public ParticleAshSwirl(World world, ClientAshClouds.Cloud cloud, double ox, double oy, double oz, Kind kind,
                            double swirlSpeed) {
        super(world, cloud.x + ox, cloud.y + oy, cloud.z + oz, 0.0D, 0.0D, 0.0D);
        this.cloudId = cloud.id;
        this.ox = ox;
        this.oy = oy;
        this.oz = oz;
        this.kind = kind;
        this.spin = swirlSpeed * (this.rand.nextFloat() < 0.5F ? 0.85D : 1.15D);
        this.driftX = 0.0D;
        this.driftZ = 0.0D;
        this.baseAlpha = AshSwirlShape.alpha(kind, this.rand);
        init();
        counts(cloudId, 1);
    }

    /** A warning wisp drifting with the given wind. */
    public ParticleAshSwirl(World world, double x, double y, double z, Kind kind, double windX, double windZ) {
        super(world, x, y, z, 0.0D, 0.0D, 0.0D);
        this.cloudId = -1;
        this.kind = kind;
        this.spin = 0.0D;
        this.driftX = windX;
        this.driftZ = windZ;
        this.baseAlpha = AshSwirlShape.alpha(kind, this.rand) * 0.8F;
        init();
        counts(-1, 1);
    }

    private void init() {
        ACTIVE_COUNT.incrementAndGet();
        this.canCollide = false;
        this.particleGravity = 0.0F;
        int rgb = AshSwirlShape.colour(kind, this.rand);
        this.particleRed = ((rgb >> 16) & 0xFF) / 255.0F;
        this.particleGreen = ((rgb >> 8) & 0xFF) / 255.0F;
        this.particleBlue = (rgb & 0xFF) / 255.0F;
        this.particleAlpha = 0.0F;
        this.particleScale = AshSwirlShape.scale(kind, this.rand);
        this.particleMaxAge = AshSwirlShape.lifetime(kind, this.rand);
        this.particleAge = 0;
        // Soft puff for the sheets, a grain for flecks and cinders.
        this.setParticleTextureIndex(kind == Kind.SHEET ? 7 : 0);
    }

    private static void counts(int cloudId, int delta) {
        PER_CLOUD.computeIfAbsent(cloudId, k -> new AtomicInteger()).addAndGet(delta);
    }

    /** Cinders glow; the rest are lit by the world. */
    @Override
    public int getBrightnessForRender(float partialTicks) {
        return kind == Kind.EMBER ? 15728880 : super.getBrightnessForRender(partialTicks);
    }

    /** Only the big soft sheets near the camera ignore depth, so ash is not drawn through walls and terrain. */
    @Override
    public boolean shouldDisableDepth() {
        if (kind != Kind.SHEET) return false;
        Entity view = Minecraft.getMinecraft().getRenderViewEntity();
        if (view == null) return false;
        double dx = view.posX - this.posX;
        double dy = view.posY - this.posY;
        double dz = view.posZ - this.posZ;
        return dx * dx + dy * dy + dz * dz < 144.0D;
    }

    @Override
    public void setExpired() {
        super.setExpired();
        if (this.counted) {
            this.counted = false;
            ACTIVE_COUNT.decrementAndGet();
            counts(cloudId, -1);
        }
    }

    @Override
    public void onUpdate() {
        this.prevPosX = this.posX;
        this.prevPosY = this.posY;
        this.prevPosZ = this.posZ;

        if (this.particleAge++ >= this.particleMaxAge) {
            this.setExpired();
            return;
        }

        float fade = 1.0F;
        if (cloudId >= 0) {
            ClientAshClouds.Cloud cloud = ClientAshClouds.get(cloudId);
            if (cloud == null) {
                this.setExpired();
                return;
            }
            double d = AshSwirlShape.radius(ox, oy, oz, cloud.reachH, cloud.reachV);
            if (d > 1.05D) {
                this.setExpired();
                return;
            }
            double a = AshSwirlShape.spinRate(d, spin);
            double cos = Math.cos(a);
            double sin = Math.sin(a);
            double nx = ox * cos - oz * sin;
            double nz = ox * sin + oz * cos;
            ox = nx;
            oz = nz;
            oy += AshSwirlShape.roll(d);
            this.posX = cloud.x + ox;
            this.posY = cloud.y + oy;
            this.posZ = cloud.z + oz;
            fade = AshSwirlShape.edgeFade(d);
            // The coarse occupancy can change as the fill is redone: leave quickly if the cell is no longer open.
            if ((this.particleAge & 3) == 0 && !cloud.contains(this.posX, this.posY, this.posZ)) {
                this.setExpired();
                return;
            }
        } else {
            this.posX += driftX;
            this.posY += 0.005D;
            this.posZ += driftZ;
        }

        float life = (float) this.particleAge / (float) this.particleMaxAge;
        float envelope;
        if (life < 0.15F) {
            envelope = life / 0.15F;
        } else if (life > 0.75F) {
            envelope = (1.0F - life) / 0.25F;
        } else {
            envelope = 1.0F;
        }
        this.particleAlpha = this.baseAlpha * envelope * fade;

        if ((this.particleAge & 1) == 0 && this.world.isBlockLoaded(this.posCheck.setPos(this.posX, this.posY, this.posZ))) {
            IBlockState state = this.world.getBlockState(this.posCheck);
            if (state.getMaterial().isSolid()) {
                this.setExpired();
            }
        }
    }
}
