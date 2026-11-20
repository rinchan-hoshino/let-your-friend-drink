#!/usr/bin/env bash
set -euo pipefail

REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "${REPO_ROOT}"

USE_CONTAINER=0
for arg in "$@"; do
    case "${arg}" in
        --container)
            USE_CONTAINER=1
            ;;
        -h|--help)
            echo "Usage: $0 [--container]"
            exit 0
            ;;
        *)
            echo "Unknown option: ${arg}" >&2
            exit 2
            ;;
    esac
done

discover_jdk() {
    local major="$1"
    local var_name="JAVA_${major}_HOME"
    if [[ -n "${!var_name:-}" && -x "${!var_name}/bin/java" ]]; then
        printf '%s\n' "${!var_name}"
        return 0
    fi
    for candidate in \
        "/opt/jdks/temurin-${major}" \
        "/opt/homebrew/opt/openjdk@${major}/libexec/openjdk.jdk/Contents/Home" \
        "${HOME}/Library/Java/JavaVirtualMachines/temurin-${major}.jdk/Contents/Home" \
        /opt/homebrew/Cellar/openjdk@${major}/*/libexec/openjdk.jdk/Contents/Home \
        "${HOME}"/.gradle/jdks/*-"${major}"-*/Contents/Home; do
        if [[ -x "${candidate}/bin/java" ]]; then
            printf '%s\n' "${candidate}"
            return 0
        fi
    done
    return 1
}

run_in_container() {
    local engine=""
    if command -v docker >/dev/null 2>&1; then
        engine="docker"
    elif command -v podman >/dev/null 2>&1; then
        engine="podman"
    else
        echo "Error: Neither docker nor podman is available, and required JDKs (17/21/25) are missing on host." >&2
        exit 1
    fi

    local image_tag="let-your-friend-drink-verify:local"
    echo "==> Building verification container image (${image_tag}) with ${engine}..."
    "${engine}" build -f "${REPO_ROOT}/Dockerfile.verify" -t "${image_tag}" "${REPO_ROOT}"

    echo "==> Running full verification gates inside container..."
    exec "${engine}" run --rm \
        -v "${REPO_ROOT}:/workspace" \
        -v rinchan-gradle-cache:/root/.gradle \
        -w /workspace \
        "${image_tag}" \
        bash -lc './scripts/verify.sh'
}

if [[ "${USE_CONTAINER}" -eq 1 ]]; then
    run_in_container
fi

JDK17="$(discover_jdk 17 || true)"
JDK21="$(discover_jdk 21 || true)"
JDK25="$(discover_jdk 25 || true)"

if [[ -z "${JDK17}" || -z "${JDK21}" || -z "${JDK25}" ]]; then
    echo "==> Host is missing one or more required JDKs (17/21/25); falling back to container verification..."
    run_in_container
fi

export JAVA_17_HOME="${JDK17}"
export JAVA_21_HOME="${JDK21}"
export JAVA_25_HOME="${JDK25}"
export JAVA_HOME="${JDK21}"

echo "==> Gate 2 + 8-Target Matrix Build + Gate 3 (Unit/Property >=2,000 trials, ABI Audit, 8 JARs, Architecture Contract)"
python3 scripts/build-matrix.py

echo "==> Gate 1 (Packaged-JAR Real Runtime E2E: MC 1.21.1 NeoForge GameTestServer + FriendDrinkConfigScreen)"
./gradlew :neoforge:e2eTest \
    -Pminecraft_version=1.21.1 \
    -Ploader=neoforge \
    "-Porg.gradle.java.installations.paths=${JAVA_17_HOME},${JAVA_21_HOME},${JAVA_25_HOME}" \
    --no-configuration-cache

echo "==> ALL VERIFICATION GATES PASSED."
