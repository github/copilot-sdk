---
description: |
  Adapt handwritten Java SDK code to work with regenerated types after a
  @github/copilot version bump. Assumes codegen succeeded and generated code
  compiles. Fixes handwritten source and tests only.

on:
  workflow_dispatch:
    inputs:
      branch:
        description: 'Branch containing the upgrade PR'
        required: true
        type: string
      pr_number:
        description: 'PR number to push fixes to'
        required: true
        type: string

permissions:
  contents: read
  actions: read

timeout-minutes: 60

network:
  allowed:
    - defaults
    - github

tools:
  github:
    toolsets: [context, repos]

safe-outputs:
  push-to-pull-request-branch:
    target: "*"
    labels: [dependencies, sdk/java]
  add-comment:
    target: "*"
    max: 10
  noop:
    report-as-issue: false
---
# Java Handwritten Code Adaptation After CLI Upgrade

You are an automation agent that fixes handwritten Java SDK source and test code after a `@github/copilot` version bump has regenerated the typed schemas.

## Assumptions

- The branch `${{ inputs.branch }}` already has:
  - Updated `java/scripts/codegen/package.json` with the new version
  - Regenerated `java/src/generated/java/` code that compiles successfully
  - Updated `java/.lastmerge` and POM property
- Your job is ONLY to fix **handwritten** code, NOT generated code.

## Boundaries

- ❌ Do NOT edit anything under `java/src/generated/java/`
- ❌ Do NOT edit `java/scripts/codegen/java.ts`
- ❌ Do NOT create or modify tests in the `com.github.copilot.generated` test package (`java/src/test/java/com/github/copilot/sdk/generated/`)
- ✅ DO edit `java/src/main/java/com/github/copilot/sdk/**`
- ✅ DO edit `java/src/test/java/com/github/copilot/sdk/**` (excluding the `generated` subpackage)
- ✅ DO add new test methods or test classes if new user-facing API surface is introduced

## Instructions

### Step 0: Setup

```bash
git checkout "${{ inputs.branch }}"
git pull origin "${{ inputs.branch }}"
```

Verify Java environment:
```bash
java -version
mvn --version
node --version
```

### Step 1: Reproduce failures

```bash
cd java
mvn clean test-compile jar:jar
mvn verify -Dskip.test.harness=true 2>&1 | tee /tmp/mvn-verify.log
```

If `mvn verify` succeeds (exit code 0), call `noop` with message "All tests pass on branch ${{ inputs.branch }}. No handwritten fixes needed." and stop.

### Step 2: Analyze compilation errors

Read the build output. Common patterns after a schema bump:

1. **Constructor arity mismatch** — A generated Java record gained new fields, changing its constructor signature. Fix: add `null` (or appropriate default) for new parameters at every call site.
2. **Missing enum constants** — A generated enum gained new values that existing switch/if-else does not cover. Fix: add cases or ensure default handling.
3. **Type changes** — A field type changed (e.g., `String` → enum, `double` → `Long`). Fix: update usages.
4. **New event types** — New session event classes were generated. If `CopilotSession.java` or event handlers reference events by explicit type listing, add the new types.

### Step 3: Fix compilation errors

Apply minimal targeted fixes:
- Search for compilation errors referencing generated type names.
- Update constructor calls to match new arity.
- Update type references if renamed/moved.
- Do NOT over-engineer — just make it compile.

After each fix round, verify:
```bash
cd java && mvn compile -Pskip-test-harness
```

### Step 4: Fix test failures

Once compilation passes, run tests:
```bash
cd java && mvn verify -Dskip.test.harness=true 2>&1 | tee /tmp/mvn-test.log
```

Fix failing assertions:
- Update expected constructor arg counts in test utility calls.
- Update expected enum values in assertions.
- Add coverage for new public API if introduced (new getters, new config options).

### Step 5: Format

```bash
cd java && mvn spotless:apply
```

### Step 6: Final validation

```bash
cd java
mvn clean test-compile jar:jar
mvn verify -Dskip.test.harness=true
```

If this passes, commit and push:
```bash
git add java/src/main/java java/src/test/java
git commit -m "Fix handwritten Java code for @github/copilot schema changes

Adapt constructor calls, enum references, and test assertions to match
regenerated types after CLI version bump."
git push origin "${{ inputs.branch }}"
```

Then add a comment to PR #${{ inputs.pr_number }} summarizing what was fixed.

If after 3 full fix-compile-test cycles the build still fails, add a comment to the PR describing the remaining failures and stop.
