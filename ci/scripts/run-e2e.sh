#!/usr/bin/env bash
set -euo pipefail

BASE_URL=${BASE_URL:-http://localhost:8080}
USERNAME=${E2E_USERNAME:-demo}
PASSWORD=${E2E_PASSWORD:-password}

WORKDIR=$(mktemp -d)
COOKIES="$WORKDIR/cookies.txt"
BODY="$WORKDIR/body.json"

echo "Executing login smoke test against ${BASE_URL}"
STATUS=$(curl -s -o "$BODY" -w "%{http_code}" -X POST "${BASE_URL}/api/login" \
  -H "Content-Type: application/json" \
  -c "$COOKIES" \
  -d "{\"username\":\"${USERNAME}\",\"password\":\"${PASSWORD}\"}")

if [[ "$STATUS" != "200" ]]; then
  echo "Login failed with status ${STATUS}"
  cat "$BODY"
  exit 1
fi

HOME_ROUTE=$(python - <<'PY'
import json,sys
try:
    data=json.load(open(sys.argv[1]))
    print(data.get("homeRoute",""))
except Exception:
    print("")
PY
"$BODY")

if [[ -z "$HOME_ROUTE" ]]; then
  echo "Login response missing homeRoute"
  cat "$BODY"
  exit 1
fi

echo "Login successful; homeRoute=${HOME_ROUTE}"
