# Phase 2: CI Workflows — Agent Prompt

## Context

You are working in `copilot-sdk-00`, a local clone of `https://github.com/github/copilot-sdk`. The Java SDK source repository is at `../copilot-sdk-java-00` (a local clone of `https://github.com/github/copilot-sdk-java`).

You are implementing one phase of the work to make it so the `copilot-sdk/java` directory is the new home for what is currently `copilot-sdk-java`, with all source code, workflows and maintenance affordances migrated.

Phase 0 (pre-flight) and Phase 1 (copy source code) are already complete. The Java source code is already present under `java/` in this repository and `mvn clean verify` passes from that directory.

## First Step — Read the Master Plan

Before doing any work, read the file:

```
80-java-monorepo-add-01-remove-before-merge/dd-2989727-move-java-to-monorepo-plan.md
```

This contains the full migration plan. Focus on the "Phase 2: CI Workflows" section, but also review the naming conventions in §3 and the workflow inventory tables in §5 for context.

**You are executing Phase 2 only. Do NOT perform any other phases (Phase 0, 1, 3, 4, 5, or 6).**

## Working Branch and Commits

You are safe to do all work on the current topic branch. Make fine-grained commits with clear, descriptive commit log messages (e.g., "Add java-sdk-tests.yml adapted from build-test.yml", "Add Java job to codegen-check.yml"). Do not squash — keep commits granular so they are easy to review.

## Phase 2 Goal

Java CI runs on PRs and pushes to `main` within the monorepo. This phase creates/updates 5 things:

1. A new `java-sdk-tests.yml` workflow
2. A merged Java job in the existing `codegen-check.yml`
3. A new `java-codegen-agentic-fix.md` agentic workflow (+ compiled `.lock.yml`)
4. Java tooling added to the existing `copilot-setup-steps.yml`
5. Maven ecosystem entry added to the existing `dependabot.yaml`

---

## Task 1: Create `.github/workflows/java-sdk-tests.yml`

**Source to adapt from:** `../copilot-sdk-java-00/.github/workflows/build-test.yml`

**Reference for monorepo style:** `.github/workflows/dotnet-sdk-tests.yml` or `.github/workflows/go-sdk-tests.yml` (read one of these to match trigger structure and job naming conventions).

### Requirements

- **Triggers:**
  - `push` to `main` with paths: `java/**`, `test/**`, `.github/workflows/java-sdk-tests.yml`
  - `pull_request` with same paths
  - `workflow_dispatch`

- **OS matrix:** Run on `ubuntu-latest`, `windows-latest`, `macos-latest` (match other SDK test workflows in this repo).

- **JDK version:** 17 (use `actions/setup-java` with `temurin` distribution).

- **Steps (adapt from `build-test.yml`):**
  1. Checkout
  2. Set up JDK 17
  3. Cache Maven dependencies (`~/.m2/repository`)
  4. Set up Node.js (needed for E2E test harness — the replay proxy is Node-based, located at `test/harness/`)
  5. Run `mvn spotless:check` (formatting gate)
  6. Run `mvn clean verify` (build + all tests including E2E)
  7. Upload test results (Surefire reports) as artifacts on failure

- **Working directory:** All Maven commands must use `working-directory: ./java`

- **Important differences from source:**
  - The source `build-test.yml` clones the `copilot-sdk` repo at build time (via `generate-test-resources` Maven phase) to get `test/harness/` and `test/snapshots/`. In the monorepo these are already present at the repo root under `test/`. The `java/pom.xml` has already been updated in Phase 1 to reference the local `test/` directory, so no special handling is needed — just ensure the checkout is a full checkout (not shallow if tests need git history — check if this matters).
  - The source has a `smoke-test` job that calls `run-smoke-test.yml`. Do NOT include the smoke test in this workflow — that is a Phase 3 concern (`java-smoke-test.yml`).
  - The source has coverage badge generation. Do NOT include coverage badge generation — keep the workflow focused on build+test.
  - The source has a Javadoc generation step. Include a `mvn javadoc:javadoc` step (non-failing, just to verify Javadoc compiles) OR fold it into the `mvn verify` if the POM already runs Javadoc during verify. Check `java/pom.xml` to see if Javadoc is part of the verify lifecycle.

- **Do NOT include:**
  - Smoke test job (Phase 3)
  - Deploy/publish steps (Phase 3)
  - Any cross-repo clone of `copilot-sdk` (no longer needed)

---

## Task 2: Merge Java into `.github/workflows/codegen-check.yml`

**Source to adapt from:** `../copilot-sdk-java-00/.github/workflows/codegen-check.yml`

**Target to modify:** `.github/workflows/codegen-check.yml` (already exists in the monorepo)

### Requirements

- Read the existing `codegen-check.yml` to understand its structure. It already has jobs for Node, .NET, Python, Go, and Rust codegen verification.

- **Add `java/src/generated/**` to the path triggers\*\* (both push and pull_request).

- **Add a new job** (e.g., `java-codegen`) that:
  1. Checks out the repo
  2. Sets up Node.js (the Java codegen script `java/scripts/codegen/java.ts` is a TypeScript file that runs via `npx tsx`)
  3. Sets up JDK 17 (needed if the codegen script validates against Java compilation)
  4. Installs codegen dependencies: `cd java/scripts/codegen && npm ci`
  5. Runs the Java codegen: `cd java/scripts/codegen && npx tsx java.ts`
  6. Checks for uncommitted changes in `java/src/generated/` using `git diff --exit-code java/src/generated/`
  7. If changes exist, fails with a message indicating codegen is out of date

- **Match the job structure** of the other language codegen jobs in the same file (same checkout action version, same diff pattern, etc.).

---

## Task 3: Create `.github/workflows/java-codegen-agentic-fix.md`

**Source to adapt from:** `../copilot-sdk-java-00/.github/workflows/codegen-agentic-fix.md`

Also read the corresponding `.lock.yml` from the source to understand the compiled structure.

### What codegen-check.yml and `codegen-agentic-fix` Actually Own

These two workflows form a **self-contained codegen CI pipeline** with one concern only:

> **Keep java in sync with whatever `@github/copilot` schemas are declared in package.json.**

Their flow:

1. codegen-check.yml: On PR or push, re-runs codegen → if drift detected, pushes regen'd files → if `mvn verify` fails, triggers the agentic fix
2. codegen-agentic-fix.lock.yml: AI agent that fixes `java.ts` and/or handwritten source until `mvn verify` passes, then pushes to the PR

They do **not** interact with .lastmerge, the CLI version property, or the test harness clone.

### Requirements

- This is a `gh-aw` (GitHub Agentic Workflows) markdown file. It defines an agentic workflow that auto-fixes compilation/test failures caused by Java codegen changes.

- **Adapt the source** with these changes:
  - All paths updated to reflect the monorepo structure (e.g., `java/src/generated/`, `java/scripts/codegen/`, etc.)
  - Remove any references to cross-repo operations
  - Update the workflow trigger to fire when `java-codegen` job (from `codegen-check.yml`) fails
  - Update instructions to run `cd java && mvn verify` for validation
  - Update codegen command to `cd java/scripts/codegen && npx tsx java.ts`

- **After creating the `.md` file**, compile it:

  ```
  gh aw compile java-codegen-fix
  ```

  This generates `.github/workflows/java-codegen-fix.lock.yml`. Both files must be committed.

- **If `gh aw` is not available or the compile fails**, note this in a commit message and commit just the `.md` file. The `.lock.yml` can be generated later.

---

## Task 4: Merge Java into `.github/workflows/copilot-setup-steps.yml`

**Target to modify:** `.github/workflows/copilot-setup-steps.yml` (already exists)

### Requirements

- Read the existing `copilot-setup-steps.yml`. It sets up the environment for the Copilot coding agent (Node, Python, Go, .NET, Rust, etc.).

- **Add the following steps** (in a logical position alongside other language setups):
  1. **Set up JDK 17:**
     ```yaml
     - uses: actions/setup-java@v4
       with:
         distribution: "microsoft"
         java-version: "17"
         cache: "maven"
     ```
  2. **Set up Maven cache** (if not handled by the `cache: 'maven'` option above, add explicit caching of `~/.m2/repository`).
  3. **Enable Java git hooks** (the Java pre-commit hook):
     ```yaml
     - name: Enable Java pre-commit hook
       run: |
         cd java
         git config core.hooksPath .githooks
     ```
     Only add this if `java/.githooks/pre-commit` exists. Check first.

- **Also check** `../copilot-sdk-java-00/.github/workflows/copilot-setup-steps.yml` for any other setup steps that the Java coding agent environment needs (e.g., `gh aw` installation, specific npm global installs). Port those over if they aren't already present in the monorepo version.

---

## Task 5: Update `.github/dependabot.yaml`

**Target to modify:** `.github/dependabot.yaml` (already exists)

### Requirements

- Read the existing file to understand its structure.

- **Add a Maven ecosystem entry** for the `java/` directory:

  ```yaml
  - package-ecosystem: "maven"
    directory: "/java"
    schedule:
      interval: "weekly"
    labels:
      - "dependencies"
      - "sdk/java"
  ```

- Match the style (labels, schedule interval, grouping) of the existing entries. If other entries use `groups`, add an appropriate group for Java dependencies.

- **Also check** if `../copilot-sdk-java-00/.github/dependabot.yml` has any additional ecosystems configured (e.g., `github-actions` scoped to Java workflows, or `npm` for `java/scripts/codegen/`). If so, add those entries too.

---

## Verification

After completing all 5 tasks, verify:

1. **YAML syntax:** Run `python -c "import yaml; yaml.safe_load(open('.github/workflows/java-sdk-tests.yml'))"` (or equivalent) to check YAML validity of new/modified workflow files.
2. **No broken references:** Ensure no workflow file references actions or paths that don't exist.
3. **Consistent style:** The new `java-sdk-tests.yml` should look similar in structure to `dotnet-sdk-tests.yml` or `go-sdk-tests.yml`.
4. **Commit each task separately** with a clear commit message.

## Summary of Files to Create/Modify

| Action     | File                                                                |
| ---------- | ------------------------------------------------------------------- |
| **Create** | `.github/workflows/java-sdk-tests.yml`                              |
| **Modify** | `.github/workflows/codegen-check.yml`                               |
| **Create** | `.github/workflows/java-codegen-fix.md`                             |
| **Create** | `.github/workflows/java-codegen-fix.lock.yml` (via `gh aw compile`) |
| **Modify** | `.github/workflows/copilot-setup-steps.yml`                         |
| **Modify** | `.github/dependabot.yaml`                                           |

## Reminders

- All Maven commands use `working-directory: ./java`
- Do NOT touch Phase 3 (publish), Phase 4 (agentic sync), Phase 5 (cross-cutting), or Phase 6 (cutover)
- Do NOT modify `java/pom.xml` or any Java source code
- Do NOT modify `test/harness/` or `test/snapshots/`
- Follow the naming convention: language-specific workflows use `java-` prefix
- Make fine-grained commits on the current branch
