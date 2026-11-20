#!/usr/bin/env python3
"""Build all 8 supported loader/version targets from profiles.json, verify upstream ABI, and audit JARs."""
import argparse
import fcntl
import hashlib
import json
import os
from pathlib import Path
import shutil
import subprocess
import urllib.request
import zipfile

ROOT = Path(__file__).resolve().parents[1]
MAX_JAR_BYTES = 256 * 1024
REQUIRED_LANGS = ("en_us.json", "zh_cn.json", "zh_tw.json", "ja_jp.json")


def discover_jdk(major: int) -> str:
    for env_key in (
        f"JAVA_{major}_HOME",
        f"JAVA_HOME_{major}_X64",
        f"JAVA_HOME_{major}_ARM64",
    ):
        val = os.environ.get(env_key, "").strip()
        if val and Path(val).is_dir():
            return val
    candidates = [
        Path(f"/opt/homebrew/opt/openjdk@{major}/libexec/openjdk.jdk/Contents/Home"),
        Path(f"/Users/rin/Library/Java/JavaVirtualMachines/temurin-{major}.jdk/Contents/Home"),
    ]
    for pattern in (
        f"/opt/homebrew/Cellar/openjdk@{major}/*/libexec/openjdk.jdk/Contents/Home",
        f"{Path.home()}/.gradle/jdks/*-{major}-*/Contents/Home",
    ):
        for p in sorted(Path("/").glob(pattern.lstrip("/")), reverse=True):
            candidates.append(p)
    for c in candidates:
        if (c / "bin/java").is_file():
            return str(c)
    raise RuntimeError(f"Could not find JDK {major}; set {env_key}")


def ensure_and_verify_lyfe_abi(mc: str, loader: str, meta: dict, javap_bin: str):
    cache = ROOT / "build/deps/lyfe"
    cache.mkdir(parents=True, exist_ok=True)
    jar_path = cache / meta["file"]
    if not jar_path.is_file() or jar_path.stat().st_size == 0:
        req = urllib.request.Request(meta["url"], headers={"User-Agent": "let-your-friend-drink/2.0.0"})
        with urllib.request.urlopen(req) as resp:
            jar_path.write_bytes(resp.read())

    pkg = meta["package"]
    expected_method = meta["method"]
    feeder_cls = f"{pkg}.logic.PlayerFeeder"
    cooldown_cls = f"{pkg}.manager.CooldownManager"
    stats_cls = f"{pkg}.data.FeedStatsSavedData"

    out = subprocess.check_output(
        [javap_bin, "-cp", str(jar_path), "-p", feeder_cls, cooldown_cls, stats_cls],
        text=True,
    )
    for needle in (
        f"{expected_method}(",
        "canFeed(java.util.UUID)",
        "recordFeed(java.util.UUID)",
        "incrementFedCount(java.util.UUID)",
        "incrementEatenCount(java.util.UUID)",
    ):
        if needle not in out:
            raise RuntimeError(f"Upstream ABI mismatch in {mc} {loader} ({meta['file']}): missing {needle}")
    print(f"ABI OK {mc} {loader}: {feeder_cls}#{expected_method}", flush=True)


def verify_jar(jar_path: Path, mc: str):
    size = jar_path.stat().st_size
    if size >= MAX_JAR_BYTES:
        raise RuntimeError(f"JAR exceeds budget ({MAX_JAR_BYTES}B): {jar_path.name} ({size} bytes)")
    with zipfile.ZipFile(jar_path) as zf:
        names = set(zf.namelist())
        for name in names:
            lower = name.lower()
            if "rinlib" in lower:
                raise RuntimeError(f"Forbidden rinlib entry found in {jar_path.name}: {name}")
            if "com/terraformersmc/modmenu/" in lower:
                raise RuntimeError(f"Forbidden modmenu stub leaked into {jar_path.name}: {name}")
        if "assets/let_your_friend_drink/icon.png" not in names:
            raise RuntimeError(f"Missing icon.png in {jar_path.name}")
        for lang in REQUIRED_LANGS:
            expected = f"assets/let_your_friend_drink/lang/{lang}"
            if expected not in names:
                raise RuntimeError(f"Missing lang file {expected} in {jar_path.name}")
        tag_path = (
            "data/let_your_friend_drink/tags/items/excluded.json"
            if mc == "1.20.1"
            else "data/let_your_friend_drink/tags/item/excluded.json"
        )
        if tag_path not in names:
            raise RuntimeError(f"Missing datapack tag {tag_path} in {jar_path.name}")


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--profile", help="Optional single MC version to build")
    parser.add_argument("--loader", help="Optional single loader to build")
    parser.add_argument("--skip-tests", action="store_true", help="Skip JUnit 5 pure-logic test run")
    args = parser.parse_args()

    profiles = json.loads((ROOT / "profiles.json").read_text())
    version = next(
        line.split("=", 1)[1].strip()
        for line in (ROOT / "gradle.properties").read_text().splitlines()
        if line.startswith("mod_version=")
    )
    output = ROOT / "build/releases"
    output.mkdir(parents=True, exist_ok=True)
    lock = (ROOT / "build/matrix.lock").open("w")
    fcntl.flock(lock, fcntl.LOCK_EX)

    all_jdks = {major: discover_jdk(major) for major in sorted({p["java"] for p in profiles.values()})}
    installations_paths = ",".join(all_jdks.values())
    javap_bin = str(Path(all_jdks[max(all_jdks)]) / "bin/javap")

    if not args.skip_tests:
        test_env = os.environ.copy()
        test_env["JAVA_HOME"] = all_jdks.get(21, all_jdks[max(all_jdks)])
        subprocess.run(
            [
                str(ROOT / "gradlew"),
                ":test",
                "-Pminecraft_version=1.21.1",
                "-Ploader=all",
                "-Porg.gradle.java.installations.paths=" + installations_paths,
                "--no-configuration-cache",
            ],
            cwd=ROOT,
            env=test_env,
            check=True,
        )
        print("UNIT TESTS PASSED", flush=True)

    manifest = []
    for minecraft, profile in profiles.items():
        if args.profile and minecraft != args.profile:
            continue
        loaders = [l for l in profile["loaders"] if not args.loader or l == args.loader]
        if not loaders:
            continue

        for loader in loaders:
            ensure_and_verify_lyfe_abi(minecraft, loader, profile["lyfe"][loader], javap_bin)

        env = os.environ.copy()
        daemon_java = all_jdks.get(21) if profile["java"] < 21 else all_jdks[profile["java"]]
        env["JAVA_HOME"] = daemon_java

        tasks = [f":{loader}:clean" for loader in loaders] + [f":{loader}:build" for loader in loaders]
        command = [
            str(ROOT / "gradlew"),
            *tasks,
            "-Pminecraft_version=" + minecraft,
            "-Ploader=all",
            "-Porg.gradle.java.installations.paths=" + installations_paths,
            "--no-configuration-cache",
            "--max-workers=2",
        ]
        subprocess.run(command, cwd=ROOT, env=env, check=True)

        for loader in loaders:
            source = (
                ROOT
                / f"build/profiles/{minecraft}/{loader}/libs/let_your_friend_drink-{loader}-{version}+{minecraft}.jar"
            )
            target = output / f"let-your-friend-drink-{version}+{minecraft}-{loader}.jar"
            shutil.copy2(source, target)
            verify_jar(target, minecraft)
            binary = target.read_bytes()
            manifest.append(
                dict(
                    minecraft=minecraft,
                    loader=loader,
                    file=str(target),
                    bytes=len(binary),
                    sha256=hashlib.sha256(binary).hexdigest(),
                    sha512=hashlib.sha512(binary).hexdigest(),
                )
            )
            print(f"BUILT {minecraft} {loader}: {target.name} ({len(binary):,} bytes)", flush=True)

    (output / "manifest.json").write_text(json.dumps(manifest, indent=2) + "\n")
    print(f"ALL DONE ({len(manifest)} targets).", flush=True)


if __name__ == "__main__":
    main()
