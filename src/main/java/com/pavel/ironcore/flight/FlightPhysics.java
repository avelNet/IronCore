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

    public static Vec3 applyAutoLandOverride(Vec3 current, boolean autoLandEnabled, boolean onGround) {
        if (!autoLandEnabled && onGround && current.y < 0.2) {
            return new Vec3(current.x, 0.2, current.z);
        }
        return current;
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
}
