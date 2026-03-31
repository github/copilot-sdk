# Dev Environment Caveats

What's available (and what isn't) for implementing the SDK gaps on this machine.

---

## Language Runtimes

| Language | Available | Version | Notes |
|----------|-----------|---------|-------|
| **Node.js** | ✅ | v24.13.0 | npm 11.6.3, vitest 4.0.18, `node_modules` + `@github/copilot` present |
| **Python** | ✅ | 3.14.3 | pip 25.3, pytest 9.0.2, SDK installed in editable mode |
| **.NET** | ✅ | 10.0.201 | Builds and restores cleanly, test project compiles |
| **Go** | ❌ | — | `go` and `gofmt` not on PATH. **Cannot build, test, or format Go code.** |

## Test Suites

| SDK | Unit Tests | E2E Tests |
|-----|-----------|-----------|
| **Node.js** | ✅ vitest works | ✅ harness + snapshots available |
| **Python** | ✅ 70/70 pass (ignoring e2e/) | ⚠️ E2E hangs — harness spawns but tests don't connect (likely harness startup race on Windows) |
| **.NET** | ✅ 149 pass, 6 skipped, 0 failed | ✅ Included in main test project |
| **Go** | ❌ Can't run | ❌ Can't run |

## Missing Tools

| Tool | Used For | Impact |
|------|----------|--------|
| `go` | Build, test, `go fmt` | **Cannot work on Go SDK at all** |
| `gofmt` | Format generated Go code | Blocked by missing Go runtime |
| `uv` | Python fast installer (used by `just install`) | Not critical — `pip install -e ".[dev]"` works fine as a substitute |
| `just` | Monorepo task runner | Not critical — can run per-language commands directly |

## Recommendations

1. **Python and .NET are fully workable** — code, unit-test, and iterate without issues.
2. **Go is blocked** — install Go (1.21+) and add it to PATH before attempting Go SDK work.
3. **Python E2E tests** may need manual attention on Windows — unit tests are sufficient for validating SDK-layer changes; E2E can be verified in CI.
4. **Node.js** is the reference implementation and fully functional for cross-referencing.
