package dev.rinchan.letyourfrienddrink.config;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Zero-dependency TOML configuration for Let Your Friend Drink (config/let_your_friend_drink.toml). */
public final class FriendDrinkConfig {
    public static final String FILE_NAME = "let_your_friend_drink.toml";

    private static volatile FriendDrinkConfig active = defaults();
    private static volatile Path configPath;

    private final boolean allowHarmfulDrinks;
    private final boolean allowFeedingAtFullHunger;
    private final List<String> excludedItems;
    private final ExclusionMatcher exclusionMatcher;

    public FriendDrinkConfig(
        boolean allowHarmfulDrinks,
        boolean allowFeedingAtFullHunger,
        List<String> excludedItems
    ) {
        this.allowHarmfulDrinks = allowHarmfulDrinks;
        this.allowFeedingAtFullHunger = allowFeedingAtFullHunger;
        this.exclusionMatcher = ExclusionMatcher.compile(excludedItems);
        this.excludedItems = this.exclusionMatcher.normalizedEntries();
    }

    public static FriendDrinkConfig defaults() {
        return new FriendDrinkConfig(false, true, List.of());
    }

    public static FriendDrinkConfig get() {
        return active;
    }

    public static synchronized FriendDrinkConfig load(Path configDir) {
        Path file = configDir.resolve(FILE_NAME);
        configPath = file;
        FriendDrinkConfig loaded;
        if (Files.isRegularFile(file)) {
            try {
                String content = Files.readString(file, StandardCharsets.UTF_8);
                loaded = parse(content);
            } catch (IOException ignored) {
                loaded = defaults();
            }
        } else {
            loaded = defaults();
            saveTo(file, loaded);
        }
        active = loaded;
        return loaded;
    }

    public static synchronized void updateAndSave(FriendDrinkConfig updated) {
        active = updated != null ? updated : defaults();
        Path file = configPath;
        if (file != null) {
            saveTo(file, active);
        }
    }

    public static void saveTo(Path file, FriendDrinkConfig config) {
        try {
            Path parent = file.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            Path tmp = file.resolveSibling(file.getFileName() + ".tmp");
            Files.writeString(tmp, config.serialize(), StandardCharsets.UTF_8);
            Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException ignored) {
        }
    }

    public static FriendDrinkConfig parse(String content) {
        Map<String, Object> map = SimpleToml.parse(content);
        boolean allowHarmful = readBoolean(map, "general.allowHarmfulDrinks", "allowHarmfulDrinks", false);
        boolean allowFullHunger = readBoolean(map, "general.allowFeedingAtFullHunger", "allowFeedingAtFullHunger", true);
        List<String> excluded = readStringList(map, "general.excludedItems", "excludedItems");
        return new FriendDrinkConfig(allowHarmful, allowFullHunger, excluded);
    }

    public static List<String> parseCommaSeparatedEntries(String raw) {
        List<String> entries = new ArrayList<>();
        if (raw == null || raw.isBlank()) return entries;
        for (String part : raw.split(",")) {
            String trimmed = part.trim();
            if (!trimmed.isEmpty()) {
                entries.add(trimmed);
            }
        }
        return entries;
    }

    public String serialize() {
        StringBuilder sb = new StringBuilder();
        sb.append("# Let Your Friend Drink configuration\n\n");
        sb.append("[general]\n");
        sb.append("# Allow feeding drinks that apply harmful status effects (e.g. Poison, Harming, Ominous Bottle).\n");
        sb.append("# Default: false (blocks griefing and shows an actionbar warning to the feeder).\n");
        sb.append("allowHarmfulDrinks = ").append(allowHarmfulDrinks).append('\n');
        sb.append('\n');
        sb.append("# Allow feeding normal food items through Let Your Friend Eating even when the recipient's hunger bar is full (>= 20).\n");
        sb.append("# Default: true (useful for Golden Apples, buff foods, and modded cuisine).\n");
        sb.append("allowFeedingAtFullHunger = ").append(allowFeedingAtFullHunger).append('\n');
        sb.append('\n');
        sb.append("# Item IDs (\"minecraft:ominous_bottle\") or item tags (\"#c:drinks\") excluded from friend drink feeding.\n");
        sb.append("# Items in the datapack tag #let_your_friend_drink:excluded are also excluded.\n");
        sb.append("excludedItems = ").append(SimpleToml.formatStringArray(excludedItems)).append('\n');
        return sb.toString();
    }

    public boolean allowHarmfulDrinks() {
        return allowHarmfulDrinks;
    }

    public boolean allowFeedingAtFullHunger() {
        return allowFeedingAtFullHunger;
    }

    public List<String> excludedItems() {
        return excludedItems;
    }

    public ExclusionMatcher exclusionMatcher() {
        return exclusionMatcher;
    }

    public String formatExcludedItemsCsv() {
        return String.join(", ", excludedItems);
    }

    private static boolean readBoolean(Map<String, Object> map, String qualifiedKey, String fallbackKey, boolean def) {
        Object val = map.get(qualifiedKey);
        if (val == null) val = map.get(fallbackKey);
        if (val instanceof Boolean b) return b;
        if (val instanceof String s) {
            if ("true".equalsIgnoreCase(s)) return true;
            if ("false".equalsIgnoreCase(s)) return false;
        }
        return def;
    }

    private static List<String> readStringList(Map<String, Object> map, String qualifiedKey, String fallbackKey) {
        Object val = map.get(qualifiedKey);
        if (val == null) val = map.get(fallbackKey);
        List<String> out = new ArrayList<>();
        if (val instanceof List<?> list) {
            for (Object item : list) {
                if (item != null) {
                    out.add(String.valueOf(item));
                }
            }
        }
        return out;
    }
}
