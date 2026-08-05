## Overview

Convert the single-module `java/pom.xml` into a multi-module Maven reactor. Move the existing SDK code into a `sdk/` subdirectory while preserving its GAV (`com.github:copilot-sdk-java`).

**This is task 4.1 of 9 in the implementation plan.** Tasks are assigned, completed, and merged serially in this listed order.

**Branch:** `edburns/1917-java-embed-rust-cli-runtime-dd-3039145` on `upstream`

## Plan and supporting resources

On the `edburns/1917-java-embed-rust-cli-runtime-dd-3039145` branch, the directory `1917-java-embed-rust-cli-runtime-remove-before-merge` contains the plan (`1917-embed-cli-runtime-ignorance-reduction-plan.md`) and supporting resources (spikes, prototypes, diagrams).

**Read the entire plan before working.**

## Relevant plan sections to carefully re-read

- **Section 3.1 — Maven module structure for per-platform classifier JARs** — Resolution: Option B — hybrid multi-module reactor. The reactor structure is:
  ```
  java/
  ├── pom.xml                          (parent, packaging=pom, new GAV: com.github:copilot-sdk-java-parent)
  ├── sdk/
  │   └── pom.xml                      (existing SDK, KEEPS GAV: com.github:copilot-sdk-java)
  ├── copilot-native/
  │   └── pom.xml                      (new GAV: com.github:copilot-sdk-java-runtime)
  ├── copilot-native-all/
  │   └── pom.xml                      (optional monolithic: com.github:copilot-sdk-java-runtime-all)
  ```
  Key decisions: the existing `copilot-sdk-java` GAV is preserved (no breaking change); the parent POM is `packaging=pom` and internal-only; no dependency from `copilot-sdk-java` to `copilot-sdk-java-runtime`.
- **Section 4.1 — Parent POM restructure** (the primary task description)
- **TDD discipline for all implementation steps** — every step must follow test-driven workflow: write tests first, implement until green, refactor, gate before proceeding.

## Deliverables

### Files to create

1. **`java/pom.xml`** — New parent POM (`com.github:copilot-sdk-java-parent`, `packaging=pom`). Declares `<modules>` for `sdk`, `copilot-native`, and `copilot-native-all`. Centralizes shared properties, plugin versions, and `copilot.sdk.root` path. The `copilot-native` and `copilot-native-all` modules do NOT need to exist yet — they are created in later tasks. Include them in `<modules>` commented out or in a profile, so the reactor builds with just `sdk` for now.

### Files to move

2. **Existing `java/pom.xml` → `java/sdk/pom.xml`** — Add `<parent>` pointing to `copilot-sdk-java-parent`. Preserve existing GAV `com.github:copilot-sdk-java`. All existing source, test, and resource paths must resolve correctly from the new `java/sdk/` location.
3. **Existing `java/src/` → `java/sdk/src/`**
4. **Existing `java/config/` → `java/sdk/config/`** (or keep at `java/config/` and reference via `${project.parent.basedir}/config/` — choose whichever keeps paths simpler)

### Files to update

5. **`justfile`** — Update `java/` paths to `java/sdk/` where needed.
6. **`.github/workflows/java-sdk-tests.yml`** — Update working directory references from `java/` to `java/sdk/` or `java/` as appropriate for the reactor.
7. **Any other workflows referencing `java/pom.xml`** — search `.github/workflows/` for references to `java/pom.xml` or `java/` build commands and update them.

## Gating tests and criteria

All of the following must pass before this task is considered complete:

1. **Reactor build:** `mvn clean verify` from `java/` runs the full reactor successfully.
2. **SDK-only build:** `mvn -pl sdk clean verify` from `java/` builds and tests the SDK exactly as before the restructure.
3. **All existing tests pass:** Every existing unit test and integration test passes without modification (unless path changes require adjustment).
4. **CI workflows work:** The updated workflow YAML files reference the correct directories and would run correctly.
5. **GAV preservation:** The SDK artifact's GAV remains `com.github:copilot-sdk-java` — no consumer-visible change.
6. **Spotless compliance:** `mvn spotless:check` passes.

## Out of scope

- Creating the `copilot-native` or `copilot-native-all` module directories or POMs (tasks 4.7 and later).
- Any native binary handling, JNA dependencies, or FFI code.
- Changes to Java source code (only build/project structure changes).
