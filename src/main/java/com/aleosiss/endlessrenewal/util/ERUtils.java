package com.aleosiss.endlessrenewal.util;

import com.aleosiss.endlessrenewal.mixin.EndDragonFightAccessor;
import com.aleosiss.endlessrenewal.mixin.EnderDragonFightDataAccessor;
import com.aleosiss.endlessrenewal.mixin.SessionAccessor;
import net.minecraft.entity.boss.dragon.EnderDragonFight;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.registry.RegistryKey;
import net.minecraft.world.World;
import net.minecraft.world.chunk.ChunkStatus;
import net.minecraft.world.level.storage.LevelStorage;
import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.IOException;

public class ERUtils {
    private static final Logger logger = LogManager.getLogger();


    public static void resetAlternateEnd(MinecraftServer server, ServerWorld world) {
        EnderDragonFight oldFight = world.getEnderDragonFight();
        EnderDragonFightDataAccessor endExitPortalLocationAccessor = (EnderDragonFightDataAccessor) oldFight;
        if(oldFight != null) {
            if(!oldFight.hasPreviouslyKilled()) {
                logger.warn("We haven't killed the dragon, but we're trying to restart the fight?");
            }

            for(ServerPlayerEntity playerEntity : world.getPlayers()) {
                BlockPos dest = endExitPortalLocationAccessor.getExitPortalLocation();
                playerEntity.teleport(dest.getX(), dest.getY(), dest.getZ());
            }
        }

        
        rebuildEndIsland(server, world);
        startNewDragonFight(server, world);
    }

    private static void rebuildEndIsland(MinecraftServer server, ServerWorld world) {
        RegistryKey<World> registryKey = world.getRegistryKey();

        SessionAccessor sessionAccessor = (SessionAccessor) server;
        LevelStorage.Session session = sessionAccessor.getSession();

        File worldDirectory = session.getWorldDirectory(registryKey);
        if(worldDirectory.exists()) {
            try {
                FileUtils.deleteDirectory(worldDirectory);
            } catch (IOException e) {
                logger.warn("Failed to reset end island!");
            }
        }
    }


    public static void startNewDragonFight(MinecraftServer server, ServerWorld alternateEnd) {
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
