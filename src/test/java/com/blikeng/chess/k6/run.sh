#!/usr/bin/env bash
#
# run.sh — full load-test sweep against the chess API.
#
# Phase 1 ("run"): realistic mixed-traffic split (mixed.js)
#                        50% queue+game / 30% profile / 20% stats.
# Phase 2 (per-area):    each area in isolation so per-endpoint slowdowns
#                        stand out — matchmaking+playing, login, profile, stats.
#

# Every phase's k6 summary is captured and merged into ONE combined JSON
# report: results/run-<timestamp>.json
# Flags:
#   --seed         (re)register users + regenerate results/sessions.json first
#   --mixed-only   run only the mixed phase
#   --skip-mixed   run only the per-area phases

set -euo pipefail
cd "$(dirname "$0")"

USERS="${USERS:-500}"
BASE_URL="${BASE_URL:-http://localhost:8081}"
SESSIONS_FILE="results/sessions.json"
TS="$(date +%Y%m%d-%H%M%S)"
OUTDIR="results/run-${TS}"
COMBINED="results/run-mixed-${TS}.json"

SEED=false
RUN_MIXED=true
RUN_AREAS=true
for arg in "$@"; do
    case "$arg" in
        --seed)       SEED=true ;;
        --mixed-only) RUN_AREAS=false ;;
        --skip-mixed) RUN_MIXED=false ;;
        *) echo "unknown flag: $arg" >&2; exit 1 ;;
    esac
done

command -v k6 >/dev/null || { echo "k6 not installed (brew install k6)"; exit 1; }
command -v jq >/dev/null || { echo "jq not installed (brew install jq)"; exit 1; }
mkdir -p "$OUTDIR"
export BASE_URL

if $SEED; then
    echo "==> seeding $USERS users against $BASE_URL"
    k6 run -e USERS="$USERS" seed.js
fi
[ -f "$SESSIONS_FILE" ] || { echo "missing $SESSIONS_FILE — run once with --seed"; exit 1; }

run_phase() {
    local name="$1"; shift
    local script="$1"; shift
    local summary="$OUTDIR/${name}.json"
    echo "==> phase: $name ($script)"
    k6 run \
        -e SESSIONS_FILE="$SESSIONS_FILE" \
        -e USERS="$USERS" \
        "$@" \
        --summary-export "$summary" \
        "$script" || echo "   (phase '$name' had threshold failures — continuing)"
}

if $RUN_MIXED; then
    run_phase mixed mixed.js
fi
if $RUN_AREAS; then
    run_phase matchmaking queue.js
    run_phase login       login.js
    run_phase profile     profile.js
    run_phase stats       stats.js -e TC=RAPID
fi

echo "==> writing combined report: $COMBINED"
jq -n \
    --arg ts "$TS" \
    --arg base "$BASE_URL" \
    --argjson users "$USERS" \
    '{ runId: $ts, baseUrl: $base, users: $users, phases: {} }' > "$COMBINED"

for f in "$OUTDIR"/*.json; do
    [ -e "$f" ] || continue
    name="$(basename "$f" .json)"
    tmp="$(mktemp)"
    jq --arg name "$name" --slurpfile s "$f" '.phases[$name] = $s[0]' "$COMBINED" > "$tmp" && mv "$tmp" "$COMBINED"
done

echo "==> done."
echo "    per-phase summaries: $OUTDIR/"
echo "    combined report:     $COMBINED"
