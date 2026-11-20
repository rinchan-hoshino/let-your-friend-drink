package dev.rinchan.letyourfrienddrink;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

final class FriendDrinkPolicyPropertyTest {
    @Test
    void exhaustiveDrinkAndHarmfulTruthTable() {
        boolean[] bools = {false, true};
        for (boolean drink : bools) {
            for (boolean milk : bools) {
                for (boolean excluded : bools) {
                    assertEquals(
                        drink && !milk && !excluded,
                        FriendDrinkPolicy.shouldHandle(drink, milk, excluded)
                    );
                }
            }
        }
        for (boolean harmful : bools) {
            for (boolean allowHarmful : bools) {
                assertEquals(
                    harmful && !allowHarmful,
                    FriendDrinkPolicy.shouldBlockHarmfulDrink(harmful, allowHarmful)
                );
            }
        }
    }

    @Test
    void fullHungerRestorationNeverReducesOriginalFoodLevel() {
        for (int original = 20; original <= 30; original++) {
            for (int after = 0; after <= 30; after++) {
                for (boolean consumed : new boolean[] {false, true}) {
                    int restored = FriendDrinkPolicy.computeRestoredFoodLevel(original, after, consumed);
                    assertTrue(restored >= original, "Restored food level must never drop below original");
                }
            }
        }
    }
}
