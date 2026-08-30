package dev.rinchan.letyourfrienddrink;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.function.BiPredicate;
import org.junit.jupiter.api.Test;

final class FriendDrinkPolicyPropertyTest {
    @Test
    void ownershipAndConsumptionPoliciesMatchTheirTruthTablesForEveryInput() {
        verifyShouldHandle(FriendDrinkPolicy::shouldHandle);
        verifyPreservesHeldItem(FriendDrinkPolicy::preservesHeldItem);
    }

    @Test
    void negativeFixturesRejectTypicalOrAndAndRegressions() {
        BiPredicate<Boolean, Boolean> handlesWithOr = (drinkAnimation, handledByBaseMod) ->
            drinkAnimation || !handledByBaseMod;
        BiPredicate<Boolean, Boolean> preservesWithAnd = (feederCreative, unchangedRemainder) ->
            feederCreative && unchangedRemainder;

        assertThrows(AssertionError.class, () -> verifyShouldHandle(handlesWithOr));
        assertThrows(AssertionError.class, () -> verifyPreservesHeldItem(preservesWithAnd));
    }

    private static void verifyShouldHandle(BiPredicate<Boolean, Boolean> rule) {
        for (boolean drinkAnimation : new boolean[] {false, true}) {
            for (boolean handledByBaseMod : new boolean[] {false, true}) {
                boolean expected = drinkAnimation && !handledByBaseMod;
                assertEquals(
                    expected,
                    rule.test(drinkAnimation, handledByBaseMod),
                    () -> "drinkAnimation=" + drinkAnimation + ", handledByBaseMod=" + handledByBaseMod
                );
            }
        }
    }

    private static void verifyPreservesHeldItem(BiPredicate<Boolean, Boolean> rule) {
        for (boolean feederCreative : new boolean[] {false, true}) {
            for (boolean unchangedRemainder : new boolean[] {false, true}) {
                boolean expected = feederCreative || unchangedRemainder;
                assertEquals(
                    expected,
                    rule.test(feederCreative, unchangedRemainder),
                    () -> "feederCreative=" + feederCreative + ", unchangedRemainder=" + unchangedRemainder
                );
            }
        }
    }
}
