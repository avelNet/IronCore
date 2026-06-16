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
        double targetY = target.y > 0
                ? target.y * config.verticalUpMultiplier() + config.verticalUpOffset()
                : target.y * config.verticalDownMultiplier();

        return new Vec3(
                current.x + (target.x - current.x) * acceleration,
                current.y + (targetY - current.y) * acceleration,
                current.z + (target.z - current.z) * acceleration
        );
    }

    public static Vec3 computeHoverVelocity(Vec3 current, FlightConfig config) {
        double horizontalSpeed = Math.sqrt(current.x * current.x + current.z * current.z);
        double x = current.x;
        double z = current.z;
        if (horizontalSpeed > config.hoverMaxSpeed()) {
            x *= config.hoverDamping();
            z *= config.hoverDamping();
        }

        // Vanilla creative-fly descend (sneak) is a flat, slow speed regardless of tier.
        // Once the player commits to descending, accelerate like a powered dive instead.
        double y = current.y < -0.02
                ? Math.max(current.y - config.diveAccel(), -config.diveTerminalSpeed())
                : current.y;

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

    public static Vec3 compensateFlyingYDamping(Vec3 desired) {
        return new Vec3(desired.x, desired.y / VANILLA_FLYING_Y_DAMPING, desired.z);
    }
}
