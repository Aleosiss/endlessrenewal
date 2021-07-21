package com.aleosiss.endlessrenewal;

import com.aleosiss.endlessrenewal.data.EndlessRenewalState;
import com.aleosiss.endlessrenewal.mixin.EndDragonFightAccessor;
import com.aleosiss.endlessrenewal.util.CommandUtils;
import com.aleosiss.endlessrenewal.util.ERUtils;
import com.aleosiss.endlessrenewal.world.AlternateEndChunkGenerator;
import com.mojang.brigadier.context.CommandContext;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.dimension.v1.FabricDimensions;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.entity.Entity;
import net.minecraft.entity.boss.dragon.EnderDragonFight;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.registry.Registry;
import net.minecraft.util.registry.RegistryKey;
import net.minecraft.world.TeleportTarget;
import net.minecraft.world.World;
import net.minecraft.world.dimension.DimensionOptions;
import net.minecraft.world.dimension.DimensionType;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.function.Supplier;

import static net.minecraft.entity.EntityType.COW;

public class EndlessRenewal implements ModInitializer {
    private static final Logger logger = LogManager.getLogger();
    public static final String MOD_ID = "endless_renewal";

    // The dimension options refer to the JSON-file in the dimension subfolder of the datapack,
    // which will always share it's ID with the world that is created from it
    public static final Identifier                     DIMENSION_IDENTIFER         = new Identifier(MOD_ID, "alternate_end");
    public static final Identifier                     DIMENSION_TYPE_IDENTIFER    = new Identifier(MOD_ID, "alternate_end_type");
    public static final RegistryKey<DimensionOptions>  DIMENSION_KEY               = RegistryKey.of(Registry.DIMENSION_OPTIONS, DIMENSION_IDENTIFER);
    public static final RegistryKey<World>             WORLD_KEY                   = RegistryKey.of(Registry.DIMENSION, DIMENSION_KEY.getValue());
    public static final RegistryKey<DimensionType>     DIMENSION_TYPE_KEY          = RegistryKey.of(Registry.DIMENSION_TYPE_KEY, DIMENSION_TYPE_IDENTIFER);

    // our persistent data
    public static EndlessRenewalState STATE;

    // did we kill the real ender dragon?
    public static boolean MOD_ACTIVE;

    // is the alternate end ready for us to fight the dragon in?
    public static boolean ALTERATE_END_READY;

    public static CommandUtils commandUtils = new CommandUtils();

    private static void onServerStarted(MinecraftServer server) {
        logger.info("Endless Renewal caught the server start!");

        ServerWorld overworld = server.getWorld(World.OVERWORLD);
        ServerWorld end = server.getWorld(World.END);
        ServerWorld alternateEnd = server.getWorld(WORLD_KEY);
        Supplier<EndlessRenewalState> stateSupplier = () -> new EndlessRenewalState(server, MOD_ID);
        STATE = alternateEnd.getPersistentStateManager().getOrCreate(stateSupplier, MOD_ID);

        EnderDragonFight enderDragonFight = end.getEnderDragonFight();
        if(enderDragonFight == null) {
            logger.warn("The original enderdragon fight was not detected.");
        }

        enderDragonFight = alternateEnd.getEnderDragonFight();
        if(STATE.getEnderDragonFight() == null && enderDragonFight == null) {
            logger.warn("The alternate enderdragon fight was not detected.");
            init(server, alternateEnd);
        } else if(STATE.getEnderDragonFight() != null) {
            logger.info("Setting alternate ender dragon fight from save.");
            EndDragonFightAccessor alternateEndDragonFight = (EndDragonFightAccessor) alternateEnd;
            alternateEndDragonFight.setEnderDragonFight(STATE.getEnderDragonFight());
        }


        testDimension(overworld, alternateEnd);

        MOD_ACTIVE = true;
        //MOD_ACTIVE = enderDragonFight.hasPreviouslyKilled();
    }

    private static void testDimension(ServerWorld overworld, ServerWorld targetWorld) {
        if (targetWorld == null) {
            throw new AssertionError("AlternateEnd doesn't exist.");
        }

        Entity entity = COW.create(overworld);
        if (!entity.world.getRegistryKey().equals(World.OVERWORLD)) {
            throw new AssertionError("Entity starting world isn't the overworld");
        }

        TeleportTarget target = new TeleportTarget(Vec3d.ZERO, new Vec3d(1, 70, 1), 45f, 60f);

        Entity teleported = FabricDimensions.teleport(entity, targetWorld, target);

        if (teleported == null) {
            throw new AssertionError("Entity didn't teleport");
        }

        if (!teleported.world.getRegistryKey().equals(WORLD_KEY)) {
            throw new AssertionError("Target world not reached.");
        }

        if (!teleported.getPos().equals(target.position)) {
            throw new AssertionError("Target Position not reached.");
        }
    }

    @Override
    public void onInitialize() {
        Registry.register(Registry.CHUNK_GENERATOR, DIMENSION_IDENTIFER, AlternateEndChunkGenerator.CODEC);

        ServerLifecycleEvents.SERVER_STARTED.register(EndlessRenewal::onServerStarted);

        commandUtils.registerCommands();
    }



    public static void init(MinecraftServer server, ServerWorld alternateEnd) {
        boolean requiresReset = false;

        requiresReset = ERUtils.doesServerWorldNeedDragonFightReset(alternateEnd);
        if(requiresReset) {
            ERUtils.resetAlternateEnd(server, alternateEnd);
        }
    }

    private static ServerWorld getModWorld(CommandContext<ServerCommandSource> context) {
        return getWorld(context, WORLD_KEY);
    }

    private static ServerWorld getWorld(CommandContext<ServerCommandSource> context, RegistryKey<World> dimensionRegistryKey) {
        return context.getSource().getMinecraftServer().getWorld(dimensionRegistryKey);
    }
}
