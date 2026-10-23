package com.pavel.ironcore.item.module;

import com.pavel.ironcore.capability.SuitCapability;
import com.pavel.ironcore.item.BaseSuitItem;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * Хранение модулей в NBT нагрудника и их эффекты. Модули живут в самом нагруднике, поэтому
 * переезжают вместе с костюмом (сундук, смерть, другой игрок).
 */
public final class SuitModules {
    public static final int MAX_SLOTS = 3;
    private static final String TAG = "IronCoreModules";

    private static final int THERMAL_COST = 4;      // FE/тик, пока игрок горит или в лаве
    private static final int COOLANT_COST = 1;      // FE/тик, пока костюм горячий
    private static final int REBREATHER_COST = 2;   // FE/тик под водой
    private static final int REPAIR_COST = 50;      // FE за 1 ед. прочности
    private static final int RADAR_COST = 20;       // FE за скан (раз в секунду)
    private static final double RADAR_RANGE = 24.0;

    private SuitModules() { }

    /** Слотов модулей у нагрудника: Mk1 — 0, Mk2 — 1, Mk3 — 3. */
    public static int slotsFor(ItemStack chest) {
        if (chest.getItem() instanceof BaseSuitItem suit) {
            return switch (suit.getTierId()) {
                case "mk2" -> 1;
                case "mk3" -> 3;
                default -> 0;
            };
        }
        return 0;
    }

    public static ItemStack getModuleStack(ItemStack chest, int slot) {
        CompoundTag root = chest.getTag();
        if (root == null || !root.contains(TAG, Tag.TAG_LIST)) return ItemStack.EMPTY;
        ListTag list = root.getList(TAG, Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag entry = list.getCompound(i);
            if (entry.getByte("Slot") == slot) return ItemStack.of(entry);
        }
        return ItemStack.EMPTY;
    }

    public static void setModuleStack(ItemStack chest, int slot, ItemStack module) {
        CompoundTag root = chest.getOrCreateTag();
        ListTag list = root.getList(TAG, Tag.TAG_COMPOUND);
        list.removeIf(t -> ((CompoundTag) t).getByte("Slot") == slot);
        if (!module.isEmpty()) {
            CompoundTag entry = module.save(new CompoundTag());
            entry.putByte("Slot", (byte) slot);
            list.add(entry);
        }
        if (list.isEmpty()) {
            root.remove(TAG);
            if (root.isEmpty()) chest.setTag(null);
        } else {
            root.put(TAG, list);
        }
    }

    public static List<SuitModule> installed(ItemStack chest) {
        List<SuitModule> result = new ArrayList<>();
        int slots = slotsFor(chest);
        for (int i = 0; i < slots; i++) {
            if (getModuleStack(chest, i).getItem() instanceof ModuleItem item) result.add(item.getModule());
        }
        return result;
    }

    public static boolean has(ItemStack chest, SuitModule module) {
        return installed(chest).contains(module);
    }

    /** Эффекты установленных модулей. Вызывается каждый серверный тик при надетом полном костюме. */
    public static void tick(ServerPlayer player, SuitCapability suit, ItemStack chest) {
        WeaponModules.tickCooldowns(suit);
        List<SuitModule> modules = installed(chest);
        if (modules.isEmpty()) return;

        if (modules.contains(SuitModule.THERMAL_SHIELD) && (player.isOnFire() || player.isInLava())
                && spend(suit, THERMAL_COST)) {
            player.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 40, 0, false, false, true));
            // Гасит нагрев от огня/лавы (у Mk3 +0.5/тик)
            suit.setHeat(suit.getHeat() - 0.45f);
        }

        if (modules.contains(SuitModule.COOLANT) && suit.getHeat() > 0 && spend(suit, COOLANT_COST)) {
            suit.setHeat(suit.getHeat() - 0.03f);
        }

        if (modules.contains(SuitModule.REBREATHER) && player.isEyeInFluid(FluidTags.WATER)
                && spend(suit, REBREATHER_COST)) {
            player.setAirSupply(player.getMaxAirSupply());
        }

        if (modules.contains(SuitModule.AUTO_REPAIR) && player.tickCount % 40 == 0) {
            for (EquipmentSlot slot : new EquipmentSlot[] {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
                ItemStack piece = player.getItemBySlot(slot);
                if (piece.isDamaged() && spend(suit, REPAIR_COST)) {
                    piece.setDamageValue(piece.getDamageValue() - 1);
                }
            }
        }

        if (modules.contains(SuitModule.RADAR) && player.tickCount % 20 == 0 && spend(suit, RADAR_COST)) {
            for (Monster monster : player.level().getEntitiesOfClass(Monster.class, player.getBoundingBox().inflate(RADAR_RANGE),
                    m -> m.distanceToSqr(player) <= RADAR_RANGE * RADAR_RANGE)) {
                monster.addEffect(new MobEffectInstance(MobEffects.GLOWING, 30, 0, false, false, false));
            }
        }
    }

    private static boolean spend(SuitCapability suit, int cost) {
        if (suit.getEnergy() < cost) return false;
        suit.setEnergy(suit.getEnergy() - cost);
        return true;
    }
}
