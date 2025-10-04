package com.mishkis.orbitalrailgun.client.railgun;

import com.mishkis.orbitalrailgun.ForgeOrbitalRailgunMod;
import com.mojang.blaze3d.shaders.Uniform;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.EffectInstance;
import net.minecraft.client.renderer.PostChain;
import net.minecraft.client.renderer.PostPass;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.fml.util.ObfuscationReflectionHelper;
import org.joml.Matrix4f;
import org.slf4j.Logger;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.Collections;
import java.util.List;

public class PostChainManager implements ResourceManagerReloadListener {
    private static final Logger LOGGER = ForgeOrbitalRailgunMod.LOGGER;
    private static final ResourceLocation WORLD_CHAIN = ForgeOrbitalRailgunMod.id("shaders/post/orbital_railgun.json");
    private static final ResourceLocation GUI_CHAIN = ForgeOrbitalRailgunMod.id("shaders/post/orbital_railgun_gui.json");
    private static final Field PASSES_FIELD = ObfuscationReflectionHelper.findField(PostChain.class, "passes");

    private final Minecraft minecraft = Minecraft.getInstance();
    private PostChain worldChain;
    private PostChain guiChain;
    private int width = -1;
    private int height = -1;

    private Matrix4f cachedModelViewMatrix;
    private Matrix4f cachedProjectionMatrix;
    private Vec3 cachedCameraPosition;
    private Float cachedPartialTick;

    private static final PostChainManager INSTANCE = new PostChainManager();

    static {
        PASSES_FIELD.setAccessible(true);
    }

    private PostChainManager() {}

    public static PostChainManager getInstance() {
        return INSTANCE;
    }

    @Override
    public void onResourceManagerReload(ResourceManager resourceManager) {
        closeChains();
        TextureManager textureManager = minecraft.getTextureManager();
        if (minecraft.getMainRenderTarget() == null) {
            return;
        }

        try {
            worldChain = new PostChain(textureManager, resourceManager, minecraft.getMainRenderTarget(), WORLD_CHAIN);
            guiChain = new PostChain(textureManager, resourceManager, minecraft.getMainRenderTarget(), GUI_CHAIN);
            resizeChains();
        } catch (IOException exception) {
            LOGGER.error("Failed to load orbital railgun shaders", exception);
            worldChain = null;
            guiChain = null;
        }
    }

    public void resizeChains() {
        if (minecraft.getMainRenderTarget() == null) {
            return;
        }
        int newWidth = minecraft.getMainRenderTarget().width;
        int newHeight = minecraft.getMainRenderTarget().height;
        if (newWidth == width && newHeight == height) {
            return;
        }
        width = newWidth;
        height = newHeight;
        if (worldChain != null) {
            worldChain.resize(width, height);
        }
        if (guiChain != null) {
            guiChain.resize(width, height);
        }
    }

    public void processWorld(RenderLevelStageEvent event) {
        cacheCameraPose(event);
        cachedPartialTick = event.getPartialTick();

        if (worldChain == null) {
            return;
        }
        RailgunState state = RailgunState.getInstance();
        Level level = minecraft.level;
        if (!state.isStrikeActive() || level == null || state.getStrikeDimension() == null || !state.getStrikeDimension().equals(level.dimension())) {
            return;
        }

        resizeChains();
        float partialTicks = cachedPartialTick;
        applyUniforms(worldChain, state.getStrikePos(), state.getStrikeSeconds(partialTicks), false);
        worldChain.process(partialTicks);
    }

    public void processGui(RenderLevelStageEvent event) {
        if (guiChain == null) {
            return;
        }
        RailgunState state = RailgunState.getInstance();
        if (!state.isCharging()) {
            return;
        }

        if (cachedModelViewMatrix == null || cachedProjectionMatrix == null || cachedCameraPosition == null || cachedPartialTick == null) {
            return;
        }

        resizeChains();
        applyUniforms(guiChain, state.getHitPos(), state.getChargeSeconds(cachedPartialTick), true);
        guiChain.process(cachedPartialTick);
    }

    @SuppressWarnings("unchecked")
    private List<PostPass> getPasses(PostChain chain) {
        if (chain == null) {
            return Collections.emptyList();
        }
        try {
            return (List<PostPass>) PASSES_FIELD.get(chain);
        } catch (IllegalAccessException exception) {
            LOGGER.error("Failed to access post chain passes", exception);
            return Collections.emptyList();
        }
    }

    private void cacheCameraPose(RenderLevelStageEvent event) {
        cachedModelViewMatrix = new Matrix4f(event.getPoseStack().last().pose());
        cachedProjectionMatrix = new Matrix4f(event.getProjectionMatrix());
        cachedCameraPosition = event.getCamera().getPosition();
    }

    private void applyUniforms(PostChain chain, Vec3 blockPos, float time, boolean guiPass) {
        List<PostPass> passes = getPasses(chain);
        if (passes.isEmpty()) {
            return;
        }

        if (cachedModelViewMatrix == null || cachedProjectionMatrix == null || cachedCameraPosition == null) {
            return;
        }

        Matrix4f modelView = new Matrix4f(cachedModelViewMatrix);
        Matrix4f inverse = new Matrix4f(cachedProjectionMatrix).invert();
        Vec3 cameraPos = cachedCameraPosition;
        RailgunState state = RailgunState.getInstance();
        float distance = guiPass ? state.getHitDistance() : (float) cameraPos.distanceTo(blockPos);

        for (PostPass pass : passes) {
            EffectInstance effect = pass.getEffect();
            if (effect == null) {
                continue;
            }

            setMatrix(effect, "InverseTransformMatrix", inverse);
            setMatrix(effect, "ModelViewMat", modelView);
            setVec3(effect, "CameraPosition", cameraPos);
            setVec3(effect, "BlockPosition", blockPos);
            setFloat(effect, "iTime", time);
            setVec2(effect, "OutSize", minecraft.getWindow().getWidth(), minecraft.getWindow().getHeight());
            setFloat(effect, "Distance", distance);
            if (guiPass) {
                setFloat(effect, "IsBlockHit", state.getHitKind() == RailgunState.HitKind.NONE ? 0.0F : 1.0F);
                setInt(effect, "HitKind", state.getHitKind().ordinal());
            }
        }
    }

    private void setMatrix(EffectInstance effect, String name, Matrix4f matrix) {
        Uniform uniform = effect.getUniform(name);
        if (uniform != null) {
            uniform.set(matrix);
        }
    }

    private void setVec3(EffectInstance effect, String name, Vec3 vec) {
        Uniform uniform = effect.getUniform(name);
        if (uniform != null) {
            uniform.set((float) vec.x, (float) vec.y, (float) vec.z);
        }
    }

    private void setVec2(EffectInstance effect, String name, int width, int height) {
        Uniform uniform = effect.getUniform(name);
        if (uniform != null) {
            uniform.set((float) width, (float) height);
        }
    }

    private void setFloat(EffectInstance effect, String name, float value) {
        Uniform uniform = effect.getUniform(name);
        if (uniform != null) {
            uniform.set(value);
        }
    }

    private void setInt(EffectInstance effect, String name, int value) {
        Uniform uniform = effect.getUniform(name);
        if (uniform != null) {
            uniform.set(value);
        }
    }

    private void closeChains() {
        if (worldChain != null) {
            worldChain.close();
            worldChain = null;
        }
        if (guiChain != null) {
            guiChain.close();
            guiChain = null;
        }
    }
}
