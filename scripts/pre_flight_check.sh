#!/usr/bin/env bash
set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_DIR="$(cd "${SCRIPT_DIR}/.." && pwd)"

cd "${REPO_DIR}"

echo "================================================================================"
echo " Running Local Pre-Flight Check for osm-tools"
echo "================================================================================"

# 1. Check Git Status
echo "[1/3] Checking Git working tree..."
git status -s

# 2. Run Full Maven Test Suite (including ScaleStressTest)
echo "[2/3] Running Java unit and scale tests (mvn test)..."
mvn test

# 3. Optional Docker clean-room build test if docker is available
if command -v docker &> /dev/null; then
  echo "[3/3] Verifying clean Maven build in clean-room Docker container..."
  docker run --rm -v "$(pwd)":/workspace -v "${HOME}/.m2":/root/.m2 -w /workspace maven:3-sapmachine mvn test -q
else
  echo "[3/3] Skipping Docker container verification (docker command not found)."
fi

echo "================================================================================"
echo " ALL LOCAL PRE-FLIGHT CHECKS PASSED SUCCESSFULLY!"
echo " Safe to commit and push to origin/master."
echo "================================================================================"
