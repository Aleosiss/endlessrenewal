package com.aleosiss.endlessrenewal.data;

import com.aleosiss.endlessrenewal.EndlessRenewal;
import net.minecraft.entity.boss.dragon.EnderDragonFight;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.PersistentState;

public class EndlessRenewalState extends PersistentState {
    private EnderDragonFight enderDragonFight;
    private final ServerWorld world;

    private static final String ALTERNATE_END_DRAGON_FIGHT_KEY = "alternateEndDragonFight";

    @Override
    public void fromTag(CompoundTag tag) {
        CompoundTag dragonData = (CompoundTag) tag.get(ALTERNATE_END_DRAGON_FIGHT_KEY);

        enderDragonFight = new EnderDragonFight(world, world.getSeed(), dragonData);
    }

    @Override
    public CompoundTag toTag(CompoundTag tag) {
        if(enderDragonFight != null) {
            tag.put(ALTERNATE_END_DRAGON_FIGHT_KEY, enderDragonFight.toTag());
        }

        return tag;
    }

    public EndlessRenewalState(MinecraftServer server, String key) {
        super(key);

        world = server.getWorld(EndlessRenewal.WORLD_KEY);
    }

    public EnderDragonFight getEnderDragonFight() {
        return enderDragonFight;
    }

    public void setEnderDragonFight(EnderDragonFight enderDragonFight) {
        this.enderDragonFight = enderDragonFight;
    }

    @Override
    public boolean isDirty() {
        // TODO: Improve this logic
        return true;
    }
}
