package net.tysontheember.orbitalrailgun.network;

import net.tysontheember.orbitalrailgun.client.railgun.RailgunState;
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

public record S2C_PlayStrikeEffects(BlockPos pos, ResourceKey<Level> dimension) {
    public static void encode(S2C_PlayStrikeEffects packet, FriendlyByteBuf buf) {
        buf.writeBlockPos(packet.pos);
        buf.writeResourceLocation(packet.dimension.location());
    }

    public static S2C_PlayStrikeEffects decode(FriendlyByteBuf buf) {
        BlockPos pos = buf.readBlockPos();
        ResourceLocation dim = buf.readResourceLocation();
        return new S2C_PlayStrikeEffects(pos, ResourceKey.create(Registries.DIMENSION, dim));
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

        // Sound is handled server-side when the strike is triggered.
    }
}
