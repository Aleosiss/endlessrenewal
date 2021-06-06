package com.aleosiss.endlessrenewal.util;

import com.aleosiss.endlessrenewal.mixin.EndDragonFightAccessor;
import net.minecraft.entity.boss.dragon.EnderDragonFight;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;

public class ERUtils {
    public static void resetAlternateEnd(MinecraftServer server, ServerWorld alternateEnd) {
        CompoundTag dragonFightTagData = new CompoundTag();
        dragonFightTagData.putBoolean("DragonKilled", false);
        dragonFightTagData.putBoolean("PreviouslyKilled", false);

        EnderDragonFight alternateEndDragonFightData = new EnderDragonFight(alternateEnd, server.getSaveProperties().getGeneratorOptions().getSeed(), dragonFightTagData);
        alternateEndDragonFightData.resetEndCrystals();
        alternateEndDragonFightData.respawnDragon();
        alternateEndDragonFightData.tick();

        EndDragonFightAccessor alternateEndDragonFight = (EndDragonFightAccessor) alternateEnd;
        alternateEndDragonFight.setEnderDragonFight(alternateEndDragonFightData);
    }

    public static boolean doesServerWorldNeedDragonFightReset(ServerWorld alternateEnd) {
        boolean requiresReset;

        EnderDragonFight previousEndDragonFight = alternateEnd.getEnderDragonFight();
        requiresReset = alternateEnd.getDimension() == null;
        requiresReset = requiresReset || previousEndDragonFight == null;
        requiresReset = requiresReset || previousEndDragonFight.hasPreviouslyKilled();

        return requiresReset;
    }
}
