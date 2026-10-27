#!/usr/bin/env bash
# SPDX-FileCopyrightText: 2026 Stefan Richter
# SPDX-License-Identifier: MIT
# Builds the signed release APK and AAB in CI and copies them to dist/ with checksums.
#   build_signed.sh <label> [--require-version]
# <label> becomes part of the file names (BirthdaySync-<label>.apk). With --require-version the
# label must equal versionName from app/build.gradle.kts (release tags).
# Expects SIGNING_KEYSTORE_BASE64, SIGNING_STORE_PASSWORD, SIGNING_KEY_ALIAS, SIGNING_KEY_PASSWORD.
set -euo pipefail
label="$1"
cd "$(dirname "$0")/../.."

version=$(sed -nE 's/^ *versionName = "([^"]+)".*/\1/p' app/build.gradle.kts)
if [[ "${2:-}" == "--require-version" && "$label" != "$version" ]]; then
  echo "::error::Tag version $label does not match versionName $version in app/build.gradle.kts"
  exit 1
fi
for v in SIGNING_KEYSTORE_BASE64 SIGNING_STORE_PASSWORD SIGNING_KEY_ALIAS SIGNING_KEY_PASSWORD; do
  if [[ -z "${!v:-}" ]]; then echo "::error::Secret $v is not set"; exit 1; fi
done

keystore="${RUNNER_TEMP:-/tmp}/release.jks"
trap 'rm -f "$keystore"' EXIT
printf '%s' "$SIGNING_KEYSTORE_BASE64" | base64 --decode > "$keystore"
export SIGNING_STORE_FILE="$keystore"

./gradlew --no-daemon assembleRelease bundleRelease

apk=app/build/outputs/apk/release/app-release.apk
aab=app/build/outputs/bundle/release/app-release.aab
# Newest build-tools that actually contain apksigner (the runner image has several versions).
apksigner=$(ls -d "$ANDROID_HOME"/build-tools/*/ | sort -V | while read -r d; do
  [[ -x "$d/apksigner" ]] && echo "$d/apksigner"; done | tail -1)
if [[ -z "$apksigner" ]]; then echo "::error::apksigner not found under $ANDROID_HOME/build-tools"; exit 1; fi
certs=$("$apksigner" verify --print-certs "$apk")
echo "$certs" | grep -E 'Signer #1 certificate (DN|SHA-256)'

mkdir -p dist
cp "$apk" "dist/BirthdaySync-$label.apk"
cp "$aab" "dist/BirthdaySync-$label.aab"
(cd dist && sha256sum BirthdaySync-* > SHA256SUMS.txt && cat SHA256SUMS.txt)
