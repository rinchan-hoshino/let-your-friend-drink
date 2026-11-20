package dev.rinchan.letyourfrienddrink;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.rinchan.letyourfrienddrink.config.ExclusionMatcher;
import dev.rinchan.letyourfrienddrink.config.FriendDrinkConfig;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.SplittableRandom;
import org.junit.jupiter.api.Test;

/** Fixed-seed property-based boundary fuzz tests (>= 2,000 iterations per invariant). */
final class FriendDrinkPolicyPropertyTest {
    private static final int ITERATIONS = 2_000;
    private static final long SEED_MULTI_EFFECT = 0x5EED_0001_2026L;
    private static final long SEED_HUNGER = 0x5EED_0002_2026L;
    private static final long SEED_BLACKLIST = 0x5EED_0003_2026L;
    private static final long SEED_GENERAL = 0x5EED_0004_2026L;

    private static final String[] NAMESPACES = {"minecraft", "c", "forge", "lyfd", "farmersdelight", "create"};
    private static final String[] PATHS = {
        "potion", "honey_bottle", "ominous_bottle", "golden_apple", "bread",
        "suspicious_stew", "milk_bucket", "drinks/tea", "brews/dark_ale", "food/soup"
    };
    private static final String[] INVALID_ENTRIES = {
        "", "   ", "Invalid:Caps", "double::colon", "bad space:item", "#bad::tag", "minecraft:", ":only_path", "#"
    };

    private enum EffectKind {
        BENEFICIAL,
        NEUTRAL,
        HARMFUL
    }

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
    void multiEffectPotionHarmfulAggregationAndBlockingProperty() {
        SplittableRandom rng = new SplittableRandom(SEED_MULTI_EFFECT);
        EffectKind[] kinds = EffectKind.values();

        for (int i = 0; i < ITERATIONS; i++) {
            int effectCount = rng.nextInt(0, 13);
            boolean ominous = rng.nextInt(8) == 0;
            boolean allowHarmful = rng.nextBoolean();

            List<Boolean> effectHarmfulness = new ArrayList<>(effectCount);
            boolean expectedHarmful = ominous;
            boolean hasBeneficial = false;

            for (int e = 0; e < effectCount; e++) {
                EffectKind kind = kinds[rng.nextInt(kinds.length)];
                if (kind == EffectKind.HARMFUL) {
                    effectHarmfulness.add(Boolean.TRUE);
                    expectedHarmful = true;
                } else {
                    effectHarmfulness.add(Boolean.FALSE);
                    if (kind == EffectKind.BENEFICIAL) {
                        hasBeneficial = true;
                    }
                }
            }

            boolean actualHarmful = FriendDrinkPolicy.hasAnyHarmfulEffect(ominous, effectHarmfulness);
            assertEquals(expectedHarmful, actualHarmful, "Iteration " + i + ": harmful aggregation mismatch");

            boolean blocked = FriendDrinkPolicy.shouldBlockHarmfulDrink(actualHarmful, allowHarmful);
            if (expectedHarmful && !allowHarmful) {
                assertTrue(blocked, "Iteration " + i + ": mixed/harmful drink MUST be blocked when allowHarmfulDrinks=false");
            } else {
                assertFalse(blocked, "Iteration " + i + ": safe or allowed drink must not be blocked");
            }

            // Explicitly verify mixed beneficial + harmful combinations are ALWAYS blocked when allowHarmful=false
            if (hasBeneficial && expectedHarmful) {
                assertTrue(
                    FriendDrinkPolicy.shouldBlockHarmfulDrink(actualHarmful, false),
                    "Iteration " + i + ": beneficial effect must NEVER mask a harmful effect"
                );
            }
        }
    }

    @Test
    void randomHungerBoundaryStateRestorationProperty() {
        SplittableRandom rng = new SplittableRandom(SEED_HUNGER);

        for (int i = 0; i < ITERATIONS; i++) {
            // Cover 0..20 heavily (85%) and over-capped modded hunger 21..30 (15%)
            int originalFoodLevel = rng.nextInt(100) < 85 ? rng.nextInt(0, 21) : rng.nextInt(21, 31);
            boolean allowFullHunger = rng.nextBoolean();
            boolean isDrink = rng.nextBoolean();
            boolean isFood = rng.nextBoolean();
            boolean excluded = rng.nextBoolean();
            boolean itemConsumed = rng.nextBoolean();
            int nutrition = rng.nextInt(0, 21);

            boolean shouldBridge = FriendDrinkPolicy.shouldBridgeFullHunger(
                allowFullHunger, isDrink, isFood, excluded, originalFoodLevel
            );

            if (originalFoodLevel < FriendDrinkPolicy.FULL_HUNGER_LEVEL) {
                assertFalse(shouldBridge, "Iteration " + i + ": must never bridge when hunger < 20");
            } else {
                assertEquals(
                    allowFullHunger && !isDrink && isFood && !excluded,
                    shouldBridge,
                    "Iteration " + i + ": full-hunger bridge predicate mismatch"
                );
            }

            if (shouldBridge) {
                int lowered = FriendDrinkPolicy.BRIDGED_HUNGER_LEVEL;
                assertEquals(19, lowered);
                int afterFeed = itemConsumed ? Math.min(20, lowered + nutrition) : lowered;
                int restored = FriendDrinkPolicy.computeRestoredFoodLevel(originalFoodLevel, afterFeed, itemConsumed);

                assertTrue(
                    restored >= originalFoodLevel,
                    "Iteration " + i + ": restored hunger (" + restored + ") dropped below original (" + originalFoodLevel + ")"
                );
                if (originalFoodLevel <= FriendDrinkPolicy.FULL_HUNGER_LEVEL) {
                    assertTrue(
                        restored >= 0 && restored <= FriendDrinkPolicy.FULL_HUNGER_LEVEL,
                        "Iteration " + i + ": restored vanilla hunger out of [0..20]: " + restored
                    );
                }
                if (!itemConsumed) {
                    assertEquals(
                        originalFoodLevel,
                        restored,
                        "Iteration " + i + ": unconsumed feed must restore exact original foodLevel"
                    );
                }
                if (originalFoodLevel == FriendDrinkPolicy.FULL_HUNGER_LEVEL) {
                    assertEquals(
                        FriendDrinkPolicy.FULL_HUNGER_LEVEL,
                        restored,
                        "Iteration " + i + ": full-hunger (20) feed must end at legal foodLevel 20"
                    );
                }
            } else {
                // Even if computeRestoredFoodLevel is invoked on arbitrary 0..20 values, it must remain legal
                int afterArbitrary = rng.nextInt(0, 21);
                int restored = FriendDrinkPolicy.computeRestoredFoodLevel(originalFoodLevel, afterArbitrary, itemConsumed);
                assertTrue(restored >= originalFoodLevel, "Iteration " + i + ": restored dropped below original");
                if (originalFoodLevel <= FriendDrinkPolicy.FULL_HUNGER_LEVEL) {
                    assertTrue(restored <= FriendDrinkPolicy.FULL_HUNGER_LEVEL, "Iteration " + i + ": vanilla hunger overflow");
                }
            }
        }
    }

    @Test
    void randomBlacklistIdAndTagMatchingAndTomlRoundTripProperty() {
        SplittableRandom rng = new SplittableRandom(SEED_BLACKLIST);

        for (int i = 0; i < ITERATIONS; i++) {
            int entryCount = rng.nextInt(0, 10);
            List<String> rawEntries = new ArrayList<>(entryCount);
            Set<String> expectedItems = new HashSet<>();
            Set<String> expectedTags = new HashSet<>();

            for (int e = 0; e < entryCount; e++) {
                int mode = rng.nextInt(4);
                if (mode == 0) {
                    // Explicit namespace:path item ID
                    String ns = NAMESPACES[rng.nextInt(NAMESPACES.length)];
                    String path = PATHS[rng.nextInt(PATHS.length)];
                    String id = ns + ":" + path;
                    rawEntries.add(rng.nextBoolean() ? " " + id + " " : id);
                    expectedItems.add(id);
                } else if (mode == 1) {
                    // Implicit minecraft namespace item ID
                    String path = PATHS[rng.nextInt(PATHS.length)];
                    rawEntries.add(path);
                    expectedItems.add("minecraft:" + path);
                } else if (mode == 2) {
                    // Tag entry (#namespace:path or #path)
                    boolean implicitNs = rng.nextBoolean();
                    String ns = implicitNs ? "minecraft" : NAMESPACES[rng.nextInt(NAMESPACES.length)];
                    String path = PATHS[rng.nextInt(PATHS.length)];
                    rawEntries.add("#" + (implicitNs ? path : ns + ":" + path));
                    expectedTags.add(ns + ":" + path);
                } else {
                    // Invalid entry — must be ignored cleanly
                    rawEntries.add(INVALID_ENTRIES[rng.nextInt(INVALID_ENTRIES.length)]);
                }
            }

            ExclusionMatcher matcher = ExclusionMatcher.compile(rawEntries);
            assertEquals(expectedItems, matcher.itemIds(), "Iteration " + i + ": compiled item IDs mismatch");
            assertEquals(expectedTags, matcher.tagIds(), "Iteration " + i + ": compiled tag IDs mismatch");

            // Probe random candidate item + tag set
            String candidateItem = NAMESPACES[rng.nextInt(NAMESPACES.length)] + ":" + PATHS[rng.nextInt(PATHS.length)];
            Set<String> candidateTags = new HashSet<>();
            int tagCount = rng.nextInt(0, 4);
            for (int t = 0; t < tagCount; t++) {
                candidateTags.add(NAMESPACES[rng.nextInt(NAMESPACES.length)] + ":" + PATHS[rng.nextInt(PATHS.length)]);
            }

            boolean expectedMatch = expectedItems.contains(candidateItem)
                || candidateTags.stream().anyMatch(expectedTags::contains);
            assertEquals(
                expectedMatch,
                matcher.matches(candidateItem, candidateTags::contains),
                "Iteration " + i + ": ExclusionMatcher.matches mismatch"
            );

            // Every 10th iteration, verify full TOML serialize/parse round-trip preserves exact matcher semantics
            if (i % 10 == 0) {
                FriendDrinkConfig cfg = new FriendDrinkConfig(rng.nextBoolean(), rng.nextBoolean(), rawEntries);
                FriendDrinkConfig reparsed = FriendDrinkConfig.parse(cfg.serialize());
                assertEquals(cfg.allowHarmfulDrinks(), reparsed.allowHarmfulDrinks());
                assertEquals(cfg.allowFeedingAtFullHunger(), reparsed.allowFeedingAtFullHunger());
                assertEquals(cfg.excludedItems(), reparsed.excludedItems());
                assertEquals(
                    expectedMatch,
                    reparsed.exclusionMatcher().matches(candidateItem, candidateTags::contains)
                );
            }
        }
    }

    @Test
    void randomDrinkHandlingAndRemainderPreservationProperty() {
        SplittableRandom rng = new SplittableRandom(SEED_GENERAL);
        for (int i = 0; i < ITERATIONS; i++) {
            boolean drink = rng.nextBoolean();
            boolean milk = rng.nextBoolean();
            boolean excluded = rng.nextBoolean();
            assertEquals(
                drink && !milk && !excluded,
                FriendDrinkPolicy.shouldHandle(drink, milk, excluded)
            );

            boolean creative = rng.nextBoolean();
            boolean unchanged = rng.nextBoolean();
            assertEquals(
                creative || unchanged,
                FriendDrinkPolicy.preservesHeldItem(creative, unchanged)
            );
        }
    }
}
