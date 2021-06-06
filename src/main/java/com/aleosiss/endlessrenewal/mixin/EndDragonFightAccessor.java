package com.aleosiss.endlessrenewal.mixin;

import net.minecraft.entity.boss.dragon.EnderDragonFight;
import net.minecraft.server.world.ServerWorld;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ServerWorld.class)
public interface EndDragonFightAccessor {
    @Accessor("enderDragonFight")
    @Mutable
    void setEnderDragonFight(@Nullable EnderDragonFight enderDragonFight);
}
