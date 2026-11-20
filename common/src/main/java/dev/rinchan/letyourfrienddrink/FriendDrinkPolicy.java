package dev.rinchan.letyourfrienddrink;

/** Pure-Java policy predicates for drink handling, harmful-drink protection, remainder transfer, and full-hunger bridging. */
public final class FriendDrinkPolicy {
    public static final int FULL_HUNGER_LEVEL = 20;
    public static final int BRIDGED_HUNGER_LEVEL = 19;

    private FriendDrinkPolicy() {
    }

    public static boolean shouldHandle(boolean drinkAnimation, boolean handledByBaseMod) {
        return drinkAnimation && !handledByBaseMod;
    }

    public static boolean shouldHandle(boolean drinkAnimation, boolean handledByBaseMod, boolean excluded) {
        return drinkAnimation && !handledByBaseMod && !excluded;
    }

    public static boolean shouldBlockHarmfulDrink(boolean hasHarmfulEffect, boolean allowHarmfulDrinks) {
        return hasHarmfulEffect && !allowHarmfulDrinks;
    }

    public static boolean hasAnyHarmfulEffect(boolean ominous, Iterable<Boolean> effectHarmfulness) {
        if (ominous) {
            return true;
        }
        if (effectHarmfulness != null) {
            for (Boolean harmful : effectHarmfulness) {
                if (Boolean.TRUE.equals(harmful)) {
                    return true;
                }
            }
        }
        return false;
    }

    public static boolean preservesHeldItem(boolean feederCreative, boolean unchangedRemainder) {
        return feederCreative || unchangedRemainder;
    }

    public static boolean shouldBridgeFullHunger(
        boolean allowFeedingAtFullHunger,
        boolean drinkAnimation,
        boolean isFood,
        boolean excluded,
        int recipientFoodLevel
    ) {
        return allowFeedingAtFullHunger
            && !drinkAnimation
            && isFood
            && !excluded
            && recipientFoodLevel >= FULL_HUNGER_LEVEL;
    }

    public static int computeRestoredFoodLevel(
        int originalFoodLevel,
        int currentFoodLevelAfterFeed,
        boolean itemConsumed
    ) {
        if (!itemConsumed) {
            return originalFoodLevel;
        }
        if (originalFoodLevel <= FULL_HUNGER_LEVEL) {
            return Math.min(FULL_HUNGER_LEVEL, Math.max(originalFoodLevel, currentFoodLevelAfterFeed));
        }
        return Math.max(originalFoodLevel, currentFoodLevelAfterFeed);
    }
}
