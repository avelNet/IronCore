package com.pavel.ironcore.capability;

import net.minecraft.nbt.CompoundTag;
import net.minecraftforge.common.util.INBTSerializable;

public class SuitCapability implements INBTSerializable<CompoundTag> {
    private int energy = 0;
    private int maxEnergy = 10000;
    private int frameDurability = 100;
    private int maxFrameDurability = 100;
    private String suitTier = "none";
    private float heat = 0.0f;
    private int palladiumPoisoning = 0;

    public int getEnergy() { return energy; }
    public void setEnergy(int energy) { this.energy = Math.max(0, Math.min(energy, maxEnergy)); }

    public int getMaxEnergy() { return maxEnergy; }
    public void setMaxEnergy(int maxEnergy) { this.maxEnergy = maxEnergy; }

    public int getFrameDurability() { return frameDurability; }
    public void setFrameDurability(int durability) { this.frameDurability = Math.max(0, Math.min(durability, maxFrameDurability)); }

    public String getSuitTier() { return suitTier; }
    public void setSuitTier(String tier) { this.suitTier = tier; }

    public float getHeat() { return heat; }
    public void setHeat(float heat) { this.heat = heat; }

    public int getPalladiumPoisoning() { return palladiumPoisoning; }
    public void setPalladiumPoisoning(int level) { this.palladiumPoisoning = level; }

    public void saveNBTData(CompoundTag nbt) {
        nbt.putInt("energy", energy);
        nbt.putInt("maxEnergy", maxEnergy);
        nbt.putInt("durability", frameDurability);
        nbt.putInt("maxDurability", maxFrameDurability);
        nbt.putString("tier", suitTier);
        nbt.putFloat("heat", heat);
        nbt.putInt("poisoning", palladiumPoisoning);
    }

    public void loadNBTData(CompoundTag nbt) {
        energy = nbt.getInt("energy");
        maxEnergy = nbt.getInt("maxEnergy");
        frameDurability = nbt.getInt("durability");
        maxFrameDurability = nbt.getInt("maxDurability");
        suitTier = nbt.getString("tier");
        heat = nbt.getFloat("heat");
        palladiumPoisoning = nbt.getInt("poisoning");
    }

    @Override
    public CompoundTag serializeNBT() {
        CompoundTag nbt = new CompoundTag();
        saveNBTData(nbt);
        return nbt;
    }

    @Override
    public void deserializeNBT(CompoundTag nbt) {
        loadNBTData(nbt);
    }
}
