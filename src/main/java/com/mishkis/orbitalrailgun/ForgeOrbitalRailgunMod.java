package com.mishkis.orbitalrailgun;

import com.mishkis.orbitalrailgun.item.OrbitalRailgunItem;
import com.mishkis.orbitalrailgun.network.Network;
import com.mishkis.orbitalrailgun.util.OrbitalRailgunStrikeManager;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.bernie.geckolib.GeckoLib;

@Mod(ForgeOrbitalRailgunMod.MOD_ID)
public class ForgeOrbitalRailgunMod {
    public static final String MOD_ID = "orbital_railgun";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, MOD_ID);
    public static final RegistryObject<OrbitalRailgunItem> ORBITAL_RAILGUN = ITEMS.register("orbital_railgun",
        () -> new OrbitalRailgunItem(new Item.Properties().stacksTo(1).rarity(Rarity.EPIC)));

    public ForgeOrbitalRailgunMod() {
        GeckoLib.initialize();

        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();
        ITEMS.register(modBus);
        modBus.addListener(this::onCommonSetup);

        OrbitalRailgunStrikeManager.register();
    }

    private void onCommonSetup(final FMLCommonSetupEvent event) {
        event.enqueueWork(Network::init);
    }

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
    }
}
