package net.tysontheember.orbitalrailgun.item;

import net.minecraft.ChatFormatting;
import net.tysontheember.orbitalrailgun.client.item.OrbitalRailgunRenderer;
import net.tysontheember.orbitalrailgun.registry.ModSounds;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.function.Consumer;

public class OrbitalRailgunItem extends Item implements GeoItem {

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    public OrbitalRailgunItem(Properties properties) {
        super(properties);
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.NONE;
    }

    @Override
    public int getUseDuration(ItemStack stack) {
        return 72000;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player.getCooldowns().isOnCooldown(this)) {
            return InteractionResultHolder.fail(stack);
        }

        player.startUsingItem(hand);
        if (level.isClientSide && ModSounds.SCOPE_ON.isPresent()) {
            player.playSound(ModSounds.SCOPE_ON.get(), 1.0F, 1.0F);
        }
        return InteractionResultHolder.consume(stack);
    }

    @Override
    public void releaseUsing(ItemStack stack, Level level, LivingEntity livingEntity, int timeLeft) {
        if (livingEntity instanceof Player player) {
            player.gameEvent(GameEvent.ITEM_INTERACT_FINISH);
        }
    }

    public void applyCooldown(Player player) {
        int ticks = Math.max(0, net.tysontheember.orbitalrailgun.config.OrbitalConfig.COOLDOWN.get());

        // Prefer server-side so the ClientboundCooldownPacket is sent automatically.
        if (player instanceof net.minecraft.server.level.ServerPlayer sp) {
            sp.getCooldowns().addCooldown(this, ticks);
        } else {
            // Fallback (e.g., if called client-side by mistake)
            player.getCooldowns().addCooldown(this, ticks);
        }
    }

    @Override
    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        consumer.accept(new IClientItemExtensions() {
            private OrbitalRailgunRenderer renderer;

            @Override
            public BlockEntityWithoutLevelRenderer getCustomRenderer() {
                if (renderer == null) {
                    renderer = new OrbitalRailgunRenderer();
                }
                return renderer;
            }
        });
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllerRegistrar) {
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }
    @Override
    public void appendHoverText(ItemStack stack, @org.jetbrains.annotations.Nullable Level level,
                                java.util.List<net.minecraft.network.chat.Component> tooltip,
                                net.minecraft.world.item.TooltipFlag flag) {
        // Basic one-line tooltip from lang file
        tooltip.add(net.minecraft.network.chat.Component.translatable(getDescriptionId(stack) + ".tooltip")
                .withStyle(net.minecraft.ChatFormatting.GRAY));

        // Optional: extra details on SHIFT
        if (net.minecraft.client.gui.screens.Screen.hasShiftDown()) {
            tooltip.add(net.minecraft.network.chat.Component.translatable(getDescriptionId(stack) + ".tooltip.shift1")
                    .withStyle(net.minecraft.ChatFormatting.DARK_AQUA));
            tooltip.add(net.minecraft.network.chat.Component.translatable(getDescriptionId(stack) + ".tooltip.shift2")
                    .withStyle(ChatFormatting.DARK_GREEN));
            tooltip.add(net.minecraft.network.chat.Component.translatable(getDescriptionId(stack) + ".tooltip.shift3")
                    .withStyle(ChatFormatting.DARK_RED));
        } else {
            tooltip.add(net.minecraft.network.chat.Component.translatable("tooltip.orbital_railgun.hold_shift")
                    .withStyle(net.minecraft.ChatFormatting.YELLOW, net.minecraft.ChatFormatting.ITALIC));
        }
    }

}
