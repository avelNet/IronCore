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
    private float palladiumPoisoning = 0.0f;
    private String activeReactorType = "none";
    private float icingLevel = 0.0f;
    private boolean isFlying = false;
    private double failureYPos = -1.0; 
    private boolean isBoostKeyHeld = false; 

    public int getEnergy() { return energy; }
    public void setEnergy(int energy) { this.energy = Math.max(0, Math.min(energy, maxEnergy)); }

    public int getMaxEnergy() { return maxEnergy; }
    public void setMaxEnergy(int maxEnergy) { this.maxEnergy = maxEnergy; }

    public int getFrameDurability() { return frameDurability; }
    public void setFrameDurability(int durability) { this.frameDurability = Math.max(0, Math.min(durability, maxFrameDurability)); }

    public String getSuitTier() { return suitTier; }
    public void setSuitTier(String tier) { this.suitTier = tier; }

    public float getHeat() { return heat; }
    public void setHeat(float heat) { this.heat = Math.max(0.0f, Math.min(heat, 100.0f)); }

    public float getPalladiumPoisoning() { return palladiumPoisoning; }
    public void setPalladiumPoisoning(float level) { this.palladiumPoisoning = Math.max(0.0f, Math.min(level, 100.0f)); }

    public String getActiveReactorType() { return activeReactorType; }
    public void setActiveReactorType(String type) { this.activeReactorType = type; }

    public float getIcingLevel() { return icingLevel; }
    public void setIcingLevel(float level) { this.icingLevel = Math.max(0.0f, Math.min(level, 100.0f)); }

    public boolean isFlying() { return isFlying; }
    public void setFlying(boolean flying) { this.isFlying = flying; }

    public double getFailureYPos() { return failureYPos; }
    public void setFailureYPos(double yPos) { this.failureYPos = yPos; }

    public boolean isBoostKeyHeld() { return isBoostKeyHeld; }
    public void setBoostKeyHeld(boolean held) { this.isBoostKeyHeld = held; }

    public void saveNBTData(CompoundTag nbt) {
        nbt.putInt("energy", energy);
        nbt.putInt("maxEnergy", maxEnergy);
        nbt.putInt("durability", frameDurability);
        nbt.putInt("maxDurability", maxFrameDurability);
        nbt.putString("tier", suitTier);
        nbt.putFloat("heat", heat);
        nbt.putFloat("poisoning", palladiumPoisoning);
        nbt.putString("reactor", activeReactorType);
        nbt.putFloat("icing", icingLevel);
        nbt.putBoolean("flying", isFlying);
        nbt.putDouble("failureY", failureYPos);
    }

    public void loadNBTData(CompoundTag nbt) {
        energy = nbt.getInt("energy");
        maxEnergy = nbt.getInt("maxEnergy");
        frameDurability = nbt.getInt("durability");
        maxFrameDurability = nbt.getInt("maxDurability");
        suitTier = nbt.getString("tier");
        heat = nbt.getFloat("heat");
        palladiumPoisoning = nbt.getFloat("poisoning");
        activeReactorType = nbt.getString("reactor");
        icingLevel = nbt.getFloat("icing");
        isFlying = nbt.getBoolean("flying");
        failureYPos = nbt.getDouble("failureY");
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
