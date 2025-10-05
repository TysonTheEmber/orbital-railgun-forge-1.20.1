package net.tysontheember.orbitalrailgun.client;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.RegisterClientCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.tysontheember.orbitalrailgun.ForgeOrbitalRailgunMod;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

@OnlyIn(Dist.CLIENT)
@Mod.EventBusSubscriber(modid = ForgeOrbitalRailgunMod.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class RailgunClientCommands {
    private static final String COMPOSITE_INCLUDE = """
#version 150
#include "../common/orbital_uniforms.glsl"

vec4 applyOrbitalRailgun(vec4 color, vec2 uv, float Time, float Flash01, int HitKind, vec3 HitPos, float Distance) {
    float intensity = Flash01 + float(HitKind != 0) * 0.35;
    vec2 aberrOffset = vec2(sin(Time + uv.y * 40.0), cos(Time + uv.x * 40.0)) * (0.002 + 0.004 * intensity);
    vec3 chroma;
    chroma.r = texture(colortex0, uv + aberrOffset).r;
    chroma.g = texture(colortex0, uv).g;
    chroma.b = texture(colortex0, uv - aberrOffset).b;
    float vignette = smoothstep(1.15, 0.35, length(uv - 0.5));
    vec3 blended = mix(color.rgb, chroma, 0.55);
    blended *= vignette;
    return vec4(blended, color.a);
}
""";

    private static final String FINAL_INCLUDE = """
#version 150
#include "../common/orbital_uniforms.glsl"

vec4 applyOrbitalRailgunFinal(vec4 color, vec2 uv, float Time, float Flash01, int HitKind, vec3 HitPos, float Distance) {
    float flash = Flash01;
    vec3 flashColor = vec3(1.2, 0.9, 0.7) * flash;
    return vec4(color.rgb + flashColor, color.a);
}
""";

    private static final String UNIFORMS_INCLUDE = """
#ifndef ORBITAL_RAILGUN_UNIFORMS
#define ORBITAL_RAILGUN_UNIFORMS
uniform float orbital_Time;
uniform float orbital_Flash01;
uniform int orbital_HitKind;
uniform vec3 orbital_HitPos;
uniform float orbital_Distance;
#endif
""";

    private static final String README_TEXT = """
Orbital Railgun Shaderpack Bridge
=================================

The files in this directory provide helper functions for integrating the Orbital Railgun Forge visuals with Iris/Oculus shaderpacks.

How to use:
1. Copy the contents of `common/orbital_uniforms.glsl` into the uniform section of your composite or final pass.
2. Include either `composite/orbital_railgun_include.glsl` or `final/orbital_railgun_include.glsl` in the stage where you want the effect.
3. Call `applyOrbitalRailgun(...)` or `applyOrbitalRailgunFinal(...)` with the uniforms provided by the mod (exposed as `orbital_*`).

Example (composite):
```
#include "../_OrbitalRailgunBridge/<packName>/composite/orbital_railgun_include.glsl"
color = applyOrbitalRailgun(color, texcoord, orbital_Time, orbital_Flash01, orbital_HitKind, orbital_HitPos, orbital_Distance);
```

The exporter can be run again to overwrite these files with the latest templates.
""";

    private RailgunClientCommands() {
    }

    @SubscribeEvent
    public static void onRegisterClientCommands(RegisterClientCommandsEvent event) {
        event.getDispatcher().register(
            Commands.literal("orbitalrailgun")
                .then(Commands.literal("exportShaderpackBridge")
                    .then(Commands.argument("packName", StringArgumentType.word())
                        .executes(context -> exportBridge(context, StringArgumentType.getString(context, "packName")))))
        );
    }

    private static int exportBridge(CommandContext<CommandSourceStack> context, String packName) {
        String sanitized = packName.replaceAll("[^A-Za-z0-9_\\-]", "_");
        Path baseDir = Minecraft.getInstance().gameDirectory.toPath()
            .resolve("shaderpacks")
            .resolve("_OrbitalRailgunBridge")
            .resolve(sanitized);
        try {
            writeBridgeFiles(baseDir);
            context.getSource().sendSuccess(() -> Component.literal("Exported Orbital Railgun bridge to " + baseDir), false);
            return 1;
        } catch (IOException exception) {
            context.getSource().sendFailure(Component.literal("Failed to export bridge: " + exception.getMessage()));
            return 0;
        }
    }

    private static void writeBridgeFiles(Path baseDir) throws IOException {
        Files.createDirectories(baseDir.resolve("composite"));
        Files.createDirectories(baseDir.resolve("final"));
        Files.createDirectories(baseDir.resolve("common"));

        Files.writeString(baseDir.resolve("composite").resolve("orbital_railgun_include.glsl"), COMPOSITE_INCLUDE, StandardCharsets.UTF_8);
        Files.writeString(baseDir.resolve("final").resolve("orbital_railgun_include.glsl"), FINAL_INCLUDE, StandardCharsets.UTF_8);
        Files.writeString(baseDir.resolve("common").resolve("orbital_uniforms.glsl"), UNIFORMS_INCLUDE, StandardCharsets.UTF_8);
        Files.writeString(baseDir.resolve("README.txt"), README_TEXT, StandardCharsets.UTF_8);
    }
}
