package net.tysontheember.orbitalrailgun;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.util.Objects;
import java.util.function.Supplier;

public final class ModCreativeTabs {
    private ModCreativeTabs() {}

    public static final DeferredRegister<CreativeModeTab> TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, ForgeOrbitalRailgunMod.MOD_ID);

    private static final ResourceLocation ICON_ITEM = new ResourceLocation(
            ForgeOrbitalRailgunMod.MOD_ID, "orbital_railgun"
    );

    private static Supplier<ItemStack> iconStack() {
        Item item = ForgeRegistries.ITEMS.getValue(ICON_ITEM);
        return () -> new ItemStack(Objects.requireNonNull(item, "Icon item not found: " + ICON_ITEM));
    }

    public static final RegistryObject<CreativeModeTab> ORBITAL_RAILGUN_TAB =
            TABS.register("main", () -> CreativeModeTab.builder()
                    .title(Component.translatable("creativetab.orbital_railgun"))
                    .icon(iconStack())
                    .displayItems((params, output) -> {
                        accept(output, "orbital_railgun");
                    })
                    .build());

    private static void accept(CreativeModeTab.Output output, String path) {
        Item it = ForgeRegistries.ITEMS.getValue(new ResourceLocation(ForgeOrbitalRailgunMod.MOD_ID, path));
        if (it != null) output.accept(it);
    }
}
