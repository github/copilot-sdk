# Implementation plan: tool-as-lambda ergonomics (issue #1810)

Human DRI: Ed Burns  
ADR: `java/docs/adr/adr-006-tool-definition-inline.md`  
Related ADR: `java/docs/adr/adr-005-tool-definition.md`  
Issue: #1810

---

## Completed phases

### Phase 1 ✅ — Define the problem and architectural decision

* We evaluated method-reference and inline approaches for `ToolDefinition.from(...)`.
* Decision (ADR-006): implement inline tool definition with explicit metadata (name, description, parameter definitions) and typed lambda handlers.
* Key decision driver: metadata quality and schema stability.

---

## Phase 2 — Baseline verification before new API work

This phase confirms the current runtime behavior we must preserve.

### 2.1 — Confirm low-level tool behavior contract

**Question:** What behavior must inline tools match exactly?

Use existing tests and code as ground truth:

* `ToolDefinition.create(...)` family in `java/src/main/java/com/github/copilot/rpc/ToolDefinition.java`
* Existing ergonomic behavior in `ToolDefinition.fromObject(...)` tests
* E2E tests under `java/src/test/java/com/github/copilot/e2e/`

**Contract to preserve:**

* `String` return is sent as-is.
* `void` return maps to `"Success"`.
* non-`String` return values are JSON-serialized.
* `CompletableFuture<T>` is supported.
* `overridesBuiltInTool`, `skipPermission`, and `defer` are carried through.

**Resolution target:** Document this contract as explicit acceptance criteria for all new `from(...)` overloads.

---

## Phase 3 — Ignorance reduction: questions to answer before writing code

Resolve these unknowns before production edits.

### 3.1 — Public API shape for `ToolDefinition.from(...)`

**Question:** Which overloads ship in v1?

Candidate API:

```java
ToolDefinition.from(String name, String description, Params params, ToolFn1<String, String> handler)
ToolDefinition.from(String name, String description, Params params, AsyncToolFn1<String, String> handler)
ToolDefinition.from(String name, String description, Params params, ToolFn2<A, B, R> handler)
// ...up to a practical arity cap
```

Open decisions:

1. Arity cap for v1 (`1..3` vs `1..5`).
1. Whether zero-arg tools need a dedicated overload.
1. Whether `ToolInvocation` context injection is included in v1.

**Recommendation:** start with arity `0..3`, plus context-capable variants only if they remain concise.

### 3.2 — Functional interface set and type inference

**Question:** What functional interfaces are needed for clean lambda syntax without casts?

Unknowns:

* Naming (`ToolFn1`, `ToolFn2`, `AsyncToolFn1`, etc.).
* Package placement (`com.github.copilot.rpc` vs `com.github.copilot.tool`).
* How to avoid ambiguous overload resolution between sync and async lambdas.

**Recommendation:** use distinct interfaces for sync and async handlers and keep overload count minimal to reduce ambiguity.

### 3.3 — Parameter metadata DSL design

**Question:** What is the smallest expressive parameter-definition API that preserves schema quality?

Candidate concepts:

* `ParamDef` builders (type, name, description, required/default).
* `Params.of(...)` container preserving declaration order.
* Optional helpers for common primitives.

Unknowns:

1. How defaults are represented and validated by type.
1. How optionality interacts with default values.
1. Whether descriptions are required by policy.

**Recommendation:** align with `@Param` semantics from ADR-005 wherever possible.

### 3.4 — Type-to-JSON-schema mapping for inline params

**Question:** Which Java parameter types are supported in v1 for inline definitions?

Minimum set:

* `String`
* numeric primitives/boxed
* `boolean`/`Boolean`
* enums
* `List<T>` for simple `T`
* `Map<String, T>` (or defer typed map support if not stable)
* record/POJO as parameter type

Unknowns:

* Whether nested objects and polymorphic types are in scope for v1.
* Whether schema generation should reuse existing tool schema utilities directly.

**Recommendation:** implement the subset already validated by existing ergonomic and low-level tests, then extend.

### 3.5 — Invocation and coercion policy

**Question:** How are JSON arguments coerced into typed lambda arguments?

Options:

* Reuse the same `ObjectMapper` conversion policy used by existing ergonomic tooling.
* Add bespoke coercion logic per primitive and complex type.

**Recommendation:** reuse existing mapper policy for consistency and reduced risk.

### 3.6 — Tool options and advanced flags

**Question:** How do callers set `overridesBuiltInTool`, `skipPermission`, and `defer` on inline tools?

Candidates:

* Overloads with an options object.
* Fluent builder wrapping `ToolDefinition.from(...)`.

**Recommendation:** options object first, to avoid overload explosion.

### 3.7 — Error model and validation boundaries

**Question:** Which invalid states should fail early?

Must-validate cases:

* duplicate parameter names
* missing required metadata (name/type)
* unsupported type mappings
* incompatible default values

**Recommendation:** fail fast at tool construction with precise `IllegalArgumentException` messages.

### 3.8 — Binary compatibility and package placement

**Question:** Where do new public types live without destabilizing existing API?

Unknowns:

* whether to place new functional interfaces and param DSL under `rpc` or `tool`
* impact on `module-info.java` exports

**Recommendation:** place user-facing ergonomics in the package users already discover for tools, and keep internal helpers package-private.

### 3.9 — E2E test scenario and snapshot reuse

**Question:** Do we need a new replay snapshot?

Because wire format should match existing tool definitions, we should attempt snapshot reuse first.

**Recommendation:** start with existing tool-definition snapshot; only add a new YAML if wire traffic differs.

---

## Phase 4 — Implementation (build order)

After Phase 3 is resolved, implement in this order.

### 4.1 — Add public API types

**What:** Introduce functional interfaces and parameter metadata classes for inline tools.

**Likely files:**

* `java/src/main/java/com/github/copilot/rpc/` (new interfaces and metadata types)

**Gating criteria:** compile passes; API signatures are stable and unambiguous for common lambda call sites.

### 4.2 — Implement `ToolDefinition.from(...)` overloads

**What:** Add typed overloads that build `ToolDefinition` plus invocation adapter.

**Likely files:**

* `java/src/main/java/com/github/copilot/rpc/ToolDefinition.java`

**Gating criteria:** unit tests prove schema output and handler invocation for arities and sync/async paths.

### 4.3 — Implement schema and coercion internals

**What:** Build internal mapping from `ParamDef` + handler type info to JSON schema and typed invocation.

**Likely files:**

* new internal helper(s) under `java/src/main/java/com/github/copilot/rpc/` or `.../tool/`

**Gating criteria:** matches baseline behavior contract from Phase 2.

### 4.4 — Unit tests for API behavior and validation

**What:** Add focused tests for:

* successful inline definitions (0..N args)
* sync and async handlers
* option flags propagation
* default/required semantics
* error paths

**Likely files:**

* `java/src/test/java/com/github/copilot/rpc/*`

**Gating criteria:** deterministic tests covering success + failure paths.

### 4.5 — E2E integration test

**What:** Add/extend a Java E2E test that uses inline tool definition in a real session.

**Likely files:**

* `java/src/test/java/com/github/copilot/e2e/*`
* `test/snapshots/tools/*` (only if new snapshot required)

**Gating criteria:** E2E passes with expected assistant behavior and tool side effects.

### 4.6 — Documentation updates

**What:** Document inline tool definition in Java README and link ADR-006.

**Likely files:**

* `java/README.md`
* `java/docs/adr/adr-006-tool-definition-inline.md` (if follow-up clarifications are needed)

**Gating criteria:** examples compile conceptually and reflect final API names.

---

## Phase 5 — Portability and follow-on work

### 5.1 — Evaluate method-reference API as separate workstream

Method-reference registration can be implemented independently after inline tool definition. Track this as separate scope to keep issue #1810 focused.

### 5.2 — Expand type coverage

After v1, add deeper schema coverage (nested objects, richer map/list combinations, polymorphic payloads) based on real usage demand.

---

## Acceptance checklist

Before calling implementation complete:

1. Inline tool definitions can be authored at call site without annotation processing.
1. Metadata quality (name/description/params/defaults/required) is explicit and stable.
1. Runtime behavior matches existing tool contract (`String`/`void`/JSON/async/options flags).
1. Unit and E2E tests pass for the implemented scope.
1. Java README includes at least one concise inline tool example.
