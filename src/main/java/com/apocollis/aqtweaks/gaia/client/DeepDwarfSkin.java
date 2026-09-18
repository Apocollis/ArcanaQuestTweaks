package com.apocollis.aqtweaks.gaia.client;

import com.apocollis.aqtweaks.ArcanaQuestTweaks;
import gaia.GaiaReference;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.TextureUtil;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;

@SideOnly(Side.CLIENT)
public final class DeepDwarfSkin {

    private static final ResourceLocation[] SOURCE = {
            new ResourceLocation(GaiaReference.MOD_ID, "textures/entity/dwarf01.png"),
            new ResourceLocation(GaiaReference.MOD_ID, "textures/entity/alternate/dwarf02.png"),
            new ResourceLocation(GaiaReference.MOD_ID, "textures/entity/alternate/dwarf03.png")
    };
    private static final ResourceLocation EYES_SOURCE =
            new ResourceLocation(GaiaReference.MOD_ID, "textures/entity/layer/eyes_dwarf03.png");

    private static final ResourceLocation[] BAKED = new ResourceLocation[3];
    private static ResourceLocation bakedEyes;
    private static boolean ready;

    private DeepDwarfSkin() {}

    public static ResourceLocation body(int textureType) {
        ensure();
        int i = textureType;
        if (i < 0 || i >= BAKED.length || BAKED[i] == null) {
            i = 0;
        }
        return BAKED[i] != null ? BAKED[i] : SOURCE[0];
    }

    public static ResourceLocation eyes() {
        ensure();
        return bakedEyes != null ? bakedEyes : EYES_SOURCE;
    }

    private static void ensure() {
        if (ready) {
            return;
        }
        ready = true;
        var tm = Minecraft.getMinecraft().getTextureManager();
        var rm = Minecraft.getMinecraft().getResourceManager();
        for (int i = 0; i < SOURCE.length; i++) {
            BufferedImage img = read(rm, SOURCE[i]);
            if (img == null) {
                continue;
            }
            bakeFlesh(img);
            bakeHairWhite(img);
            var loc = new ResourceLocation(ArcanaQuestTweaks.MODID, "dynamic/deep_dwarf_" + i);
            tm.loadTexture(loc, new DynamicTexture(img));
            BAKED[i] = loc;
        }
        BufferedImage eyes = read(rm, EYES_SOURCE);
        if (eyes != null) {
            bakeEyesRed(eyes);
            bakedEyes = new ResourceLocation(ArcanaQuestTweaks.MODID, "dynamic/deep_dwarf_eyes");
            tm.loadTexture(bakedEyes, new DynamicTexture(eyes));
        }
    }

    private static BufferedImage read(net.minecraft.client.resources.IResourceManager rm, ResourceLocation loc) {
        try (InputStream in = rm.getResource(loc).getInputStream()) {
            return TextureUtil.readBufferedImage(in);
        } catch (IOException e) {
            return null;
        }
    }

    /** Head (0,0 32×16) and neck (64,36 16×8) only; peach/tan pixels → blue-purple. */
    private static void bakeFlesh(BufferedImage img) {
        int w = img.getWidth();
        int h = img.getHeight();
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                if (!isFleshIsland(x, y)) {
                    continue;
                }
                int argb = img.getRGB(x, y);
                int a = (argb >>> 24) & 0xFF;
                if (a < 16) {
                    continue;
                }
                float[] hsb = Color.RGBtoHSB((argb >> 16) & 0xFF, (argb >> 8) & 0xFF, argb & 0xFF, null);
                if (!isFleshTone(hsb)) {
                    continue;
                }
                hsb[0] = 0.73f;
                hsb[1] = Math.min(1.0f, hsb[1] * 1.25f);
                hsb[2] = hsb[2] * 0.88f;
                int rgb = Color.HSBtoRGB(hsb[0], hsb[1], hsb[2]);
                img.setRGB(x, y, (a << 24) | (rgb & 0x00FFFFFF));
            }
        }
    }

    /**
     * After {@link #bakeFlesh}: leftover orange/brown hair on the purple-baked head island
     * and the beard overlay (64,18) → white / light gray. Gear UVs stay stock. Cool purple skin is skipped.
     */
    private static void bakeHairWhite(BufferedImage img) {
        int w = img.getWidth();
        int h = img.getHeight();
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                if (!isHeadIsland(x, y) && !isBeardIsland(x, y)) {
                    continue;
                }
                int argb = img.getRGB(x, y);
                int a = (argb >>> 24) & 0xFF;
                if (a < 16) {
                    continue;
                }
                float[] hsb = Color.RGBtoHSB((argb >> 16) & 0xFF, (argb >> 8) & 0xFF, argb & 0xFF, null);
                if (!isOrangeHair(hsb)) {
                    continue;
                }
                float shade = 0.58f + hsb[2] * 0.40f;
                hsb[0] = 0.0f;
                hsb[1] = 0.04f;
                hsb[2] = Math.min(1.0f, shade);
                int rgb = Color.HSBtoRGB(hsb[0], hsb[1], hsb[2]);
                img.setRGB(x, y, (a << 24) | (rgb & 0x00FFFFFF));
            }
        }
    }

    private static boolean isFleshIsland(int x, int y) {
        if (isHeadIsland(x, y)) {
            return true;
        }
        return x >= 64 && x < 80 && y >= 36 && y < 44;
    }

    private static boolean isHeadIsland(int x, int y) {
        return x < 32 && y < 16;
    }

    /** {@code headbeard} cube at tex 64,18 size 9³ → u 64–100, v 18–36. */
    private static boolean isBeardIsland(int x, int y) {
        return x >= 64 && x < 100 && y >= 18 && y < 36;
    }

    private static boolean isFleshTone(float[] hsb) {
        float hue = hsb[0];
        float sat = hsb[1];
        float val = hsb[2];
        boolean warm = hue <= 0.12f || hue >= 0.95f;
        return warm && sat >= 0.12f && sat <= 0.62f && val >= 0.48f;
    }

    /** Saturated warm pixels left after the purple flesh bake (stock hair). */
    private static boolean isOrangeHair(float[] hsb) {
        float hue = hsb[0];
        float sat = hsb[1];
        float val = hsb[2];
        boolean warm = hue <= 0.13f || hue >= 0.94f;
        return warm && sat >= 0.40f && val >= 0.22f && val <= 0.98f;
    }

    private static void bakeEyesRed(BufferedImage img) {
        int w = img.getWidth();
        int h = img.getHeight();
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int argb = img.getRGB(x, y);
                int a = (argb >>> 24) & 0xFF;
                if (a < 16) {
                    continue;
                }
                float[] hsb = Color.RGBtoHSB((argb >> 16) & 0xFF, (argb >> 8) & 0xFF, argb & 0xFF, null);
                if (hsb[2] < 0.2f || hsb[1] < 0.15f) {
                    continue;
                }
                hsb[0] = 0.0f;
                hsb[1] = Math.min(1.0f, Math.max(hsb[1], 0.85f));
                int rgb = Color.HSBtoRGB(hsb[0], hsb[1], hsb[2]);
                img.setRGB(x, y, (a << 24) | (rgb & 0x00FFFFFF));
            }
        }
    }
}
