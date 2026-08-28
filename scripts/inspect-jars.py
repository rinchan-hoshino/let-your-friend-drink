#!/usr/bin/env python3
import argparse
import json
from pathlib import Path
from zipfile import ZipFile


def require_bytes(data: bytes, values: list[bytes], owner: str) -> None:
    for value in values:
        if value not in data:
            raise AssertionError(f"{owner} is missing {value!r}")


def require_package(data: bytes, package: str, owner: str) -> None:
    forms = [package.encode(), package.replace("/", ".").encode()]
    if not any(value in data for value in forms):
        raise AssertionError(f"{owner} does not reference package {package!r}")


def inspect_parent(path: Path, package: str, target: str) -> None:
    class_name = f"{package}/letyourfriendeating/logic/PlayerFeeder.class"
    stats_name = f"{package}/letyourfriendeating/data/FeedStatsSavedData.class"
    with ZipFile(path) as jar:
        data = jar.read(class_name)
        stats = jar.read(stats_name)
    require_bytes(data, [target.encode()], f"parent {path.name}:{class_name}")
    require_bytes(stats, [b"incrementFedCount", b"incrementEatenCount"], f"parent {path.name}:{stats_name}")


def inspect_addon(path: Path, loader: str, minecraft: str, version: str, package: str, target: str) -> None:
    prefix = f"dev/rinchan/letyourfrienddrink/{loader}"
    mixin_class = f"{prefix}/mixin/PlayerFeederMixin.class"
    bridge_name = "FabricFriendEatingBridge" if loader == "fabric" else "NeoForgeFriendEatingBridge"
    bridge_class = f"{prefix}/{bridge_name}.class"
    with ZipFile(path) as jar:
        names = set(jar.namelist())
        metadata_name = "fabric.mod.json" if loader == "fabric" else "META-INF/neoforge.mods.toml"
        metadata = jar.read(metadata_name).decode()
        mixin = jar.read(mixin_class)
        bridge = jar.read(bridge_class)
        feeder = jar.read("dev/rinchan/letyourfrienddrink/FriendDrinkFeeder.class")
        callbacks = jar.read("dev/rinchan/letyourfrienddrink/api/FriendDrinkCompletedEvents.class")

    for required in [
        "dev/rinchan/letyourfrienddrink/FriendEatingAccess.class",
        "dev/rinchan/letyourfrienddrink/api/FriendDrinkCompletedEvent.class",
        "assets/let_your_friend_drink/icon.png",
    ]:
        if required not in names:
            raise AssertionError(f"{path.name} is missing {required}")

    require_bytes(mixin, [target.encode()], f"{path.name} mixin")
    require_package(mixin, package, f"{path.name} mixin")
    bridge_contract = [
        b"CooldownManager",
        b"FeedStatsSavedData",
        b"incrementFedCount",
        b"incrementEatenCount",
    ]
    if loader == "neoforge":
        bridge_contract.extend([b"finishUsingItem", b"EventHooks", b"onItemUseFinish"])
    require_bytes(bridge, bridge_contract, f"{path.name} bridge")
    require_package(bridge, package, f"{path.name} bridge")

    require_bytes(feeder, [b"FriendEatingAccess", b"FriendDrinkCompletedEvents", b"finishDrink"], f"{path.name} feeder")
    require_bytes(
        callbacks,
        [b"Duplicate friend-drink listener id", b"Friend-drink completion listener"],
        path.name,
    )

    for value in ["Let Your Friend Drink", version, "letyourfriendeating"]:
        if value not in metadata:
            raise AssertionError(f"{path.name} metadata is missing {value!r}")
    if minecraft not in metadata:
        raise AssertionError(f"{path.name} metadata does not mention Minecraft {minecraft}")
    for forbidden in ["Let Your Friend 喝药", "wmf_feeding_compat", "NoHunger", "raise_resurrection_ascend"]:
        if forbidden in metadata:
            raise AssertionError(f"{path.name} metadata contains private identity {forbidden!r}")


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--minecraft", required=True)
    parser.add_argument("--version", required=True)
    parser.add_argument("--fabric", type=Path, required=True)
    parser.add_argument("--neoforge", type=Path, required=True)
    parser.add_argument("--parent-fabric", type=Path, required=True)
    parser.add_argument("--parent-neoforge", type=Path, required=True)
    parser.add_argument("--fabric-parent-package", required=True)
    parser.add_argument("--neoforge-parent-package", required=True)
    parser.add_argument("--fabric-target", required=True)
    parser.add_argument("--neoforge-target", required=True)
    args = parser.parse_args()

    inspect_parent(args.parent_fabric, args.fabric_parent_package, args.fabric_target)
    inspect_parent(args.parent_neoforge, args.neoforge_parent_package, args.neoforge_target)
    inspect_addon(args.fabric, "fabric", args.minecraft, args.version, args.fabric_parent_package, args.fabric_target)
    inspect_addon(args.neoforge, "neoforge", args.minecraft, args.version, args.neoforge_parent_package, args.neoforge_target)
    print(json.dumps({"minecraft": args.minecraft, "fabric": str(args.fabric), "neoforge": str(args.neoforge), "status": "ok"}))


if __name__ == "__main__":
    main()
