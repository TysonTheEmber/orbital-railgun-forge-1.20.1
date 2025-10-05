package net.tysontheember.orbitalrailgun.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.tysontheember.orbitalrailgun.ForgeOrbitalRailgunMod;
import net.tysontheember.orbitalrailgun.util.ShaderpackBridgeExporter;

@Mod.EventBusSubscriber(modid = ForgeOrbitalRailgunMod.MOD_ID)
public final class CommandEvents {
    private CommandEvents() {}

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();
        dispatcher.register(
            Commands.literal("orbitalrailgun")
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("exportShaderpackBridge")
                    .then(Commands.argument("packName", StringArgumentType.greedyString())
                        .executes(context -> ShaderpackBridgeExporter.export(context.getSource(), StringArgumentType.getString(context, "packName")))))
        );
    }
}
