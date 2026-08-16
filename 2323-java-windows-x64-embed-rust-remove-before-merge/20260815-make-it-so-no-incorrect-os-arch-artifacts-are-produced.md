# Prevent incorrect native classifier artifacts on unsupported build hosts

## Goal

Change the Java Maven build so a normal reactor build produces a platform-classified native JAR only when the build host matches a currently implemented native platform. For the current implementation:

- Linux x64 builds produce `copilot-sdk-java-runtime-<version>-linux-x64.jar`.
- macOS, Windows, Linux ARM64, and other unsupported hosts do not fetch Linux native files, stage `native/linux-x64`, run Linux-native structural checks, or produce a `*-linux-x64.jar`.
- The OS-neutral `copilot-sdk-java-runtime-<version>.jar`, sources JAR, and Javadoc JAR remain. ADR-007 requires the primary placeholder artifact for Maven Central; these artifacts make no OS/architecture claim.
- Adding a supported platform later requires one small host profile with platform properties, while the common fetch, package, and verification logic remains shared.

Do not add macOS or Windows native support in this change.

## Execution boundary and serial handoff

This plan has two serial phases:

1. The current agent performs the implementation and macOS-only validation described below. It is prohibited from exercising any Linux x64 code. It must not use Docker, a VM, emulation, forced Maven profile activation, cross-compilation, or any other mechanism to run or validate the Linux x64 native path.
2. Only after the macOS work is complete and merged to the topic branch, a separate Linux x64 agent checks out the resulting topic-branch `HEAD` and performs the Linux x64 validation and any Linux-only follow-up fixes.

The current agent must not perform phase 2. Instead, as its final implementation step, it must write a self-contained, `copilot --yolo`-ready prompt for the Linux x64 agent to:

```text
2323-java-windows-x64-embed-rust-remove-before-merge/20260816-notes-to-linux-x64-agent.md
```

Do not create that handoff prompt before the macOS implementation and validation are complete. The prompt must describe the repository state as already containing the merged macOS work and must instruct the Linux agent to work from the checked-out `HEAD`, not to repeat or revert the macOS phase.

## Existing problem

PR #2301 introduced `java/copilot-native` as an unconditional reactor module. Its POM currently:

1. Defines `copilot.native.classifier` globally as `linux-x64`.
2. Binds `fetch-native-linux-x64` to `generate-resources` on every host.
3. Binds `jar-linux-x64` and `verify-native-jars` to `package` on every host.

Consequently, a macOS build downloads and packages Linux files and leaves artifacts such as:

```text
java/copilot-native/target/copilot-sdk-java-runtime-<version>-linux-x64.jar
java/copilot-native/target/native-staging/linux-x64/...
```

This contradicts ADR-007's per-platform classifier model: classifiers identify the runtime platform and must not merely reflect a hard-coded build default.

## Design

Keep `copilot-native` in the reactor on every host so Maven still:

- cleans its `target` directory;
- builds the required OS-neutral Maven Central placeholder artifacts;
- runs the platform-independent `fetch-native.test.mjs` tests.

Make the native-specific executions dormant in the base build by assigning them the non-lifecycle phase `none`. Add a host-activated `native-linux-x64` profile that:

- activates only when Maven reports operating system `Linux` and architecture `amd64`;
- does not activate when `copilot.native.skip.download` is set;
- defines `copilot.native.classifier=linux-x64`;
- defines `copilot.native.cli.filename=copilot`;
- rebinds the shared native fetch, fetch-script test, classifier-JAR, and structural-verification executions to their real lifecycle phases.

Use generic execution IDs so future profiles can rebind the same implementation:

| Execution | Dormant phase | Linux x64 phase |
| --- | --- | --- |
| `fetch-native` | `none` | `generate-resources` |
| `test-fetch-native` | `none` | `test` |
| `jar-native` | `none` | `package` |
| `verify-native-jars` | `none` | `package` |

Maven merges profile executions with base executions by plugin coordinates and execution ID. The profile therefore needs to repeat only the execution IDs and phases, not the fetch/package/verification configuration.

Do not move `copilot-native` into a host-specific reactor profile. Excluding the module would prevent `mvn clean` on an unsupported host from removing native artifacts left by an earlier supported-host build, and it would unnecessarily remove the OS-neutral placeholder artifact from the reactor.

## Implementation

### 1. Refactor `java/copilot-native/pom.xml`

1. Remove the global hard-coded `copilot.native.classifier` property and its Linux-only comment.
2. Rename native-specific execution IDs:
   - `fetch-native-linux-x64` to `fetch-native`;
   - `jar-linux-x64` to `jar-native`.
3. Set the base phase of `fetch-native`, `test-fetch-native`, `jar-native`, and `verify-native-jars` to `none`.
4. Keep all existing goals and configuration on those base executions. Continue passing `${copilot.native.classifier}` to `fetch-native.mjs` and using it for staging paths, classifier names, and resource paths.
5. Configure `test-fetch-native` with `<skip>${skipTests}</skip>` so `-DskipTests` consistently skips the exec-based script tests.
6. Replace the structural check's hard-coded `copilot` resource name with `${copilot.native.cli.filename}`. This preserves Linux behavior and lets a future Windows profile select `copilot.exe` without copying the verification block.
7. Keep the placeholder resource and the OS-neutral sources/Javadoc JAR executions bound exactly as they are.
8. Add a `native-linux-x64` profile:
   - activate with `<os><name>Linux</name><arch>amd64</arch></os>`;
   - add a negated property activation for `copilot.native.skip.download`, so `-Dcopilot.native.skip.download=true` prevents the profile from activating;
   - set `copilot.native.classifier` to `linux-x64`;
   - set `copilot.native.cli.filename` to `copilot`;
   - re-declare the four generic execution IDs with phases `generate-resources`, `test`, `package`, and `package`, respectively.
9. Simplify `skip-native-download`:
   - preserve its existing property activation and its skip of `exec-maven-plugin`, including the script test;
   - remove overrides for the old `jar-linux-x64` and `verify-native-jars` execution IDs because the native host profile will not activate when the skip property is present.
10. Update comments and the module description only where needed to distinguish the OS-neutral placeholder build from host-matched classifier packaging.

Do not add `os-maven-plugin`. Maven's built-in OS activation is sufficient for the one currently implemented build platform, avoids a new build extension, and keeps later support explicit per classifier.

### 2. Update ADR-007

Edit `java/docs/adr/adr-007-native-bundling-strategy.md` without changing the chosen per-platform-classifier decision:

1. In **Current platform scope**, state that the Maven build binds native packaging only on a host matching an implemented classifier. Currently that is Linux x64; unsupported hosts build only the OS-neutral placeholder artifacts.
2. In **Consequences**, qualify the fetch/package statement so it says a supported host profile fetches and packages its matching `@github/copilot-<classifier>` package.
3. State that additional platform implementation consists of adding a host profile that supplies the classifier and platform CLI filename and binds the shared executions.

The existing Java README remains accurate: in-process support is still experimental and Linux x64 only. Do not change runtime platform detection or consumer dependency examples.

## Future platform extension pattern

When a new classifier is implemented, add a sibling profile in `java/copilot-native/pom.xml` with:

1. Exact Maven host OS/architecture activation.
2. `copilot.native.classifier` set to the ADR classifier, for example `darwin-arm64` or `win32-x64`.
3. `copilot.native.cli.filename` set to `copilot` or `copilot.exe`.
4. The same four generic execution IDs rebound to their lifecycle phases.

Before enabling a new profile, ensure `fetch-native.mjs`, its tests, the lockfile package, structural assertions, and CI all support that classifier. Do not activate a profile merely because `PlatformDetector` recognizes the platform at runtime.

## Validation and handoff

Run all Maven commands from `java`, with the required Java environment, `pipefail`, and tee-to-log convention.

### Phase 1: macOS regression check

Start from the current macOS host and run:

```sh
cd java
export IDEA_HOME="/Applications/IntelliJ IDEA CE.app/Contents/MacOS"
export APPCAT_HOME=/Users/edburns/.appcat
export JAVA_HOME="/Library/Java/JavaVirtualMachines/microsoft-25.jdk/Contents/Home"
export ANT_HOME="${HOME}/Downloads/apache-ant-1.10.13"
export M2_HOME="${HOME}/Downloads/apache-maven-3.9.8"
export PATH="${APPCAT_HOME}:${M2_HOME}/bin:${ANT_HOME}/bin:${JAVA_HOME}/bin:${IDEA_HOME}:${PATH}"
set -o pipefail
LOG="$(date +%Y%m%d-%H%M)-job-logs.txt"
mvn clean package -DskipTests 2>&1 | tee "$LOG"
```

Inspect that exact log and output tree. Acceptance criteria:

- Maven succeeds.
- The reactor still includes `copilot-sdk-java-runtime`.
- The log does not run `fetch-native` or `verify-native-jars`.
- `java/copilot-native/target` contains the OS-neutral primary, sources, and Javadoc JARs.
- No filename under `java/copilot-native/target` contains `linux-x64`.
- `java/copilot-native/target/native-staging` does not exist.

### Phase 1: macOS effective-profile check

On macOS, run `mvn -pl copilot-native help:active-profiles` and confirm `native-linux-x64` is absent. Do not force or otherwise activate that profile.

The current agent must stop its executable validation here. A normal macOS `test` or `verify` lifecycle must not invoke `test-fetch-native` because that execution is bound only by the Linux x64 profile. The current agent must not invoke `fetch-native.test.mjs` directly or force the Linux profile active. It must not attempt to approximate any of the following Linux checks on macOS.

### Phase 1: write the Linux x64 agent prompt

After all implementation and macOS validation are complete, write `2323-java-windows-x64-embed-rust-remove-before-merge/20260816-notes-to-linux-x64-agent.md`. Make it directly executable as a prompt to `copilot --yolo`, with no dependence on this plan or prior conversation.

The prompt must tell the Linux x64 agent:

1. It is running serially after the macOS work has been completed and merged to the topic branch.
2. The checked-out `HEAD` is the authoritative starting point containing that work. It must inspect the existing changes and must not repeat, replace, or revert the macOS implementation.
3. Its scope is to validate the Linux x64 path retained by the new host-activated Maven profile, diagnose any failures, make only the Java or Java-related GitHub Actions changes needed to preserve Linux x64 behavior, and re-run the relevant validation.
4. It must read this implementation plan, ADR-007, `java/pom.xml`, and `java/copilot-native/pom.xml` before changing code.
5. It must start every Maven command from `java`, use the required Java environment, enable `pipefail`, pipe stdout and stderr through `tee` to a timestamped filename containing the literal `job-logs`, and inspect that exact log.
6. It must not add macOS or Windows native support, broaden the native platform matrix, add `os-maven-plugin`, or weaken the macOS unsupported-host guarantees.
7. It must preserve unrelated work already present at `HEAD` and make surgical fixes only if Linux validation exposes a problem.
8. It must report the exact files changed, exact Maven commands and log filenames, produced artifact names, and whether every Linux acceptance criterion passed.

Embed the following Linux x64 validation and acceptance criteria in that prompt.

### Phase 2: Linux x64 work delegated to the later agent

On the native Linux x64 host, the later agent must run `mvn -pl copilot-native help:active-profiles` and confirm `native-linux-x64` is active. It must then run:

```sh
LOG="$(date +%Y%m%d-%H%M)-job-logs.txt"
mvn -pl copilot-native test 2>&1 | tee "$LOG"
```

Acceptance criterion: all `fetch-native.test.mjs` tests pass without downloading a real native package or producing a classifier JAR.

It must then run:

```sh
LOG="$(date +%Y%m%d-%H%M)-job-logs.txt"
mvn clean verify 2>&1 | tee "$LOG"
```

Linux x64 acceptance criteria:

- `fetch-native` runs during `generate-resources`.
- Exactly one platform classifier JAR is produced, ending in `-linux-x64.jar`.
- The existing structural verification confirms `runtime.node`, `platform.properties`, and `copilot`.
- The OS-neutral placeholder JAR does not contain native binaries.
- The full Java reactor remains green.

The later Linux x64 agent must then run:

```sh
LOG="$(date +%Y%m%d-%H%M)-job-logs.txt"
mvn clean package -pl copilot-native -DskipTests -Dcopilot.native.skip.download=true 2>&1 | tee "$LOG"
```

Acceptance criteria:

- No native download, staging directory, classifier JAR, or native structural verification occurs.
- The OS-neutral placeholder, sources, and Javadoc JARs are still produced.

If either Linux command fails or an acceptance criterion is not met, the Linux x64 agent must diagnose and fix the Linux-specific regression, then repeat the smallest relevant Maven validation and the full `mvn clean verify` before concluding.

## Completion criteria

- A clean macOS reactor build cannot produce a Linux-classified artifact.
- Linux x64 retains the classifier artifact and all existing integrity/structure checks.
- `copilot.native.skip.download=true` retains its placeholder-only behavior.
- Common native build logic exists once; platform profiles contain only activation, platform properties, and phase bindings.
- ADR-007 accurately describes host-matched native packaging and the extension pattern.
- The macOS agent has not exercised Linux x64 code.
- The macOS agent has written the required `20260816-notes-to-linux-x64-agent.md` only after its implementation and macOS validation completed.
- The handoff prompt is self-contained, assumes the merged topic-branch `HEAD`, and delegates all Linux x64 execution and Linux-only remediation to the later Linux x64 agent.
