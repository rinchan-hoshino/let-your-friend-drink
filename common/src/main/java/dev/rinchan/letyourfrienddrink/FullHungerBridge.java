package dev.rinchan.letyourfrienddrink;

import dev.rinchan.letyourfrienddrink.config.FriendDrinkConfig;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * Safely bridges Let Your Friend Eating's food feeding when a recipient's hunger bar is full (>= 20),
 * temporarily lowering foodLevel to 19 during the base mod call and restoring/clamping afterward.
 */
public final class FullHungerBridge {
    private static final ThreadLocal<PendingState> PENDING = new ThreadLocal<>();

    private FullHungerBridge() {}

    public static void beforeBaseFeed(Player feeder, Player recipient, InteractionHand hand) {
        PendingState stale = PENDING.get();
        if (stale != null) {
            PENDING.remove();
            stale.restoreUnconsumed();
        }
        if (feeder == null || recipient == null || hand == null || feeder == recipient) {
            return;
        }
        FriendDrinkConfig config = FriendDrinkConfig.get();
        ItemStack held = feeder.getItemInHand(hand);
        boolean drinkAnim = VersionBridge.isDrink(held);
        boolean isFood = VersionBridge.isFood(held, recipient);
        boolean excluded = VersionBridge.isExcluded(held, config.exclusionMatcher());
        int originalFoodLevel = recipient.getFoodData().getFoodLevel();

        if (!FriendDrinkPolicy.shouldBridgeFullHunger(
            config.allowFeedingAtFullHunger(),
            drinkAnim,
            isFood,
            excluded,
            originalFoodLevel
        )) {
            return;
        }

        recipient.getFoodData().setFoodLevel(FriendDrinkPolicy.BRIDGED_HUNGER_LEVEL);
        PENDING.set(new PendingState(recipient, originalFoodLevel, held.getCount()));
    }

    public static void afterBaseFeed(
        Player feeder,
        Player recipient,
        InteractionHand hand,
        InteractionResult result
    ) {
        PendingState state = PENDING.get();
        if (state == null) {
            return;
        }
        PENDING.remove();
        if (state.recipient != recipient) {
            state.restoreUnconsumed();
            return;
        }
        boolean consumed = (result != null && VersionBridge.consumesAction(result))
            || (feeder != null && hand != null && feeder.getItemInHand(hand).getCount() < state.heldCountBefore);
        int currentFoodLevel = recipient.getFoodData().getFoodLevel();
        int restored = FriendDrinkPolicy.computeRestoredFoodLevel(
            state.originalFoodLevel,
            currentFoodLevel,
            consumed
        );
        recipient.getFoodData().setFoodLevel(restored);
    }

    private record PendingState(Player recipient, int originalFoodLevel, int heldCountBefore) {
        void restoreUnconsumed() {
            if (recipient != null) {
                recipient.getFoodData().setFoodLevel(originalFoodLevel);
            }
        }
    }
}
