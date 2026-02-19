# Default recipe to display help
default:
    @just --list

# Format all code across all languages
format: format-go format-python format-nodejs format-dotnet

# Lint all code across all languages
lint: lint-go lint-python lint-nodejs lint-dotnet

# Run tests for all languages
test: test-go test-python test-nodejs test-dotnet

# Format Go code
format-go:
    @echo "=== Formatting Go code ==="
    @cd go && find . -name "*.go" -not -path "*/generated/*" -exec gofmt -w {} +

# Format Python code
format-python:
    @echo "=== Formatting Python code ==="
    @cd python && uv run ruff format .

# Format Node.js code
format-nodejs:
    @echo "=== Formatting Node.js code ==="
    @cd nodejs && npm run format

# Format .NET code
format-dotnet:
    @echo "=== Formatting .NET code ==="
    @cd dotnet && dotnet format src/GitHub.Copilot.SDK.csproj

# Lint Go code
lint-go:
    @echo "=== Linting Go code ==="
    @cd go && golangci-lint run ./...

# Lint Python code
lint-python:
    @echo "=== Linting Python code ==="
    @cd python && uv run ruff check . && uv run ty check copilot

# Lint Node.js code
lint-nodejs:
    @echo "=== Linting Node.js code ==="
    @cd nodejs && npm run format:check && npm run lint && npm run typecheck

# Lint .NET code
lint-dotnet:
    @echo "=== Linting .NET code ==="
    @cd dotnet && dotnet format src/GitHub.Copilot.SDK.csproj --verify-no-changes

# Test Go code
test-go:
    @echo "=== Testing Go code ==="
    @cd go && go test ./...

# Test Python code
test-python:
    @echo "=== Testing Python code ==="
    @cd python && uv run pytest

# Test Node.js code
test-nodejs:
    @echo "=== Testing Node.js code ==="
    @cd nodejs && npm test

# Test .NET code
test-dotnet:
    @echo "=== Testing .NET code ==="
    @cd dotnet && dotnet test test/GitHub.Copilot.SDK.Test.csproj

# Install all dependencies
install:
    @echo "=== Installing dependencies ==="
    @cd nodejs && npm ci
    @cd python && uv pip install -e ".[dev]"
    @cd go && go mod download
    @cd dotnet && dotnet restore
    @echo "✅ All dependencies installed"

# Run interactive SDK playground
playground:
    @echo "=== Starting SDK Playground ==="
    @cd demos/playground && npm install && npm start

# Validate documentation code examples
validate-docs: validate-docs-extract validate-docs-check

# Extract code blocks from documentation
validate-docs-extract:
    @echo "=== Extracting documentation code blocks ==="
    @cd scripts/docs-validation && npm ci --silent && npm run extract

# Validate all extracted code blocks
validate-docs-check:
    @echo "=== Validating documentation code blocks ==="
    @cd scripts/docs-validation && npm run validate

# Validate only TypeScript documentation examples
validate-docs-ts:
    @echo "=== Validating TypeScript documentation ==="
    @cd scripts/docs-validation && npm run validate:ts

# Validate only Python documentation examples
validate-docs-py:
    @echo "=== Validating Python documentation ==="
    @cd scripts/docs-validation && npm run validate:py

# Validate only Go documentation examples
validate-docs-go:
    @echo "=== Validating Go documentation ==="
    @cd scripts/docs-validation && npm run validate:go

# Validate only C# documentation examples
validate-docs-cs:
    @echo "=== Validating C# documentation ==="
    @cd scripts/docs-validation && npm run validate:cs

# Build all scenario samples (all languages)
scenario-build:
    #!/usr/bin/env bash
    set -euo pipefail
    echo "=== Building all scenario samples ==="
    TOTAL=0; PASS=0; FAIL=0

    build_lang() {
      local lang="$1" find_expr="$2" build_cmd="$3"
      echo ""
      echo "── $lang ──"
      while IFS= read -r target; do
        [ -z "$target" ] && continue
        dir=$(dirname "$target")
        scenario="${dir#test/scenarios/}"
        TOTAL=$((TOTAL + 1))
        if (cd "$dir" && eval "$build_cmd" >/dev/null 2>&1); then
          printf "  ✅ %s\n" "$scenario"
          PASS=$((PASS + 1))
        else
          printf "  ❌ %s\n" "$scenario"
          FAIL=$((FAIL + 1))
        fi
      done < <(find test/scenarios $find_expr | sort)
    }

    # TypeScript: npm install
    (cd nodejs && npm ci --ignore-scripts --silent 2>/dev/null) || true
    build_lang "TypeScript" "-path '*/typescript/package.json'" "npm install --ignore-scripts"

    # Python: syntax check
    build_lang "Python" "-path '*/python/main.py'" "python3 -c \"import ast; ast.parse(open('main.py').read())\""

    # Go: go build
    build_lang "Go" "-path '*/go/go.mod'" "go build ./..."

    # C#: dotnet build
    build_lang "C#" "-name '*.csproj' -path '*/csharp/*'" "dotnet build --nologo -v quiet"

    echo ""
    echo "══════════════════════════════════════"
    echo " Scenario build summary: $PASS passed, $FAIL failed (of $TOTAL)"
    echo "══════════════════════════════════════"
    [ "$FAIL" -eq 0 ]

# Run the full scenario verify orchestrator (build + E2E, needs real CLI)
scenario-verify:
    @echo "=== Running scenario verification ==="
    @bash test/scenarios/verify.sh

# Build scenarios for a single language (typescript, python, go, csharp)
scenario-build-lang LANG:
    #!/usr/bin/env bash
    set -euo pipefail
    echo "=== Building {{LANG}} scenarios ==="
    PASS=0; FAIL=0

    case "{{LANG}}" in
      typescript)
        (cd nodejs && npm ci --ignore-scripts --silent 2>/dev/null) || true
        for target in $(find test/scenarios -path '*/typescript/package.json' | sort); do
          dir=$(dirname "$target"); scenario="${dir#test/scenarios/}"
          if (cd "$dir" && npm install --ignore-scripts >/dev/null 2>&1); then
            printf "  ✅ %s\n" "$scenario"; PASS=$((PASS + 1))
          else
            printf "  ❌ %s\n" "$scenario"; FAIL=$((FAIL + 1))
          fi
        done
        ;;
      python)
        for target in $(find test/scenarios -path '*/python/main.py' | sort); do
          dir=$(dirname "$target"); scenario="${dir#test/scenarios/}"
          if python3 -c "import ast; ast.parse(open('$target').read())" 2>/dev/null; then
            printf "  ✅ %s\n" "$scenario"; PASS=$((PASS + 1))
          else
            printf "  ❌ %s\n" "$scenario"; FAIL=$((FAIL + 1))
          fi
        done
        ;;
      go)
        for target in $(find test/scenarios -path '*/go/go.mod' | sort); do
          dir=$(dirname "$target"); scenario="${dir#test/scenarios/}"
          if (cd "$dir" && go build ./... >/dev/null 2>&1); then
            printf "  ✅ %s\n" "$scenario"; PASS=$((PASS + 1))
          else
            printf "  ❌ %s\n" "$scenario"; FAIL=$((FAIL + 1))
          fi
        done
        ;;
      csharp)
        for target in $(find test/scenarios -name '*.csproj' -path '*/csharp/*' | sort); do
          dir=$(dirname "$target"); scenario="${dir#test/scenarios/}"
          if (cd "$dir" && dotnet build --nologo -v quiet >/dev/null 2>&1); then
            printf "  ✅ %s\n" "$scenario"; PASS=$((PASS + 1))
          else
            printf "  ❌ %s\n" "$scenario"; FAIL=$((FAIL + 1))
          fi
        done
        ;;
      *)
        echo "Unknown language: {{LANG}}. Use: typescript, python, go, csharp"
        exit 1
        ;;
    esac

    echo ""
    echo "{{LANG}} scenarios: $PASS passed, $FAIL failed"
    [ "$FAIL" -eq 0 ]
