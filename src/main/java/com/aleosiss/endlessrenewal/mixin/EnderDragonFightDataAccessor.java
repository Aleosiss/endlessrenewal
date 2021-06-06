package com.aleosiss.endlessrenewal.mixin;

import net.minecraft.block.pattern.BlockPattern;
import net.minecraft.entity.boss.dragon.EnderDragonFight;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(EnderDragonFight.class)
public interface EnderDragonFightDataAccessor {

    @Accessor("endPortalPattern")
    @Mutable
    BlockPattern getEnderPortalBlockPattern();

    @Accessor("exitPortalLocation")
    @Mutable
    BlockPos getExitPortalLocation();


}
