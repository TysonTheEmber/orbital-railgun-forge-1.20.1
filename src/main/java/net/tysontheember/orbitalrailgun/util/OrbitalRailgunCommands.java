package net.tysontheember.orbitalrailgun.util;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.tysontheember.orbitalrailgun.ForgeOrbitalRailgunMod;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

@Mod.EventBusSubscriber(modid = ForgeOrbitalRailgunMod.MOD_ID)
public final class OrbitalRailgunCommands {
    private static final String BRIDGE_ROOT = "shaderpacks/_OrbitalRailgunBridge";

    private OrbitalRailgunCommands() {
    }

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();
        LiteralArgumentBuilder<CommandSourceStack> root = Commands.literal("orbitalrailgun")
            .requires(source -> source.hasPermission(2))
            .then(Commands.literal("exportShaderpackBridge")
                .then(Commands.argument("packName", StringArgumentType.string())
                    .executes(ctx -> exportBridge(ctx, StringArgumentType.getString(ctx, "packName")))));
        dispatcher.register(root);
    }

    private static int exportBridge(CommandContext<CommandSourceStack> ctx, String packName) throws CommandSyntaxException {
        CommandSourceStack source = ctx.getSource();
        MinecraftServer server = source.getServer();
        File serverDir = server.getServerDirectory();
        Path rootPath = serverDir.toPath();
        Path bridgePath = rootPath.resolve(BRIDGE_ROOT).resolve(packName);

        try {
            writeBridge(bridgePath, packName);
        } catch (IOException exception) {
            ForgeOrbitalRailgunMod.LOGGER.error("Failed to export shaderpack bridge", exception);
            source.sendFailure(Component.literal("Failed to export Orbital Railgun bridge files: " + exception.getMessage()));
            return 0;
        }

        source.sendSuccess(() -> Component.literal("Exported Orbital Railgun shader bridge to " + bridgePath), true);
        return 1;
    }

    private static void writeBridge(Path base, String packName) throws IOException {
        Files.createDirectories(base.resolve("composite"));
        Files.createDirectories(base.resolve("final"));
        Files.createDirectories(base.resolve("common"));

        Files.writeString(base.resolve("common/orbital_uniforms.glsl"), uniformsContent(), StandardCharsets.UTF_8);
        Files.writeString(base.resolve("composite/orbital_railgun_include.glsl"), compositeContent(), StandardCharsets.UTF_8);
        Files.writeString(base.resolve("final/orbital_railgun_include.glsl"), finalContent(), StandardCharsets.UTF_8);
        Files.writeString(base.resolve("README.txt"), readmeContent(packName), StandardCharsets.UTF_8);
    }

    private static String uniformsContent() {
        return "// Orbital Railgun shaderpack bridge uniforms\n" +
            "uniform float orbital_Time;\n" +
            "uniform float orbital_Flash01;\n" +
            "uniform int orbital_HitKind;\n" +
            "uniform vec3 orbital_HitPos;\n" +
            "uniform float orbital_Distance;\n";
    }

    private static String compositeContent() {
        return "#include \"../common/orbital_uniforms.glsl\"\n" +
            "vec4 applyOrbitalRailgun(vec4 color, vec2 uv) {\n" +
            "    vec2 center = uv - 0.5;\n" +
            "    float radial = length(center);\n" +
            "    float flash = orbital_Flash01 * (1.0 - radial);\n" +
            "    vec3 tint = mix(color.rgb, vec3(0.8, 0.9, 1.2), flash);\n" +
            "    float vignette = smoothstep(0.9, 0.2, radial);\n" +
            "    return vec4(tint * vignette, color.a);\n" +
            "}\n";
    }

    private static String finalContent() {
        return "#include \"../common/orbital_uniforms.glsl\"\n" +
            "vec4 applyOrbitalRailgun(vec4 color, vec2 uv) {\n" +
            "    vec2 offset = vec2(sin(orbital_Time + uv.y * 14.0), cos(orbital_Time + uv.x * 12.0)) * 0.002;\n" +
            "    vec3 fringe;\n" +
            "    fringe.r = texture(colortex0, uv + offset).r;\n" +
            "    fringe.g = texture(colortex0, uv).g;\n" +
            "    fringe.b = texture(colortex0, uv - offset).b;\n" +
            "    float flash = orbital_Flash01;\n" +
            "    return vec4(mix(color.rgb, fringe, 0.7) + flash * vec3(1.0, 0.8, 0.6), color.a);\n" +
            "}\n";
    }

    private static String readmeContent(String packName) {
        return "Orbital Railgun Shaderpack Bridge\n" +
            "===============================\n\n" +
            "Pack: " + packName + "\n\n" +
            "Copy the include files into your shaderpack and reference applyOrbitalRailgun() " +
            "from your composite or final stage.\n" +
            "Uniforms are provided via the bridge and match the Fabric Iris implementation.\n";
    }
}
