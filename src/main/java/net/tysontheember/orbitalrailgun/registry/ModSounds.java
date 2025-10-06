package net.tysontheember.orbitalrailgun.registry;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import net.tysontheember.orbitalrailgun.ForgeOrbitalRailgunMod;

public final class ModSounds {
    public static final DeferredRegister<SoundEvent> SOUNDS =
            DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, ForgeOrbitalRailgunMod.MOD_ID);

    private static RegistryObject<SoundEvent> sound(String id) {
        return SOUNDS.register(id, () -> SoundEvent.createVariableRangeEvent(
                new ResourceLocation(ForgeOrbitalRailgunMod.MOD_ID, id)));
    }

    public static final RegistryObject<SoundEvent> EQUIP = sound("equip");
    public static final RegistryObject<SoundEvent> SCOPE_ON = sound("scope-on");
    public static final RegistryObject<SoundEvent> RAILGUN_SHOOT = sound("railgun-shoot");

    private ModSounds() {}
}
