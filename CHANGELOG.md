# Changelog

## 1.0.2

- Routes NeoForge drink completion through `EventHooks.onItemUseFinish`, matching the native living-entity completion chain.
- Lets No Hunger and other NeoForge finish listeners observe recipient-side drink completion without private callbacks.

## 1.0.1

- Add Fabric alongside NeoForge.
- Support Minecraft 1.21.1, 26.1.2 and 26.2 wherever Let Your Friend Eating is published.
- Replace the NeoForge-only completion event with a loader-neutral, failure-isolated callback API.

## 1.0.0

- Add native recipient-side completion for drink-animation items.
- Reuse Let Your Friend Eating cooldowns, feeding statistics and interaction flow.
- Return native remainder items to the feeder.
- Publish a completion event for compatibility add-ons.
- Leave the base mod's milk bucket behavior unchanged.
