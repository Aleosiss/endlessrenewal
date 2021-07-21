package com.aleosiss.endlessrenewal.util;

import com.aleosiss.endlessrenewal.EndlessRenewal;
import com.aleosiss.endlessrenewal.mixin.EndDragonFightAccessor;
import com.aleosiss.endlessrenewal.mixin.MinecraftServerAccessor;
import com.google.common.collect.ImmutableList;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.EndPortalBlockEntity;
import net.minecraft.block.pattern.BlockPattern;
import net.minecraft.block.pattern.BlockPatternBuilder;
import net.minecraft.block.pattern.CachedBlockPosition;
import net.minecraft.entity.boss.dragon.EnderDragonFight;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtHelper;
import net.minecraft.predicate.block.BlockPredicate;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.WorldGenerationProgressListener;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.registry.RegistryKey;
import net.minecraft.util.registry.SimpleRegistry;
import net.minecraft.world.Heightmap;
import net.minecraft.world.World;
import net.minecraft.world.biome.source.BiomeAccess;
import net.minecraft.world.chunk.ChunkStatus;
import net.minecraft.world.chunk.WorldChunk;
import net.minecraft.world.dimension.DimensionOptions;
import net.minecraft.world.gen.GeneratorOptions;
import net.minecraft.world.gen.chunk.ChunkGenerator;
import net.minecraft.world.gen.feature.EndPortalFeature;
import net.minecraft.world.level.UnmodifiableLevelProperties;
import net.minecraft.world.level.storage.LevelStorage;
import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;

public class ERUtils {
    private static final Logger logger = LogManager.getLogger();
    private static final BlockPattern END_PORTAL_PATTERN = BlockPatternBuilder.start()
            .aisle("       ", "       ", "       ", "   #   ", "       ", "       ", "       ")
            .aisle("       ", "       ", "       ", "   #   ", "       ", "       ", "       ")
            .aisle("       ", "       ", "       ", "   #   ", "       ", "       ", "       ")
            .aisle("  ###  ", " #   # ", "#     #", "#  #  #", "#     #", " #   # ", "  ###  ")
            .aisle("       ", "  ###  ", " ##### ", " ##### ", " ##### ", "  ###  ", "       ")
            .where('#', CachedBlockPosition.matchesBlockState(BlockPredicate.make(Blocks.BEDROCK))).build();

    public static void resetAlternateEnd(MinecraftServer server, ServerWorld world) {
        try {
            EnderDragonFight oldFight = world.getEnderDragonFight();
            if (oldFight != null) {
                unloadWorld(server, world);
                destroyWorldChunks(server, world);
                ServerWorld newWorld = loadWorld(server, emptyProgressListener());
                startNewDragonFight(server, newWorld);
            } else {
                startNewDragonFight(server, world);
            }
        } catch(Exception e) {
            logger.error("ResetAlternateEnd hit an exception!", e);
        }
    }

    public static ServerWorld loadWorld(MinecraftServer server, WorldGenerationProgressListener chunkProgressListener) {
            RegistryKey<World> resourceKey = EndlessRenewal.WORLD_KEY;
            MinecraftServerAccessor serverAccessor = (MinecraftServerAccessor) server;
            UnmodifiableLevelProperties derivedLevelData = new UnmodifiableLevelProperties(server.getSaveProperties(), server.getSaveProperties().getMainWorldProperties());
            GeneratorOptions worldGenSettings = server.getSaveProperties().getGeneratorOptions();
            SimpleRegistry<DimensionOptions> dimensionOptionsRegistry = worldGenSettings.getDimensions();
            DimensionOptions dimensionOptions = dimensionOptionsRegistry.get(EndlessRenewal.DIMENSION_KEY);

            ChunkGenerator chunkGenerator = dimensionOptions.getChunkGenerator();
            ServerWorld world = new ServerWorld(server, serverAccessor.getWorkerExecutor(),
                    serverAccessor.getSession(), derivedLevelData, resourceKey, dimensionOptions.getDimensionType(),
                    chunkProgressListener, chunkGenerator, worldGenSettings.isDebugWorld(),
                    BiomeAccess.hashSeed(worldGenSettings.getSeed()), ImmutableList.of(), false);

            serverAccessor.getWorlds().put(EndlessRenewal.WORLD_KEY, world);
            return world;
    }

    public static void unloadWorld(MinecraftServer server, ServerWorld world) {
            EnderDragonFight fight = world.getEnderDragonFight();
            world.getAliveEnderDragons().forEach(fight::dragonKilled);
            int playersInWorld = world.getPlayers().size();
            for (int i = 0; i < playersInWorld; i++) {
                world.getPlayers().get(i).moveToWorld(server.getWorld(World.OVERWORLD));
            }

            world.save(null, true, true);
            try {
                world.close();
            } catch (IOException exception) {
                logger.error("Closing dimension '" + world.getRegistryKey() + "' failed with an exception", exception);
                return;
            }
    }

    private static BlockPos findEndPortal(ServerWorld world) {
        BlockPos exitPortalLocation = null;
        int i;
        int l;
        for(i = -8; i <= 8; ++i) {
            for(l = -8; l <= 8; ++l) {
                WorldChunk worldChunk = world.getChunk(i, l);
                Iterator var4 = worldChunk.getBlockEntities().values().iterator();

                while(var4.hasNext()) {
                    BlockEntity blockEntity = (BlockEntity)var4.next();
                    if (blockEntity instanceof EndPortalBlockEntity) {
                        BlockPattern.Result result = END_PORTAL_PATTERN.searchAround(world, blockEntity.getPos());
                        if (result != null) {
                            BlockPos blockPos = result.translate(3, 3, 3).getBlockPos();
                            if (blockPos.getX() == 0 && blockPos.getZ() == 0) {
                                exitPortalLocation = blockPos;
                                return exitPortalLocation;
                            }
                        }
                    }
                }
            }
        }

        i = world.getTopPosition(Heightmap.Type.MOTION_BLOCKING, EndPortalFeature.ORIGIN).getY();

        for(l = i; l >= 0; --l) {
            BlockPattern.Result result2 = END_PORTAL_PATTERN.searchAround(world, new BlockPos(EndPortalFeature.ORIGIN.getX(), l, EndPortalFeature.ORIGIN.getZ()));
            if (result2 != null) {
                exitPortalLocation = result2.translate(3, 3, 3).getBlockPos();
                return exitPortalLocation;
            }
        }

        return null;
    }


    private static void destroyWorldChunks(MinecraftServer server, ServerWorld world) {
            RegistryKey<World> registryKey = world.getRegistryKey();

            MinecraftServerAccessor minecraftServerAccessor = (MinecraftServerAccessor) server;
            LevelStorage.Session session = minecraftServerAccessor.getSession();

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
            BlockPos exitPortalLocation = findEndPortal(alternateEnd);
            dragonFightTagData.putBoolean("DragonKilled", false);
            dragonFightTagData.putBoolean("PreviouslyKilled", false);
            if(exitPortalLocation != null) {
                dragonFightTagData.put("ExitPortalLocation", NbtHelper.fromBlockPos(exitPortalLocation));
            }

            EnderDragonFight alternateEndDragonFightData = new EnderDragonFight(alternateEnd, server.getSaveProperties().getGeneratorOptions().getSeed(), dragonFightTagData);
            EndDragonFightAccessor alternateEndDragonFight = (EndDragonFightAccessor) alternateEnd;
            alternateEndDragonFightData.resetEndCrystals();
            alternateEndDragonFightData.respawnDragon();
            alternateEndDragonFightData.tick();

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

    @NotNull
    private static WorldGenerationProgressListener emptyProgressListener() {
        return new WorldGenerationProgressListener() {
            @Override
            public void start(ChunkPos spawnPos) {
            }

            @Override
            public void setChunkStatus(ChunkPos pos, @Nullable ChunkStatus status) {
            }

            @Override
            public void stop() {
            }
        };
    }

}
