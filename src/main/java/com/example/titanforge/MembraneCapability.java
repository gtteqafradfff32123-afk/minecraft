package com.example.titanforge;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.DamageSource;
import net.minecraft.util.Direction;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityInject;
import net.minecraftforge.common.capabilities.ICapabilitySerializable;
import net.minecraftforge.common.util.LazyOptional;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class MembraneCapability {
    @CapabilityInject(MembraneData.class)
    public static final Capability<MembraneData> CAP = null;

    public static class MembraneData {
        private float stored = 0.0F;
        private int stacks = 0;
        private int burstTimer = 0;

        public void store(float amount) {
            this.stored += amount;
            this.stacks++;
        }

        public float getStored() { return stored; }
        public int getStacks() { return stacks; }

        public void startBurstTimer(int ticks) {
            this.burstTimer = ticks;
        }

        public boolean tickBurst(PlayerEntity player) {
            if (burstTimer <= 0) return false;
            if (--burstTimer == 0) {
                if (!player.world.isRemote) {
                    player.attackEntityFrom(ModDamageSources.MEMBRANE_BURST, stored * 0.8F);
                }
                reset();
                return true;
            }
            return false;
        }

        public void reset() {
            stored = 0.0F;
            stacks = 0;
            burstTimer = 0;
        }

        public CompoundNBT serialize() {
            CompoundNBT tag = new CompoundNBT();
            tag.putFloat("stored", stored);
            tag.putInt("stacks", stacks);
            tag.putInt("burstTimer", burstTimer);
            return tag;
        }

        public void deserialize(CompoundNBT tag) {
            stored = tag.getFloat("stored");
            stacks = tag.getInt("stacks");
            burstTimer = tag.getInt("burstTimer");
        }
    }

    public static class Provider implements ICapabilitySerializable<CompoundNBT> {
        private final MembraneData data = new MembraneData();
        private final LazyOptional<MembraneData> opt = LazyOptional.of(() -> data);

        @Nonnull
        @Override
        public <T> LazyOptional<T> getCapability(@Nonnull Capability<T> cap, @Nullable Direction side) {
            return CAP == cap ? opt.cast() : LazyOptional.empty();
        }

        @Override
        public CompoundNBT serializeNBT() {
            return data.serialize();
        }

        @Override
        public void deserializeNBT(CompoundNBT tag) {
            data.deserialize(tag);
        }

        public MembraneData getData() { return data; }
    }
}
