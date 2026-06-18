## Goal

Port the Java integration test behavior from:

- `java/src/test/java/com/github/copilot/LowLevelToolDefinitionIT.java`
- test method: `lowLevelToolDefinition`
- snapshot: `test/snapshots/tools/low_level_tool_definition.yaml`

to the following non-Java SDKs, using each language's native E2E test infrastructure:

1. `dotnet`
2. `go`
3. `nodejs`
4. `python`
5. `rust`

The new/updated tests in each language must use the **same snapshot scenario** (`tools/low_level_tool_definition`) and validate the same behavior.

---

## Required test behavior to port

From a test perspective, replicate this behavior:

1. Define a `set_current_phase` tool that accepts a `phase` argument (string, enum: `["searching", "analyzing", "done"]`) and returns `"Phase set to {phase}"`. The tool handler must also store the phase value in test-local state.
2. Define a `search_items` tool that accepts a `keyword` argument (string) and returns `"Found: item_alpha, item_beta"`.
3. Define a `grep` override tool (using whatever "override" mechanism the language provides) that accepts a `query` argument (string) and returns `"CUSTOM_GREP: {query}"`.
4. Create a session with:
   - Permission handler that auto-approves all requests.
   - Available tools: all custom tools (`*`) plus built-in `web_fetch`.
   - The three tool definitions registered on the session.
5. Send prompt: `"First, set the current phase to 'analyzing'. Then search for items with keyword 'copilot'. Report the phase and search results."`
6. Assert:
   - The assistant response is non-null/non-empty.
   - The response content (case-insensitive) contains `"analyzing"`.
   - The response content contains `"item_alpha"` or `"item_beta"`.
   - The test-local phase state equals `"analyzing"` (verifying the tool handler was actually invoked).

Do not weaken these assertions.

---

## Critical execution constraint (must follow exactly)

Proceed through languages **one-at-a-time** in this exact order:

1. `dotnet`
2. `go`
3. `nodejs`
4. `python`
5. `rust`

❌❌ **Do not continue to the next language unless and until the current language gets a clean run with the new test in isolation.** ❌❌

Do **not** run full cross-language or full-repo test suites. Let CI/CD handle broad runs.

---

## Snapshot/name mapping requirements

Ensure each language's test naming/harness maps to:

- snapshot folder: `tools`
- snapshot file: `low_level_tool_definition.yaml`

Do not create alternate snapshot names for this scenario.

---

## Per-language isolated run commands

Use these commands for isolated validation while iterating.

### 1) dotnet

Implement in dotnet E2E tests (preferred: new `LowLevelToolDefinitionE2ETests` class or add to existing `ToolsE2ETests` class using snapshot category `tools`, test method `Low_Level_Tool_Definition`).

Isolated run:

```bash
cd dotnet && dotnet test test/GitHub.Copilot.SDK.Test.csproj --filter "FullyQualifiedName~Low_Level_Tool_Definition"
```

### 2) go

Implement in Go E2E tests with snapshot mapping to `tools/low_level_tool_definition` (preferred: add to existing `go/internal/e2e/tools_e2e_test.go` or create new file, subtest name exactly `low_level_tool_definition`).

Isolated run:

```bash
cd go && go test ./internal/e2e -run 'TestToolsE2E/low_level_tool_definition$' -count=1
```

### 3) nodejs

Implement in Node E2E Vitest (preferred: add to existing `nodejs/test/e2e/tools.e2e.test.ts` or create new file, test name mapping to `low_level_tool_definition`).

Isolated run:

```bash
cd nodejs && npm test -- test/e2e/tools.e2e.test.ts -t "low_level_tool_definition"
```

### 4) python

Implement in Python E2E pytest (preferred: add to existing `python/e2e/test_tools_e2e.py` or create new file, test function `test_low_level_tool_definition`).

Isolated run:

```bash
cd python && uv run pytest e2e/test_tools_e2e.py::test_low_level_tool_definition
```

### 5) rust

Implement in Rust E2E tests (preferred: add to existing `rust/tests/e2e/tools.rs`; use `with_e2e_context("tools", "low_level_tool_definition", ...)`).

Isolated run:

```bash
cd rust && cargo test --features test-support --test e2e tools::low_level_tool_definition -- --exact
```

---

## Implementation notes

1. Reuse existing per-language E2E harness helpers and style conventions.
2. Keep changes scoped to test code and required wiring.
3. Do not hand-edit generated code.
4. ❌❌❌ DO NOT CHANGE ANY non-test CODE.❌❌❌
5. ✅✅Put the test in the "right place" for each language. That means put it "near" any similar existing tests. The existing tools E2E test files are:
   - `dotnet/test/E2E/ToolsE2ETests.cs`
   - `go/internal/e2e/tools_e2e_test.go`
   - `nodejs/test/e2e/tools.e2e.test.ts`
   - `python/e2e/test_tools_e2e.py`
   - `rust/tests/e2e/tools.rs`
   Put the new test near those. ✅✅
6. The snapshot `test/snapshots/tools/low_level_tool_definition.yaml` involves **two conversations**: one where tool calls are made without prior tool results, and one full round-trip (tool calls → tool results → final assistant message). Each language's replay proxy handles this; just ensure the test sends the right prompt and processes tool invocations correctly.
7. The `grep` override tool uses whatever "tool override" mechanism exists in each language (e.g., `ToolDefinition.createOverride` in Java, or the equivalent in each SDK). If a language has no override concept, define it as a regular custom tool named `grep`.

---

## Deliverable

When done, provide:

1. files changed per language,
2. isolated command used per language,
3. pass/fail result per language (must be passing before moving to next),
4. any blockers (if any language cannot be completed).
