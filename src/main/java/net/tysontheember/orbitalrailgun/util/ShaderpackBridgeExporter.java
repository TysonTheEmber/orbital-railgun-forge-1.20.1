package net.tysontheember.orbitalrailgun.util;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraftforge.server.ServerLifecycleHooks;
import net.tysontheember.orbitalrailgun.ForgeOrbitalRailgunMod;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

public final class ShaderpackBridgeExporter {
    private static final SimpleCommandExceptionType SERVER_UNAVAILABLE = new SimpleCommandExceptionType(Component.literal("Server unavailable"));
    private static final SimpleCommandExceptionType PACK_NAME_EMPTY = new SimpleCommandExceptionType(Component.literal("Pack name cannot be empty"));
    private static final SimpleCommandExceptionType EXPORT_FAILED = new SimpleCommandExceptionType(Component.literal("Failed to export shaderpack bridge"));

    private ShaderpackBridgeExporter() {}

    public static int export(CommandSourceStack source, String packName) throws CommandSyntaxException {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) {
            throw SERVER_UNAVAILABLE.create();
        }

        String normalized = packName.trim();
        if (normalized.isEmpty()) {
            throw PACK_NAME_EMPTY.create();
        }

        String safeName = normalized.replaceAll("[^A-Za-z0-9-_]", "_");
        File serverDirectory = server.getServerDirectory();
        Path serverDir = serverDirectory != null ? serverDirectory.toPath() : null;
        if (serverDir == null) {
            serverDir = server.getWorldPath(LevelResource.ROOT);
        }
        if (serverDir == null) {
            throw SERVER_UNAVAILABLE.create();
        }

        Path baseDir = serverDir.resolve("shaderpacks").resolve("_OrbitalRailgunBridge").resolve(safeName);

        try {
            writeBridgeFiles(baseDir, normalized);
        } catch (IOException exception) {
            ForgeOrbitalRailgunMod.LOGGER.error("Failed to export shaderpack bridge", exception);
            throw EXPORT_FAILED.create();
        }

        source.sendSuccess(() -> Component.literal("Exported Orbital Railgun bridge to " + baseDir), false);
        return 1;
    }

    private static void writeBridgeFiles(Path baseDir, String packName) throws IOException {
        Files.createDirectories(baseDir.resolve("composite"));
        Files.createDirectories(baseDir.resolve("final"));
        Files.createDirectories(baseDir.resolve("common"));

        writeFile(baseDir.resolve("common").resolve("orbital_uniforms.glsl"), uniformsContent());
        writeFile(baseDir.resolve("composite").resolve("orbital_railgun_include.glsl"), includeContent());
        writeFile(baseDir.resolve("final").resolve("orbital_railgun_include.glsl"), includeContent());
        writeFile(baseDir.resolve("README.txt"), readmeContent(packName));
    }

    private static void writeFile(Path path, String contents) throws IOException {
        Files.createDirectories(path.getParent());
        Files.writeString(path, contents, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE);
    }

    private static String uniformsContent() {
        return "// Orbital Railgun bridge uniforms\n"
            + "uniform float OR_Time;\n"
            + "uniform float OR_Flash01;\n"
            + "uniform int OR_HitKind;\n"
            + "uniform vec3 OR_HitPos;\n"
            + "uniform float OR_Distance;\n";
    }

    private static String includeContent() {
        return "#include \"../common/orbital_uniforms.glsl\"\n\n"
            + "vec4 applyOrbitalRailgun(vec4 color, vec2 uv, float Time, float Flash01, int HitKind, vec3 HitPos, float Distance) {\n"
            + "    float vignette = smoothstep(0.8, 0.4, length(uv * 2.0 - 1.0));\n"
            + "    float fringe = sin(Time * 12.0 + uv.x * 8.0) * 0.02;\n"
            + "    vec2 offset = vec2(fringe, -fringe);\n"
            + "    vec3 tint = mix(color.rgb, vec3(0.4, 0.8, 1.0), Flash01);\n"
            + "    vec3 distorted = vec3(\n"
            + "        color.r,\n"
            + "        color.g + offset.x,\n"
            + "        color.b - offset.y\n"
            + "    );\n"
            + "    vec3 result = mix(distorted, tint, Flash01);\n"
            + "    return vec4(result * vignette, color.a);\n"
            + "}\n";
    }

    private static String readmeContent(String packName) {
        return "Orbital Railgun Shaderpack Bridge\n\n"
            + "Generated for pack: " + packName + "\n\n"
            + "Copy the include files into your shaderpack and insert the applyOrbitalRailgun call into your composite or final stage.";
    }
}
