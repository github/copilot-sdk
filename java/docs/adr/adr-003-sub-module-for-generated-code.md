# Sub-module for generated code

## Context and Problem Statement

Regarding the goal of more effectively passing on the stability and deprecation metadata from the `@github/copilot` Zod schema to end consumers of `copilot-sdk-java`, Partner Software Engineer Stephen Toub stated, "The ideal is to do the best each language has to offer."

## Considered Options

* Status quo: keep generated code in the same `copilot-sdk-java` module.

* Option 1: Move all generated code (both `com.github.copilot.generated` and `com.github.copilot.generated.rpc`) to a single internal Maven module (`copilot-sdk-generated`), bundled back into the published `copilot-sdk-java` artifact via `maven-dependency-plugin`.

* Option 2: Move generated code into two internal Maven modules (`copilot-sdk-events` for session-event types, `copilot-sdk-rpc-generated` for RPC types), bundled back into the published artifact.

### Analysis

The generated code is deeply embedded in the public API surface of `copilot-sdk-java`: `CopilotSession.getRpc()` returns `SessionRpc`, `CopilotClient.getRpc()` returns `ServerRpc`, `sendAndWait()` returns `AssistantMessageEvent`, and the event handler API accepts all generated event subclasses. Approximately 730 of 914 generated classes are part of the externally-visible API. Any module split is therefore a build-time concern only — it cannot reduce the consumer-facing footprint.

The dependency direction is clean (hand-written → generated, never reverse), making a split technically feasible without circular dependencies.

However, the specific goal of conveying stability/deprecation metadata requires a `@CopilotExperimental` annotation visible at compile time to both the generated and hand-written code. In the status quo, this annotation lives in `src/main/java/` and is freely importable by `src/generated/java/` since they compile together. In a split-module reactor, the generated module compiles *before* the hand-written module, so the annotation must either be emitted by the codegen script as another generated file, or extracted into a third annotations-only module. Both add complexity without advancing the stability-metadata goal.

Module separation is orthogonal to — and slightly complicates — the stability/deprecation work. The codegen script changes to read and propagate `stability`/`deprecated` from schema nodes are identical regardless of module structure.

## Decision Outcome

Keep the status quo: keep the generated code in the same `copilot-sdk-java` module.

The primary benefit of module separation (compile-time isolation, cleaner PR diffs) does not justify the added reactor complexity, `maven-dependency-plugin` configuration, and annotation-placement constraints — particularly given that the immediate priority is implementing stability/deprecation metadata propagation, which is simpler in a single-module build.

## Related work items

- https://devdiv.visualstudio.com/DevDiv/_workitems/edit/3013416

- https://github.com/github/copilot-sdk/issues/1573
