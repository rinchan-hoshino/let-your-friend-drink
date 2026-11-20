package dev.rinchan.letyourfrienddrink.fabric.mixin;

import dev.rinchan.letyourfrienddrink.FriendDrinkFeeder;
import dev.rinchan.letyourfrienddrink.FullHungerBridge;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Intercepts Let Your Friend Eating 1.1.x (th.in.tamkungz) PlayerFeeder.tryFeed on Fabric. */
@Pseudo
@Mixin(targets = "th.in.tamkungz.letyourfriendeating.logic.PlayerFeeder", remap = false)
public abstract class PlayerFeederMixin {
    @Inject(method = "tryFeed", at = @At("HEAD"), cancellable = true, remap = false)
    private static void letYourFriendDrink$beforeFeed(
        Player feeder,
        Player recipient,
        InteractionHand hand,
        CallbackInfoReturnable<InteractionResult> cir
    ) {
        InteractionResult drinkResult = FriendDrinkFeeder.tryFeedDrink(feeder, recipient, hand);
        if (drinkResult != null) {
            cir.setReturnValue(drinkResult);
            return;
        }
        FullHungerBridge.beforeBaseFeed(feeder, recipient, hand);
    }

    @Inject(method = "tryFeed", at = @At("RETURN"), remap = false)
    private static void letYourFriendDrink$afterFeed(
        Player feeder,
        Player recipient,
        InteractionHand hand,
        CallbackInfoReturnable<InteractionResult> cir
    ) {
        FullHungerBridge.afterBaseFeed(feeder, recipient, hand, cir.getReturnValue());
    }
}
