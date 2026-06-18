# Implementation plan: `@CopilotTool` ergonomics (issue #1682)

Human DRI: Ed Burns  
ADR: `java/docs/adr/adr-005-tool-definition.md`  
Issue: https://github.com/github/copilot-sdk/issues/1682

---

## Completed phases

### Phase 1 ✅ — Define the problem and architectural decision

- ADR-005 evaluates three options (status quo, record-as-schema, annotation-on-method).
- Decision: annotation-on-method with compile-time JSR 269 processor (langchain4j-style API, Micronaut-style implementation).

### Phase 2 ✅ — Verify the existing low-level path works in Java

- `test/snapshots/tools/low_level_tool_definition.yaml` created.
- `LowLevelToolDefinitionIT` passes with explicit `ToolDefinition.create()` / `createOverride()`.
- This proves the low-level API is correct and will serve as the foundation that the high-level API delegates to.

---

## Phase 3 — Ignorance reduction: questions to answer before writing code

This phase is about eliminating unknowns. Each item is a question or spike. Resolve these **before** writing production code.

### 3.1 — Package placement

**Question:** Where do `@CopilotTool` and `@Param` live?

Current SDK structure is a single module (`copilot-sdk-java`). Two options:

| Option | Location | Trade-off |
|--------|----------|-----------|
| A | `com.github.copilot.rpc` (alongside `ToolDefinition`) | Keeps everything together but the `rpc` package is already dense (40+ classes). |
| B | New package `com.github.copilot.tool` | Cleaner separation; the `tool` package holds annotations, processor, and `ToolDefinition.fromObject()`. But `ToolDefinition` itself stays in `rpc` (it's a JSON-RPC type). |

**Recommendation:** Option B — new `com.github.copilot.tool` package for annotations + processor + schema generation. `ToolDefinition` stays in `rpc` and gets a new static method `fromObject(Object)` that delegates to `tool` package internals.

**Action:** Decide; update `module-info.java` exports if new package is added.

**Resolution:** Select Option B.

### 3.2 — `@CopilotTool` annotation design

**Question:** What attributes does `@CopilotTool` need?

Based on ADR-005 and the C#/langchain4j comparisons:

```java
@Documented
@Retention(RetentionPolicy.SOURCE)   // only needed at compile time for processor
@Target(ElementType.METHOD)
@CopilotExperimental
public @interface CopilotTool {
    /** Tool description (sent to the model). */
    String value();

    /** Tool name. Defaults to method name converted to snake_case. */
    String name() default "";

    /** Whether this tool overrides a built-in tool. */
    boolean overridesBuiltInTool() default false;

    /** Whether to skip permission checks. */
    boolean skipPermission() default false;
}
```

**Open questions:**

1. Should `@CopilotTool` have `@Retention(SOURCE)` (processor-only, like Dagger) or `RUNTIME` (fallback reflection path, like langchain4j)? ADR-005 says "compile-time preferred, runtime fallback" — if we want a fallback path, we need `RUNTIME`. If we commit to processor-only, `SOURCE` is sufficient.

2. Is `ToolDefer` (the defer config from `ToolDefinition`) needed on the annotation, or is that too niche for v1?

**Recommendation:** Start with `RUNTIME` retention so the reflection fallback works. Defer `ToolDefer` support to a follow-up.

**Resolution:** Select `RUNTIME` and `ToolDefer` support.

### 3.3 — `@Param` annotation design

**Question:** What attributes does `@Param` need?

```java
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.PARAMETER)
public @interface Param {
    /** Parameter description (sent to the model). */
    String value() default "";

    /** Parameter name override. Defaults to the actual parameter name. */
    String name() default "";

    /** Whether this parameter is required. Default true. */
    boolean required() default true;

    /** Optional default value when the argument is omitted. */
    String defaultValue() default "";
}
```

**Resolution:** Support `defaultValue()` in v1 (langchain4j parity) and make it behaviorally effective, not docs-only.

Implementation rules:
- Emit JSON Schema defaults at `properties.<param>.default` for model guidance.
- Apply defaults at invocation time when an argument key is missing, then do normal coercion/casting and method invocation.
- Forbid `required=true` together with a non-empty `defaultValue()` (compile-time error in processor, matching runtime reflection fallback validation).
- Parse and validate `defaultValue()` against the Java parameter type (fail fast on mismatch).
- Ensure compile-time generated path and runtime reflection fallback use identical defaulting semantics.

### 3.4 — Type-to-JSON-Schema mapping

**Question:** What Java types do we need to map to JSON Schema, and how?

Minimum viable set (from langchain4j's `JsonSchemaElementUtils`):

| Java type | JSON Schema |
|-----------|-------------|
| `String` | `{"type": "string"}` |
| `int`, `Integer`, `long`, `Long` | `{"type": "integer"}` |
| `double`, `Double`, `float`, `Float` | `{"type": "number"}` |
| `boolean`, `Boolean` | `{"type": "boolean"}` |
| `enum` types | `{"type": "string", "enum": ["V1", "V2", ...]}` |
| `List<T>`, `Collection<T>` | `{"type": "array", "items": <schema-of-T>}` |
| `Map<String, T>` | `{"type": "object"}` (opaque — no inner schema) |
| Records / POJOs | `{"type": "object", "properties": {...}, "required": [...]}` |

**Key design decision:** The annotation processor must generate this schema at compile time from `javax.lang.model` types (`TypeMirror`, `DeclaredType`, etc.), NOT from `java.lang.reflect` types. This is different from langchain4j which does it at runtime.

**Spike needed:** Write a small proof-of-concept that maps `TypeMirror` → JSON Schema `Map` literal in generated source code. The tricky cases are:
- Enum constants (processor can see them via `ElementKind.ENUM_CONSTANT`)
- Generic type arguments on `List<Foo>` (processor sees them via `DeclaredType.getTypeArguments()`)
- Recursive/nested records (need `$defs` or just go one level deep for v1)

**Recommendation:** Start with the flat types (primitives, String, enums) and `List<primitive>`. Defer nested records and polymorphic types to a follow-up.

### 3.5 — Generated code shape

**Question:** What exactly does the processor generate?

ADR-005 proposes:

```java
// GENERATED — do not edit
final class MyTools$$CopilotToolMeta {
    static List<ToolDefinition> definitions(MyTools instance) {
        return List.of(
            new ToolDefinition("set_current_phase", "Sets the current phase",
                Map.of("type", "object",
                       "properties", Map.of("phase", Map.of("type", "string",
                               "description", "The phase to transition to")),
                       "required", List.of("phase")),
                invocation -> {
                    String phase = (String) invocation.getArguments().get("phase");
                    return CompletableFuture.completedFuture(
                        instance.setCurrentPhase(phase));
                }, null, null, null)
        );
    }
}
```

**Open questions:**

1. **Method invocation in generated code:** The generated lambda calls `instance.setCurrentPhase(phase)` directly — no reflection at runtime. But this requires the method to be accessible (not private). What access levels do we support? langchain4j uses `method.setAccessible(true)` at runtime. We'd need to either:
   - Require `public` or package-private methods, OR
   - Generate a helper that uses `MethodHandles.Lookup` to access private methods (complex), OR
   - Just require non-private.

   **Recommendation:** Require at least package-private. The generated `$$CopilotToolMeta` class is in the same package, so package-private and above work. Emit a compile error for `private` methods annotated with `@CopilotTool`.

2. **Return type handling:** What does the generated code do with the method's return value?

   | Return type | Generated behavior |
   |-------------|-------------------|
   | `String` | Wrap in `CompletableFuture.completedFuture(result)` |
   | `CompletableFuture<String>` | Use as-is (native async) |
   | `CompletableFuture<T>` | `.thenApply(objectMapper::writeValueAsString)` |
   | `void` | `CompletableFuture.completedFuture("Success")` |
   | Other `T` | JSON-serialize via Jackson `ObjectMapper` |

   **Recommendation:** Support `String`, `void`, `CompletableFuture<String>`, and `CompletableFuture<Object>` for v1. Other return types get JSON-serialized (since Jackson is already a dependency).

3. **Argument deserialization in generated code:** How does the generated lambda extract and coerce arguments?

   For simple types, the generated code can cast directly from the `Map<String, Object>` returned by `invocation.getArguments()`:
   ```java
   String city = (String) invocation.getArguments().get("city");
   int count = ((Number) invocation.getArguments().get("count")).intValue();
   ```

   For complex types (records, enums), use `invocation.getArgumentsAs()` or Jackson's `ObjectMapper.convertValue()`:
   ```java
   Phase phase = objectMapper.convertValue(invocation.getArguments().get("phase"), Phase.class);
   ```

   **Recommendation:** Generate direct casts for primitives/String, and `ObjectMapper.convertValue()` for enums, records, and complex types. The `ObjectMapper` instance can come from a static field in the generated class.

### 3.6 — `ToolDefinition.fromObject(Object)` registration API

**Question:** How does the user get from "an object with `@CopilotTool` methods" to a `List<ToolDefinition>`?

```java
// Primary API — loads generated $$CopilotToolMeta class
List<ToolDefinition> tools = ToolDefinition.fromObject(myToolsInstance);

// Variant: from class (for static tools)
List<ToolDefinition> tools = ToolDefinition.fromClass(MyTools.class);
```

**Implementation:**

```java
public static List<ToolDefinition> fromObject(Object instance) {
    Class<?> clazz = instance.getClass();
    String metaClassName = clazz.getName() + "$$CopilotToolMeta";
    try {
        Class<?> metaClass = Class.forName(metaClassName);
        Method defs = metaClass.getMethod("definitions", clazz);
        return (List<ToolDefinition>) defs.invoke(null, instance);
    } catch (ClassNotFoundException e) {
        // Fallback: runtime reflection (if we support it)
        return fromObjectReflective(instance);
    }
}
```

**Open question:** Do we want the reflection fallback? It's nice for users who don't run the processor (e.g., scripting, prototyping), but it adds code and the `-parameters` concern.

**Recommendation:** Implement the reflection fallback but mark it `@CopilotExperimental` separately. The primary path is the generated `$$CopilotToolMeta`.

### 3.7 — `module-info.java` impact

The SDK uses JPMS. The processor generates classes into the user's module, not the SDK's. But `fromObject()` uses `Class.forName()` which needs the generated class to be accessible.

**Question:** Does the generated `$$CopilotToolMeta` class in the user's module need to be exported for `fromObject()` to find it?

**Answer:** No. `Class.forName()` with the caller's classloader works within the same module. And in the typical unnamed-module (classpath) case, everything is accessible. If the user has a named module, the generated class is in the same package as their tools class, so it's accessible.

**Action:** Verify this works in a simple named-module test.

### 3.8 — Processor registration

**Question:** How is the new `@CopilotTool` processor registered alongside `CopilotExperimentalProcessor`?

The existing `META-INF/services/javax.annotation.processing.Processor` lists `CopilotExperimentalProcessor`. Add the new processor to the same file:

```
com.github.copilot.CopilotExperimentalProcessor
com.github.copilot.tool.CopilotToolProcessor
```

And in `module-info.java`:
```java
provides javax.annotation.processing.Processor
    with CopilotExperimentalProcessor, CopilotToolProcessor;
```

**No issues expected here** — this is standard JSR 269 multi-processor registration.

---

## Phase 4 — Implementation (the build order)

After Phase 3 questions are resolved, implement in this order. Each step should be a separately testable commit.

### 4.1 — Annotations (`@CopilotTool`, `@Param`)

**What:** Create the two annotation classes.

**Files to create:**
- `java/src/main/java/com/github/copilot/tool/CopilotTool.java`
- `java/src/main/java/com/github/copilot/tool/Param.java`

**Tests:**
- Compile-only: ensure they compile, can be applied to methods/parameters, and are annotated with `@CopilotExperimental`.
- No runtime behavior yet.

**Gating criteria:** `mvn clean compile` passes.

### 4.2 — Schema generation utility (compile-time)

**What:** A utility class that, given `javax.lang.model` types, produces the `Map<String, Object>` JSON Schema as a Java source code literal.

**Files to create:**
- `java/src/main/java/com/github/copilot/tool/SchemaGenerator.java` (compile-time, works with `TypeMirror`)

**Tests:**
- Unit tests that exercise the type-to-schema mapping with mock `TypeMirror` instances (or integration tests via the annotation processor in a test compilation).

**Gating criteria:** Can generate correct schema `Map` source code for: `String`, `int`, `boolean`, `double`, `enum`, `List<String>`, a simple record.

### 4.3 — Annotation processor (`CopilotToolProcessor`)

**What:** JSR 269 processor that finds `@CopilotTool` methods and generates `$$CopilotToolMeta` classes.

**Files to create:**
- `java/src/main/java/com/github/copilot/tool/CopilotToolProcessor.java`

**Files to modify:**
- `java/src/main/resources/META-INF/services/javax.annotation.processing.Processor` — add the new processor
- `java/src/main/java/module-info.java` — add `provides` clause and `exports com.github.copilot.tool`

**Tests:**
- **Compilation tests:** Compile test source files with `@CopilotTool` methods and verify:
  - `$$CopilotToolMeta` class is generated
  - Generated schema matches expected JSON Schema
  - Compile errors emitted for: private methods, unsupported parameter types, duplicate tool names
- Use `javax.tools.JavaCompiler` programmatically (same pattern langchain4j uses for testing annotation processors).

**Gating criteria:** Processor generates correct `$$CopilotToolMeta` for a class with 2-3 `@CopilotTool` methods.

### 4.4 — `ToolDefinition.fromObject(Object)`

**What:** The runtime bridge that loads generated metadata and returns `List<ToolDefinition>`.

**Files to modify:**
- `java/src/main/java/com/github/copilot/rpc/ToolDefinition.java` — add `fromObject(Object)` and `fromClass(Class<?>)` static methods

**Tests:**
- Unit test: create a test class with `@CopilotTool` methods, compile it (processor generates metadata), call `fromObject()`, verify the returned `List<ToolDefinition>` has correct names, descriptions, schemas, and working handlers.

**Gating criteria:** `ToolDefinition.fromObject(new MyTestTools())` returns a list with working tool definitions.

### 4.5 — E2E integration test

**What:** An E2E failsafe IT that uses `@CopilotTool` + `ToolDefinition.fromObject()` against the replay proxy.

**Files to create:**
- `test/snapshots/tools/ergonomic_tool_definition.yaml` — new snapshot (may be identical to `low_level_tool_definition.yaml` since the wire format is the same)
- `java/src/test/java/com/github/copilot/ErgonomicToolDefinitionIT.java`

**The test will look like:**

```java
class MyTestTools {
    String currentPhase;

    @CopilotTool("Sets the current phase of the agent")
    String setCurrentPhase(@Param("The phase to transition to") String phase) {
        currentPhase = phase;
        return "Phase set to " + phase;
    }

    @CopilotTool("Search for items by keyword")
    String searchItems(@Param("Search keyword") String keyword) {
        return "Found: item_alpha, item_beta";
    }

    @CopilotTool(value = "Custom grep override", name = "grep", overridesBuiltInTool = true)
    String grepOverride(@Param("Search query") String query) {
        return "CUSTOM_GREP: " + query;
    }
}

@Test
void ergonomicToolDefinition() throws Exception {
    MyTestTools tools = new MyTestTools();
    List<ToolDefinition> toolDefs = ToolDefinition.fromObject(tools);

    // ... create session with toolDefs, send prompt, assert same behavior
    // as LowLevelToolDefinitionIT
}
```

**Gating criteria:** Test passes with the same assertions as `LowLevelToolDefinitionIT` — proving the ergonomic API produces identical behavior to the explicit API.

### 4.6 — Reflection fallback (optional, can defer)

**What:** `fromObject()` falls back to runtime reflection when `$$CopilotToolMeta` is not found.

**Files to create/modify:**
- `java/src/main/java/com/github/copilot/tool/ReflectiveToolScanner.java`
- Modify `ToolDefinition.fromObject()` to call this on `ClassNotFoundException`

**Tests:**
- Compile a test class WITHOUT the annotation processor, call `fromObject()`, verify it still works (with `-parameters` flag).

**Gating criteria:** Fallback path produces the same `List<ToolDefinition>` as the processor-generated path.

---

## Phase 5 — Documentation and examples

- Update `java/README.md` with the ergonomic tool definition example.
- Add a "Tools" section showing both the low-level and high-level APIs.
- Reference ADR-005 for design rationale.

---

## Phase 6 — Port to `add-tests-that-use-ergonomic_tool_definition.yaml.md`

Same cycle as Phase 2 → `add-tests-that-use-low_level_tool_definition.yaml.md`: once the Java E2E test passes with the ergonomic API, create a prompt to port the test to dotnet/go/nodejs/python/rust.

**Note:** This may not be applicable — the ergonomic API (`@CopilotTool`) is Java-specific. The other SDKs already have their own ergonomic paths. The snapshot can be shared, but the test code is language-specific by nature. Evaluate whether this phase is needed after Phase 5.

---

## Reference: how langchain4j and Micronaut do it

### langchain4j (runtime reflection)

- `@Tool` on methods, `@P` on parameters.
- `ToolSpecifications.toolSpecificationsFrom(Object)` scans methods at runtime.
- `JsonSchemaElementUtils` maps `java.lang.reflect.Type` → JSON Schema.
- `DefaultToolExecutor.executeWithContext()` invokes via `Method.invoke()` with argument coercion.
- Requires `-parameters` javac flag or explicit `@P(name="...")`.
- Source: `langchain4j-core/src/main/java/dev/langchain4j/agent/tool/`

### Micronaut (compile-time annotation processor)

- `AbstractInjectAnnotationProcessor` (extends `AbstractProcessor`) is the JSR 269 entry point.
- `TypeElementVisitor<C, E>` SPI pattern: visitors registered via SPI walk the AST.
- `BeanDefinitionWriter` generates bytecode companion classes (`$Definition`, `$Definition$Exec`).
- `ParameterElement.getName()` at compile time — no `-parameters` flag needed.
- Source: `inject-java/src/main/java/io/micronaut/annotation/processing/`

### Our approach: langchain4j's API + Micronaut's implementation strategy

- **User-facing API** matches langchain4j: `@CopilotTool` on methods, `@Param` on parameters, `fromObject()` to discover.
- **Implementation** matches Micronaut: JSR 269 processor generates companion classes at compile time, no runtime reflection in the happy path, no `-parameters` requirement.
- **Fallback** path uses langchain4j-style runtime reflection for users who don't run the processor (prototyping, scripting).
