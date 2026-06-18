# Plan: Add E2E test for non-ergonomic (low-level) tool definition

## Goal

Add a failsafe IT test that exercises the **current explicit** `ToolDefinition.create()` / `ToolDefinition.createOverride()` API — the "non-ergonomic" approach — with multiple tools, `ToolSet` with `addCustom`/`addBuiltIn`, `getArgumentsAs()` deserialization into a record, and a tool handler that mutates application state. This establishes baseline test coverage before issue #1682 adds the annotation-driven ergonomic API.

## Instructions

Read `java.instructions.md` in my User level Copilot instructions. This session is about Java.

Use the `new-java-e2e-test-yaml-and-test` skill to create a new failsafe IT test that exercises the non-ergonomic-tool-definition approach to tool definition.

### What the test must exercise

The test class should be `LowLevelToolDefinitionIT.java` in `java/src/test/java/com/github/copilot/`. It must demonstrate **all** of the following in a single session:

1. **`ToolDefinition.create(name, description, schema, handler)`** — define at least two custom tools explicitly with `Map<String, Object>` schemas.
2. **`ToolDefinition.createOverride(name, description, schema, handler)`** — define one tool that overrides a built-in tool.
3. **`invocation.getArgumentsAs(SomeRecord.class)`** — at least one handler must deserialize arguments into a Java record (not `getArguments()` returning raw Map).
4. **`invocation.getArguments()`** — at least one handler must use the raw `Map<String, Object>` accessor.
5. **`ToolSet` with `addCustom("*").addBuiltIn("web_fetch")`** — pass `setAvailableTools(...)` on the `SessionConfig`.
6. **Handler mutates state** — one tool handler should mutate a field on the test class and the test should assert that the field was updated after the response.
7. **Handler returns `CompletableFuture.completedFuture(...)`** — all handlers return completed futures (as is the current pattern).

### Concrete test design

#### Snapshot category

`tools` (reuse the existing category under `test/snapshots/tools/`).

#### Snapshot file

`test/snapshots/tools/low_level_tool_definition.yaml`

#### Java test method name

`lowLevelToolDefinition` (converts to `low_level_tool_definition` for snapshot lookup).

#### Tool definitions for the test

| Tool | Factory | Name | Description | Schema | Handler behavior |
|------|---------|------|-------------|--------|-----------------|
| Set Phase | `create` | `set_current_phase` | "Sets the current phase of the agent" | `{ type: object, properties: { phase: { type: string, enum: [searching, analyzing, done] } }, required: [phase] }` | Deserializes via `getArgumentsAs(PhaseArgs.class)` where `record PhaseArgs(String phase) {}`. Mutates a `currentPhase` field on the test. Returns `"Phase set to " + phase`. |
| Search | `create` | `search_items` | "Search for items by keyword" | `{ type: object, properties: { keyword: { type: string } }, required: [keyword] }` | Uses `getArguments()` raw Map. Returns a fixed string like `"Found: item_alpha, item_beta"`. |
| Override grep | `createOverride` | `grep` | "Custom grep override" | `{ type: object, properties: { query: { type: string } }, required: [query] }` | Uses `getArguments()`. Returns `"CUSTOM_GREP: " + query`. |

#### Prompt

```
First, set the current phase to 'analyzing'. Then search for items with keyword 'copilot'. Report the phase and search results.
```

#### YAML snapshot structure

Two conversations (one for the tool-call turn, one for the final response turn after tool results are provided):

- **Conversation 1** (tool call turn): system `${system}` + user prompt → assistant with `tool_calls` for `set_current_phase` and `search_items`.
- **Conversation 2** (final response turn): full history including tool results → assistant final content mentioning "analyzing", "item_alpha", "item_beta".

Study the existing snapshot files in `test/snapshots/tools/` carefully. In particular, study the snapshot file for the `testInvokesCustomTool` test in `ToolsTest.java` (`test/snapshots/tools/invokes_custom_tool.yaml`). It shows how tool call and tool result conversations are structured. Additionally, study `test/snapshots/tools/should_execute_multiple_custom_tools_in_parallel_single_turn.yaml` which shows multiple parallel tool calls in a single turn.

#### Assertions

1. `response` is not null.
2. Response content contains `"analyzing"` (confirming the phase tool was called).
3. Response content contains `"item_alpha"` or `"item_beta"` (confirming search tool was called).
4. The `currentPhase` field on the test class equals `"analyzing"` (confirming handler mutated state).

#### Session config

```java
new SessionConfig()
    .setOnPermissionRequest(PermissionHandler.APPROVE_ALL)
    .setAvailableTools(new ToolSet().addCustom("*").addBuiltIn("web_fetch"))
    .setTools(List.of(setPhaseTool, searchTool, grepOverrideTool))
```

### Step-by-step execution

1. Create the YAML snapshot file at `test/snapshots/tools/low_level_tool_definition.yaml`.
2. Create the Java IT file at `java/src/test/java/com/github/copilot/LowLevelToolDefinitionIT.java`.
3. Run `mvn spotless:apply` from the `java/` directory (using the background + log pattern from `java.instructions.md`).
4. Run the test in isolation:
   ```sh
   cd java
   LOG="$(date +%Y%m%d-%H%M)-job-logs.txt" && mvn failsafe:integration-test -Dit.test="LowLevelToolDefinitionIT#lowLevelToolDefinition" -Denforcer.skip=true > "$LOG" 2>&1 & tail -f "$LOG"
   ```
5. Fix any failures. Iterate until the isolated test passes cleanly.
6. Run the full build:
   ```sh
   cd java
   LOG="$(date +%Y%m%d-%H%M)-job-logs.txt" && mvn clean verify > "$LOG" 2>&1 & tail -f "$LOG"
   ```
7. Fix any failures from the full build. Iterate until `mvn clean verify` passes cleanly.
