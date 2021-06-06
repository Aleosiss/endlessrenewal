package com.aleosiss.endlessrenewal.mixin;

import com.aleosiss.endlessrenewal.EndlessRenewal;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.registry.RegistryKey;
import net.minecraft.world.Heightmap;
import net.minecraft.world.TeleportTarget;
import net.minecraft.world.World;
import org.apache.logging.log4j.Logger;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(ServerPlayerEntity.class)
public abstract class EntityTeleportationMixin extends Entity {
    public EntityTeleportationMixin(EntityType<?> type, World world) {
        super(type, world);
    }

    @ModifyVariable(method = "moveToWorld", at = @At("STORE"))
    private TeleportTarget ModifyEntityTeleportationLogic(TeleportTarget teleportTarget, ServerWorld destination) {
        RegistryKey<World> currentWorldRegKey = this.world.getRegistryKey();
        boolean leavingAlternateEnd =  currentWorldRegKey == EndlessRenewal.WORLD_KEY && destination.getRegistryKey() == World.OVERWORLD;
        boolean enteringAlternateEnd = destination.getRegistryKey() == World.END || destination.getRegistryKey() == EndlessRenewal.WORLD_KEY;

        BlockPos blockPos;
        if(enteringAlternateEnd) {
            ServerWorld.createEndSpawnPlatform(destination);
            blockPos = ServerWorld.END_SPAWN_POS;
            TeleportTarget tempTarget = new TeleportTarget(new Vec3d((double)blockPos.getX() + 0.5D, blockPos.getY(), (double)blockPos.getZ() + 0.5D), this.getVelocity(), this.yaw, this.pitch);
            Vec3d vec3d = tempTarget.position.add(0.0D, -1.0D, 0.0D);
            return new TeleportTarget(vec3d, Vec3d.ZERO, 90.0F, 0.0F);
        }

        if(leavingAlternateEnd) {
            blockPos = destination.getTopPosition(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, destination.getSpawnPos());
            return new TeleportTarget(new Vec3d((double)blockPos.getX() + 0.5D, blockPos.getY(), (double)blockPos.getZ() + 0.5D), this.getVelocity(), this.yaw, this.pitch);
        }

        return teleportTarget;
    }
}
