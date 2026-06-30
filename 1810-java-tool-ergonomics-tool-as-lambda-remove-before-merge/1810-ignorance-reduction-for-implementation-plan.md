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

## Phase 2 ✅ — Baseline verification before new API work

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

## Phase 3 ✅ — Ignorance reduction: questions to answer before writing code

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

**Resolutions:**

**Arity cap for v1 (`1..3` vs `1..5`).**: 

Assume current annotation class `com.github.copilot.tool.Param` is renamed to `CopilotToolParam`.

Then, the answer to the arity question is shown here.

```java
package com.github.copilot.tool;

import java.util.Objects;

/**
 * Runtime parameter metadata for lambda-defined tools.
 * Mirrors the fields of @CopilotToolParam.
 */
public final class Param<T> {

    private final Class<T> type;
    private final String description;
    private final String name;
    private final boolean required;
    private final String defaultValue;

    private Param(Class<T> type, String description, String name, boolean required, String defaultValue) {
        this.type = Objects.requireNonNull(type, "type");
        this.description = requireNonBlank(description, "description");
        this.name = requireNonBlank(name, "name");
        this.defaultValue = defaultValue == null ? "" : defaultValue;
        this.required = required;

        if (this.required && !this.defaultValue.isEmpty()) {
            throw new IllegalArgumentException("required=true cannot be combined with a non-empty defaultValue");
        }

        validateDefaultValue(type, this.defaultValue);
    }

    /** Minimal fluent entrypoint (required=true, no default). */
    public static <T> Param<T> of(Class<T> type, String name, String description) {
        return new Param<>(type, description, name, true, "");
    }

    /** Full factory for parity with annotation fields. */
    public static <T> Param<T> of(
            Class<T> type,
            String name,
            String description,
            boolean required,
            String defaultValue) {
        return new Param<>(type, description, name, required, defaultValue);
    }

    public Param<T> name(String name) {
        return new Param<>(this.type, this.description, name, this.required, this.defaultValue);
    }

    public Param<T> description(String description) {
        return new Param<>(this.type, description, this.name, this.required, this.defaultValue);
    }

    /**
     * Alias for annotation parity.
     */
    public Param<T> value(String value) {
        return description(value);
    }

    public Param<T> required(boolean required) {
        return new Param<>(this.type, this.description, this.name, required, this.defaultValue);
    }

    /**
     * Setting a default makes the parameter optional.
     */
    public Param<T> defaultValue(String defaultValue) {
        return new Param<>(this.type, this.description, this.name, false, defaultValue);
    }

    public Class<T> type() {
        return type;
    }

    /**
     * Alias kept for annotation parity.
     */
    public String value() {
        return description;
    }

    public String description() {
        return description;
    }

    public String name() {
        return name;
    }

    public boolean required() {
        return required;
    }

    public String defaultValue() {
        return defaultValue;
    }

    public boolean hasDefaultValue() {
        return !defaultValue.isEmpty();
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Param<?> other)) {
            return false;
        }
        return required == other.required
                && Objects.equals(type, other.type)
                && Objects.equals(description, other.description)
                && Objects.equals(name, other.name)
                && Objects.equals(defaultValue, other.defaultValue);
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, description, name, required, defaultValue);
    }

    private static String requireNonBlank(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be null or blank");
        }
        return value;
    }

    private static <T> void validateDefaultValue(Class<T> type, String defaultValue) {
        if (defaultValue == null || defaultValue.isEmpty()) {
            return;
        }

        try {
            if (type == String.class) {
                return;
            }
            if (type == Integer.class || type == int.class) {
                Integer.parseInt(defaultValue);
                return;
            }
            if (type == Long.class || type == long.class) {
                Long.parseLong(defaultValue);
                return;
            }
            if (type == Double.class || type == double.class) {
                Double.parseDouble(defaultValue);
                return;
            }
            if (type == Float.class || type == float.class) {
                Float.parseFloat(defaultValue);
                return;
            }
            if (type == Short.class || type == short.class) {
                Short.parseShort(defaultValue);
                return;
            }
            if (type == Byte.class || type == byte.class) {
                Byte.parseByte(defaultValue);
                return;
            }
            if (type == Boolean.class || type == boolean.class) {
                if (!"true".equalsIgnoreCase(defaultValue) && !"false".equalsIgnoreCase(defaultValue)) {
                    throw new IllegalArgumentException("must be 'true' or 'false'");
                }
                return;
            }
            if (type.isEnum()) {
                @SuppressWarnings({ "rawtypes", "unchecked" })
                Class<? extends Enum> enumType = (Class<? extends Enum>) type;
                Enum.valueOf(enumType, defaultValue);
                return;
            }
        } catch (RuntimeException ex) {
            throw new IllegalArgumentException(
                    "defaultValue '" + defaultValue + "' is not valid for type " + type.getSimpleName(), ex);
        }

        throw new IllegalArgumentException(
                "defaultValue is not supported for type " + type.getName() + " without a custom coercion policy");
    }
}
```

Then the API:

```java
// -------------------------------------------------------
// from(...) — sync, no ToolInvocation, arity 0..2
// -------------------------------------------------------

// 0-arg: Supplier<R>
static <R> ToolDefinition from(
    String name,
    String description,
    Supplier<R> handler);

// 1-arg: Function<T1, R>
static <T1, R> ToolDefinition from(
    String name,
    String description,
    Param<T1> p1,
    Function<T1, R> handler);

// 2-arg: BiFunction<T1, T2, R>
static <T1, T2, R> ToolDefinition from(
    String name,
    String description,
    Param<T1> p1,
    Param<T2> p2,
    BiFunction<T1, T2, R> handler);

// -------------------------------------------------------
// fromAsync(...) — async, no ToolInvocation, arity 0..2
// -------------------------------------------------------

// 0-arg: Supplier<CompletableFuture<R>>
static <R> ToolDefinition fromAsync(
    String name,
    String description,
    Supplier<CompletableFuture<R>> handler);

// 1-arg: Function<T1, CompletableFuture<R>>
static <T1, R> ToolDefinition fromAsync(
    String name,
    String description,
    Param<T1> p1,
    Function<T1, CompletableFuture<R>> handler);

// 2-arg: BiFunction<T1, T2, CompletableFuture<R>>
static <T1, T2, R> ToolDefinition fromAsync(
    String name,
    String description,
    Param<T1> p1,
    Param<T2> p2,
    BiFunction<T1, T2, CompletableFuture<R>> handler);
```

**Whether zero-arg tools need a dedicated overload.**: Yes. And it needs two. See the preceding answer.

**Whether `ToolInvocation` context injection is included in v1.**: 

Yes, it must be. Here is the shape.

```java
// -----------------------------
// With ToolInvocation context
// -----------------------------

// 0 visible args + ToolInvocation, sync:
// Function<ToolInvocation, R>
static <R> ToolDefinition fromWithToolInvocation(
    String name,
    String description,
    Function<ToolInvocation, R> handler);

// 0 visible args + ToolInvocation, async:
// Function<ToolInvocation, CompletableFuture<R>>
static <R> ToolDefinition fromAsyncWithToolInvocation(
    String name,
    String description,
    Function<ToolInvocation, CompletableFuture<R>> handler);

// 1 visible arg + ToolInvocation, sync:
// BiFunction<T1, ToolInvocation, R>
static <T1, R> ToolDefinition fromWithToolInvocation(
    String name,
    String description,
    Param p1,
    BiFunction<T1, ToolInvocation, R> handler);

// 1 visible arg + ToolInvocation, async:
// BiFunction<T1, ToolInvocation, CompletableFuture<R>>
static <T1, R> ToolDefinition fromAsyncWithToolInvocation(
    String name,
    String description,
    Param p1,
    BiFunction<T1, ToolInvocation, CompletableFuture<R>> handler);
```

Usage examples.

```java
import java.util.concurrent.CompletableFuture;
import java.util.function.BiFunction;
import java.util.function.Function;

Param<String> phaseParam = Param.of(String.class, "phase", "Current phase");

// -------------------------------------------
// fromWithToolInvocation(...)
// -------------------------------------------

// 0 visible args + ToolInvocation, sync:
// Function<ToolInvocation, R>
ToolDefinition sessionInfoSync = ToolDefinition.fromWithToolInvocation(
    "session_info",
    "Return the current session id",
    invocation -> "sessionId=" + invocation.getSessionId()
);

// 1 visible arg + ToolInvocation, sync:
// BiFunction<T1, ToolInvocation, R>
ToolDefinition reportPhaseSync = ToolDefinition.fromWithToolInvocation(
    "report_phase",
    "Report the current phase along with invocation context",
    phaseParam,
    (phase, invocation) ->
        "phase=" + phase + ", toolCallId=" + invocation.getToolCallId()
);

// -------------------------------------------
// fromAsyncWithToolInvocation(...)
// -------------------------------------------

// 0 visible args + ToolInvocation, async:
// Function<ToolInvocation, CompletableFuture<R>>
ToolDefinition sessionInfoAsync = ToolDefinition.fromAsyncWithToolInvocation(
    "session_info_async",
    "Return the current session id asynchronously",
    invocation -> CompletableFuture.completedFuture(
        "sessionId=" + invocation.getSessionId()
    )
);

// 1 visible arg + ToolInvocation, async:
// BiFunction<T1, ToolInvocation, CompletableFuture<R>>
ToolDefinition reportPhaseAsync = ToolDefinition.fromAsyncWithToolInvocation(
    "report_phase_async",
    "Report the current phase with invocation context asynchronously",
    phaseParam,
    (phase, invocation) -> CompletableFuture.completedFuture(
        "phase=" + phase + ", toolCallId=" + invocation.getToolCallId()
    )
);
```

### 3.2 — Functional interface set and type inference

**Question:** What functional interfaces are needed for clean lambda syntax without casts?

Unknowns:

* Naming (`ToolFn1`, `ToolFn2`, `AsyncToolFn1`, etc.).
* Package placement (`com.github.copilot.rpc` vs `com.github.copilot.tool`).
* How to avoid ambiguous overload resolution between sync and async lambdas.

**Recommendation:** use distinct interfaces for sync and async handlers and keep overload count minimal to reduce ambiguity.

**Resolution:**

* Naming (`ToolFn1`, `ToolFn2`, `AsyncToolFn1`, etc.): see 3.1.
* Package placement `com.github.copilot.tool`.
* How to avoid ambiguous:  Tools-as-lambda uses only JDK functional interfaces; sync and async are separated by method-family naming (`from`/`fromAsync`/`fromWithToolInvocation`/`fromAsyncWithToolInvocation`); no custom SAMs required.

### 3.3 — Parameter metadata DSL design

**Question:** What is the smallest expressive parameter-definition API that preserves schema quality?

Candidate concepts:

* `ParamDef` builders (type, name, description, required/default).
* `Params.of(...)` container preserving declaration order.
* Optional helpers for common primitives.

Unknowns:

1. How defaults are represented and validated by type.
2. How optionality interacts with default values.
3. Whether descriptions are required by policy.

**Recommendation:** align with `@Param` semantics from ADR-005 wherever possible.

**Resolution:**

`Params.of(...)`: not needed.

Use the above `Param` class. 

- Lambda API enforcement
   - `Param.of(...)` and fluent mutators reject blank `name`/`description`.
   - `Param.defaultValue(...)` validates the value against `Class<T>`.
   - `required=true` with non-empty `defaultValue` is rejected.
   - Every `ToolDefinition.from` / `fromAsync` overload re-validates supplied `Param<?>` objects before building the tool.


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

**Resolution:**

For 3.4, I’d resolve it at this level:

- tool-as-lambda supports exactly the same parameter-type surface already supported by the existing Java schema/tool pipeline, reused for lambda tools.
- This includes the minimal set you listed.
- No new schema semantics are invented for tool-as-lambda.
- If a type is not already supported by the current Java ergonomic/low-level tool path, it is out of scope for tool-as-lambda.

### 3.5 — Invocation and coercion policy

**Question:** How are JSON arguments coerced into typed lambda arguments?

Options:

* Reuse the same `ObjectMapper` conversion policy used by existing ergonomic tooling.
* Add bespoke coercion logic per primitive and complex type.

**Recommendation:** reuse existing mapper policy for consistency and reduced risk.

**Resolution:** Use the existing `ObjectMapper`, eliminating DRY violations if any crop up.

### 3.6 — Tool options and advanced flags

**Question:** How do callers set `overridesBuiltInTool`, `skipPermission`, and `defer` on inline tools?

Candidates:

* Overloads with an options object.
* Fluent builder wrapping `ToolDefinition.from(...)`.

**Recommendation:** options object first, to avoid overload explosion.

**Resolution:**

Use fluent immutable modifier methods on `ToolDefinition` rather than introducing a separate options object in v1.

Because `ToolDefinition` is already an immutable record carrying `overridesBuiltInTool`, `skipPermission`, and `defer`, the lambda-based `from*` factories should return a `ToolDefinition` that callers may further customize with copy-style fluent methods.

Example:

```java
ToolDefinition tool = ToolDefinition.from(
        "report_intent",
        "Reports the agent's current intent",
        Param.of(String.class, "intent", "The intent to report"),
        intent -> "Reported intent: " + intent)
    .overridesBuiltInTool(true)
    .skipPermission(true)
    .defer(ToolDefer.AUTO);
```

Equivalent context-aware example:

```
ToolDefinition tool = ToolDefinition.fromWithToolInvocation(
        "report_phase",
        "Reports the current phase with invocation context",
        Param.of(String.class, "phase", "The current phase"),
        (phase, invocation) -> "phase=" + phase + ", toolCallId=" + invocation.getToolCallId())
    .skipPermission(true)
    .defer(ToolDefer.NEVER);
```

The modifier surface for v1 is:

```
ToolDefinition overridesBuiltInTool(boolean value);
ToolDefinition skipPermission(boolean value);
ToolDefinition defer(ToolDefer value);
```

Notes:

- `defer` should use the existing `ToolDefer` enum, not a boolean.
- This keeps the API aligned with the existing `ToolDefinition` data model.
- This avoids introducing a separate options type solely for inline/lambda-defined tools.
- Existing low-level factories (`createOverride`, `createSkipPermission`, `createWithDefer`) may remain for compatibility, but the new lambda-based API should prefer the fluent style.

### 3.7 — Error model and validation boundaries

**Question:** Which invalid states should fail early?

Must-validate cases:

* duplicate parameter names
* missing required metadata (name/type)
* unsupported type mappings
* incompatible default values

**Recommendation:** fail fast at tool construction with precise `IllegalArgumentException` messages.

**Resolution:**

- Construction-time validation for lambda tools:
   - all `ToolDefinition.from*` factories must validate before returning
   - failures use `IllegalArgumentException`
   - messages should identify the offending tool name and parameter name when possible
- `Param`-local validation:
   - blank name/description
   - `required=true` with default
   - default incompatible with declared type
- Cross-parameter validation:
   - duplicate parameter names
   - unsupported schema/type mappings


### 3.8 — Binary compatibility and package placement

**Question:** Where do new public types live without destabilizing existing API?

Unknowns:

* whether to place new functional interfaces and param DSL under `rpc` or `tool`
* impact on `module-info.java` exports

**Recommendation:** place user-facing ergonomics in the package users already discover for tools, and keep internal helpers package-private.

**Resolution:**

- new public helper types like `Param<T>` belong in `com.github.copilot.tool`
- any necessary `module-info.java` export updates should expose only that user-facing package surface
- no extra public internal-helper types should leak just to support lambda tools


### 3.9 — E2E test scenario and snapshot reuse

**Question:** Do we need a new replay snapshot?

Because wire format should match existing tool definitions, we should attempt snapshot reuse first.

**Recommendation:** start with existing tool-definition snapshot; only add a new YAML if wire traffic differs.

**Resolution:**

Yes. start with existing tool-definition snapshot; only add a new YAML if wire traffic differs.

---

## Phase 4 — Implementation (build order)

After Phase 3 is resolved, implement in this order.

### Phase 4 progress checklist

- [x] 4.1 — Add public API types ([#1839](https://github.com/github/copilot-sdk/issues/1839))
- [ ] 4.2 — Implement `ToolDefinition.from(...)` overloads ([#1840](https://github.com/github/copilot-sdk/issues/1840))
- [ ] 4.3 — Implement schema and coercion internals ([#1841](https://github.com/github/copilot-sdk/issues/1841))
- [ ] 4.4 — Unit tests for API behavior and validation ([#1842](https://github.com/github/copilot-sdk/issues/1842))
- [ ] 4.5 — E2E integration test ([#1843](https://github.com/github/copilot-sdk/issues/1843))
- [ ] 4.6 — Documentation updates ([#1844](https://github.com/github/copilot-sdk/issues/1844))

### 4.1 — Add public API types

**What:** Introduce functional interfaces and parameter metadata classes for inline tools.

**Likely files:**

* `java/src/main/java/com/github/copilot/tool/` (new interfaces and metadata types)

**Gating criteria:** compile passes; API signatures are stable and unambiguous for common lambda call sites.

### 4.2 — Implement `ToolDefinition.from(...)` overloads

**What:** Add typed overloads that build `ToolDefinition` plus invocation adapter.

**Likely files:**

* `java/src/main/java/com/github/copilot/rpc/ToolDefinition.java`

**Gating criteria:** unit tests prove schema output and handler invocation for arities and sync/async paths.

### 4.3 — Implement schema and coercion internals

**What:** Build internal mapping from `Param<T>` + handler type info to JSON schema and typed invocation.

**Likely files:**

* new internal helper(s) under `java/src/main/java/com/github/copilot/tool/`.

**Gating criteria:** matches baseline behavior contract from Phase 2.

### 4.4 — Unit tests for API behavior and validation

**What:** Add focused tests for:

* successful inline definitions (0..N args)
* sync and async handlers
* option flags propagation
* default/required semantics
* error paths

**Likely files:**

* `java/src/test/java/com/github/copilot/tool/*`

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
