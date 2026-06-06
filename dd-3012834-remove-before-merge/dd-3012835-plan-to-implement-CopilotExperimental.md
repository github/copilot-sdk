# Implement CopilotExperimental compile-time opt-in for copilot-sdk-java

You are working in the `copilot-sdk` monorepo, Java module only.

## Goal

Implement a Java experimental-API gate with these properties:

1. Generated experimental APIs are annotated with `@CopilotExperimental`.
2. Consumer code that uses those APIs fails compilation by default.
3. Compilation succeeds only when an explicit compiler option is provided.
4. Add tests proving both fail-by-default and allow-when-opted-in behavior.
5. Regenerate generated Java code and run tests.

This work is intentionally scoped to only the program elements already represented by existing `@apiNote` placement in generated code:

- Types in `src/generated/java/**`
- Methods in generated `*Api.java` wrappers
- No constructor/field handling required beyond what type/method coverage already implies

## Required Context

Read and follow:

- `~/.copilot/instructions/java.instructions.md`
- `java/README.md`
- `java/scripts/codegen/java.ts`
- `nodejs/node_modules/@github/copilot/schemas/api.schema.json`
- `nodejs/node_modules/@github/copilot/schemas/session-events.schema.json`

Generated metadata source of truth:

- Experimental signal is schema `stability: "experimental"`

## Enforcement spec to implement

Treat use of experimental API as compile error unless allow flag is present.

### What is considered a forbidden use

Forbidden without allow flag:

1. Any reference to a type annotated with `@CopilotExperimental` in source declarations:
   - field types
   - method parameter/return types
   - throws types
   - extends/implements
   - generic arguments/bounds
2. Instantiating annotated types (`new`)
3. Invoking methods annotated with `@CopilotExperimental`
4. Method references to annotated methods
5. Access through member-select/identifier where resolved symbol or enclosing type is annotated

### Opt-in switch

Use one compiler option key:

- `-Acopilot.experimental.allowed=true`

Behavior:

- If option is exactly `true`, checker is disabled (allow experimental use)
- Otherwise checker is enabled and emits errors for forbidden use

### Annotation semantics

Create annotation with at least:

- Retention: `CLASS`
- Targets: `TYPE`, `METHOD`
- Documented and public

When an experimental type is annotated, all member uses should be treated as experimental by checker logic even if method itself is not annotated.

## Implementation tasks

### 1) Add the annotation type

Add a public annotation in main Java sources, package under `com.github.copilot` (or a clearly named subpackage under it).

Suggested file:

- `java/src/main/java/com/github/copilot/CopilotExperimental.java`

### 2) Add the checker as an annotation processor

Add a processor that enforces the above spec.

Requirements:

- Processor supports option `copilot.experimental.allowed`
- Processor runs with `@SupportedAnnotationTypes("*")`
- Uses javac Trees API (`com.sun.source.util.Trees`, `TreePathScanner`) so expression-level usage is caught
- Emits `Diagnostic.Kind.ERROR` with clear message mentioning `-Acopilot.experimental.allowed=true`

Suggested file:

- `java/src/main/java/com/github/copilot/CopilotExperimentalProcessor.java`

Also add service registration:

- `java/src/main/resources/META-INF/services/javax.annotation.processing.Processor`
  - contains FQCN of processor

### 3) Wire build so internal module compiles cleanly

This module has tests and sources that intentionally use generated APIs. Since generated APIs will become annotated, the Java module itself should compile with explicit opt-in.

Update `java/pom.xml` compiler plugin configuration to pass:

- `-Acopilot.experimental.allowed=true`

Do this for main/test compile executions that need it, while preserving existing release/profile behavior.

### 4) Update generator to emit annotation where current apiNote is emitted

Modify `java/scripts/codegen/java.ts` so any place that currently emits experimental `@apiNote` also emits `@CopilotExperimental`.

Specifically for:

- generated event/type classes in `com.github.copilot.generated`
- generated RPC params/results/etc in `com.github.copilot.generated.rpc`
- generated experimental methods in API wrapper classes (`*Api.java`)

Ensure generated files include required import for annotation where needed.

Keep existing `@apiNote` behavior intact.

### 5) Add tests proving fail/allow behavior

Add a focused test class that compiles in-memory snippets via `javax.tools.JavaCompiler` and explicitly runs `CopilotExperimentalProcessor`.

Suggested test file:

- `java/src/test/java/com/github/copilot/CopilotExperimentalProcessorTest.java`

Required test cases:

1. Fails by default when code references an annotated experimental generated type
2. Fails by default when code invokes annotated experimental generated method
3. Passes when same code is compiled with `-Acopilot.experimental.allowed=true`

Test implementation notes:

- Use small source strings and JavaFileObject wrappers
- Add current test/runtime classpath into compilation task options
- Set processors explicitly (`setProcessors`) to avoid environment sensitivity
- Assert diagnostics include actionable guidance text

### 6) Regenerate and verify

Run codegen and tests from `java/` directory.

Use background + log pattern required by java instructions:

- `LOG="$(date +%Y%m%d-%H%M)-job-logs.txt" && mvn generate-sources -Pcodegen > "$LOG" 2>&1 & tail -f "$LOG"`
- `LOG="$(date +%Y%m%d-%H%M)-job-logs.txt" && mvn verify > "$LOG" 2>&1 & tail -f "$LOG"`

After each command, read the same `LOG` filename you created and summarize success/failure.

## Constraints

1. Do not edit files outside `java/**` except this prompt file is already provided.
2. Do not hand-edit `java/src/generated/java/**`; regenerate via codegen.
3. Preserve existing public API except for addition of annotation/processor behavior and generated annotations.
4. Keep changes minimal and focused.

## Acceptance criteria

1. `@CopilotExperimental` exists and is public.
2. Generated experimental types/methods are annotated after regeneration.
3. Processor exists, is service-registered, and enforces fail-by-default behavior.
4. Java module build uses explicit allow option for internal compile.
5. New tests demonstrate:
   - compile failure without allow option
   - compile success with allow option
6. `mvn verify` passes.

## Deliverables in final summary

Report:

1. Files changed
2. Exact enforcement behavior implemented
3. Sample diagnostic text
4. Confirmation codegen ran and what regenerated
5. Test results from verify
