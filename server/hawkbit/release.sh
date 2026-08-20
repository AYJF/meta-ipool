#!/bin/bash
# One-command release: upload a .raucb to hawkBit, wrap it in a distribution set,
# and optionally assign it to a target.
#
#   ./release.sh <version> [/path/to/bundle.raucb] [controller-id]
#
# Uses the mgmt REST API directly rather than the web UI. The UI receives an
# upload into a JVM and forwards it, which for a ~356MB bundle means large heap
# and multipart limits; curl streams it straight to hawkBit instead.
set -euo pipefail

SERVER="${SERVER:-https://updates.nexpoolsystem.com}"
SECRETS="${SECRETS:-$HOME/Documents/yocto/rockchip/secrets}"
IMAGES="${IMAGES:-$HOME/Documents/yocto/rockchip/build-sz3568/tmp/deploy/images/sz3568}"

VERSION="${1:?usage: release.sh <version> [bundle] [controller-id]}"
BUNDLE="${2:-$IMAGES/ipool-rauc-bundle-sz3568.raucb}"
TARGET="${3:-}"

[ -r "$BUNDLE" ] || { echo "bundle not found: $BUNDLE" >&2; exit 1; }
PW="$(cat "$SECRETS/hawkbit-admin-password")"
AUTH=(-u "admin:$PW")
J=(-H 'Content-Type: application/json')

api() { curl -fsS "${AUTH[@]}" "$@"; }

echo "==> bundle: $(basename "$BUNDLE") ($(du -h "$BUNDLE" | cut -f1))"

echo "==> software module poolnook-rootfs:$VERSION"
SM_ID=$(api -X POST "${J[@]}" \
  -d "[{\"name\":\"poolnook-rootfs\",\"version\":\"$VERSION\",\"type\":\"os\",\"vendor\":\"QEI Inc\"}]" \
  "$SERVER/rest/v1/softwaremodules" | python3 -c 'import json,sys; print(json.load(sys.stdin)[0]["id"])')
echo "    id=$SM_ID"

echo "==> uploading artifact (progress below)"
curl -# "${AUTH[@]}" -X POST -F "file=@$BUNDLE" \
  "$SERVER/rest/v1/softwaremodules/$SM_ID/artifacts" -o /tmp/rel_art.json
python3 - <<PY
import json
a=json.load(open('/tmp/rel_art.json'))
print(f"    uploaded: {a['providedFilename']}  size={a['size']}  sha256={a['hashes']['sha256'][:16]}...")
PY
rm -f /tmp/rel_art.json

echo "==> distribution set poolnook:$VERSION"
DS_ID=$(api -X POST "${J[@]}" \
  -d "[{\"name\":\"poolnook\",\"version\":\"$VERSION\",\"type\":\"os\",\"modules\":[{\"id\":$SM_ID}],\"requiredMigrationStep\":false}]" \
  "$SERVER/rest/v1/distributionsets" | python3 -c 'import json,sys; print(json.load(sys.stdin)[0]["id"])')
echo "    id=$DS_ID"

if [ -n "$TARGET" ]; then
  echo "==> assigning to $TARGET"
  api -X POST "${J[@]}" -d "[{\"id\":$DS_ID}]" \
    "$SERVER/rest/v1/targets/$TARGET/assignedDS" >/dev/null
  echo "    assigned -- the board will pick it up on its next poll (<=5 min)"
else
  echo "==> not assigned. To roll out:"
  echo "    ./release.sh ... <controller-id>   or assign in the UI"
fi
echo "done."
