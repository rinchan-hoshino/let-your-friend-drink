package dev.rinchan.letyourfrienddrink;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

final class FriendDrinkPolicyTest {
    @Test
    void handlesNonMilkDrinksUnlessExcluded() {
        assertTrue(FriendDrinkPolicy.shouldHandle(true, false));
        assertTrue(FriendDrinkPolicy.shouldHandle(true, false, false));
        assertFalse(FriendDrinkPolicy.shouldHandle(true, true));
        assertFalse(FriendDrinkPolicy.shouldHandle(true, false, true));
        assertFalse(FriendDrinkPolicy.shouldHandle(false, false, false));
    }

    @Test
    void blocksHarmfulDrinksByDefaultAndAllowsWhenEnabled() {
        assertTrue(FriendDrinkPolicy.shouldBlockHarmfulDrink(true, false));
        assertFalse(FriendDrinkPolicy.shouldBlockHarmfulDrink(true, true));
        assertFalse(FriendDrinkPolicy.shouldBlockHarmfulDrink(false, false));
        assertFalse(FriendDrinkPolicy.shouldBlockHarmfulDrink(false, true));
    }

    @Test
    void preservesHeldItemWhenCreativeOrUnchangedRemainder() {
        assertTrue(FriendDrinkPolicy.preservesHeldItem(true, false));
        assertTrue(FriendDrinkPolicy.preservesHeldItem(false, true));
        assertTrue(FriendDrinkPolicy.preservesHeldItem(true, true));
        assertFalse(FriendDrinkPolicy.preservesHeldItem(false, false));
    }

    @Test
    void bridgesFullHungerOnlyForAllowedNonDrinkNonExcludedFood() {
        assertTrue(FriendDrinkPolicy.shouldBridgeFullHunger(true, false, true, false, 20));
        assertTrue(FriendDrinkPolicy.shouldBridgeFullHunger(true, false, true, false, 24));
        assertFalse(FriendDrinkPolicy.shouldBridgeFullHunger(false, false, true, false, 20));
        assertFalse(FriendDrinkPolicy.shouldBridgeFullHunger(true, true, true, false, 20));
        assertFalse(FriendDrinkPolicy.shouldBridgeFullHunger(true, false, false, false, 20));
        assertFalse(FriendDrinkPolicy.shouldBridgeFullHunger(true, false, true, true, 20));
        assertFalse(FriendDrinkPolicy.shouldBridgeFullHunger(true, false, true, false, 19));
        assertFalse(FriendDrinkPolicy.shouldBridgeFullHunger(true, false, true, false, 0));
    }

    @Test
    void restoresAndClampsFullHungerStateSafely() {
        // Unconsumed (cooldown/cancelled): always restore exact original foodLevel
        assertEquals(20, FriendDrinkPolicy.computeRestoredFoodLevel(20, 19, false));
        assertEquals(24, FriendDrinkPolicy.computeRestoredFoodLevel(24, 19, false));

        // Consumed at standard full hunger (20 -> lowered to 19 -> ate food -> 20)
        assertEquals(20, FriendDrinkPolicy.computeRestoredFoodLevel(20, 20, true));
        // Consumed 0-nutrition food at full hunger (stayed 19 -> restores to 20)
        assertEquals(20, FriendDrinkPolicy.computeRestoredFoodLevel(20, 19, true));
        // Over-capped food bar from another mod (24 -> lowered to 19 -> restores to 24)
        assertEquals(24, FriendDrinkPolicy.computeRestoredFoodLevel(24, 20, true));
    }
}
