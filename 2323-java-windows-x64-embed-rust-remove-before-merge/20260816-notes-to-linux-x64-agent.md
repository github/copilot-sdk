# Validate host-gated Java native packaging on Linux x64

Work autonomously in the current `copilot-sdk` checkout. This work runs serially after the macOS implementation was completed and merged to the topic branch. The currently checked-out `HEAD` is the authoritative starting point and already contains that implementation. Inspect it before making changes. Do not repeat, replace, or revert the macOS work.

## Objective

Validate that the Java native-runtime Maven module still fetches, packages, and verifies the `linux-x64` classifier on a native Linux x64 host after native packaging was changed to use a host-activated profile. Diagnose any failures, make only the Java or Java-related GitHub Actions changes required to preserve Linux x64 behavior, and repeat validation until all acceptance criteria pass.

The implemented design is:

* Native-specific Maven executions are dormant in the base `java/copilot-native/pom.xml`.
* The `native-linux-x64` profile activates only on Linux `amd64` when `copilot.native.skip.download` is absent.
* That profile supplies `copilot.native.classifier=linux-x64` and `copilot.native.cli.filename=copilot`.
* The profile binds the shared `fetch-native`, `test-fetch-native`, `jar-native`, and `verify-native-jars` executions to the Maven lifecycle.
* Unsupported hosts still build the OS-neutral primary, sources, and Javadoc JARs without producing native-looking artifacts.

## Required preparation

Before changing code:

1. Read the repository's current Java instructions.
1. Read `2323-java-windows-x64-embed-rust-remove-before-merge/20260815-make-it-so-no-incorrect-os-arch-artifacts-are-produced.md`.
1. Read `java/docs/adr/adr-007-native-bundling-strategy.md`.
1. Read the **Development Setup for native embedding** section in `java/README.md`.
1. Read `java/pom.xml` and `java/copilot-native/pom.xml`.
1. Inspect `git status` and the relevant `HEAD` diff/history so you understand the merged macOS implementation and preserve unrelated work.
1. Confirm the host is native Linux x64 before invoking Maven. Do not force the profile active on another operating system or architecture.

Start every Maven command from `java`. Configure the Java and Maven environment required by the current repository instructions and Linux host. Enable `pipefail`, pipe both stdout and stderr through `tee`, and use a unique local-time filename matching `YYYYMMDD-HHMM-<purpose>-job-logs.txt`. Echo and retain each exact filename, then inspect that exact file rather than locating it with a glob or modification-time sorting.

## Scope constraints

* Do not add macOS or Windows native support.
* Do not broaden the native platform matrix.
* Do not add `os-maven-plugin`.
* Do not weaken the unsupported-host guarantees implemented by the macOS work.
* Preserve the common native build logic; platform profiles must contain only activation, platform properties, and lifecycle phase bindings.
* Preserve unrelated changes already present at `HEAD`.
* Make surgical fixes only when Linux validation exposes a defect.
* Do not change files outside `java` or Java-related GitHub Actions.
* Do not commit, push, or open a pull request unless explicitly requested.

## Validation

### Confirm profile activation

From `java`, run:

```sh
set -o pipefail
LOG="$(date +%Y%m%d-%H%M)-active-profiles-job-logs.txt"
echo "LOG_FILE=$LOG"
mvn -pl copilot-native help:active-profiles 2>&1 | tee "$LOG"
```

Inspect that exact log and confirm `native-linux-x64` is active.

### Run native fetch-script tests

From `java`, run:

```sh
set -o pipefail
LOG="$(date +%Y%m%d-%H%M)-native-tests-job-logs.txt"
echo "LOG_FILE=$LOG"
mvn -pl copilot-native test 2>&1 | tee "$LOG"
```

Acceptance criterion: all `fetch-native.test.mjs` tests pass without downloading a real native package or producing a classifier JAR.

### Run the full Java reactor

From `java`, run:

```sh
set -o pipefail
LOG="$(date +%Y%m%d-%H%M)-verify-job-logs.txt"
echo "LOG_FILE=$LOG"
mvn clean verify 2>&1 | tee "$LOG"
```

Inspect the exact log and output tree. All of these criteria must pass:

* `fetch-native` runs during `generate-resources`.
* Exactly one platform classifier JAR is produced, ending in `-linux-x64.jar`.
* Structural verification confirms the classifier JAR contains `native/linux-x64/runtime.node`, `native/linux-x64/platform.properties`, and `native/linux-x64/copilot`.
* The OS-neutral placeholder JAR contains no native binaries.
* The full Java reactor succeeds.

### Validate placeholder-only behavior

From `java`, run:

```sh
set -o pipefail
LOG="$(date +%Y%m%d-%H%M)-skip-native-job-logs.txt"
echo "LOG_FILE=$LOG"
mvn clean package -pl copilot-native -DskipTests -Dcopilot.native.skip.download=true 2>&1 | tee "$LOG"
```

Inspect the exact log and output tree. All of these criteria must pass:

* No native download occurs.
* No `native-staging` directory is produced.
* No platform classifier JAR is produced.
* Native structural verification does not run.
* The OS-neutral primary, sources, and Javadoc JARs are produced.

## Failure handling

If a command fails or an acceptance criterion is not met:

1. Diagnose the root cause from the exact Maven log and effective profile/build configuration.
1. Fix only the Linux-specific regression or shared profile wiring responsible for the failure.
1. Preserve the macOS unsupported-host behavior and the extension pattern described above.
1. Run the smallest relevant Maven check after each fix.
1. Before concluding, repeat `mvn clean verify` and every validation step affected by the fix.

## Documentation update

During this work, update the **Development Setup for native embedding** section in `java/README.md` so it reflects the Linux x64 behavior actually observed and any fixes made:

* Replace the pending Linux x64 validation statement with the verified result.
* Keep the required JDK, Maven, Node.js, npm authentication, and run-from-`java` prerequisites accurate.
* Keep the unsupported-host and placeholder-only instructions accurate.
* Correct commands or expected artifacts if Linux validation shows that the current text is incomplete or inaccurate.
* Do not document macOS, Windows, or another classifier as natively supported.

## Final response

Report:

* Exact files changed, including the required `java/README.md` update.
* Exact Maven commands and exact `job-logs` filenames.
* Produced native and OS-neutral artifact filenames.
* Whether each acceptance criterion passed.
* Any remaining blocker, stated plainly.
