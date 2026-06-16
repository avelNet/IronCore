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
    private int flightTimer = 0;
    private boolean autoBoostEnabled = true;
    private boolean isTurbo = false;
    private boolean wasFlyingHorizontally = false;
    private boolean hasEmbeddedReactor = false;
    private boolean isFirstNightTriggered = false;
    private int cinematicStage = 0; // 0-none, 1-ambush, 2-critical, 3-completed
    private boolean isMaskOpen = false;
    private int waterExitCooldown = 0;
    private boolean jarvisUnlocked = false;

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

    public int getFlightTimer() { return flightTimer; }
    public void setFlightTimer(int timer) { this.flightTimer = timer; }

    public boolean isAutoBoostEnabled() { return autoBoostEnabled; }
    public void setAutoBoostEnabled(boolean enabled) { this.autoBoostEnabled = enabled; }

    public boolean isTurbo() { return isTurbo; }
    public void setTurbo(boolean turbo) { this.isTurbo = turbo; }

    public boolean wasFlyingHorizontally() { return wasFlyingHorizontally; }
    public void setWasFlyingHorizontally(boolean wasFlyingHorizontally) { this.wasFlyingHorizontally = wasFlyingHorizontally; }

    public boolean hasEmbeddedReactor() { return hasEmbeddedReactor; }
    public void setEmbeddedReactor(boolean embedded) { this.hasEmbeddedReactor = embedded; }

    public boolean isFirstNightTriggered() { return isFirstNightTriggered; }
    public void setFirstNightTriggered(boolean triggered) { this.isFirstNightTriggered = triggered; }

    public int getCinematicStage() { return cinematicStage; }
    public void setCinematicStage(int stage) { this.cinematicStage = stage; }

    public boolean isMaskOpen() { return isMaskOpen; }
    public void setMaskOpen(boolean open) { this.isMaskOpen = open; }

    public int getWaterExitCooldown() { return waterExitCooldown; }
    public void setWaterExitCooldown(int cooldown) { this.waterExitCooldown = cooldown; }

    public boolean isJarvisUnlocked() { return jarvisUnlocked; }
    public void setJarvisUnlocked(boolean unlocked) { this.jarvisUnlocked = unlocked; }

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
        nbt.putBoolean("autoBoost", autoBoostEnabled);
        nbt.putBoolean("embeddedReactor", hasEmbeddedReactor);
        nbt.putBoolean("firstNightTriggered", isFirstNightTriggered);
        nbt.putInt("cinematicStage", cinematicStage);
        nbt.putBoolean("maskOpen", isMaskOpen);
        nbt.putBoolean("jarvisUnlocked", jarvisUnlocked);
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
        if (nbt.contains("autoBoost")) {
            autoBoostEnabled = nbt.getBoolean("autoBoost");
        }
        if (nbt.contains("embeddedReactor")) {
            hasEmbeddedReactor = nbt.getBoolean("embeddedReactor");
        }
        if (nbt.contains("firstNightTriggered")) {
            isFirstNightTriggered = nbt.getBoolean("firstNightTriggered");
        }
        if (nbt.contains("cinematicStage")) {
            cinematicStage = nbt.getInt("cinematicStage");
        }
        if (nbt.contains("maskOpen")) {
            isMaskOpen = nbt.getBoolean("maskOpen");
        }
        if (nbt.contains("jarvisUnlocked")) {
            jarvisUnlocked = nbt.getBoolean("jarvisUnlocked");
        }
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
