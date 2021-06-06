package com.aleosiss.endlessrenewal.mixin;

import com.aleosiss.endlessrenewal.EndlessRenewal;
import net.minecraft.block.BlockState;
import net.minecraft.block.BlockWithEntity;
import net.minecraft.block.EndPortalBlock;
import net.minecraft.entity.Entity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.registry.RegistryKey;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(EndPortalBlock.class)
public abstract class EndPortalMixin extends BlockWithEntity {
    protected EndPortalMixin(Settings settings) {
        super(settings);
    }

    @ModifyVariable(method = "onEntityCollision", at = @At("STORE"))
    private ServerWorld modifyTeleportationLogic(ServerWorld serverWorld, BlockState state, World world, BlockPos pos, Entity entity) {
        if(!EndlessRenewal.MOD_ACTIVE) {
            return serverWorld;
        }

        // don't modify teleportation logic if we're not a player or we've already killed the ender dragon
        if(entity instanceof ServerPlayerEntity spe) {
            if(spe.getAdvancementTracker().getProgress(world.getServer().getAdvancementLoader().get(Identifier.tryParse("end/kill_dragon"))).isDone()) {
                return serverWorld;
            }
        } else {
            return serverWorld;
        }

        // if we're in the alternate_end, get the overworld key
        // if we're in the overworld, get the alternate_end key
        RegistryKey<World> targetWorldKey = world.getRegistryKey() == EndlessRenewal.WORLD_KEY ? World.OVERWORLD : EndlessRenewal.WORLD_KEY;
        ServerWorld targetWorld = ((ServerWorld)world).getServer().getWorld(targetWorldKey);

        return targetWorld;
    }
}
