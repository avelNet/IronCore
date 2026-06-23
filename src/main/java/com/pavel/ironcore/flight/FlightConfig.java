package com.pavel.ironcore.flight;

/**
 * Per-tier flight tuning. Values are calibrated to match the pre-refactor
 * hardcoded constants in Mk2FrameItem/Mk3FrameItem exactly, so this extraction
 * is speed-neutral.
 */
public record FlightConfig(
        double boostMaxSpeed,
        double boostAccelBase,
        double boostAccelCurve,
        double turboMaxSpeed,
        double turboAccelCurve,
        double verticalUpMultiplier,
        double verticalUpOffset,
        double verticalDownMultiplier,
        double hoverMaxSpeed,
        double hoverDamping,
        double launchSpeedMultiplier,
        double diveAccel,
        double diveTerminalSpeed
) {
    public static final FlightConfig MK2 = new FlightConfig(
            0.85, 0.15, 0.0,
            0.85, 0.0,
            // verticalUp/Down multipliers are calibrated for the post-fix world where
            // computeBoostVelocity pre-divides Y by VANILLA_FLYING_Y_DAMPING (0.6).
            // Target: ~80 km/h dive (looking straight down + sprint).
            0.75, 0.25, 1.3,
            0.278, 0.85,
            1.3,
            0.12, 1.6
    );

    public static final FlightConfig MK3 = new FlightConfig(
            1.0, 0.15, 0.6,
            1.25, 0.7,
            // Target: ~90 km/h dive (looking straight down + sprint).
            0.9, 0.1, 1.25,
            0.3, 0.85,
            1.45,
            0.15, 2.2
    );
}
