package net.tysontheember.orbitalrailgun.network;

import net.tysontheember.orbitalrailgun.ForgeOrbitalRailgunMod;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

public final class Network {
    private static final String PROTOCOL_VERSION = "1";
    public static final SimpleChannel CHANNEL = NetworkRegistry.ChannelBuilder
        .named(ForgeOrbitalRailgunMod.id("main"))
        .networkProtocolVersion(() -> PROTOCOL_VERSION)
        .clientAcceptedVersions(PROTOCOL_VERSION::equals)
        .serverAcceptedVersions(PROTOCOL_VERSION::equals)
        .simpleChannel();

    private static int id = 0;

    private Network() {}

    public static void init() {
        CHANNEL.messageBuilder(C2S_RequestFire.class, nextId(), NetworkDirection.PLAY_TO_SERVER)
            .encoder(C2S_RequestFire::encode)
            .decoder(C2S_RequestFire::decode)
            .consumerMainThread(C2S_RequestFire::handle)
            .add();

        CHANNEL.messageBuilder(S2C_PlayStrikeEffects.class, nextId(), NetworkDirection.PLAY_TO_CLIENT)
            .encoder(S2C_PlayStrikeEffects::encode)
            .decoder(S2C_PlayStrikeEffects::decode)
            .consumerMainThread(S2C_PlayStrikeEffects::handle)
            .add();
    }

    private static int nextId() {
        return id++;
    }
}
