# Feature Map — Let Your Friend Drink 2.0.0

## Architecture Overview

```text
common/src/main/java/dev/rinchan/letyourfrienddrink/
├── LetYourFriendDrink.java            # Mod ID & config initialization entry point
├── FriendDrinkPolicy.java             # Pure-Java predicates (drink handling, harmful block, remainder, full-hunger)
├── FriendDrinkFeeder.java             # Core drink-feeding execution, feedback, container return, event dispatch
├── FullHungerBridge.java              # ThreadLocal safe hunger-level bridge for full-hunger food feeding
├── FriendEatingAccess.java            # Static bridge registry
├── FriendEatingBridge.java            # Upstream Let Your Friend Eating bridge contract
├── ReflectiveFriendEatingBridge.java  # Dual-package (me.tamkungz + th.in.tamkungz) cached reflection bridge
├── api/
│   ├── FriendDrinkCompletedEvent.java
│   └── FriendDrinkCompletedEvents.java
└── config/
    ├── SimpleToml.java                # Zero-dependency TOML parser/serializer
    ├── ExclusionMatcher.java          # Item ID & #tag blacklist compiler/matcher
    └── FriendDrinkConfig.java         # Atomic TOML config loader/saver (config/let_your_friend_drink.toml)

common/src/{v120,v121,v26}/
├── java/.../VersionBridge.java        # Version-specific drink/food/harmful/tag/sound/actionbar adapters
├── java/.../client/FriendDrinkConfigScreen.java  # Native vanilla config screen per MC GUI API
└── resources/data/let_your_friend_drink/tags/{items|item}/excluded.json

fabric/ | forge/ | neoforge/
├── Loader entrypoints + isolated `client/` config screen registration (ModMenu compile-only stub on Fabric)
├── mixin/
│   ├── PlayerFeederMixin.java         # @Pseudo @Inject HEAD+RETURN for th.in.tamkungz (1.1.x)
│   └── PlayerFeederV120Mixin.java     # @Pseudo @Inject HEAD+RETURN for me.tamkungz (1.2.x+)
└── neoforge/src/{e2e,e2eClientTest}/  # Packaged-JAR GameTestServer + Client Screen lifecycle E2E suite (MC 1.21.1)
```

## Feature Matrix

| Feature | Config Key / Tag | Default | Scope |
| --- | --- | --- | --- |
| Universal drink feeding | `UseAnim.DRINK` / `ItemUseAnimation.DRINK` | Always on (milk delegated to LYFE) | Server |
| Harmful drink blocking | `general.allowHarmfulDrinks` | `false` (block + red actionbar) | Server |
| Full-hunger food feeding | `general.allowFeedingAtFullHunger` | `true` | Server |
| Item / tag blacklist | `general.excludedItems` + `#let_your_friend_drink:excluded` | `[]` | Server |
| Container return & creative handling | Automatic | On | Server |
| Public completion API | `FriendDrinkCompletedEvents.register(...)` | Always available | Server |
| Native Config Screen | Mod Menu (Fabric), Mods screen (Forge/NeoForge) | Hot-save to TOML | Client |
| Localization | `en_us`, `zh_cn`, `zh_tw`, `ja_jp` | Bundled | Client/Server |

## Verification Gates

All three verification gates are enforced in `.github/workflows/ci.yml`:

| Gate | Scope | Command |
| --- | --- | --- |
| **Gate 1 — Packaged-JAR Real Runtime E2E** | Real MC 1.21.1 NeoForge `GameTestServer` + real packaged `.jar` + real upstream `Let Your Friend Eating` & `Cloth Config` JARs (harmful Poison/Ominous blocking + red actionbar, Regeneration/Honey feeding + empty glass bottle return, creative non-consumption, full-hunger Golden Apple/Bread safe feeding) + packaged-JAR `FriendDrinkConfigScreen` init/widget lifecycle | `./gradlew :neoforge:e2eTest -Pminecraft_version=1.21.1 -Ploader=neoforge --no-configuration-cache` |
| **Gate 2 — Property Boundary Fuzzing (>= 2,000 fixed-seed trials)** | `FriendDrinkPolicyPropertyTest` (multi-effect mixed beneficial+harmful aggregation, random hunger `0..20` & over-cap restoration invariants, random ID/#tag blacklist matching & TOML round-trip) + unit tests | `./gradlew :test -Pminecraft_version=1.21.1 -Ploader=all --no-configuration-cache` |
| **Gate 3 — Architecture Contract Assertions** | `ArchitectureContractTest` scanning real built `.jar`s: Dedicated Server bytecode constant-pool isolation (zero client/GUI refs outside `*.client.*`), Mixin bidirectional closure (`*.mixins.json` <-> JAR `.class` <-> `@Mixin` source), zero `rinlib` / `modmenu` stub leaks | `./gradlew :architectureTest -Pminecraft_version=1.21.1 -Ploader=all --no-configuration-cache` (and automatically across all 8 release JARs via `python3 scripts/build-matrix.py`) |
