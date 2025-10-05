package net.tysontheember.orbitalrailgun.util;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.loading.FMLPaths;
import net.tysontheember.orbitalrailgun.ForgeOrbitalRailgunMod;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

@Mod.EventBusSubscriber(modid = ForgeOrbitalRailgunMod.MOD_ID)
public final class OrbitalRailgunCommands {
    private OrbitalRailgunCommands() {
    }

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();
        dispatcher.register(Commands.literal("orbitalrailgun")
            .requires(source -> source.hasPermission(2))
            .then(Commands.literal("exportShaderpackBridge")
                .then(Commands.argument("packName", StringArgumentType.string())
                    .executes(context -> exportBridge(context, StringArgumentType.getString(context, "packName"))))));
    }

    private static int exportBridge(CommandContext<CommandSourceStack> context, String packName) {
        CommandSourceStack source = context.getSource();
        Path output = FMLPaths.GAMEDIR.get().resolve("shaderpacks/_OrbitalRailgunBridge").resolve(packName);
        try {
            writeBridgeFiles(output);
            source.sendSuccess(() -> Component.literal("Exported Orbital Railgun shader bridge to " + output), true);
            return 1;
        } catch (IOException exception) {
            ForgeOrbitalRailgunMod.LOGGER.error("Failed to export shader bridge", exception);
            source.sendFailure(Component.literal("Failed to export bridge: " + exception.getMessage()));
            return 0;
        }
    }

    private static void writeBridgeFiles(Path baseDir) throws IOException {
        Files.createDirectories(baseDir.resolve("composite"));
        Files.createDirectories(baseDir.resolve("final"));
        Files.createDirectories(baseDir.resolve("common"));

        write(baseDir.resolve("common/orbital_uniforms.glsl"), COMMON_UNIFORMS);
        write(baseDir.resolve("composite/orbital_railgun_include.glsl"), COMPOSITE_INCLUDE);
        write(baseDir.resolve("final/orbital_railgun_include.glsl"), FINAL_INCLUDE);
        write(baseDir.resolve("README.txt"), README);
    }

    private static void write(Path path, String contents) throws IOException {
        Files.createDirectories(path.getParent());
        Files.writeString(path, contents, StandardCharsets.UTF_8);
    }

    private static final String COMMON_UNIFORMS = """
// Orbital Railgun shader bridge uniforms
uniform float Orbital_Time;
uniform float Orbital_Flash01;
uniform int Orbital_HitKind;
uniform vec3 Orbital_HitPos;
uniform float Orbital_Distance;
""";

    private static final String COMPOSITE_INCLUDE = """
#include "../common/orbital_uniforms.glsl"

vec4 applyOrbitalRailgunComposite(vec4 color, vec2 uv) {
    vec2 center = uv - 0.5;
    float vig = smoothstep(1.0, 0.3, length(center) * 1.5);
    float flash = Orbital_Flash01;
    vec3 tint = vec3(0.45, 0.7, 1.2) * flash;
    return vec4(color.rgb + tint * vig, color.a);
}
""";

    private static final String FINAL_INCLUDE = """
#include "../common/orbital_uniforms.glsl"

vec4 applyOrbitalRailgun(vec4 color, vec2 uv) {
    float flash = Orbital_Flash01;
    vec2 center = uv - 0.5;
    float vignette = smoothstep(0.9, 0.0, length(center));
    vec3 aberration = vec3(center.x, -center.y, center.x * center.y) * 0.02;
    vec3 shifted = vec3(
        color.r + aberration.r,
        color.g,
        color.b - aberration.b
    );
    return vec4(mix(color.rgb, shifted, flash) * (1.0 + vignette * flash), color.a);
}
""";

    private static final String README = """
Orbital Railgun Shader Bridge
==============================

This folder contains GLSL snippets that replicate the Orbital Railgun screen-space
post processing. Include these snippets in your shaderpack to integrate the effect.

1. Copy the `common`, `composite`, and `final` directories into your shaderpack.
2. Include `composite/orbital_railgun_include.glsl` from your composite shader to apply
distortion and heat haze.
3. Include `final/orbital_railgun_include.glsl` from your final shader to apply the tint
   and chromatic flash.

The uniforms are expected to be populated by Orbital Railgun. When the mod detects an
Iris/Oculus shaderpack, it will update these uniforms every frame.
""";
}
