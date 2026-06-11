package com.pavel.ironcore.item;

import com.pavel.ironcore.capability.SuitCapabilityProvider;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.object.PlayState;

public class CinematicArmorItem extends BaseSuitItem {
    public CinematicArmorItem(ArmorMaterial material, ArmorItem.Type type, Properties properties) {
        super(material, type, properties);
    }

    @Override
    public String getTierId() {
        return "cinematic";
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "cinematic_controller", 5, state -> {
            if (state.getData(software.bernie.geckolib.constant.DataTickets.ENTITY) instanceof Player player) {
                return player.getCapability(SuitCapabilityProvider.SUIT_CAPABILITY).map(suit -> {
                    if (suit.getCinematicStage() == 2) {
                        return state.setAndContinue(RawAnimation.begin().thenPlayAndHold("animation.suit.kneel"));
                    }
                    return PlayState.STOP;
                }).orElse(PlayState.STOP);
            }
            return PlayState.STOP;
        }));
    }

    @Override
    protected void applyClientPhysics(Player player, com.pavel.ironcore.capability.SuitCapability suit, ItemStack stack, Level level) {
        // No physics for cinematic armor
    }

    @Override
    protected boolean applyServerLogic(ServerPlayer player, com.pavel.ironcore.capability.SuitCapability suit, ItemStack stack, Level level) {
        // Remove armor if cinematic is over
        if (suit.getCinematicStage() != 2) {
            stack.setCount(0);
        }
        return false;
    }
}
