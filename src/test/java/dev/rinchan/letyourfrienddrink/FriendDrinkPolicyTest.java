package dev.rinchan.letyourfrienddrink;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

final class FriendDrinkPolicyTest {
    @Test
    void handlesOnlyDrinkAnimationsNotOwnedByTheBaseMod() {
        assertTrue(FriendDrinkPolicy.shouldHandle(true, false));
        assertFalse(FriendDrinkPolicy.shouldHandle(false, false));
        assertFalse(FriendDrinkPolicy.shouldHandle(true, true));
    }

    @Test
    void preservesCreativeAndNativelyReusableItems() {
        assertTrue(FriendDrinkPolicy.preservesHeldItem(true, false));
        assertTrue(FriendDrinkPolicy.preservesHeldItem(false, true));
        assertFalse(FriendDrinkPolicy.preservesHeldItem(false, false));
    }
}
