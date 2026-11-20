#!/usr/bin/env bash
set -euo pipefail

REPO_ROOT="$(git rev-parse --show-toplevel)"
cd "${REPO_ROOT}"

git config core.hooksPath .githooks
chmod +x .githooks/* scripts/*.sh

echo "Configured git hooks path: $(git config --get core.hooksPath)"
