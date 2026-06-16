package com.pavel.ironcore.block.entity;

import net.minecraftforge.energy.EnergyStorage;

/**
 * EnergyStorage that notifies a callback (the owning BlockEntity's setChanged())
 * whenever receiveEnergy/extractEnergy is invoked, so the block gets marked dirty for saving.
 */
public class NotifyingEnergyStorage extends EnergyStorage {

    private final Runnable onChange;

    public NotifyingEnergyStorage(int capacity, int maxReceive, int maxExtract, Runnable onChange) {
        super(capacity, maxReceive, maxExtract);
        this.onChange = onChange;
    }

    @Override
    public int receiveEnergy(int maxReceive, boolean simulate) {
        onChange.run();
        return super.receiveEnergy(maxReceive, simulate);
    }

    @Override
    public int extractEnergy(int maxExtract, boolean simulate) {
        onChange.run();
        return super.extractEnergy(maxExtract, simulate);
    }

    public void setEnergyDirect(int amount) {
        this.energy = Math.max(0, Math.min(amount, capacity));
    }
}
