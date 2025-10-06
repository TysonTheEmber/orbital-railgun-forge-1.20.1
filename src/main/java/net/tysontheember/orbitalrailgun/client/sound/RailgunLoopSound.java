package net.tysontheember.orbitalrailgun.client.sound;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.tysontheember.orbitalrailgun.client.railgun.RailgunState;
import net.tysontheember.orbitalrailgun.registry.ModSounds;

@OnlyIn(Dist.CLIENT)
public class RailgunLoopSound extends AbstractTickableSoundInstance {
    private final LocalPlayer player;
    private boolean stopping = false;

    public RailgunLoopSound(LocalPlayer player) {
        this(player, ModSounds.CHARGE_LOOP.get(), RandomSource.create());
    }

    private RailgunLoopSound(LocalPlayer player, SoundEvent event, RandomSource randomSource) {
        super(event, SoundSource.PLAYERS, randomSource);
        this.player = player;
        this.looping = true;
        this.delay = 0;
        this.volume = 0.4F;
        this.pitch = 0.95F;
        Vec3 position = player.position();
        this.x = (float) position.x;
        this.y = (float) position.y;
        this.z = (float) position.z;
    }

    @Override
    public void tick() {
        if (this.player == null || this.player.isRemoved()) {
            this.stop();
            return;
        }

        Vec3 position = this.player.position();
        this.x = (float) position.x;
        this.y = (float) position.y;
        this.z = (float) position.z;

        if (stopping || !this.player.isUsingItem()) {
            this.volume = Math.max(0.0F, this.volume - 0.08F);
            if (this.volume <= 0.01F) {
                this.stop();
            }
            return;
        }

        float progress = RailgunState.getClientChargeProgress();
        this.volume = 0.35F + progress * 0.65F;
        this.pitch = 0.95F + progress * 0.15F;
    }

    public void beginFadeOut() {
        this.stopping = true;
    }
}
