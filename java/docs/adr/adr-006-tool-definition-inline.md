# ADR-006: Inline tool definition with lambdas

## Context and problem statement

[ADR-005](adr-005-tool-definition.md) introduced an ergonomic Java tools API based on `@CopilotTool` method annotations, `@CopilotToolParam` parameter annotations, and `ToolDefinition.fromObject(...)` for reflection-based tool registration. That model works well when teams define tools as methods on a class.

The next ergonomics goal is an inline style comparable to C# `CopilotTool.DefineTool(...)`, where developers can define a tool at the call site without creating a separate tool container class.

For this decision, we evaluated two alternatives:

* Method-reference registration (`ToolDefinition.from(tools::setCurrentPhase)`)
* Inline lambda registration (`ToolDefinition.from(..., phase -> ...)`)

The key factor is metadata quality: tool name, description, parameter names, parameter descriptions, required/default semantics, and schema stability.

## Considered options

### Option 1: Method-reference API

Example:

```java
ToolDefinition setPhase = ToolDefinition.from(tools::setCurrentPhase);
```

In this model, metadata is sourced from existing method-level annotations (`@CopilotTool`, `@Param`) on the referenced method.

Advantages:

* Closest Java analog to C# method-group ergonomics
* High-quality metadata with minimal additional API surface
* Reuses ADR-005 metadata and invocation behavior directly

Drawbacks:

* Not truly inline: still requires a declared method (and usually annotations) elsewhere
* Does not solve the "define the whole tool at the call site" use case
* Method-reference resolution adds runtime/reflection complexity

### Option 2: Inline lambda API with explicit metadata

Example:

```java
ToolDefinition setPhase = ToolDefinition.from(
        "set_current_phase",
        "Sets the current phase of the agent",
        Param.of(String.class, "phase", "The phase to transition to"),
        (String phase) -> {
            currentPhase = phase;
            return "Phase set to " + phase;
        });
```

In this model, handler logic is inline, and metadata is provided explicitly through `Param.of(...)` parameter definitions.

Advantages:

* True inline authoring at the session construction site
* No dependence on lambda parameter-name reflection or `-parameters`
* Deterministic metadata and schema generation
* Independent from annotation processing and generated companion classes

Drawbacks:

* Slightly more verbose than method-reference style because metadata is explicit
* Introduces new public API types for parameter definitions and typed lambda overloads
* Requires careful API design to stay concise for common one-parameter tools

## Decision outcome

Chosen: **Option 2 for ADR-006 scope** — inline lambda API with explicit metadata.

Rationale:

1. The primary requirement for this ADR is inline definition. Option 2 satisfies it directly; Option 1 does not.
1. Metadata quality is the critical requirement. Option 2 keeps metadata explicit and stable, instead of relying on fragile lambda introspection.
1. Option 2 can ship independently of method-reference support and without changes to annotation processing.
1. Option 2 preserves behavior parity with existing tool execution by delegating to `ToolDefinition` construction and current invocation semantics.

Option 1 remains valuable and can be added independently as a separate ergonomic layer. It is not blocked by this decision.

## Design constraints and non-goals

Constraints for the inline lambda API:

* Require explicit tool name and description.
* Require explicit parameter metadata (at minimum name and type, with optional description/required/default).
* Support both sync and async handlers (`R` and `CompletableFuture<R>`).
* Keep result semantics aligned with existing behavior (`String` passthrough, `void` maps to `"Success"`, non-string objects serialized to JSON).
* Keep override/permission/defer flags available through options, consistent with existing `ToolDefinition` fields.

Non-goals for this ADR:

* Replacing `@CopilotTool`/`fromObject` APIs.
* Defining method-reference registration behavior in detail.
* Introducing compile-time code generation for lambda metadata.

## Consequences

The SDK now provides an explicit inline path for developers who prefer to keep tool declarations at session creation while preserving high-quality schema metadata. Implemented API families include:

- `ToolDefinition.from(name, description, [params...], handler)` — sync handlers
- `ToolDefinition.fromAsync(name, description, [params...], asyncHandler)` — async handlers returning `CompletableFuture<R>`
- `ToolDefinition.fromWithToolInvocation(...)` — sync with `ToolInvocation` context injection
- `ToolDefinition.fromAsyncWithToolInvocation(...)` — async with `ToolInvocation` context injection

Parameter metadata is defined using `Param.of(type, name, description)` for required parameters and `Param.of(type, name, description, required, defaultValue)` for optional parameters with defaults.

Fluent option modifiers (`.skipPermission(boolean)`, `.defer(ToolDefer)`, `.overridesBuiltInTool(boolean)`) allow post-construction customization.

The annotation-driven API from [ADR-005](adr-005-tool-definition.md) remains the recommended path for larger tool surfaces where co-locating metadata with method implementations improves maintainability. For usage examples and complete API coverage, see the Java SDK README.

## Related work items

* #1682
* #1792
* #1810
