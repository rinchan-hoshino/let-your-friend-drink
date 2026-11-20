package dev.rinchan.letyourfrienddrink.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

final class FriendDrinkConfigTest {
    @Test
    void defaultsProtectAgainstHarmfulDrinksAndEnableFullHunger() {
        FriendDrinkConfig defaults = FriendDrinkConfig.defaults();
        assertFalse(defaults.allowHarmfulDrinks());
        assertTrue(defaults.allowFeedingAtFullHunger());
        assertTrue(defaults.excludedItems().isEmpty());
    }

    @Test
    void roundTripsThroughSimpleToml() {
        FriendDrinkConfig original = new FriendDrinkConfig(
            true,
            false,
            List.of("minecraft:ominous_bottle", "honey_bottle", "#c:drinks", "invalid::id", " ")
        );
        String toml = original.serialize();
        FriendDrinkConfig parsed = FriendDrinkConfig.parse(toml);

        assertTrue(parsed.allowHarmfulDrinks());
        assertFalse(parsed.allowFeedingAtFullHunger());
        assertEquals(
            List.of("minecraft:ominous_bottle", "minecraft:honey_bottle", "#c:drinks"),
            parsed.excludedItems()
        );
    }

    @Test
    void exclusionMatcherSupportsItemIdsAndTags() {
        ExclusionMatcher matcher = ExclusionMatcher.compile(
            List.of("minecraft:ominous_bottle", "suspicious_stew", "#let_your_friend_drink:blocked_brews")
        );
        assertTrue(matcher.matches("minecraft:ominous_bottle", tag -> false));
        assertTrue(matcher.matches("minecraft:suspicious_stew", tag -> false));
        assertFalse(matcher.matches("minecraft:honey_bottle", tag -> false));
        assertTrue(matcher.matches(
            "modid:mystery_tea",
            tag -> Set.of("let_your_friend_drink:blocked_brews").contains(tag)
        ));
    }

    @Test
    void loadsCreatesAndHotSavesTomlFile(@TempDir Path tempDir) {
        FriendDrinkConfig initial = FriendDrinkConfig.load(tempDir);
        Path configFile = tempDir.resolve(FriendDrinkConfig.FILE_NAME);
        assertTrue(Files.isRegularFile(configFile));
        assertFalse(initial.allowHarmfulDrinks());
        assertTrue(initial.allowFeedingAtFullHunger());

        FriendDrinkConfig updated = new FriendDrinkConfig(true, false, List.of("minecraft:potion", "#c:alcohol"));
        FriendDrinkConfig.updateAndSave(updated);

        FriendDrinkConfig reloaded = FriendDrinkConfig.load(tempDir);
        assertTrue(reloaded.allowHarmfulDrinks());
        assertFalse(reloaded.allowFeedingAtFullHunger());
        assertEquals(List.of("minecraft:potion", "#c:alcohol"), reloaded.excludedItems());
    }
}
