package net.tysontheember.orbitalrailgun.client.fx;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.shaders.Uniform;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.EffectInstance;
import net.minecraft.client.renderer.PostChain;
import net.minecraft.client.renderer.PostPass;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.tysontheember.orbitalrailgun.ForgeOrbitalRailgunMod;
import net.tysontheember.orbitalrailgun.client.OrbitalShaderUniforms;
import net.tysontheember.orbitalrailgun.client.railgun.RailgunState;
import net.tysontheember.orbitalrailgun.client.util.ClientRaycast;
import net.tysontheember.orbitalrailgun.config.OrbitalConfig;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.Collections;
import java.util.List;

/** Handles the strike marker post-processing chain (highlight while charging). */
public final class StrikePostChains {
    private static final ResourceLocation STRIKE_MARKER_CHAIN_ID = ForgeOrbitalRailgunMod.id("shaders/post/strike_marker.json");
    private static final Field PASSES_FIELD = findPassesField();

    private static PostChain strikeMarkerChain;
    private static boolean markerReady;
    private static int markerWidth = -1;
    private static int markerHeight = -1;

    private StrikePostChains() {}

    public static void reload(ResourceManager resourceManager) {
        Minecraft minecraft = Minecraft.getInstance();
        close();

        if (minecraft.getMainRenderTarget() == null) {
            markerReady = false;
            return;
        }

        try {
            strikeMarkerChain = new PostChain(
                    minecraft.getTextureManager(),
                    resourceManager,
                    minecraft.getMainRenderTarget(),
                    STRIKE_MARKER_CHAIN_ID
            );
            markerReady = true;
            markerWidth = -1;
            markerHeight = -1;
            resize(minecraft);
        } catch (IOException exception) {
            ForgeOrbitalRailgunMod.LOGGER.error("Failed to load orbital railgun strike marker post chain", exception);
            close();
        }
    }

    public static void resize(Minecraft minecraft) {
        if (!markerReady || strikeMarkerChain == null) {
            return;
        }

        RenderTarget target = minecraft.getMainRenderTarget();
        if (target == null) {
            return;
        }

        int width = target.width;
        int height = target.height;
        if (width == markerWidth && height == markerHeight) {
            return;
        }

        strikeMarkerChain.resize(width, height);
        markerWidth = width;
        markerHeight = height;
    }

    public static void renderMarker(Matrix4f projectionMatrix,
                                    Matrix4f inverseProjectionMatrix,
                                    Matrix4f modelViewMatrix,
                                    Vector3f cameraPosition,
                                    float partialTick,
                                    boolean charging,
                                    RailgunState state) {
        if (!markerReady || strikeMarkerChain == null) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            return;
        }

        resize(minecraft);

        List<PostPass> passes = getPasses();
        if (passes.isEmpty()) {
            return;
        }

        float strikeRadius = state.getTransientVisualStrikeRadius()
                .orElse((float) (OrbitalConfig.DESTRUCTION_DIAMETER.get() * 0.5D));

        float timeSeconds = ((minecraft.level.getGameTime() + partialTick) / 20.0F);

        // Always update StrikeActive uniform so the shader can immediately hide when charging stops.
        setStrikeActive(passes, charging ? 1.0F : 0.0F);
        if (!charging) {
            return;
        }

        var minecraftInstance = Minecraft.getInstance();
        var blockPos = ClientRaycast.pickBlockPos(minecraftInstance, partialTick);
        Vector3f blockCenter = new Vector3f(blockPos.getX() + 0.5F, blockPos.getY() + 0.5F, blockPos.getZ() + 0.5F);

        for (PostPass pass : passes) {
            EffectInstance shader = pass.getEffect();
            if (shader == null) {
                continue;
            }

            setMatrix(shader, "ProjMat", projectionMatrix);
            setMatrix(shader, "InverseTransformMatrix", inverseProjectionMatrix);
            setMatrix(shader, "ModelViewMat", modelViewMatrix);
            setVec3(shader, "CameraPosition", cameraPosition);
            setVec3(shader, "BlockPosition", blockCenter);
            setFloat(shader, "iTime", timeSeconds);
            setFloat(shader, "StrikeRadius", strikeRadius);
        }

        OrbitalShaderUniforms.applyColorUniforms(passes);
        strikeMarkerChain.process(partialTick);
        minecraft.getMainRenderTarget().bindWrite(false);
    }

    public static void close() {
        if (strikeMarkerChain != null) {
            try {
                strikeMarkerChain.close();
            } catch (Exception ignored) {
            }
            strikeMarkerChain = null;
        }
        markerReady = false;
        markerWidth = -1;
        markerHeight = -1;
        ClientRaycast.reset();
    }

    private static void setStrikeActive(List<PostPass> passes, float active) {
        for (PostPass pass : passes) {
            EffectInstance shader = pass.getEffect();
            if (shader == null) {
                continue;
            }
            Uniform uniform = shader.getUniform("StrikeActive");
            if (uniform != null) {
                uniform.set(active);
            }
        }
    }

    private static void setMatrix(EffectInstance effect, String name, Matrix4f matrix) {
        Uniform uniform = effect.getUniform(name);
        if (uniform != null) {
            uniform.set(matrix);
        }
    }

    private static void setVec3(EffectInstance effect, String name, Vector3f vec) {
        Uniform uniform = effect.getUniform(name);
        if (uniform != null) {
            uniform.set(vec.x(), vec.y(), vec.z());
        }
    }

    private static void setFloat(EffectInstance effect, String name, float value) {
        Uniform uniform = effect.getUniform(name);
        if (uniform != null) {
            uniform.set(value);
        }
    }

    private static List<PostPass> getPasses() {
        if (strikeMarkerChain == null || PASSES_FIELD == null) {
            return Collections.emptyList();
        }
        try {
            Object value = PASSES_FIELD.get(strikeMarkerChain);
            if (value instanceof List<?> list) {
                @SuppressWarnings("unchecked")
                List<PostPass> passes = (List<PostPass>) list;
                return passes;
            }
        } catch (IllegalAccessException exception) {
            ForgeOrbitalRailgunMod.LOGGER.error("Failed to access strike marker PostChain passes", exception);
        }
        return Collections.emptyList();
    }

    private static Field findPassesField() {
        try {
            Field field = PostChain.class.getDeclaredField("passes");
            field.setAccessible(true);
            return field;
        } catch (NoSuchFieldException ignored) {
            try {
                Field field = PostChain.class.getDeclaredField("f_110009_");
                field.setAccessible(true);
                return field;
            } catch (NoSuchFieldException exception) {
                ForgeOrbitalRailgunMod.LOGGER.error("Unable to find PostChain passes field for strike marker", exception);
                return null;
            }
        }
    }
}
