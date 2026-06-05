package com.pavel.ironcore.capability;

import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;
import net.minecraftforge.common.capabilities.ICapabilitySerializable;
import net.minecraftforge.common.util.LazyOptional;

public class SuitCapabilityProvider implements ICapabilitySerializable<CompoundTag> {
    public static Capability<SuitCapability> SUIT_CAPABILITY = CapabilityManager.get(new CapabilityToken<>() {});

    private SuitCapability suitCapability = null;
    private final LazyOptional<SuitCapability> optional = LazyOptional.of(this::createSuitCapability);

    private SuitCapability createSuitCapability() {
        if (this.suitCapability == null) {
            this.suitCapability = new SuitCapability();
        }
        return this.suitCapability;
    }

    @Override
    public <T> LazyOptional<T> getCapability(Capability<T> cap, Direction side) {
        if (cap == SUIT_CAPABILITY) {
            return optional.cast();
        }
        return LazyOptional.empty();
    }

    @Override
    public CompoundTag serializeNBT() {
        CompoundTag nbt = new CompoundTag();
        createSuitCapability().saveNBTData(nbt);
        return nbt;
    }

    @Override
    public void deserializeNBT(CompoundTag nbt) {
        createSuitCapability().loadNBTData(nbt);
    }
}
