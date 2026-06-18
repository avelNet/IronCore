# Flight Mechanics Rework Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use compose:subagent (recommended) or compose:execute to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace vanilla creative-flight with a custom physics-based flight system that provides inertia, momentum, loops, and a powerful Fisk-style takeoff.

**Architecture:** New `FlightPhysics` class encapsulates all flight math (velocity vector, inertia, gravity, drag, agility). Each Mk frame item delegates to FlightPhysics with tier-specific parameters. Client-side `FlightCameraHandler` handles FOV, shake, and smooth camera. Server-side stores velocity in SuitCapability for network sync.

**Tech Stack:** Minecraft 1.20.1 Forge, Java 17, GeckoLib

---

## File Structure

**New files:**
- `src/main/java/com/pavel/ironcore/flight/FlightPhysics.java` — Core flight math engine
- `src/main/java/com/pavel/ironcore/flight/FlightConfig.java` — Per-tier flight parameters
- `src/main/java/com/pavel/ironcore/client/FlightCameraHandler.java` — Client camera effects

**Modified files:**
- `src/main/java/com/pavel/ironcore/capability/SuitCapability.java` — Add velocityVec, isLaunched fields
- `src/main/java/com/pavel/ironcore/item/Mk2FrameItem.java` — Replace direct velocity with FlightPhysics
- `src/main/java/com/pavel/ironcore/item/Mk3FrameItem.java` — Replace direct velocity with FlightPhysics
- `src/main/java/com/pavel/ironcore/item/Mk1FrameItem.java` — Minor: launch impulse on sprint-jump

---

## Task 1: FlightConfig — Per-Tier Parameters

**Covers:** Mk2/Mk3 differentiation, Mk3 not being perfect

**Files:**
- Create: `src/main/java/com/pavel/ironcore/flight/FlightConfig.java`

- [ ] **Step 1: Create FlightConfig with tier parameters**

```java
package com.pavel.ironcore.flight;

public class FlightConfig {
    public final double maxSpeed;
    public final double boostAccel;
    public final double hoverMaxSpeed;
    public final double gravity;
    public final double drag;
    public final double agility;
    public final double launchPower;
    public final double loopGravityReduction;
    public final double energyPerTick;
    public final double turboMaxSpeed;
    public final double turboEnergyPerTick;
    public final double turboHeatPerTick;
    public final double boostHeatPerTick;

    private FlightConfig(double maxSpeed, double boostAccel, double hoverMaxSpeed,
                         double gravity, double drag, double agility, double launchPower,
                         double loopGravityReduction, double energyPerTick,
                         double turboMaxSpeed, double turboEnergyPerTick,
                         double turboHeatPerTick, double boostHeatPerTick) {
        this.maxSpeed = maxSpeed;
        this.boostAccel = boostAccel;
        this.hoverMaxSpeed = hoverMaxSpeed;
        this.gravity = gravity;
        this.drag = drag;
        this.agility = agility;
        this.launchPower = launchPower;
        this.loopGravityReduction = loopGravityReduction;
        this.energyPerTick = energyPerTick;
        this.turboMaxSpeed = turboMaxSpeed;
        this.turboEnergyPerTick = turboEnergyPerTick;
        this.turboHeatPerTick = turboHeatPerTick;
        this.boostHeatPerTick = boostHeatPerTick;
    }

    // Mk1: No flight, only sprint-boost on ground
    public static final FlightConfig MK1 = new FlightConfig(
        0.55, 0.15, 0.0, 0.04, 0.95, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0
    );

    // Mk2: First flying suit — heavier, less responsive
    public static final FlightConfig MK2 = new FlightConfig(
        0.60, 0.10, 0.25, 0.020, 0.97, 0.07, 1.2, 0.3, 0.05, 0.0, 0.0, 0.0, 0.02
    );

    // Mk3: Slightly better — not perfect, just more stable than Mk2
    public static final FlightConfig MK3 = new FlightConfig(
        0.80, 0.14, 0.30, 0.015, 0.98, 0.10, 1.5, 0.4, 0.20, 1.25, 4.0, 0.04, 0.03
    );

    public static FlightConfig forTier(String tier) {
        return switch (tier) {
            case "mk1" -> MK1;
            case "mk2" -> MK2;
            case "mk3" -> MK3;
            default -> MK2;
        };
    }
}
```

- [ ] **Step 2: Commit**

```bash
git add src/main/java/com/pavel/ironcore/flight/FlightConfig.java
git commit -m "feat: add FlightConfig with per-tier flight parameters"
```

---

## Task 2: SuitCapability — Add Flight State Fields

**Covers:** Server-side flight state storage

**Files:**
- Modify: `src/main/java/com/pavel/ironcore/capability/SuitCapability.java`

- [ ] **Step 1: Add new fields to SuitCapability**

Add after `isMaskOpen` field (line 26):

```java
// Flight physics state
private double velocityX = 0.0;
private double velocityY = 0.0;
private double velocityZ = 0.0;
private boolean isLaunched = false;
private int launchCooldown = 0;
```

- [ ] **Step 2: Add getters/setters**

Add after the `isMaskOpen` getter/setter (after line 83):

```java
public double getVelocityX() { return velocityX; }
public double getVelocityY() { return velocityY; }
public double getVelocityZ() { return velocityZ; }
public void setVelocity(double x, double y, double z) {
    this.velocityX = x;
    this.velocityY = y;
    this.velocityZ = z;
}

public boolean isLaunched() { return isLaunched; }
public void setLaunched(boolean launched) { this.isLaunched = launched; }

public int getLaunchCooldown() { return launchCooldown; }
public void setLaunchCooldown(int cooldown) { this.launchCooldown = cooldown; }
```

- [ ] **Step 3: Add to saveNBTData (before closing brace, around line 101)**

```java
nbt.putDouble("velX", velocityX);
nbt.putDouble("velY", velocityY);
nbt.putDouble("velZ", velocityZ);
nbt.putBoolean("launched", isLaunched);
nbt.putInt("launchCooldown", launchCooldown);
```

- [ ] **Step 4: Add to loadNBTData (after maskOpen load, around line 130)**

```java
if (nbt.contains("velX")) {
    velocityX = nbt.getDouble("velX");
    velocityY = nbt.getDouble("velY");
    velocityZ = nbt.getDouble("velZ");
}
if (nbt.contains("launched")) {
    isLaunched = nbt.getBoolean("launched");
}
if (nbt.contains("launchCooldown")) {
    launchCooldown = nbt.getInt("launchCooldown");
}
```

- [ ] **Step 5: Add resetVelocity helper**

```java
public void resetVelocity() {
    this.velocityX = 0.0;
    this.velocityY = 0.0;
    this.velocityZ = 0.0;
    this.isLaunched = false;
}

public Vec3 getVelocityVec() {
    return new Vec3(velocityX, velocityY, velocityZ);
}
```

Add import: `import net.minecraft.world.phys.Vec3;`

- [ ] **Step 6: Commit**

```bash
git add src/main/java/com/pavel/ironcore/capability/SuitCapability.java
git commit -m "feat: add flight velocity state fields to SuitCapability"
```

---

## Task 3: FlightPhysics — Core Flight Engine

**Covers:** Inertia, momentum, loops, takeoff impulse

**Files:**
- Create: `src/main/java/com/pavel/ironcore/flight/FlightPhysics.java`

- [ ] **Step 1: Create FlightPhysics class**

```java
package com.pavel.ironcore.flight;

import com.pavel.ironcore.capability.SuitCapability;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public class FlightPhysics {

    public static class FlightResult {
        public final Vec3 newVelocity;
        public final boolean launched;
        public final boolean shouldBreakFlight;
        public final float heatDelta;

        public FlightResult(Vec3 newVelocity, boolean launched, boolean shouldBreakFlight, float heatDelta) {
            this.newVelocity = newVelocity;
            this.launched = launched;
            this.shouldBreakFlight = shouldBreakFlight;
            this.heatDelta = heatDelta;
        }
    }

    /**
     * Calculate next tick's velocity for a flying player.
     * Call this each tick from applyServerLogic when the player is flying.
     */
    public static FlightResult tick(
            Player player,
            SuitCapability suit,
            FlightConfig config,
            boolean isBoosting,
            boolean isTurboActive,
            Level level
    ) {
        Vec3 current = suit.getVelocityVec();
        Vec3 look = player.getLookAngle();
        boolean onGround = player.onGround();
        float heatDelta = 0.0f;
        boolean launched = false;
        boolean shouldBreak = false;

        // Launch impulse — transition from ground to air
        if (onGround && !suit.isLaunched() && suit.getLaunchCooldown() <= 0) {
            Vec3 impulse = look.scale(config.launchPower);
            current = current.add(impulse);
            launched = true;
            suit.setLaunched(true);
            suit.setLaunchCooldown(5);
            heatDelta += config.boostHeatPerTick * 3;
        }

        // Decrement launch cooldown
        if (suit.getLaunchCooldown() > 0) {
            suit.setLaunchCooldown(suit.getLaunchCooldown() - 1);
        }

        // Reset launch flag when grounded
        if (onGround && current.length() < 0.1) {
            suit.setLaunched(false);
        }

        // Determine effective max speed and acceleration
        double effectiveMaxSpeed;
        double effectiveAccel;
        double effectiveDrag;
        if (isTurboActive && config.turboMaxSpeed > 0) {
            effectiveMaxSpeed = config.turboMaxSpeed;
            effectiveAccel = config.boostAccel * 1.3;
            effectiveDrag = config.drag * 0.995;
            heatDelta += config.turboHeatPerTick;
        } else if (isBoosting) {
            effectiveMaxSpeed = config.maxSpeed;
            effectiveAccel = config.boostAccel;
            effectiveDrag = config.drag;
            heatDelta += config.boostHeatPerTick;
        } else {
            effectiveMaxSpeed = config.hoverMaxSpeed;
            effectiveAccel = 0.0;
            effectiveDrag = config.drag * 0.92;
        }

        // Acceleration toward look direction (agility determines turn rate)
        if (isBoosting || isTurboActive) {
            Vec3 target = look.scale(effectiveMaxSpeed);
            double agility = config.agility;
            Vec3 delta = target.subtract(current);
            current = current.add(delta.scale(agility));
        }

        // Apply drag
        current = current.scale(effectiveDrag);

        // Apply reduced gravity
        // More upward velocity = less gravity (lift effect during boost)
        double liftFactor = isBoosting ? config.loopGravityReduction : 1.0;
        double effectiveGravity = config.gravity * liftFactor;
        current = current.add(0, -effectiveGravity, 0);

        // Clamp horizontal speed
        double hSpeed = Math.sqrt(current.x * current.x + current.z * current.z);
        if (hSpeed > effectiveMaxSpeed && hSpeed > 0) {
            double scale = effectiveMaxSpeed / hSpeed;
            current = new Vec3(current.x * scale, current.y, current.z * scale);
        }

        // Clamp total speed
        if (current.length() > effectiveMaxSpeed * 1.5) {
            current = current.normalize().scale(effectiveMaxSpeed * 1.5);
        }

        // Break flight if speed too low and no boost and falling
        if (!isBoosting && !isTurboActive && current.length() < 0.05 && current.y < -0.1) {
            shouldBreak = true;
        }

        return new FlightResult(current, launched, shouldBreak, heatDelta);
    }

    /**
     * Get the current flight speed in blocks/tick for display purposes.
     */
    public static double getSpeed(Vec3 velocity) {
        return velocity.length();
    }

    /**
     * Get G-force approximation for camera effects.
     * Positive = pulling up, negative = pulling down.
     */
    public static double getGForce(Vec3 oldVelocity, Vec3 newVelocity, double gravity) {
        if (gravity == 0) return 0;
        Vec3 delta = newVelocity.subtract(oldVelocity);
        return (delta.y + gravity) / gravity;
    }
}
```

- [ ] **Step 2: Commit**

```bash
git add src/main/java/com/pavel/ironcore/flight/FlightPhysics.java
git commit -m "feat: add FlightPhysics core engine with inertia and loop support"
```

---

## Task 4: Mk2FrameItem — Integrate FlightPhysics

**Covers:** Mk2 flight rework

**Files:**
- Modify: `src/main/java/com/pavel/ironcore/item/Mk2FrameItem.java`

- [ ] **Step 1: Replace applyClientPhysics**

Replace the entire `applyClientPhysics` method (lines 28-61):

```java
@Override
protected void applyClientPhysics(Player player, SuitCapability suit, ItemStack stack, Level level) {
    if (!player.getAbilities().flying) return;

    boolean isBoosting = net.minecraft.client.Minecraft.getInstance().options.keySprint.isDown();
    boolean enginesFrozen = suit.getIcingLevel() >= 100.0f;
    boolean enginesOverheated = suit.getHeat() >= 100.0f;

    if (player.isInWater() && !player.isCreative()) {
        player.getAbilities().flying = false;
        player.onUpdateAbilities();
        return;
    }

    if (enginesOverheated || enginesFrozen) {
        Vec3 current = player.getDeltaMovement();
        player.setDeltaMovement(current.x * 0.9, current.y - 0.1, current.z * 0.9);
        player.hasImpulse = true;
        return;
    }

    // Apply stored velocity from server
    Vec3 velocity = suit.getVelocityVec();
    player.setDeltaMovement(velocity);
    player.hasImpulse = true;
}
```

- [ ] **Step 2: Replace applyServerLogic**

Replace the entire `applyServerLogic` method (lines 65-103):

```java
@Override
protected boolean applyServerLogic(ServerPlayer player, SuitCapability suit, ItemStack stack, Level level) {
    boolean changed = false;
    player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 40, 0, false, false, true));

    FlightConfig config = FlightConfig.MK2;
    boolean systemsFunctional = player.isCreative() || (
        suit.getIcingLevel() < 100.0f && suit.getHeat() < 100.0f && suit.getEnergy() >= 4 && !player.isInWater()
    );
    boolean prevMayfly = player.getAbilities().mayfly;
    player.getAbilities().mayfly = systemsFunctional;

    if (player.getAbilities().flying) {
        if (!systemsFunctional) {
            player.getAbilities().flying = false;
            suit.resetVelocity();
            player.onUpdateAbilities();
        } else {
            boolean isBoosting = suit.isBoostKeyHeld();
            FlightPhysics.FlightResult result = FlightPhysics.tick(player, suit, config, isBoosting, false, level);

            suit.setVelocity(result.newVelocity.x, result.newVelocity.y, result.newVelocity.z);
            player.setDeltaMovement(result.newVelocity);
            player.hasImpulse = true;

            // Energy drain
            if (player.tickCount % 4 == 0) {
                suit.setEnergy(suit.getEnergy() - 1);
            }
            suit.setHeat(Math.max(0, Math.min(100, suit.getHeat() + result.heatDelta)));

            // Particles
            ServerLevel serverLevel = (ServerLevel) level;
            Vec3 pos = player.position();
            if (player.tickCount % 2 == 0) {
                serverLevel.sendParticles(ParticleTypes.FLAME, pos.x, pos.y + 0.1, pos.z, 1, 0.1, 0.0, 0.1, 0.02);
            }

            if (result.shouldBreakFlight) {
                player.getAbilities().flying = false;
                suit.resetVelocity();
                player.onUpdateAbilities();
            }
        }
    } else {
        if (suit.isLaunched() || suit.getVelocityVec().length() > 0.1) {
            suit.resetVelocity();
            changed = true;
        }
    }

    // Icing logic (Mk2 specific)
    if (player.getY() > 170 && player.getAbilities().flying) {
        suit.setIcingLevel(suit.getIcingLevel() + 0.1f);
    } else {
        suit.setIcingLevel(Math.max(0, suit.getIcingLevel() - 0.2f));
    }

    // Heat cooling
    float coolingRate = player.isInWater() ? 0.5f : 0.1f;
    if (!suit.isBoostKeyHeld() || !player.getAbilities().flying) {
        suit.setHeat(Math.max(0, suit.getHeat() - coolingRate));
    }

    if (prevMayfly != player.getAbilities().mayfly) {
        player.onUpdateAbilities();
    }
    return true;
}
```

- [ ] **Step 3: Add import**

Add at top: `import com.pavel.ironcore.flight.FlightConfig;`
Add at top: `import com.pavel.ironcore.flight.FlightPhysics;`

- [ ] **Step 4: Commit**

```bash
git add src/main/java/com/pavel/ironcore/item/Mk2FrameItem.java
git commit -m "feat: integrate FlightPhysics into Mk2 frame"
```

---

## Task 5: Mk3FrameItem — Integrate FlightPhysics

**Covers:** Mk3 flight rework (slightly better than Mk2, not perfect)

**Files:**
- Modify: `src/main/java/com/pavel/ironcore/item/Mk3FrameItem.java`

- [ ] **Step 1: Replace applyClientPhysics**

Replace the entire `applyClientPhysics` method (lines 28-67):

```java
@Override
protected void applyClientPhysics(Player player, SuitCapability suit, ItemStack stack, Level level) {
    if (!player.getAbilities().flying) return;

    boolean isBoosting = net.minecraft.client.Minecraft.getInstance().options.keySprint.isDown();
    boolean enginesOverheated = suit.getHeat() >= 100.0f;

    if (player.isInWater() && !player.isCreative()) {
        player.getAbilities().flying = false;
        player.onUpdateAbilities();
        return;
    }

    if (enginesOverheated) {
        Vec3 current = player.getDeltaMovement();
        player.setDeltaMovement(current.x * 0.9, current.y - 0.1, current.z * 0.9);
        player.hasImpulse = true;
        return;
    }

    // Apply stored velocity from server
    Vec3 velocity = suit.getVelocityVec();
    player.setDeltaMovement(velocity);
    player.hasImpulse = true;
}
```

- [ ] **Step 2: Replace applyServerLogic**

Replace the entire `applyServerLogic` method (lines 71-140):

```java
@Override
protected boolean applyServerLogic(ServerPlayer player, SuitCapability suit, ItemStack stack, Level level) {
    boolean changed = false;

    player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 40, 0, false, false, true));
    player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 40, 0, false, false, true));

    FlightConfig config = FlightConfig.MK3;
    boolean systemsFunctional = player.isCreative() || (
        suit.getIcingLevel() < 100.0f && suit.getHeat() < 100.0f && suit.getEnergy() >= 4 && !player.isInWater()
    );
    boolean prevMayfly = player.getAbilities().mayfly;
    player.getAbilities().mayfly = systemsFunctional;

    if (player.getAbilities().flying) {
        if (!systemsFunctional) {
            player.getAbilities().flying = false;
            suit.resetVelocity();
            suit.setTurbo(false);
            suit.setFlightTimer(0);
            player.onUpdateAbilities();
        } else {
            boolean isBoosting = suit.isBoostKeyHeld();
            boolean isTurbo = suit.isTurbo();

            // Turbo activation: after 100 ticks of boost
            if (isBoosting && suit.getEnergy() > 1000) {
                suit.setFlightTimer(suit.getFlightTimer() + 1);
                if (suit.isAutoBoostEnabled() && suit.getFlightTimer() >= 100 && !isTurbo) {
                    suit.setTurbo(true);
                    changed = true;
                }
            } else {
                if (suit.getFlightTimer() > 0 || isTurbo) {
                    suit.setFlightTimer(0);
                    suit.setTurbo(false);
                    changed = true;
                }
            }

            FlightPhysics.FlightResult result = FlightPhysics.tick(player, suit, config, isBoosting, isTurbo, level);

            suit.setVelocity(result.newVelocity.x, result.newVelocity.y, result.newVelocity.z);
            player.setDeltaMovement(result.newVelocity);
            player.hasImpulse = true;

            // Energy drain
            int energyDrain = isTurbo ? 80 : 4;
            suit.setEnergy(suit.getEnergy() - energyDrain);
            stack.getOrCreateTag().putInt("SuitEnergy", suit.getEnergy());

            suit.setHeat(Math.max(0, Math.min(100, suit.getHeat() + result.heatDelta)));

            // Particles
            ServerLevel serverLevel = (ServerLevel) level;
            Vec3 pos = player.position();
            if (isTurbo) {
                for (int i = 0; i < 3; i++) {
                    serverLevel.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, pos.x, pos.y + 0.1, pos.z, 1, 0.05, 0.05, 0.05, 0.05);
                }
            } else if (player.tickCount % 2 == 0) {
                serverLevel.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, pos.x, pos.y + 0.1, pos.z, 1, 0.1, 0.0, 0.1, 0.02);
            }

            if (result.shouldBreakFlight) {
                player.getAbilities().flying = false;
                suit.resetVelocity();
                suit.setTurbo(false);
                suit.setFlightTimer(0);
                player.onUpdateAbilities();
            }
        }
    } else {
        if (suit.isLaunched() || suit.getVelocityVec().length() > 0.1 || suit.isTurbo() || suit.getFlightTimer() > 0) {
            suit.resetVelocity();
            suit.setTurbo(false);
            suit.setFlightTimer(0);
            changed = true;
        }
    }

    if (prevMayfly != player.getAbilities().mayfly) {
        player.onUpdateAbilities();
    }

    // Mk3 has no icing, but decays any residual
    if (suit.getIcingLevel() > 0) suit.setIcingLevel(suit.getIcingLevel() - 0.5f);

    // Heat cooling with biome consideration
    float coolingRate = player.isInWater() ? 0.8f : (level.getBiome(player.blockPosition()).value().getBaseTemperature() < 0.2f ? 0.4f : 0.2f);
    if (level.dimension() == Level.NETHER) suit.setHeat(suit.getHeat() + 0.03f);
    if (player.isOnFire() || player.isInLava()) suit.setHeat(suit.getHeat() + 0.5f);
    if (!suit.isBoostKeyHeld() || !player.getAbilities().flying) suit.setHeat(suit.getHeat() - coolingRate);

    player.setTicksFrozen(0);
    return true;
}
```

- [ ] **Step 3: Add imports**

Add at top: `import com.pavel.ironcore.flight.FlightConfig;`
Add at top: `import com.pavel.ironcore.flight.FlightPhysics;`

- [ ] **Step 4: Commit**

```bash
git add src/main/java/com/pavel/ironcore/item/Mk3FrameItem.java
git commit -m "feat: integrate FlightPhysics into Mk3 frame"
```

---

## Task 6: Mk1FrameItem — Add Launch Impulse

**Counds:** Mk1 sprint-jump improvement

**Files:**
- Modify: `src/main/java/com/pavel/ironcore/item/Mk1FrameItem.java`

- [ ] **Step 1: Enhance Mk1 sprint physics with better impulse**

Replace the `applyClientPhysics` method (lines 26-40):

```java
@Override
protected void applyClientPhysics(Player player, SuitCapability suit, ItemStack stack, Level level) {
    if (!player.getAbilities().flying) return;
    if (player.onGround() || player.isInWater() || player.isInLava()) return;

    if (player.isSprinting()) {
        Vec3 look = player.getLookAngle();
        Vec3 current = player.getDeltaMovement();
        double speed = 0.55;
        player.setDeltaMovement(new Vec3(
            current.x + (look.x * speed - current.x) * 0.1,
            current.y + (look.y * speed - current.y) * 0.1,
            current.z + (look.z * speed - current.z) * 0.1
        ));
        player.hasImpulse = true;
    }
}
```

- [ ] **Step 2: Commit**

```bash
git add src/main/java/com/pavel/ironcore/item/Mk1FrameItem.java
git commit -m "fix: clean up Mk1 sprint physics"
```

---

## Task 7: FlightCameraHandler — Client Camera Effects

**Covers:** FOV, shake, 3rd person smooth follow

**Files:**
- Create: `src/main/java/com/pavel/ironcore/client/FlightCameraHandler.java`

- [ ] **Step 1: Create FlightCameraHandler**

```java
package com.pavel.ironcore.client;

import com.pavel.ironcore.capability.SuitCapabilityProvider;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = "ironcore", bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class FlightCameraHandler {

    private static float currentFovModifier = 0.0f;
    private static float targetFovModifier = 0.0f;
    private static float shakeIntensity = 0.0f;
    private static int shakeTimer = 0;

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        mc.player.getCapability(SuitCapabilityProvider.SUIT_CAPABILITY).ifPresent(suit -> {
            if (suit.getSuitTier().equals("none") || suit.isMaskOpen()) {
                targetFovModifier = 0.0f;
                return;
            }

            boolean isBoosting = mc.options.keySprint.isDown() && suit.isFlying();
            boolean isTurbo = suit.isTurbo();

            if (isTurbo) {
                targetFovModifier = 15.0f;
            } else if (isBoosting) {
                targetFovModifier = 10.0f;
            } else if (suit.isFlying()) {
                targetFovModifier = 3.0f;
            } else {
                targetFovModifier = 0.0f;
            }

            // Smooth FOV interpolation
            currentFovModifier += (targetFovModifier - currentFovModifier) * 0.15f;

            // Shake timer
            if (shakeTimer > 0) {
                shakeTimer--;
                shakeIntensity *= 0.85f;
            }
        });
    }

    @SubscribeEvent
    public static void onViewportComputeFov(ViewportEvent.ComputeFov event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        mc.player.getCapability(SuitCapabilityProvider.SUIT_CAPABILITY).ifPresent(suit -> {
            if (!suit.getSuitTier().equals("none") && !suit.isMaskOpen()) {
                float newFov = event.getFOV() + currentFovModifier;

                // Apply shake
                if (shakeTimer > 0) {
                    newFov += (mc.player.getRandom().nextFloat() - 0.5f) * shakeIntensity;
                }

                event.setFOV(newFov);
            }
        });
    }

    public static void triggerLaunchShake() {
        shakeIntensity = 3.0f;
        shakeTimer = 6;
    }

    public static float getCurrentFovModifier() {
        return currentFovModifier;
    }
}
```

- [ ] **Step 2: Hook launch shake into FlightPhysics**

In `FlightPhysics.java`, add a call to `FlightCameraHandler.triggerLaunchShake()` when a launch happens. Since FlightPhysics is shared code and FlightCameraHandler is client-only, use a callback pattern:

Add to FlightResult:
```java
public final boolean triggerLaunchEffect;
```

Update constructor and the launch code to set `triggerLaunchEffect = true` when launched.

In `Mk2FrameItem.applyClientPhysics` and `Mk3FrameItem.applyClientPhysics`, check `result.triggerLaunchEffect` and call `FlightCameraHandler.triggerLaunchShake()`.

- [ ] **Step 3: Commit**

```bash
git add src/main/java/com/pavel/ironcore/client/FlightCameraHandler.java
git add src/main/java/com/pavel/ironcore/flight/FlightPhysics.java
git add src/main/java/com/pavel/ironcore/item/Mk2FrameItem.java
git add src/main/java/com/pavel/ironcore/item/Mk3FrameItem.java
git commit -m "feat: add FlightCameraHandler with FOV effects and launch shake"
```

---

## Task 8: Build & Verify

**Covers:** Compilation check

- [ ] **Step 1: Build the project**

Run: `./gradlew build`
Expected: BUILD SUCCESSFUL (may have pre-existing warnings)

- [ ] **Step 2: Verify no new warnings from our changes**

Check build output for any warnings from `com.pavel.ironcore.flight.*` or `com.pavel.ironcore.client.FlightCameraHandler`

- [ ] **Step 3: Commit any fixes if needed**

---

## Parameters Summary

| Parameter | Mk1 | Mk2 | Mk3 |
|-----------|-----|-----|-----|
| maxSpeed | 0.55 | 0.60 | 0.80 |
| boostAccel | 0.15 | 0.10 | 0.14 |
| hoverMaxSpeed | 0.0 | 0.25 | 0.30 |
| gravity | 0.04 | 0.020 | 0.015 |
| drag | 0.95 | 0.97 | 0.98 |
| agility | 0.0 | 0.07 | 0.10 |
| launchPower | 0.0 | 1.2 | 1.5 |
| loopGravityReduction | 0.0 | 0.3 | 0.4 |
| energyPerTick | 0.0 | 0.05 | 0.20 |
| turboMaxSpeed | 0.0 | 0.0 | 1.25 |
| turboEnergyPerTick | 0.0 | 0.0 | 4.0 |
| turboHeatPerTick | 0.0 | 0.0 | 0.04 |
| boostHeatPerTick | 0.0 | 0.02 | 0.03 |

**Note:** Mk3 is slightly better than Mk2 in every metric, but not dramatically — leaves room for Mk4, Mk5, etc.
