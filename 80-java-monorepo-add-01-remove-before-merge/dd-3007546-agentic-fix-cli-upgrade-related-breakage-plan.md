# DD-3007546: Include Java in CLI Version Upgrade — One Lane + Two Lanes

## Context

Read the master plan: `80-java-monorepo-add-01-remove-before-merge/dd-2989727-move-java-to-monorepo-plan.md`.

The existing workflow `.github/workflows/update-copilot-dependency.yml` bumps `@github/copilot` for Node/TS, Python, Go, .NET, and Rust but **excludes Java**. This plan adds Java to the same deterministic bump ("one lane") and then provides repair mechanisms ("two lanes") for when schema changes break handwritten Java code.

**Branch**: `upstream/edburns/80-java-monorepo-iterating`

**Key files**:
- `.github/workflows/update-copilot-dependency.yml` — the bump workflow to modify
- `java/pom.xml` — contains POM property `readonly-copilot-sdk-ref-impl-version-from-lastmerge-file-updated-by-reference-impl-sync`
- `java/.lastmerge` — monorepo commit SHA for the test harness pin
- `java/scripts/codegen/package.json` — Java codegen `@github/copilot` dependency
- `.github/workflows/java-sdk-tests.yml` — Java CI tests
- `.github/workflows/java-codegen-fix.md` — existing agentic codegen fixer

---

## Phase: One Lane

### Goal

When `.github/workflows/update-copilot-dependency.yml` is invoked with a version, the existing non-Java upgrade happens **and** the following Java artifacts are also updated in the same PR commit:

1. `java/scripts/codegen/package.json` — `@github/copilot` dependency updated to the input version.
2. `java/.lastmerge` — updated to the commit SHA of the current checkout (the commit that contains the harness bump).
3. `java/pom.xml` property `readonly-copilot-sdk-ref-impl-version-from-lastmerge-file-updated-by-reference-impl-sync` — updated to `^<VERSION>` (matching input).
4. Java codegen is run: `cd java && mvn generate-sources -Pcodegen` — produces regenerated `java/src/generated/java/` files.
5. Java compilation is validated: `cd java && mvn compile -Pskip-test-harness` — if this fails, the workflow job fails (same behavior as the shared codegen step).

If **any** of steps 4-5 fail, the workflow fails with no PR created (identical to existing behavior for other languages).

### Implementation Steps

Add these steps to `.github/workflows/update-copilot-dependency.yml` **after** the "Format generated code" step and **before** the "Create pull request" step:

```yaml
      - uses: actions/setup-java@v5
        with:
          java-version: '25'
          distribution: 'microsoft'

      - name: Update @github/copilot in Java codegen
        env:
          VERSION: ${{ inputs.version }}
        working-directory: ./java/scripts/codegen
        run: npm install "@github/copilot@$VERSION"

      - name: Update Java .lastmerge to current commit
        run: git rev-parse HEAD > java/.lastmerge

      - name: Update Java POM CLI version property
        env:
          VERSION: ${{ inputs.version }}
        working-directory: ./java
        run: |
          PROP="readonly-copilot-sdk-ref-impl-version-from-lastmerge-file-updated-by-reference-impl-sync"
          sed -i "s|<${PROP}>.*</${PROP}>|<${PROP}>^${VERSION}</${PROP}>|" pom.xml

      - name: Run Java codegen
        working-directory: ./java
        run: mvn generate-sources -Pcodegen

      - name: Compile Java SDK (validate generated code)
        working-directory: ./java
        run: mvn compile -Pskip-test-harness
```

### Loop

Repeat up to **10** iterations:

1. Make the changes locally in `.github/workflows/update-copilot-dependency.yml`.
2. Commit and push to `upstream/edburns/80-java-monorepo-iterating`.
3. Trigger the workflow **on this branch** via:
   ```bash
   gh workflow run "Update @github/copilot Dependency" \
     --ref edburns/80-java-monorepo-iterating \
     -f version=1.0.57
   ```
   Since 1.0.57 is already installed, this is effectively a no-op bump — the workflow should still execute all steps successfully and produce "No changes detected" at commit time (which is a valid success signal for our purposes: all steps ran without error).

   If the "No changes detected" exit-early behavior makes validation ambiguous, create a **trivial version bump scenario** by:
   - Temporarily downgrading `test/harness/package.json` and `nodejs/package.json` to `1.0.55-5`
   - Pushing that state
   - Then triggering the workflow with `version=1.0.57` so a real diff is produced

4. Check the workflow run result:
   ```bash
   gh run list --workflow="Update @github/copilot Dependency" \
     --branch=edburns/80-java-monorepo-iterating --limit=1 --json status,conclusion,databaseId
   ```
   - If `conclusion` is `failure`: inspect logs via `gh run view <id> --log-failed`, diagnose, fix, and re-enter the loop.
   - If `conclusion` is `success`: exit the loop and proceed to validation.

5. If 10 iterations are reached without success, declare failure and stop.

### Validation

After the loop exits successfully, execute `80-java-monorepo-add-01-remove-before-merge/phase-one-lane-validation.md`.

---

## Phase: Two Lanes

This phase provides repair paths when the deterministic Java codegen/compile in "one lane" **succeeds** (generated code compiles) but handwritten code or tests break against the new generated types.

### Lane 01: Manual Fix Plan in PR Body

#### Goal

When `update-copilot-dependency` creates a draft PR, the PR body includes a detailed agentic plan (a checklist of steps) that a human developer can follow to fix handwritten Java code breakages.

#### Implementation Steps

Modify the PR body in the "Create pull request" step of `.github/workflows/update-copilot-dependency.yml`. After the existing body text, append:

```markdown
### Java Handwritten Code Adaptation Plan

If `java-sdk-tests` CI fails on this PR, follow these steps:

1. **Identify failures**: Run `mvn verify` from `java/` locally or check the `java-sdk-tests` workflow run logs.
2. **Categorize errors**:
   - Constructor signature changes (new fields added to generated records)
   - Enum value additions/renames in generated types
   - New event types requiring handler registration
   - Removed or renamed generated types
3. **Fix handwritten source** (`java/src/main/java/com/github/copilot/sdk/`):
   - Update call sites passing positional constructor args to include new fields (typically `null` for optional new fields).
   - Update switch/if-else over enum values to handle new cases.
   - Register handlers for new event types in `CopilotSession.java` if applicable.
4. **Fix handwritten tests** (`java/src/test/java/com/github/copilot/sdk/`):
   - Same constructor/enum fixes as above.
   - Add new test methods for new functionality if the change adds user-facing API surface.
5. **Validate**: `cd java && mvn clean test-compile jar:jar && mvn verify -Dskip.test.harness=true`
6. **Format**: `cd java && mvn spotless:apply`
7. Push fixes to this PR branch.

> To automate this, trigger the `java-adapt-handwritten-code-to-accept-upgrade-changes` agentic workflow instead.
```

#### Loop

Repeat up to **10** iterations:

1. Update the workflow file locally with the new PR body text.
2. Push to `upstream/edburns/80-java-monorepo-iterating`.
3. Trigger the workflow (same as Phase One Lane loop step 3).
4. If a PR is created, inspect its body:
   ```bash
   gh pr view update-copilot-<VERSION> --json body --jq '.body'
   ```
   - If the body contains the "Java Handwritten Code Adaptation Plan" section: exit the loop.
   - If not: diagnose and fix.
5. If no PR is created (no changes detected), verify the workflow ran to completion and the body template is present in the workflow file itself — that suffices for validation.

If 10 iterations are reached without success, declare failure and stop.

#### Validation

After the loop exits, execute `80-java-monorepo-add-01-remove-before-merge/phase-two-lanes-manual-fix-validation.md`.

---

### Lane 02: Agentic Fix Workflow

#### Goal

An agentic workflow `.github/workflows/java-adapt-handwritten-code-to-accept-upgrade-changes.md` exists that:

1. Assumes Java codegen has already succeeded (generated code compiles).
2. Fixes handwritten Java SDK code in `java/src/main/java/` to work with the new generated types.
3. Fixes handwritten Java tests in `java/src/test/java/` (but **NOT** in the `com.github.copilot.generated` test package).
4. Runs `java-sdk-tests.yml` equivalent commands to validate all tests pass.
5. Commits and pushes fixes to the PR branch.

#### Implementation Steps

Create `.github/workflows/java-adapt-handwritten-code-to-accept-upgrade-changes.md` with this content:

```markdown
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
```

Then compile the lock file:
```bash
gh aw compile java-adapt-handwritten-code-to-accept-upgrade-changes
```

#### Loop

Repeat up to **10** iterations:

1. Create or update `.github/workflows/java-adapt-handwritten-code-to-accept-upgrade-changes.md`.
2. Compile with `gh aw compile java-adapt-handwritten-code-to-accept-upgrade-changes`.
3. Verify `.github/workflows/java-adapt-handwritten-code-to-accept-upgrade-changes.lock.yml` exists.
4. Push to `upstream/edburns/80-java-monorepo-iterating`.
5. Trigger the agentic workflow on the topic branch:
   ```bash
   gh workflow run "java-adapt-handwritten-code-to-accept-upgrade-changes" \
     --ref edburns/80-java-monorepo-iterating \
     -f branch=edburns/80-java-monorepo-iterating \
     -f pr_number=<PR_NUMBER>
   ```
   (Use the PR number of the current feature branch PR, or a test PR.)
6. Wait for completion:
   ```bash
   gh run list --workflow="java-adapt-handwritten-code-to-accept-upgrade-changes" \
     --branch=edburns/80-java-monorepo-iterating --limit=1 --json status,conclusion,databaseId
   ```
7. Check result:
   - If `conclusion` is `success` or the agent posted `noop` (no fixes needed): exit loop.
   - If `conclusion` is `failure`: inspect logs, update the `.md` workflow, re-enter loop.

If 10 iterations are reached without success, declare failure and stop.

#### Validation

After the loop exits, execute `80-java-monorepo-add-01-remove-before-merge/phase-two-lanes-agentic-fix-validation.md`.

---

## Execution Order

1. Execute **Phase: One Lane** first.
2. Only after One Lane validation passes, execute **Phase: Two Lanes — Lane 01**.
3. Only after Lane 01 validation passes, execute **Phase: Two Lanes — Lane 02**.
4. If any phase declares failure, stop entirely.
