# Let Your Friend Drink

<p align="center">
  <img src="docs/icon.png" alt="Let Your Friend Drink icon" width="128" height="128">
</p>

An addon for **[Let Your Friend Eating](https://modrinth.com/mod/let-your-friend-eating)** (`qkQbFn8p`) that lets players share potions, honey bottles, and modded drinks with friends — with built-in harmful-drink protection, configurable item/tag blacklists, and full-hunger food feeding support.

## Features

- **Universal Drink Feeding (`UseAnim.DRINK` / `ItemUseAnimation.DRINK`)**: Feed vanilla potions, honey bottles, and modded drinks to other players using Let Your Friend Eating's native interaction, cooldowns, and statistics. Milk buckets remain delegated to Let Your Friend Eating.
- **Harmful Drink Protection (Default ON)**: Blocks feeding drinks containing any `MobEffectCategory.HARMFUL` status effect (Poison, Instant Damage, Weakness, Ominous Bottle, harmful Suspicious Stew, etc.) when `allowHarmfulDrinks = false`, displaying a red actionbar notice to the feeder. Flip `allowHarmfulDrinks = true` for friendly pranks.
- **Full-Hunger Food Feeding Bridge (Default ON)**: When `allowFeedingAtFullHunger = true`, allows feeding normal non-drink food items (Golden Apples, buff foods, modded cuisine) through Let Your Friend Eating even when the recipient's hunger bar is already full (`>= 20`), safely restoring/clamping hunger state afterward.
- **Blacklist & Datapack Tag**: Exclude specific item IDs (`minecraft:ominous_bottle`) or item tags (`#c:drinks`) via `config/let_your_friend_drink.toml`, or through the built-in `#let_your_friend_drink:excluded` datapack tag.
- **Native In-Game Config Screen & 4 Languages**: Zero-dependency vanilla config screen accessible via Mod Menu (Fabric) and native Forge / NeoForge mod screens, localized in English (`en_us`), Simplified Chinese (`zh_cn`), Traditional Chinese (`zh_tw`), and Japanese (`ja_jp`).
- **Accurate Container Return & Public Event API**: Returns empty glass bottles/containers to the feeder's hand, inventory, or ground; preserves items in Creative mode; fires `FriendDrinkCompletedEvents` after every successful drink feed.

## Requirements

- **Required**: [Let Your Friend Eating](https://modrinth.com/mod/let-your-friend-eating) (`>= 1.1.2`)
- **Zero other library dependencies** (no RinLib, no Fabric API required).

## Supported Matrix (8 Targets)

| Minecraft | Loaders | Java | Upstream LYFE Tested |
| --- | --- | --- | --- |
| `1.20.1` | Fabric, Forge | 17 | Fabric `1.1.2`, Forge `1.1.3` |
| `1.21.1` | Fabric, NeoForge | 21 | Fabric `1.1.2`, NeoForge `1.1.4` |
| `26.1.2` | Fabric, NeoForge | 25 | Fabric `1.2.0`, NeoForge `1.1.2` |
| `26.2` | Fabric, NeoForge | 25 | Fabric `1.1.2`, NeoForge `1.1.2` |

## Configuration

Stored at `config/let_your_friend_drink.toml` (auto-generated on first launch, hot-saved from the in-game config screen):

```toml
[general]
allowHarmfulDrinks = false
allowFeedingAtFullHunger = true
excludedItems = []
```

## Building from Source

```bash
python3 scripts/build-matrix.py
```

The build script runs the JUnit 5 test suite, automatically fetches and ABI-verifies the compile-time upstream `Let Your Friend Eating` JARs from Modrinth CDN, builds all 8 loader/version JARs into `build/releases/`, and audits every artifact.

## License

`GPL-3.0-or-later` — see [LICENSE](LICENSE).
