#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
ROOT_DIR="$(cd "$SCRIPT_DIR/../.." && pwd)"
TMP_DIR="$(mktemp -d)"
MAX_PARALLEL="${SCENARIO_PARALLEL:-6}"

cleanup() { rm -rf "$TMP_DIR"; }
trap cleanup EXIT

# ── CLI path (optional) ──────────────────────────────────────────────
if [ -n "${COPILOT_CLI_PATH:-}" ]; then
  echo "Using CLI override: $COPILOT_CLI_PATH"
else
  echo "No COPILOT_CLI_PATH set — SDKs will use their bundled CLI."
fi

# ── Auth ────────────────────────────────────────────────────────────
if [ -z "${GITHUB_TOKEN:-}" ]; then
  if command -v gh &>/dev/null; then
    export GITHUB_TOKEN=$(gh auth token 2>/dev/null || true)
  fi
fi
if [ -z "${GITHUB_TOKEN:-}" ]; then
  echo "⚠️  GITHUB_TOKEN not set"
fi

# ── Pre-install shared dependencies ────────────────────────────────
# Install Python SDK once to avoid parallel pip install races
if command -v pip3 &>/dev/null; then
  pip3 install -e "$ROOT_DIR/python" --quiet 2>/dev/null || true
fi

# ── Discover verify scripts ────────────────────────────────────────
VERIFY_SCRIPTS=()
while IFS= read -r script; do
  VERIFY_SCRIPTS+=("$script")
done < <(find "$SCRIPT_DIR" -mindepth 3 -maxdepth 3 -name verify.sh -type f | sort)

TOTAL=${#VERIFY_SCRIPTS[@]}

# ── SDK icon helpers ────────────────────────────────────────────────
sdk_icons() {
  local log="$1"
  local ts py go cs
  ts="$(sdk_status "$log" "TypeScript")"
  py="$(sdk_status "$log" "Python")"
  go="$(sdk_status "$log" "Go ")"
  cs="$(sdk_status "$log" "C#")"
  printf "TS %s  PY %s  GO %s  C# %s" "$ts" "$py" "$go" "$cs"
}

sdk_status() {
  local log="$1" sdk="$2"
  if ! grep -q "$sdk" "$log" 2>/dev/null; then
    printf "·"; return
  fi
  if grep "$sdk" "$log" | grep -q "❌"; then
    printf "✗"; return
  fi
  if grep "$sdk" "$log" | grep -q "⏭\|SKIP"; then
    printf "⊘"; return
  fi
  printf "✓"
}

# ── Display helpers ─────────────────────────────────────────────────
BOLD="\033[1m"
DIM="\033[2m"
RESET="\033[0m"
RED="\033[31m"
GREEN="\033[32m"
YELLOW="\033[33m"
CYAN="\033[36m"
CLR_LINE="\033[2K"

BAR_WIDTH=20

progress_bar() {
  local done_count="$1" total="$2"
  local filled=$(( done_count * BAR_WIDTH / total ))
  local empty=$(( BAR_WIDTH - filled ))
  printf "${DIM}["
  [ "$filled" -gt 0 ] && printf "%0.s█" $(seq 1 "$filled")
  [ "$empty"  -gt 0 ] && printf "%0.s░" $(seq 1 "$empty")
  printf "]${RESET}"
}

declare -a SCENARIO_NAMES=()
declare -a SCENARIO_STATES=()   # waiting | running | done
declare -a SCENARIO_RESULTS=()  # "" | PASS | FAIL | SKIP
declare -a SCENARIO_PIDS=()
declare -a SCENARIO_ICONS=()

for script in "${VERIFY_SCRIPTS[@]}"; do
  rel="${script#"$SCRIPT_DIR"/}"
  name="${rel%/verify.sh}"
  SCENARIO_NAMES+=("$name")
  SCENARIO_STATES+=("waiting")
  SCENARIO_RESULTS+=("")
  SCENARIO_PIDS+=("")
  SCENARIO_ICONS+=("")
done

# ── Execution ───────────────────────────────────────────────────────
RUNNING_COUNT=0
NEXT_IDX=0
PASSED=0; FAILED=0; SKIPPED=0
DONE_COUNT=0

# The progress line is the ONE line we update in-place via \r.
# When a scenario completes, we print its result as a permanent line
# above the progress line.
COLS="${COLUMNS:-$(tput cols 2>/dev/null || echo 80)}"

print_progress() {
  local running_names=""
  for i in "${!SCENARIO_STATES[@]}"; do
    if [ "${SCENARIO_STATES[$i]}" = "running" ]; then
      [ -n "$running_names" ] && running_names="$running_names, "
      running_names="$running_names${SCENARIO_NAMES[$i]}"
    fi
  done
  # Build the prefix: "  3/33 [████░░░░░░░░░░░░░░░░]  "
  local prefix
  prefix=$(printf "  %d/%d " "$DONE_COUNT" "$TOTAL")
  local prefix_len=$(( ${#prefix} + BAR_WIDTH + 4 ))  # +4 for []+ spaces
  # Truncate running names to fit in one terminal line
  local max_names=$(( COLS - prefix_len - 1 ))
  if [ "${#running_names}" -gt "$max_names" ] && [ "$max_names" -gt 3 ]; then
    running_names="${running_names:0:$((max_names - 1))}…"
  fi
  printf "\r${CLR_LINE}"
  printf "%s" "$prefix"
  progress_bar "$DONE_COUNT" "$TOTAL"
  printf "  ${CYAN}%s${RESET}" "$running_names"
}

print_result() {
  local i="$1"
  local name="${SCENARIO_NAMES[$i]}"
  local result="${SCENARIO_RESULTS[$i]}"
  local icons="${SCENARIO_ICONS[$i]}"

  # Clear the progress line, print result, then reprint progress below
  printf "\r${CLR_LINE}"
  case "$result" in
    PASS) printf "  ${GREEN}✅${RESET} %-36s  %s\n" "$name" "$icons" ;;
    FAIL) printf "  ${RED}❌${RESET} %-36s  %s\n" "$name" "$icons" ;;
    SKIP) printf "  ${YELLOW}⏭${RESET}  %-36s  %s\n" "$name" "$icons" ;;
  esac
}

start_scenario() {
  local i="$1"
  local script="${VERIFY_SCRIPTS[$i]}"
  local name="${SCENARIO_NAMES[$i]}"
  local log_file="$TMP_DIR/${name//\//__}.log"

  bash "$script" >"$log_file" 2>&1 &
  SCENARIO_PIDS[$i]=$!
  SCENARIO_STATES[$i]="running"
  RUNNING_COUNT=$((RUNNING_COUNT + 1))
}

finish_scenario() {
  local i="$1" exit_code="$2"
  local name="${SCENARIO_NAMES[$i]}"
  local log_file="$TMP_DIR/${name//\//__}.log"

  SCENARIO_STATES[$i]="done"
  RUNNING_COUNT=$((RUNNING_COUNT - 1))
  DONE_COUNT=$((DONE_COUNT + 1))

  if grep -q "^SKIP:" "$log_file" 2>/dev/null; then
    SCENARIO_RESULTS[$i]="SKIP"
    SKIPPED=$((SKIPPED + 1))
  elif [ "$exit_code" -eq 0 ]; then
    SCENARIO_RESULTS[$i]="PASS"
    PASSED=$((PASSED + 1))
  else
    SCENARIO_RESULTS[$i]="FAIL"
    FAILED=$((FAILED + 1))
  fi

  SCENARIO_ICONS[$i]="$(sdk_icons "$log_file")"
  print_result "$i"
}

echo ""

# Launch initial batch
while [ "$NEXT_IDX" -lt "$TOTAL" ] && [ "$RUNNING_COUNT" -lt "$MAX_PARALLEL" ]; do
  start_scenario "$NEXT_IDX"
  NEXT_IDX=$((NEXT_IDX + 1))
done
print_progress

# Poll for completion and launch new scenarios
while [ "$RUNNING_COUNT" -gt 0 ]; do
  for i in "${!SCENARIO_STATES[@]}"; do
    if [ "${SCENARIO_STATES[$i]}" = "running" ]; then
      pid="${SCENARIO_PIDS[$i]}"
      if ! kill -0 "$pid" 2>/dev/null; then
        wait "$pid" 2>/dev/null && exit_code=0 || exit_code=$?
        finish_scenario "$i" "$exit_code"

        # Launch next if available
        if [ "$NEXT_IDX" -lt "$TOTAL" ] && [ "$RUNNING_COUNT" -lt "$MAX_PARALLEL" ]; then
          start_scenario "$NEXT_IDX"
          NEXT_IDX=$((NEXT_IDX + 1))
        fi

        print_progress
      fi
    fi
  done
  sleep 0.2
done

# Clear the progress line
printf "\r${CLR_LINE}"
echo ""

# ── Final summary ──────────────────────────────────────────────────
printf "  ${BOLD}%d${RESET} scenarios" "$TOTAL"
[ "$PASSED"  -gt 0 ] && printf "  ${GREEN}${BOLD}%d passed${RESET}" "$PASSED"
[ "$FAILED"  -gt 0 ] && printf "  ${RED}${BOLD}%d failed${RESET}" "$FAILED"
[ "$SKIPPED" -gt 0 ] && printf "  ${YELLOW}${BOLD}%d skipped${RESET}" "$SKIPPED"
echo ""

# ── Failed scenario logs ───────────────────────────────────────────
if [ "$FAILED" -gt 0 ]; then
  echo ""
  printf "${BOLD}══════════════════════════════════════════════════════════════════════════${RESET}\n"
  printf "${RED}${BOLD} Failed Scenario Logs${RESET}\n"
  printf "${BOLD}══════════════════════════════════════════════════════════════════════════${RESET}\n"
  for i in "${!SCENARIO_NAMES[@]}"; do
    if [ "${SCENARIO_RESULTS[$i]}" = "FAIL" ]; then
      local_name="${SCENARIO_NAMES[$i]}"
      local_log="$TMP_DIR/${local_name//\//__}.log"
      echo ""
      printf "${RED}━━━ %s ━━━${RESET}\n" "$local_name"
      printf "    %s\n" "${SCENARIO_ICONS[$i]}"
      echo ""
      tail -30 "$local_log" | sed 's/^/    /'
    fi
  done
  exit 1
fi
