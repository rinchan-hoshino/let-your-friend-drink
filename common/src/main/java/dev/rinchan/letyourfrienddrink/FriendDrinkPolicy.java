package dev.rinchan.letyourfrienddrink;

public final class FriendDrinkPolicy {
    private FriendDrinkPolicy() {
    }

    public static boolean shouldHandle(boolean drinkAnimation, boolean handledByBaseMod) {
        return drinkAnimation && !handledByBaseMod;
    }

    public static boolean preservesHeldItem(boolean feederCreative, boolean unchangedRemainder) {
        return feederCreative || unchangedRemainder;
    }
}
