# ADR-004: @CopilotExperimental annotation processor — pure JSR 269 approach

## Context and Problem Statement

The Java SDK needs a compile-time gate that prevents accidental use of experimental APIs (types and methods marked with `@CopilotExperimental`). The annotation processor must detect consumer-side references to experimental elements and emit a compilation error unless the consumer explicitly opts in with `-Acopilot.experimental.allowed=true`.

The fundamental question is: should the processor use the Compiler Tree API (`com.sun.source.util.Trees`, `TreePathScanner`) for full expression-level coverage, or restrict itself to standard JSR 269 (`javax.lang.model.*`) for portability at the cost of reduced detection scope?

## Considered Options

### Option 1: Compiler Tree API (`com.sun.source.*`)

Uses `Trees.instance(processingEnv)` and `TreePathScanner` to walk the full AST of every compilation unit, resolving symbols at expression level.

**What it catches additionally:**
- `new ExperimentalType()` inside method bodies
- `ExperimentalType.staticMethod()` inline calls
- Method references (`ExperimentalType::method`)
- Local variable types
- Casts to experimental types

**Drawbacks:**
- Depends on `jdk.compiler` module — ties the processor to javac specifically.
- Does not work with ECJ (Eclipse Compiler for Java), which has its own AST.
- Requires `requires static jdk.compiler` in module-info.java.
- Requires `--add-modules jdk.compiler --add-exports jdk.compiler/com.sun.source.util=ALL-UNNAMED --add-exports jdk.compiler/com.sun.source.tree=ALL-UNNAMED` in surefire test configuration.
- The `com.sun.source.*` package, while more stable than `com.sun.tools.javac.*`, is still not part of the Java SE specification. It is a JDK-specific API.

### Option 2: Pure JSR 269 (`javax.lang.model.*`) — declaration-level only

Uses only standard annotation processing APIs to walk declared elements (types, methods, fields) and inspect their type mirrors.

**What it catches:**
- Field types referencing experimental classes
- Method parameter types
- Method return types
- Superclass / implemented interfaces
- Thrown exception types
- Generic type arguments and bounds

**What it cannot catch:**
- `new ExperimentalType()` purely inside a method body with no declaration footprint
- Inline static method calls with no stored result
- Method references to experimental methods
- Local variable types (not visible to processors)

**Advantages:**
- Works with any compliant Java compiler (javac, ECJ, IntelliJ's compiler, etc.)
- No dependency on JDK-internal modules
- No `--add-exports` hacks in build configuration
- Simpler module-info (no `requires static jdk.compiler`)
- Easier to maintain and less fragile across JDK versions

## Decision Outcome

**Chosen: Option 2 — Pure JSR 269.**

### Rationale

1. **The SDK's experimental APIs are predominantly types (records, classes).** Table `apiNote` from the codegen analysis shows 316 experimental types vs. 159 experimental methods. Any meaningful use of an experimental record (params, results, events) requires declaring it somewhere — a field, a method parameter, a return type, or a superclass. Pure body-level usage with zero declaration footprint is a degenerate edge case for this SDK.

2. **Portability matters for a published library.** The SDK is distributed on Maven Central. Consumers may use Eclipse (ECJ), IntelliJ's compiler, or other toolchains where `com.sun.source.*` is unavailable. A processor that silently does nothing on non-javac compilers provides false confidence.

3. **Build simplicity.** Avoiding `jdk.compiler` eliminates module-system friction: no `requires static jdk.compiler`, no `--add-exports` in surefire, no risk of `IllegalAccessError` on future JDK versions that further restrict internal APIs.

4. **The gap is well-documented and acceptable.** The README explicitly lists what the processor does and does not catch, with suggested workarounds. This transparency is preferable to a fragile implementation with full coverage.

5. **Error Prone or similar tools can fill the gap later.** If full expression-level enforcement becomes necessary in the future, it can be implemented as a separate Error Prone check (which is already designed for AST-level analysis) without changing the annotation or the processor's declaration-level behavior.

## Consequences

- Consumers who use experimental APIs only in fully-inline expressions (no field, no parameter, no return type) will not receive a compile error. This is expected and documented.
- The processor works identically across javac, ECJ, and any JSR 269-compliant compiler.
- No JDK-internal API dependency in the module descriptor or test infrastructure.
- Future enhancement path is clear: add an optional Error Prone check for body-level coverage without changing the existing processor.

## Related work items

- https://github.com/github/copilot-sdk/pull/1601
- https://devdiv.visualstudio.com/DevDiv/_workitems/edit/3012835
