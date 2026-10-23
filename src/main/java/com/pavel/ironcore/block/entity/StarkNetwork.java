package com.pavel.ironcore.block.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Stark Network: зона вокруг Arc Reactor Core, где станки работают быстрее.
 * Реестр загруженных ядер по измерениям (только сервер) — чтобы машинам не сканировать
 * 33³ блоков, а проверить расстояние до нескольких известных ядер.
 */
public final class StarkNetwork {
    private static final Map<ResourceKey<Level>, Set<BlockPos>> CORES = new HashMap<>();

    private StarkNetwork() { }

    static void register(Level level, BlockPos pos) {
        if (level.isClientSide) return;
        CORES.computeIfAbsent(level.dimension(), k -> new HashSet<>()).add(pos.immutable());
    }

    static void unregister(Level level, BlockPos pos) {
        if (level.isClientSide) return;
        Set<BlockPos> cores = CORES.get(level.dimension());
        if (cores != null) cores.remove(pos);
    }

    /** Есть ли рядом запитанное ядро. Заодно чистит записи о ядрах, которых уже нет. */
    public static boolean isCovered(Level level, BlockPos pos) {
        Set<BlockPos> cores = CORES.get(level.dimension());
        if (cores == null || cores.isEmpty()) return false;
        double rangeSq = ArcReactorCoreBlockEntity.RANGE * ArcReactorCoreBlockEntity.RANGE;
        boolean covered = false;
        var it = cores.iterator();
        while (it.hasNext()) {
            BlockPos core = it.next();
            if (!level.isLoaded(core)) continue;
            if (!(level.getBlockEntity(core) instanceof ArcReactorCoreBlockEntity reactor)) {
                it.remove();
                continue;
            }
            if (!covered && core.distSqr(pos) <= rangeSq && reactor.isPowered()) covered = true;
        }
        return covered;
    }
}
