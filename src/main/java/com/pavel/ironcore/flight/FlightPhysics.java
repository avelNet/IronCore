package com.pavel.ironcore.flight;

import net.minecraft.world.phys.Vec3;

/**
 * Shared velocity math for suit flight, extracted from the formerly
 * copy-pasted per-tier logic in Mk2FrameItem/Mk3FrameItem. Pure functions only -
 * energy/heat/icing bookkeeping and mode selection stay in the frame items.
 */
public final class FlightPhysics {
    private FlightPhysics() {}

    public static Vec3 computeBoostVelocity(Vec3 current, Vec3 look, FlightConfig config, boolean turbo) {
        double maxSpeed = turbo ? config.turboMaxSpeed() : config.boostMaxSpeed();
        double accelCurve = turbo ? config.turboAccelCurve() : config.boostAccelCurve();

        double speedRatio = Math.min(current.length() / maxSpeed, 1.0);
        double acceleration = config.boostAccelBase() + Math.pow(speedRatio, 2.0) * accelCurve;

        Vec3 target = look.scale(maxSpeed);
        boolean descending = target.y <= 0;
        double targetY = descending
                ? target.y * config.verticalDownMultiplier()
                : target.y * config.verticalUpMultiplier() + config.verticalUpOffset();

        double newY = current.y + (targetY - current.y) * acceleration;
        if (descending) {
            // Same vanilla 0.6x-per-tick Y damping as the hover dive - boosting downward was
            // never separately compensated for it, so looking down while boosting converged to
            // a steady state far weaker than verticalDownMultiplier intended. The ascending
            // branch is left alone since verticalUpMultiplier/Offset were already hand-tuned
            // against the uncompensated damping.
            newY /= VANILLA_FLYING_Y_DAMPING;
        }

        return new Vec3(
                current.x + (target.x - current.x) * acceleration,
                newY,
                current.z + (target.z - current.z) * acceleration
        );
    }

    public static Vec3 computeHoverVelocity(Vec3 current, FlightConfig config, boolean descending) {
        double horizontalSpeed = Math.sqrt(current.x * current.x + current.z * current.z);
        double x = current.x;
        double z = current.z;
        if (horizontalSpeed > config.hoverMaxSpeed()) {
            x *= config.hoverDamping();
            z *= config.hoverDamping();
        }

        double y;
        if (descending) {
            // Vanilla creative-fly descend (sneak) is a flat, slow speed regardless of tier.
            // While the player is actively holding the descend key, accelerate like a powered
            // dive instead - and pre-divide by the same factor vanilla's Player#travel()
            // multiplies our Y by every tick while flying, so this actually reaches
            // diveTerminalSpeed instead of stalling at ~1.5x diveAccel.
            double dived = Math.max(current.y - config.diveAccel(), -config.diveTerminalSpeed());
            y = dived / VANILLA_FLYING_Y_DAMPING;
        } else {
            // Not actively descending - leave Y alone. Vanilla's own per-tick damping settles
            // any leftover vertical momentum (from a released boost, a released dive, etc.) back
            // toward level flight on its own; compensating it here would freeze that momentum in
            // place instead of letting it decay, which is what made the player drift up/down on
            // their own after letting go of a key.
            y = current.y;
        }

        return new Vec3(x, y, z);
    }

    public static Vec3 computeOverheatVelocity(Vec3 current) {
        return new Vec3(current.x * 0.9, current.y - 0.1, current.z * 0.9);
    }

    public static Vec3 computeLaunchVelocity(Vec3 look, FlightConfig config) {
        Vec3 boost = look.scale(config.launchSpeedMultiplier()).add(0, 0.6, 0);
        if (boost.y < 0.7) {
            boost = new Vec3(boost.x, 0.7, boost.z);
        }
        return boost;
    }

    /**
     * Vanilla's {@code Player#travel} runs right after our client tick hook and, whenever
     * {@code abilities.flying} is true, unconditionally multiplies whatever Y velocity we just
     * set by this factor before the position is integrated (confirmed via decompiling
     * {@code Player.class} - it's not something this mod controls). Every Y handed to
     * {@code setDeltaMovement} while flying must be pre-divided by it, otherwise every vertical
     * number in {@link FlightConfig} (dive speed in particular) ends up converging to roughly
     * {@code 1.5 * diveAccel} instead of {@code diveTerminalSpeed}.
     */
    public static final double VANILLA_FLYING_Y_DAMPING = 0.6;
}
