package net.tysontheember.orbitalrailgun.client.render;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;

import java.util.Map;

/**
 * Minimal, stable RenderTypes for beam/marker that:
 *  - bind our custom Forge shaders (registered in ShaderRegistration)
 *  - bind the mask texture
 *  - avoid referencing protected static shards (so it compiles on 1.20.1)
 *
 * We keep states minimal because our fragment writes transparent color in vanilla.
 */
public final class OrbRenderTypes {

    private static final Map<ResourceLocation, RenderType> BEAM_CACHE   = new Object2ObjectOpenHashMap<>();
    private static final Map<ResourceLocation, RenderType> MARKER_CACHE = new Object2ObjectOpenHashMap<>();

    private OrbRenderTypes() {}

    public static RenderType beam(ResourceLocation maskTex) {
        return BEAM_CACHE.computeIfAbsent(maskTex, OrbRenderTypes::createBeamType);
    }

    public static RenderType marker(ResourceLocation maskTex) {
        return MARKER_CACHE.computeIfAbsent(maskTex, OrbRenderTypes::createMarkerType);
    }

    private static RenderType createBeamType(ResourceLocation maskTex) {
        RenderType.CompositeState state = RenderType.CompositeState.builder()
                .setShaderState(new RenderStateShard.ShaderStateShard(() -> ClientRender.BEAM_SHADER))
                .setTextureState(new RenderStateShard.TextureStateShard(maskTex, false, false))
                // Do NOT reference protected RenderStateShard constants to avoid access errors.
                // We rely on default depth/cull/blend; our fragment output is fully transparent in vanilla.
                .createCompositeState(true);
        return RenderType.create(
                "orbital_railgun_beam",
                DefaultVertexFormat.POSITION_COLOR_TEX,
                VertexFormat.Mode.QUADS,
                256,
                true,   // affects crumbling
                true,   // sort on upload (sane default for translucent-like)
                state
        );
    }

    private static RenderType createMarkerType(ResourceLocation maskTex) {
        RenderType.CompositeState state = RenderType.CompositeState.builder()
                .setShaderState(new RenderStateShard.ShaderStateShard(() -> ClientRender.MARKER_SHADER))
                .setTextureState(new RenderStateShard.TextureStateShard(maskTex, false, false))
                .createCompositeState(true);
        return RenderType.create(
                "orbital_railgun_marker",
                DefaultVertexFormat.POSITION_COLOR_TEX,
                VertexFormat.Mode.QUADS,
                256,
                true,
                true,
                state
        );
    }
}
