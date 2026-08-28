package dev.rinchan.letyourfrienddrink.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import dev.rinchan.letyourfrienddrink.FriendDrinkFeeder;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;

@Pseudo
@Mixin(targets = "th.in.tamkungz.letyourfriendeating.logic.PlayerFeeder", remap = false)
public abstract class PlayerFeederMixin {
    @WrapMethod(method = "tryFeedPlayer")
    private static InteractionResult letYourFriendDrink$handleDrinkFirst(
        Player feeder,
        Player recipient,
        InteractionHand hand,
        Operation<InteractionResult> original
    ) {
        InteractionResult drink = FriendDrinkFeeder.tryFeedDrink(feeder, recipient, hand);
        return drink != null ? drink : original.call(feeder, recipient, hand);
    }
}
