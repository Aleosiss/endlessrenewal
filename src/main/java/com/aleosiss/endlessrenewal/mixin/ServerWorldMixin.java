package com.aleosiss.endlessrenewal.mixin;

import com.aleosiss.endlessrenewal.EndlessRenewal;
import net.minecraft.entity.boss.dragon.EnderDragonFight;
import net.minecraft.server.world.ServerChunkManager;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.profiler.Profiler;
import net.minecraft.util.registry.RegistryKey;
import net.minecraft.world.MutableWorldProperties;
import net.minecraft.world.World;
import net.minecraft.world.dimension.DimensionType;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.Supplier;

@Mixin(ServerWorld.class)
public abstract class ServerWorldMixin extends World  {
    @Shadow
    public abstract ServerChunkManager getChunkManager();

    @Shadow @Final @Nullable private EnderDragonFight enderDragonFight;

    protected ServerWorldMixin(MutableWorldProperties properties, RegistryKey<World> registryRef, DimensionType dimensionType, Supplier<Profiler> profiler, boolean isClient, boolean debugWorld, long seed) {
        super(properties, registryRef, dimensionType, profiler, isClient, debugWorld, seed);
    }

    @Inject(method = "saveLevel", at = @At("HEAD"), cancellable = true)
    private void ModifySaveLevel(CallbackInfo ci) {
        if(this.getRegistryKey().equals(EndlessRenewal.WORLD_KEY)) {
            // save dragon fight data elsewhere
            EndlessRenewal.STATE.setEnderDragonFight(this.enderDragonFight);
            // do normal save stuff
            this.getChunkManager().getPersistentStateManager().save();

            // cancel out so we do not overwrite the true end dragon fight state
            ci.cancel();
        }

    }

}
