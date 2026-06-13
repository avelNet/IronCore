package com.pavel.ironcore.item;

import com.pavel.ironcore.IronCore;
import com.pavel.ironcore.capability.SuitCapabilityProvider;
import com.pavel.ironcore.client.renderer.SuitRenderer;
import com.pavel.ironcore.network.ModMessages;
import com.pavel.ironcore.network.PacketSyncSuitData;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import org.jetbrains.annotations.NotNull;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.constant.DefaultAnimations;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.function.Consumer;

public abstract class BaseSuitItem extends ArmorItem implements GeoItem {
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    public BaseSuitItem(ArmorMaterial material, ArmorItem.Type type, Properties properties) {
        super(material, type, properties);
    }

    public abstract String getTierId();

    @Override
    public String getArmorTexture(ItemStack stack, Entity entity, EquipmentSlot slot, String type) {
        return IronCore.MODID + ":textures/armor/blank.png";
    }

    @Override
    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        consumer.accept(new IClientItemExtensions() {
            private SuitRenderer renderer;

            @Override
            public @NotNull HumanoidModel<?> getHumanoidArmorModel(LivingEntity livingEntity, ItemStack itemStack, EquipmentSlot equipmentSlot, HumanoidModel<?> original) {
                if (this.renderer == null) {
                    this.renderer = new SuitRenderer();
                }

                this.renderer.prepForRender(livingEntity, itemStack, equipmentSlot, original);
                return this.renderer;
            }
        });
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new software.bernie.geckolib.core.animation.AnimationController<>(this, "helmet_controller", 5, state -> {
            boolean isMaskOpen = false;
            if (state.getData(software.bernie.geckolib.constant.DataTickets.ENTITY) instanceof Player player) {
                isMaskOpen = player.getCapability(SuitCapabilityProvider.SUIT_CAPABILITY).map(com.pavel.ironcore.capability.SuitCapability::isMaskOpen).orElse(false);
            }
            
            String tier = getTierId();
            if (tier.equals("mk2")) {
                if (isMaskOpen) {
                    return state.setAndContinue(software.bernie.geckolib.core.animation.RawAnimation.begin().thenPlayAndHold("animation.mark_2.helmet_open"));
                } else {
                    return state.setAndContinue(software.bernie.geckolib.core.animation.RawAnimation.begin().thenPlayAndHold("animation.mark_2.helmet_close"));
                }
            } else if (tier.equals("mk3")) {
                if (isMaskOpen) {
                    return state.setAndContinue(software.bernie.geckolib.core.animation.RawAnimation.begin().thenPlayAndHold("animation.mark_3.helmet_open"));
                } else {
                    return state.setAndContinue(software.bernie.geckolib.core.animation.RawAnimation.begin().thenPlayAndHold("animation.mark_3.helmet_close"));
                }
            }
            
            return software.bernie.geckolib.core.object.PlayState.CONTINUE;
        }));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slotId, boolean isSelected) {
        if (entity instanceof Player player) {
            if (this.getType() == ArmorItem.Type.CHESTPLATE && player.getItemBySlot(EquipmentSlot.CHEST) == stack) {
                if (level.isClientSide) {
                    onClientTick(player, stack, level);
                } else {
                    onServerTick((ServerPlayer) player, stack, level);
                }
            }
        }
    }

    protected void onClientTick(Player player, ItemStack stack, Level level) {
        player.getCapability(SuitCapabilityProvider.SUIT_CAPABILITY).ifPresent(suit -> {
            if (isFullSuitEquipped(player)) {
                applyClientPhysics(player, suit, stack, level);
            }
        });
    }

    protected void onServerTick(ServerPlayer player, ItemStack stack, Level level) {
        player.getCapability(SuitCapabilityProvider.SUIT_CAPABILITY).ifPresent(suit -> {
            boolean changed = false;
            boolean isFull = isFullSuitEquipped(player);

            if (isFull) {
                CompoundTag chestTag = stack.getOrCreateTag();
                String installedReactor = chestTag.getString("InstalledReactor");
                int suitEnergy = chestTag.getInt("SuitEnergy");
                int suitMaxEnergy = chestTag.getInt("SuitMaxEnergy");

                if ((installedReactor.isEmpty() || installedReactor.equals("none")) && !suit.hasEmbeddedReactor()) {
                    if (!suit.getSuitTier().equals("none")) {
                        handleNoReactor(player, suit);
                        sync(player, suit);
                    }
                    return;
                }

                if (!suit.getSuitTier().equals(getTierId())) {
                    suit.setSuitTier(getTierId());
                    changed = true;
                }

                if (suit.hasEmbeddedReactor()) {
                    suit.setActiveReactorType("palladium");
                    suit.setMaxEnergy(50000);
                    if (suit.getEnergy() < suit.getMaxEnergy()) {
                        suit.setEnergy(suit.getMaxEnergy());
                        changed = true;
                    }
                } else {
                    if (installedReactor.contains("palladium")) suit.setActiveReactorType("palladium");
                    else if (installedReactor.contains("coal")) suit.setActiveReactorType("coal");
                    
                    suit.setMaxEnergy(suitMaxEnergy);
                    if (suit.getEnergy() != suitEnergy) {
                       suit.setEnergy(suitEnergy); 
                    }
                }

                changed |= applyServerLogic(player, suit, stack, level);
            } else {
                if (suit.getSuitTier().equals(getTierId())) {
                    handleUnequipped(player, suit);
                    changed = true;
                }
            }

            if (changed || player.tickCount % 20 == 0) {
                sync(player, suit);
            }
        });
    }

    protected void handleNoReactor(ServerPlayer player, com.pavel.ironcore.capability.SuitCapability suit) {
        suit.setSuitTier("none");
        suit.setActiveReactorType("none");
        suit.setEnergy(0);
        suit.setMaxEnergy(0);
        suit.setFlying(false);
        if (!player.isCreative() && !player.isSpectator()) {
            player.getAbilities().mayfly = false;
            player.getAbilities().flying = false;
            player.onUpdateAbilities();
        }
    }

    protected void handleUnequipped(ServerPlayer player, com.pavel.ironcore.capability.SuitCapability suit) {
        suit.setSuitTier("none");
        suit.setFlying(false);
        if (!player.isCreative() && !player.isSpectator()) {
            player.getAbilities().mayfly = false;
            player.getAbilities().flying = false;
            player.onUpdateAbilities();
        }
    }

    protected abstract void applyClientPhysics(Player player, com.pavel.ironcore.capability.SuitCapability suit, ItemStack stack, Level level);
    protected abstract boolean applyServerLogic(ServerPlayer player, com.pavel.ironcore.capability.SuitCapability suit, ItemStack stack, Level level);

    protected void sync(ServerPlayer player, com.pavel.ironcore.capability.SuitCapability suit) {
        ModMessages.sendSyncPacket(player);
    }

    protected boolean isFullSuitEquipped(Player player) {
        return player.getItemBySlot(EquipmentSlot.HEAD).getItem() instanceof BaseSuitItem helmet && helmet.getTierId().equals(this.getTierId()) &&
               player.getItemBySlot(EquipmentSlot.CHEST).getItem() instanceof BaseSuitItem chest && chest.getTierId().equals(this.getTierId()) &&
               player.getItemBySlot(EquipmentSlot.LEGS).getItem() instanceof BaseSuitItem legs && legs.getTierId().equals(this.getTierId()) &&
               player.getItemBySlot(EquipmentSlot.FEET).getItem() instanceof BaseSuitItem boots && boots.getTierId().equals(this.getTierId());
    }
}
