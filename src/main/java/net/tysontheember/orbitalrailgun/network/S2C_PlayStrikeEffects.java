package net.tysontheember.orbitalrailgun.network;

import net.tysontheember.orbitalrailgun.client.railgun.RailgunState;
import net.tysontheember.orbitalrailgun.config.OrbitalConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record S2C_PlayStrikeEffects(BlockPos pos, ResourceKey<Level> dimension, float serverStrikeRadius) {
    public static void encode(S2C_PlayStrikeEffects packet, FriendlyByteBuf buf) {
        buf.writeBlockPos(packet.pos);
        buf.writeResourceLocation(packet.dimension.location());
        buf.writeFloat(packet.serverStrikeRadius);
    }

    public static S2C_PlayStrikeEffects decode(FriendlyByteBuf buf) {
        BlockPos pos = buf.readBlockPos();
        ResourceLocation dim = buf.readResourceLocation();
        float radius = buf.readFloat();
        return new S2C_PlayStrikeEffects(pos, ResourceKey.create(Registries.DIMENSION, dim), radius);
    }

    public static void handle(S2C_PlayStrikeEffects packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> handleClient(packet));
        context.setPacketHandled(true);
    }

    @OnlyIn(Dist.CLIENT)
    private static void handleClient(S2C_PlayStrikeEffects packet) {
        Minecraft mc = Minecraft.getInstance();
        RailgunState state = RailgunState.getInstance();
        state.onStrikeStarted(packet.pos(), packet.dimension());
        state.setTransientVisualStrikeRadius(packet.serverStrikeRadius(), 40);
    }
}
