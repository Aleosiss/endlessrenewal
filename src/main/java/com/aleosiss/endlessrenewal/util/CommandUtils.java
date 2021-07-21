package com.aleosiss.endlessrenewal.util;

import com.aleosiss.endlessrenewal.EndlessRenewal;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.fabricmc.fabric.api.command.v1.CommandRegistrationCallback;
import net.fabricmc.fabric.api.dimension.v1.FabricDimensions;
import net.minecraft.command.CommandException;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.LiteralText;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.registry.RegistryKey;
import net.minecraft.world.TeleportTarget;
import net.minecraft.world.World;

import static net.minecraft.server.command.CommandManager.literal;

public class CommandUtils {

    public void registerCommands() {
        CommandRegistrationCallback.EVENT.register((dispatcher, dedicated) -> dispatcher.register(literal("er_goto").executes(CommandUtils.this::swapTargeted)));
        CommandRegistrationCallback.EVENT.register((dispatcher, dedicated) -> dispatcher.register(literal("er_init").executes(CommandUtils.this::alternateEndInit)));
        CommandRegistrationCallback.EVENT.register((dispatcher, dedicated) -> dispatcher.register(literal("er_getdimension").executes(CommandUtils.this::getCurrentDimension)));
        CommandRegistrationCallback.EVENT.register((dispatcher, dedicated) -> dispatcher.register(literal("er_toggleactive").executes(CommandUtils.this::toggleModActive)));
        CommandRegistrationCallback.EVENT.register((dispatcher, dedicated) -> dispatcher.register(literal("er_rebuild").executes(CommandUtils.this::rebuildEnd)));
    }

    private int rebuildEnd(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerPlayerEntity player = context.getSource().getPlayer();
        MinecraftServer server = context.getSource().getMinecraftServer();
        ServerWorld modWorld = getModWorld(context);
        ERUtils.resetAlternateEnd(server, modWorld);

        return 1;
    }

    private int swapTargeted(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerPlayerEntity player = context.getSource().getPlayer();
        ServerWorld serverWorld = player.getServerWorld();
        ServerWorld modWorld = getModWorld(context);

        if (serverWorld != modWorld) {
            player.moveToWorld(modWorld);

            if (player.world != modWorld) {
                throw new CommandException(new LiteralText("Teleportation failed!"));
            }
        } else {
            TeleportTarget target = new TeleportTarget(new Vec3d(0, 100, 0), Vec3d.ZERO,
                    (float) Math.random() * 360 - 180, (float) Math.random() * 360 - 180);
            FabricDimensions.teleport(player, getWorld(context, World.OVERWORLD), target);
        }

        return 1;
    }

    private int toggleModActive(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        EndlessRenewal.MOD_ACTIVE = !EndlessRenewal.MOD_ACTIVE;

        ServerPlayerEntity player = context.getSource().getPlayer();
        player.sendSystemMessage(new LiteralText("EndlessRenewal MOD_ACTIVE is " + EndlessRenewal.MOD_ACTIVE), Util.NIL_UUID);
        return 1;
    }

    private int getCurrentDimension(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerPlayerEntity player = context.getSource().getPlayer();
        Identifier worldId = player.getServerWorld().getRegistryKey().getValue();

        player.sendSystemMessage(new LiteralText("Currently in dimension with type: " + worldId), Util.NIL_UUID);

        return 1;
    }

    private int alternateEndInit(CommandContext<ServerCommandSource> context) {
        MinecraftServer server = context.getSource().getMinecraftServer();
        ServerWorld alternateEnd = getModWorld(context);

        EndlessRenewal.init(server, alternateEnd);

        return 1;
    }

    private ServerWorld getModWorld(CommandContext<ServerCommandSource> context) {
        return getWorld(context, EndlessRenewal.WORLD_KEY);
    }

    private ServerWorld getWorld(CommandContext<ServerCommandSource> context, RegistryKey<World> dimensionRegistryKey) {
        return context.getSource().getMinecraftServer().getWorld(dimensionRegistryKey);
    }
}

