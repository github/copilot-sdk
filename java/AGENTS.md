<!-- Copyright (c) Microsoft Corporation. All rights reserved. -->

# Java SDK

This directory is the Java SDK's Maven reactor. Paths and commands below are
relative to this directory, whether it is nested at `src/sdk/java` or exported
as `java`. Keep API usage examples in [README.md](README.md), the
[SDK documentation](../docs/getting-started.md), and public Javadoc rather than
duplicating them in these contributor instructions.

## Layout and compatibility

- `sdk/src/main/java/` contains the handwritten client, session, and RPC APIs.
  `sdk/src/test/java/` contains unit and integration tests.
- `sdk/src/generated/java/` contains generated protocol and event types.
- `copilot-native/` packages platform-specific runtime artifacts separately
  from the pure-Java SDK.
- Build with **JDK 25 or later**, as required by [sdk/pom.xml](sdk/pom.xml).
  The base sources target Java 17; Java 25-specific implementations belong in
  the existing `sdk/src/main/java25/` multi-release overlay. Do not raise the
  base target or introduce newer APIs into the Java 17 sources.

## API conventions

- Preserve the `CompletableFuture`-based asynchronous API and fluent setters
  on configuration classes. Preserve wire names and serialization behavior
  when changing RPC types.
- Use try-with-resources for `CopilotClient` and `CopilotSession`. Close event
  and lifecycle subscriptions when no longer needed.
- Use typed event handlers and the existing `ToolDefinition` and
  `ToolInvocation` helpers rather than ad hoc wire payloads. Prefer
  `getArgumentsAs()` for typed tool arguments.
- Creating and resuming sessions requires a permission handler.
  `PermissionHandler.APPROVE_ALL` is for scenarios that do not exercise
  permission decisions; it cannot bypass managed-settings approval.
- Handle exceptional future completion and `SessionErrorEvent`. Streaming
  consumers must handle both incremental delta events and final messages.
- Add Javadoc for public APIs and follow the existing four-space,
  Spotless/Eclipse formatting. Checkstyle defines the Javadoc checks and
  exclusions in `sdk/config/checkstyle/checkstyle.xml`.

## Build, format, and test

Prefer the checked-in Maven wrapper (`./mvnw`, or `.\mvnw.cmd` on Windows).
Follow [SDK setup](../CONTRIBUTING.md#developing-an-sdk) for the JDK and
layout-appropriate Node.js version. Tests use Node.js for the replay harness and
runtime preparation tooling. Maven's `generate-test-resources` phase installs
their dependencies from the SDK tree. In the runtime repository, prefer
`npm --prefix <SDK_ROOT> run test:java`; it refreshes the selected schemas and
Java projection and requests a current host CLI before running the tests.
Direct Maven commands bypass those prerequisites and are appropriate after
preparing the checkout, or in the standalone SDK repository.

```bash
./mvnw clean verify
./mvnw -pl sdk spotless:apply
./mvnw -pl sdk spotless:check checkstyle:check
```

Run `verify` without `-q` or piping through `grep` so failures remain visible.
`verify` does not run Spotless: apply and check formatting separately for Java
source changes. Tests must exercise public APIs, not expose internals solely
for tests.

For replay-backed Java integration tests, use the on-demand
[`sdk-java-e2e-test` skill](../.github/skills/sdk-java-e2e-test/SKILL.md).
Its snapshot workflow and companion examples are not required for unrelated
Java edits. For JDK 17 compatibility testing, run the JDK 25-built artifact on
JDK 17 without recompiling it, following
[Development Setup](README.md#development-setup).

## Generated sources

Do not edit `sdk/src/generated/java/` by hand. Update the generator in
`scripts/codegen/java.ts` when needed. From `<SDK_ROOT>`, regenerate with the
SDK facade so the runtime layout selects checked-out schemas and the standalone
layout selects its pinned release schemas:

```bash
npm run generate:java
```

The protocol constant `sdk/src/main/java/com/github/copilot/SdkProtocolVersion.java`
is also generated, despite living outside `src/generated/`. Change the shared
`<SDK_ROOT>/sdk-protocol-version.json` and run
`npm --prefix <SDK_ROOT>/nodejs run update:protocol-version` to update all six SDKs.

Use the SDK's selected schema inputs; do not update release-derived CLI pins
as an incidental part of implementation work.
