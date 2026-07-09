# ADR-005: Ergonomic tool definition API — annotation-on-method approach

## Context and Problem Statement

The Java SDK's current tool definition API requires developers to manually provide every piece of tool metadata: name, description, JSON Schema (as a `Map<String, Object>`), and a handler lambda. This results in highly verbose, error-prone code:

```java
ToolDefinition.create("set_current_phase",
        "Sets the current phase of the agent. Use this to report progress.",
        Map.of("type", "object",
               "properties", Map.of("phase", Map.of("type", "string", "enum",
                       List.of("searching", "analyzing", "done"))),
               "required", List.of("phase")),
        invocation -> {
            Phase phase = invocation.getArgumentsAs(PhaseArgs.class).phase();
            this.phase = phase;
            updateUi();
            return CompletableFuture.completedFuture("Phase set to " + phase);
        })
```

Compare this with the C# SDK where reflection on `[DisplayName]`, `[Description]`, and method parameters auto-generates everything:

```csharp
CopilotTool.DefineTool(SetCurrentPhase)
```

Or with Go, where generics derive the schema from the input type:

```go
DefineTool[PhaseArgs, string]("set_current_phase", "Sets phase", handler)
```

The Java SDK needs a higher-level API that is idiomatic Java while dramatically reducing boilerplate.

## Considered Options

### Option 1: Current API (status quo)

Explicit `ToolDefinition.create(name, description, schema, handler)` with a hand-written `Map<String, Object>` JSON Schema and a `ToolHandler` lambda.

**Advantages:**
- No reflection or annotation processing at runtime.
- Full explicit control over every aspect of the tool spec.

**Drawbacks:**
- Extremely verbose — a single tool definition can span 10+ lines.
- Error-prone — typos in schema keys (`"tpye"` instead of `"type"`) produce runtime failures, not compile-time errors.
- No type safety on arguments — developers must call `invocation.getArgumentsAs(T.class)` manually inside the handler.
- Inconsistent with every other SDK in the mono-repo, all of which offer a higher-level path.

### Option 2: Record-as-schema with generic factory

Define a record for the tool's arguments and use a generic factory method to auto-generate the schema from the record's `RecordComponent[]` metadata. Because `@CopilotToolParam` targets `ElementType.PARAMETER` (method parameters only), it cannot be placed on record components; per-field descriptions are not supported in this option:

```java
record PhaseArgs(Phase phase) {}

ToolDefinition.define("set_current_phase",
        "Sets the current phase of the agent.",
        PhaseArgs.class,
        (args, invocation) -> {
            this.phase = args.phase();
            updateUi();
            return CompletableFuture.completedFuture("Phase set to " + args.phase());
        });
```

**Advantages:**
- Schema is auto-generated from the record — no hand-written `Map`.
- Type-safe handler — the lambda receives the deserialized record directly.
- Closest analog to Go's `DefineTool[T, U]`.
- No classpath scanning or special framework plumbing.

**Drawbacks:**
- Tool name and description are still explicit string arguments.
- Requires a separate record class for every tool's args (even trivial single-param tools).
- The handler is still an explicit lambda — the "tool" is not the method itself.
- Per-field descriptions cannot be provided: `@CopilotToolParam` targets method parameters only, not record components.
- Nested or complex schemas (arrays of objects, polymorphic types) need additional mapping logic.
- No analog in the broader Java ecosystem; Java developers are not accustomed to defining a record per function call.

### Option 3: Annotation-on-method (langchain4j-style)

Annotate existing Java methods with `@Tool` (or a Copilot-specific equivalent) and annotate parameters with `@P`/`@CopilotToolParam`. The framework discovers tools by scanning methods on a given object, auto-generates `ToolSpecification` / `ToolDefinition` from the method signature, and dispatches invocations directly to the annotated method.

```java
class MyTools {

    @CopilotTool("Sets the current phase of the agent. Use this to report progress.")
    String setCurrentPhase(@CopilotToolParam("The phase to transition to") Phase phase) {
        this.phase = phase;
        updateUi();
        return "Phase set to " + phase;
    }

    @CopilotTool(name = "report_intent", value = "Reports the agent's intent",
                 overridesBuiltInTool = true)
    String reportIntent(@CopilotToolParam("The intent") String intent) {
        // ...
    }
}

// Registration:
var tools = ToolDefinition.fromObject(myToolsInstance);
// → List<ToolDefinition> with schema, description, and handler wired automatically.
```

This is the approach used by [langchain4j](https://github.com/langchain4j/langchain4j) (see [High Level Tool API](https://github.com/langchain4j/langchain4j/blob/main/docs/docs/tutorials/tools.md#high-level-tool-api)), which is the most widely adopted Java AI framework.

**What the framework does automatically:**
1. **Name** — derived from `@CopilotTool(name=...)` or the method name (converted to snake_case).
2. **Description** — from `@CopilotTool("...")` or `@CopilotTool(value="...")`.
3. **Parameter schema** — generated by reflecting on method parameters: types map to JSON Schema types; `@CopilotToolParam` provides descriptions; `Optional<T>` or `@CopilotToolParam(required=false)` marks optional params.
4. **Handler** — the method itself. The framework deserializes JSON arguments into the method's parameter types and invokes the method reflectively. The return value is serialized back to a string result.

**Advantages:**
- **Minimal boilerplate** — a tool is just an annotated method. No records, no lambdas, no schema maps.
- **Idiomatic Java** — this pattern is familiar from JAX-RS (`@Path`/`@GET`), Spring MVC (`@RequestMapping`), and CDI (`@Inject`). Java developers are accustomed to annotation-driven frameworks.
- **The method IS the handler** — no separation between "tool definition" and "tool implementation". Everything is co-located.
- **Proven at scale** — langchain4j has validated this design across thousands of production deployments.
- **Inheritance and discovery** — tools can be inherited from superclasses, composed from multiple objects, and discovered dynamically.
- **Ecosystem alignment** — closest to what C#'s `CopilotTool.DefineTool(MethodGroup)` achieves via reflection, adapted to Java idioms.
- **Parameter-level type safety** — each parameter is a method argument with its own Java type. No single "args" record needed.

**Drawbacks:**
- Requires runtime reflection for method invocation and schema generation.
- One-time scanning cost at registration time (negligible for typical tool counts).
- Return type handling needs a policy: `String` → sent as-is; `void` → "Success"; other types → JSON-serialized.
- Async story: methods could return `CompletableFuture<T>` for async tools, or the framework could invoke synchronous methods on a configurable executor.
- New annotation(s) added to the public API surface (`@CopilotTool`, `@CopilotToolParam`).
- Requires `-parameters` javac flag for parameter name preservation (or explicit `@CopilotToolParam(name=...)` — same constraint as langchain4j).

## Decision Outcome

**Chosen: Option 3 — Annotation-on-method (langchain4j-style).**

### Rationale

1. **Java developers expect annotation-driven APIs.** Every major Java framework (Spring, Jakarta EE, Quarkus, Micronaut, langchain4j) uses annotations on methods/parameters as the primary developer-facing abstraction. This is idiomatic Java; records-as-schema is not.

2. **Minimum viable tool is one annotated method.** With Option 3, the absolute minimum code to define a tool is:
   ```java
   @CopilotTool("Gets the weather")
   String getWeather(@CopilotToolParam("City") String city) { return weatherApi.get(city); }
   ```
   With Option 2, you need a record class *and* a lambda. With Option 1, you need a record class, a Map schema, *and* a lambda.

3. **The method IS the tool.** Co-locating metadata (name, description, parameter descriptions) with implementation eliminates drift between the spec and the code. When someone adds a parameter, the schema updates automatically.

4. **Proven design.** langchain4j's `@Tool` / `@P` design has been adopted by thousands of Java projects and validated against real LLM providers. We can learn from their design decisions (handling of `Optional`, `void` returns, `@Description` on nested types, inheritance rules) rather than inventing from scratch.

5. **Closes the ergonomics gap with C# and Go.** The C# SDK's `CopilotTool.DefineTool(SetCurrentPhase)` achieves one-line tool definition via reflection. Option 3 is the Java equivalent — the annotation-on-method pattern is Java's analog to C#'s attribute-on-method + method-group-to-delegate pattern.

6. **Option 1 remains available as the low-level API.** Users who need full control (dynamic tools, computed schemas, tools from external config) can still use `ToolDefinition.create(...)`. Option 3 is a higher-level convenience that delegates to Option 1 under the hood — the same two-level architecture langchain4j uses (Low Level Tool API vs High Level Tool API).

## Implementation: JSR 269 annotation processor for compile-time metadata generation

A key improvement over langchain4j's pure-runtime-reflection approach: we will use a **JSR 269 annotation processor** (the same mechanism used for `@CopilotExperimental`) to generate tool metadata at compile time. This eliminates the `-parameters` javac flag requirement entirely.

### Why this works

`javax.lang.model.element.VariableElement.getSimpleName()` always returns the real parameter name at compile time, regardless of whether `-parameters` is passed to `javac`. The `-parameters` flag only controls whether those names survive into `.class` bytecode for runtime reflection. An annotation processor sees the source-level names unconditionally.

### How it works

The processor runs at compile time, finds all `@CopilotTool`-annotated methods, and generates a companion metadata class per tool-bearing class:

```java
// GENERATED — do not edit
final class MyTools$$CopilotToolMeta {
    static List<ToolDefinition> definitions(MyTools instance) {
        return List.of(
            new ToolDefinition("set_current_phase",
                "Sets the current phase of the agent.",
                Map.of("type", "object",
                       "properties", Map.of("phase", Map.of("type", "string",
                               "description", "The phase to transition to")),
                       "required", List.of("phase")),
                invocation -> {
                    Phase phase = invocation.getArgumentsAs(Phase.class);
                    return CompletableFuture.completedFuture(
                        instance.setCurrentPhase(phase));
                }, null, null, null)
        );
    }
}
```

At runtime, `ToolDefinition.fromObject(myTools)` loads the generated `$$CopilotToolMeta` class — zero reflection, zero dependency on `-parameters`.

### Compile-time validation

Because the processor has full access to the source AST, it can emit compile errors for:
- Missing `@CopilotToolParam` on parameters (when descriptions are required by policy).
- Unsupported parameter types (types without a clear JSON Schema mapping).
- Duplicate tool names within the same class hierarchy.
- Invalid annotation combinations (e.g., `overridesBuiltInTool` on a tool with `skipPermission`).

### Precedent

| Framework | Approach |
|-----------|----------|
| **Micronaut** | Annotation processor generates all DI metadata at compile time — no runtime reflection, no `-parameters` needed |
| **Dagger 2** | Processor generates `_Factory` / `_MembersInjector` classes |
| **MapStruct** | Processor generates mapper implementations from interface method signatures |
| **Our own `@CopilotExperimental`** | Processor walks declared elements via JSR 269 (see ADR-004) |

### Comparison: annotation processor vs. runtime reflection

| | Annotation processor (our approach) | Runtime reflection (langchain4j default) |
|---|---|---|
| Requires `-parameters`? | **No** | Yes (or `@P(name=...)`) |
| GraalVM native-image friendly? | **Yes** | Needs reflection config |
| Compile-time error checking? | **Yes** | Fails at runtime |
| Extra generated source files? | Yes | None |
| Works without running the processor? | No — but fails loudly at compile time | Yes (degraded) |

## Consequences

- New public annotations: `@CopilotTool` and `@CopilotToolParam` (in `com.github.copilot.rpc` or a new `com.github.copilot.tool` package).
- New JSR 269 annotation processor that generates `$$CopilotToolMeta` companion classes at compile time.
- New utility: `ToolDefinition.fromObject(Object)` / `ToolDefinition.fromClass(Class<?>)` that loads the generated metadata class (falling back to runtime reflection if the processor was not run).
- The existing `ToolDefinition.create(...)` / `ToolDefinition.createOverride(...)` APIs remain unchanged — they become the "low-level" path.
- No `-parameters` javac flag requirement for users who run the annotation processor (which happens automatically when the SDK is on the compile classpath).
- Async support: methods returning `CompletableFuture<T>` are handled natively; synchronous methods are wrapped in `CompletableFuture.completedFuture(...)` (or dispatched to an executor, TBD).
- GraalVM native-image compatibility without additional reflection configuration.
- **Experimental designation:** `@CopilotTool`, `@CopilotToolParam`, `ToolDefinition.fromObject(Object)`, and `ToolDefinition.fromClass(Class<?>)` will all be annotated with `@CopilotExperimental`. This gates adoption behind an explicit opt-in (`-Acopilot.experimental.allowed=true`) until the API surface stabilizes, consistent with the policy established in ADR-004.

## Related work items

- https://github.com/github/copilot-sdk/issues/1682
- langchain4j reference: https://github.com/langchain4j/langchain4j/blob/main/docs/docs/tutorials/tools.md#high-level-tool-api
- langchain4j `@Tool` source: https://github.com/langchain4j/langchain4j/blob/main/langchain4j-core/src/main/java/dev/langchain4j/agent/tool/Tool.java
- langchain4j `@P` source: https://github.com/langchain4j/langchain4j/blob/main/langchain4j-core/src/main/java/dev/langchain4j/agent/tool/P.java
- langchain4j `ToolSpecifications` (schema generation from methods): https://github.com/langchain4j/langchain4j/blob/main/langchain4j-core/src/main/java/dev/langchain4j/agent/tool/ToolSpecifications.java
