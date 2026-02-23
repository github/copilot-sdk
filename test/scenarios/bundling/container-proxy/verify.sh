#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
ROOT_DIR="$(cd "$SCRIPT_DIR/../../.." && pwd)"
PASS=0
FAIL=0
ERRORS=""
TIMEOUT=60

# Skip if runtime source not available (needed for Docker build)
if [ ! -d "$ROOT_DIR/runtime" ]; then
  echo "SKIP: runtime/ directory not found — cannot build Copilot CLI Docker image"
  exit 0
fi

cleanup() {
  echo ""
  if [ -n "${PROXY_PID:-}" ] && kill -0 "$PROXY_PID" 2>/dev/null; then
    echo "Stopping proxy (PID $PROXY_PID)..."
    kill "$PROXY_PID" 2>/dev/null || true
  fi
  echo "Stopping Docker container..."
  docker compose -f "$SCRIPT_DIR/docker-compose.yml" down --timeout 5 2>/dev/null || true
}
trap cleanup EXIT

# Use gtimeout on macOS, timeout on Linux
if command -v gtimeout &>/dev/null; then
  TIMEOUT_CMD="gtimeout"
elif command -v timeout &>/dev/null; then
  TIMEOUT_CMD="timeout"
else
  echo "⚠️  No timeout command found. Install coreutils (brew install coreutils)."
  echo "   Running without timeouts."
  TIMEOUT_CMD=""
fi

check() {
  local name="$1"
  shift
  printf "━━━ %s ━━━\n" "$name"
  if output=$("$@" 2>&1); then
    echo "$output"
    echo "✅ $name passed"
    PASS=$((PASS + 1))
  else
    echo "$output"
    echo "❌ $name failed"
    FAIL=$((FAIL + 1))
    ERRORS="$ERRORS\n  - $name"
  fi
  echo ""
}

run_with_timeout() {
  local name="$1"
  shift
  printf "━━━ %s ━━━\n" "$name"
  local output=""
  local code=0
  if [ -n "$TIMEOUT_CMD" ]; then
    output=$($TIMEOUT_CMD "$TIMEOUT" "$@" 2>&1) && code=0 || code=$?
  else
    output=$("$@" 2>&1) && code=0 || code=$?
  fi
  if [ "$code" -eq 0 ] && [ -n "$output" ]; then
    echo "$output"
    echo "✅ $name passed (got response)"
    PASS=$((PASS + 1))
  elif [ "$code" -eq 124 ]; then
    echo "${output:-(no output)}"
    echo "❌ $name failed (timed out after ${TIMEOUT}s)"
    FAIL=$((FAIL + 1))
    ERRORS="$ERRORS\n  - $name (timeout)"
  else
    echo "${output:-(empty output)}"
    echo "❌ $name failed (exit code $code)"
    FAIL=$((FAIL + 1))
    ERRORS="$ERRORS\n  - $name"
  fi
  echo ""
}

# Kill any stale processes on test ports from previous interrupted runs
for test_port in 3000 4000; do
  stale_pid=$(lsof -ti ":$test_port" 2>/dev/null || true)
  if [ -n "$stale_pid" ]; then
    echo "Cleaning up stale process on port $test_port (PID $stale_pid)"
    kill $stale_pid 2>/dev/null || true
  fi
done
docker compose -f "$SCRIPT_DIR/docker-compose.yml" down --timeout 5 2>/dev/null || true

# ── Start the simple proxy ───────────────────────────────────────────
PROXY_PORT=4000
PROXY_PID=""

echo "══════════════════════════════════════"
echo " Starting proxy on port $PROXY_PORT"
echo "══════════════════════════════════════"
echo ""

python3 "$SCRIPT_DIR/proxy.py" "$PROXY_PORT" &
PROXY_PID=$!
sleep 1

if kill -0 "$PROXY_PID" 2>/dev/null; then
  echo "✅ Proxy running (PID $PROXY_PID)"
else
  echo "❌ Proxy failed to start"
  exit 1
fi
echo ""

# ── Build and start container ────────────────────────────────────────
echo "══════════════════════════════════════"
echo " Building and starting Copilot CLI container"
echo "══════════════════════════════════════"
echo ""

docker compose -f "$SCRIPT_DIR/docker-compose.yml" up -d --build

# Wait for Copilot CLI to be ready
echo "Waiting for Copilot CLI to be ready..."
for i in $(seq 1 30); do
  if (echo > /dev/tcp/localhost/3000) 2>/dev/null; then
    echo "✅ Copilot CLI is ready on port 3000"
    break
  fi
  if [ "$i" -eq 30 ]; then
    echo "❌ Copilot CLI did not become ready within 30 seconds"
    docker compose -f "$SCRIPT_DIR/docker-compose.yml" logs
    exit 1
  fi
  sleep 1
done
echo ""

export COPILOT_CLI_URL="localhost:3000"

echo "══════════════════════════════════════"
echo " Phase 1: Build client samples"
echo "══════════════════════════════════════"
echo ""

# TypeScript: install + compile
check "TypeScript (install)" bash -c "cd '$SCRIPT_DIR/typescript' && npm install --ignore-scripts 2>&1"
check "TypeScript (build)"   bash -c "cd '$SCRIPT_DIR/typescript' && npm run build 2>&1"

# Python: install + syntax
check "Python (install)" bash -c "python3 -c 'import copilot' 2>/dev/null || (cd '$SCRIPT_DIR/python' && pip3 install -r requirements.txt --quiet 2>&1)"
check "Python (syntax)"  bash -c "python3 -c \"import ast; ast.parse(open('$SCRIPT_DIR/python/main.py').read()); print('Syntax OK')\""

# Go: build
check "Go (build)" bash -c "cd '$SCRIPT_DIR/go' && go build -o container-proxy-go . 2>&1"

# C#: build
check "C# (build)" bash -c "cd '$SCRIPT_DIR/csharp' && dotnet build --nologo -v q 2>&1"


echo "══════════════════════════════════════"
echo " Phase 2: E2E Run (timeout ${TIMEOUT}s each)"
echo "══════════════════════════════════════"
echo ""

# TypeScript: run
run_with_timeout "TypeScript (run)" bash -c "
  cd '$SCRIPT_DIR/typescript' && \
  output=\$(node dist/index.js 2>&1) && \
  echo \"\$output\" && \
  echo \"\$output\" | grep -qi 'Paris\|capital'
"

# Python: run
run_with_timeout "Python (run)" bash -c "
  cd '$SCRIPT_DIR/python' && \
  output=\$(python3 main.py 2>&1) && \
  echo \"\$output\" && \
  echo \"\$output\" | grep -qi 'Paris\|capital'
"

# Go: run
run_with_timeout "Go (run)" bash -c "
  cd '$SCRIPT_DIR/go' && \
  output=\$(./container-proxy-go 2>&1) && \
  echo \"\$output\" && \
  echo \"\$output\" | grep -qi 'Paris\|capital'
"

# C#: run
run_with_timeout "C# (run)" bash -c "
  cd '$SCRIPT_DIR/csharp' && \
  output=\$(COPILOT_CLI_URL=$COPILOT_CLI_URL dotnet run --no-build 2>&1) && \
  echo \"\$output\" && \
  echo \"\$output\" | grep -qi 'Paris\|capital'
"


echo "══════════════════════════════════════"
echo " Results: $PASS passed, $FAIL failed"
echo "══════════════════════════════════════"
if [ "$FAIL" -gt 0 ]; then
  echo -e "Failures:$ERRORS"
  exit 1
fi
