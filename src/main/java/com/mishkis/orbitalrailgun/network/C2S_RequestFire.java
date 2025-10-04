package com.mishkis.orbitalrailgun.network;

import com.mishkis.orbitalrailgun.item.OrbitalRailgunItem;
import com.mishkis.orbitalrailgun.util.OrbitalRailgunStrikeManager;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record C2S_RequestFire(BlockPos target) {
    public static void encode(C2S_RequestFire packet, FriendlyByteBuf buf) {
        buf.writeBlockPos(packet.target);
    }

    public static C2S_RequestFire decode(FriendlyByteBuf buf) {
        return new C2S_RequestFire(buf.readBlockPos());
    }

    public static void handle(C2S_RequestFire packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null) {
                return;
            }

            ItemStack stack = player.getMainHandItem();
            OrbitalRailgunItem railgun;
            if (stack.getItem() instanceof OrbitalRailgunItem mainHandRailgun) {
                railgun = mainHandRailgun;
            } else {
                stack = player.getOffhandItem();
                if (stack.getItem() instanceof OrbitalRailgunItem offhandRailgun) {
                    railgun = offhandRailgun;
                } else {
                    return;
                }
            }

            if (player.getCooldowns().isOnCooldown(railgun)) {
                return;
            }

            railgun.applyCooldown(player);
            OrbitalRailgunStrikeManager.startStrike(player, packet.target);
        });
        context.setPacketHandled(true);
    }
}
