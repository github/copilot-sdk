# DD-3007546: Phase 04 — Agentic Workflows and Skills Migration

❌❌❌ UNDER NO CIRCUMSTANCES EVER do you push to `upstream/main` from `copilot-sdk-00`. You must only push to `upstream/edburns/80-java-monorepo-iterating`.❌❌❌

## Context

Read the master plan: `80-java-monorepo-add-01-remove-before-merge/dd-2989727-move-java-to-monorepo-plan.md` — focus on Phase 04.

**Working directory**: All work happens in the monorepo at the repo root (`copilot-sdk-00`).

**Key change from standalone**: The reference-impl-sync mechanism no longer crosses repository boundaries. Since the Java SDK now lives in the same repo as the .NET/Node reference implementations, all diffs are intra-repo `git log`/`git diff` operations against monorepo commit SHAs. The `.lastmerge` file now stores a monorepo commit SHA.

**Trigger change**: The workflow will NOT run on a schedule. It will only run via `workflow_dispatch` (manual/imperative trigger from a human).

---

## Pre-Implementation Checklist

Before starting, verify:
- [ ] `java/.lastmerge` exists and contains a commit SHA
- [ ] `java/pom.xml` contains the `<readonly-copilot-sdk-ref-impl-version-from-lastmerge-file-updated-by-reference-impl-sync>` property
- [ ] `java/scripts/codegen/package.json` exists
- [ ] `.github/workflows/java-codegen-fix.md` exists (already migrated in Phase 02)
- [ ] `.github/skills/java-coding-skill/SKILL.md` exists
- [ ] `gh aw compile` is available (run `gh aw --version` to check)

---

## Task 1: Create `.github/scripts/java-reference-impl-sync/` Directory and Scripts

Create **5 scripts** adapted from the standalone repo's `.github/scripts/reference-impl-sync/`. All paths must be updated for monorepo layout. The scripts no longer clone an external repo — they operate on the local monorepo git history.

### 1.1 `merge-reference-impl-start.sh`

**Source**: `copilot-sdk-java-00/.github/scripts/reference-impl-sync/merge-reference-impl-start.sh`

**Changes required**:
- Remove the `git clone` of `https://github.com/github/copilot-sdk.git` — no external clone needed
- `ROOT_DIR` should resolve to the **monorepo root** (walk up until you find `justfile` or `.github/` at root, NOT `pom.xml` which is in `java/`)
- Read `.lastmerge` from `java/.lastmerge` (not repo root)
- Instead of cloning and diffing an external repo, compute: `git log <lastmerge-sha>..HEAD -- dotnet/src/ nodejs/src/ test/snapshots/ docs/ sdk-protocol-version.json`
- The `.merge-env` file should record `LAST_MERGE_COMMIT`, `BRANCH_NAME`, `CLI_VERSION` (no `REFERENCE_IMPL_DIR` — it's the local repo)
- Branch creation: keep the same logic (reuse if already on non-main, else create `merge-reference-impl-YYYYMMDD`)
- Copilot CLI update: keep as-is (if available)
- Output the commit summary using local git log

### 1.2 `merge-reference-impl-diff.sh`

**Source**: `copilot-sdk-java-00/.github/scripts/reference-impl-sync/merge-reference-impl-diff.sh`

**Changes required**:
- Remove `cd "$REFERENCE_IMPL_DIR"` — operate from monorepo root
- Change the diff range from `$LAST_MERGE_COMMIT..origin/main` to `$LAST_MERGE_COMMIT..HEAD` (since we're already in the repo)
- All `git diff` and `git log` commands operate on the local repo
- Keep the same section groupings (`.NET source`, `.NET tests`, `Test snapshots`, `Documentation`, etc.)
- `ROOT_DIR` resolves to monorepo root

### 1.3 `merge-reference-impl-finish.sh`

**Source**: `copilot-sdk-java-00/.github/scripts/reference-impl-sync/merge-reference-impl-finish.sh`

**Changes required**:
- `ROOT_DIR` resolves to monorepo root
- Format/test: run `cd java && mvn spotless:apply` and `cd java && mvn clean verify` (or keep a `format-and-test.sh` helper under `java/` or `.github/scripts/`)
- Update `.lastmerge`: write `git rev-parse HEAD` (current monorepo HEAD) to `java/.lastmerge`
- Sync pom.xml CLI version: call `sync-cli-version-from-reference-impl.sh` (updated, see below)
- Sync codegen version: call `sync-codegen-version.sh` (updated, see below)
- `git add java/.lastmerge java/pom.xml java/scripts/codegen/package.json java/scripts/codegen/package-lock.json`
- Commit message: `"Update java/.lastmerge to <SHA>, sync pom.xml CLI version and codegen @github/copilot version"`
- Push branch

### 1.4 `sync-cli-version-from-reference-impl.sh`

**Source**: `copilot-sdk-java-00/.github/scripts/reference-impl-sync/sync-cli-version-from-reference-impl.sh`

**Changes required**:
- No longer takes a `<reference-impl-dir>` argument — reads from the local `nodejs/package.json` directly (it's in the same repo)
- `ROOT_DIR` resolves to monorepo root
- Reads `@github/copilot` version from `$ROOT_DIR/nodejs/package.json`
- Updates `java/pom.xml` (not `pom.xml` at repo root) — the `find_repo_root()` function should look for `java/pom.xml` or accept a path argument
- Property name unchanged: `readonly-copilot-sdk-ref-impl-version-from-lastmerge-file-updated-by-reference-impl-sync`

### 1.5 `sync-codegen-version.sh`

**Source**: `copilot-sdk-java-00/.github/scripts/reference-impl-sync/sync-codegen-version.sh`

**Changes required**:
- No longer takes a `<reference-impl-dir>` argument — reads from local `nodejs/package.json`
- `ROOT_DIR` resolves to monorepo root
- Reads `@github/copilot` version from `$ROOT_DIR/nodejs/package.json`
- Updates `java/scripts/codegen/package.json` (not `scripts/codegen/package.json` at repo root)
- Runs `npm install` in `java/scripts/codegen/`

### 1.6 Create `format-and-test.sh` (Helper)

Create `.github/scripts/java-reference-impl-sync/format-and-test.sh` (or `.github/scripts/java-build/format-and-test.sh`):
- `cd java && mvn spotless:apply`
- `cd java && mvn clean verify`
- Support `--format-only`, `--test-only`, `--debug`, `--skip-tests` flags (match standalone behavior)

---

## Task 2: Create `.github/workflows/java-reference-impl-sync.md`

**Source**: `copilot-sdk-java-00/.github/workflows/reference-impl-sync.md`

**Key differences from standalone**:

1. **Trigger**: `workflow_dispatch` ONLY (no schedule). Remove `schedule: weekly on friday`.
2. **No external clone**: Instead of `git clone https://github.com/github/copilot-sdk.git`, compute diffs locally:
   ```bash
   LAST_MERGE=$(cat java/.lastmerge)
   CURRENT_HEAD=$(git rev-parse HEAD)
   # If identical, no changes
   COMMIT_COUNT=$(git rev-list --count "$LAST_MERGE".."$CURRENT_HEAD" -- dotnet/src/ nodejs/src/ test/snapshots/ docs/ sdk-protocol-version.json)
   ```
3. **Issue title prefix**: Keep `[reference-impl-sync]`
4. **Labels**: Keep `reference-impl-sync`
5. **Safe-outputs**: Keep same structure (create-issue, close-issue, add-comment, assign-to-agent, close-pull-request, noop)
6. **Permissions**: `contents: read`, `actions: read`, `issues: read`, `pull-requests: read` (same)
7. **Network**: Remove `github` from allowed (no external clone needed) — or keep it for the MCP tools

**Frontmatter** (adapt from standalone):
```yaml
---
description: |
  Java reference implementation sync workflow. Checks for new commits in the
  monorepo's dotnet/nodejs reference implementations since the last Java sync
  and assigns to Copilot to port changes.

on:
  workflow_dispatch:

permissions:
  contents: read
  actions: read
  issues: read
  pull-requests: read

network:
  allowed:
    - defaults
    - github

tools:
  github:
    toolsets: [context, repos, issues, pull_requests]

safe-outputs:
  create-issue:
    title-prefix: "[reference-impl-sync] "
    labels: [reference-impl-sync, sdk/java]
    expires: 6
  close-issue:
    required-labels: [reference-impl-sync]
    target: "*"
    max: 10
  add-comment:
    target: "*"
    max: 10
  assign-to-agent:
    name: "copilot"
    model: "claude-opus-4.6"
    target: "*"
  close-pull-request:
    target: "*"
    max: 10
  noop:
    report-as-issue: false
---
```

**Body instructions** (adapt Step 1 and Step 2):
- Step 1: Read `java/.lastmerge`
- Step 2: Run local `git rev-list` and `git log` to detect changes in `dotnet/src/`, `nodejs/src/`, `test/snapshots/`, `docs/`, `sdk-protocol-version.json`
- Step 3a/3b: Same logic (close stale issues/PRs or create new issue with commit summary)
- The issue body should mention this is an intra-repo sync and reference the `java-reference-impl-sync` scripts

After creating the `.md` file, **compile it**:
```bash
gh aw compile java-reference-impl-sync
```

This generates `.github/workflows/java-reference-impl-sync.lock.yml`. Both files must be committed.

---

## Task 3: Create `.github/prompts/java-agentic-merge-reference-impl.prompt.md`

**Source**: `copilot-sdk-java-00/.github/prompts/agentic-merge-reference-impl.prompt.md`

**Changes required**:
- All script paths change from `.github/scripts/reference-impl-sync/` → `.github/scripts/java-reference-impl-sync/`
- All references to "cloning the reference implementation" → "computing local diffs"
- `.lastmerge` path: `java/.lastmerge` (not repo root)
- `pom.xml` path: `java/pom.xml`
- `scripts/codegen/` path: `java/scripts/codegen/`
- `src/main/java/` path: `java/src/main/java/`
- `src/test/java/` path: `java/src/test/java/`
- `src/generated/java/` path: `java/src/generated/java/`
- `src/site/` path: `java/src/site/`
- `README.md` for Java: `java/README.md`
- `mvn` commands: run from `java/` directory (e.g., `cd java && mvn clean verify`)
- The `format-and-test.sh` reference: `.github/scripts/java-reference-impl-sync/format-and-test.sh`
- The `.merge-env` file: still at repo root (or in `java/` — pick one, be consistent)
- Key Files to Compare table: update all Java paths with `java/` prefix
- Remove all references to external repo URLs for diffing (the repo IS the reference impl)
- Update workflow overview steps to reference new script paths
- Keep the ABSOLUTE PROHIBITION section (path: `java/src/generated/java/`)
- Update `update-copilot-dependency.yml` reference (it exists in monorepo already)
- PR branch naming: `copilot/reference-impl-java` (distinguish from other SDKs)

---

## Task 4: Create `.github/prompts/java-coding-agent-merge-reference-impl-instructions.md`

**Source**: `copilot-sdk-java-00/.github/prompts/coding-agent-merge-reference-impl-instructions.md`

**Changes required**:
- Update prompt path reference: `.github/prompts/java-agentic-merge-reference-impl.prompt.md`
- Update script paths: `.github/scripts/java-reference-impl-sync/`
- Keep the same instructions about not creating a new PR
- Keep the ABSOLUTE PROHIBITION section (path: `java/src/generated/java/`)

---

## Task 5: Create `.github/skills/java-agentic-merge-reference-impl/SKILL.md`

**Source**: `copilot-sdk-java-00/.github/skills/agentic-merge-reference-impl/SKILL.md`

**Content** (minimal reference pointing to prompt):
```markdown
---
name: java-agentic-merge-reference-impl
description: Merge reference implementation changes from the monorepo's .NET/Node SDKs into the Java SDK.
license: MIT
---

Follow instructions in the [java-agentic-merge-reference-impl prompt](../../prompts/java-agentic-merge-reference-impl.prompt.md) to merge reference implementation changes into the Java SDK.
```

---

## Task 6: Migrate `commit-as-pull-request` Skill

Check if the monorepo already has a `commit-as-pull-request` skill. If not, create one.

### 6.1 Create `.github/skills/commit-as-pull-request/SKILL.md`

**Source**: `copilot-sdk-java-00/.github/skills/commit-as-pull-request/SKILL.md`

### 6.2 Create `.github/prompts/commit-as-pull-request.prompt.md`

**Source**: `copilot-sdk-java-00/.github/prompts/commit-as-pull-request.prompt.md`

**Changes required**:
- Script paths: `.github/scripts/ci/` → `.github/scripts/java-ci/` (or create shared CI scripts)
- Build verification: `cd java && mvn clean verify`
- The scripts (`parse-repo-info.sh`, `commit-and-push.sh`, `sync-after-merge.sh`) need to be created under `.github/scripts/java-ci/`

### 6.3 Create helper scripts under `.github/scripts/java-ci/`

Copy from standalone's `.github/scripts/ci/`:
- `parse-repo-info.sh` (generic, can be shared)
- `commit-and-push.sh` (adapt for monorepo — formatter runs in `java/`)
- `sync-after-merge.sh` (generic, can be shared)

---

## Task 7: Update `sdk-consistency-review.md`

**File**: `.github/workflows/sdk-consistency-review.md`

**Changes**:
1. Add `'java/**'` to the `paths:` list in the frontmatter
2. In the body, update the description from "four SDK implementations" to "five SDK implementations (Node.js/TypeScript, Python, Go, .NET, and Java)"
3. Add Java to any language lists in the review instructions

After editing, **recompile**:
```bash
gh aw compile sdk-consistency-review
```

---

## Task 8: Update `issue-triage.md`

**File**: `.github/workflows/issue-triage.md`

**Changes**:
1. Add `sdk/java` to the `allowed:` labels list in `safe-outputs.add-labels`
2. In the body, add Java to the list of SDK languages the agent should recognize
3. Add `sdk/java` label description in the body

After editing, **recompile**:
```bash
gh aw compile issue-triage
```

---

## Task 9: Verify `copilot-instructions.md` Has Java Content

**File**: `.github/copilot-instructions.md`

Verify the monorepo's copilot-instructions.md already includes Java-specific guidance (this should have been done in Phase 05). If not present, add a Java section with:
- Build commands (`cd java && mvn clean verify`)
- Testing note (use `mvn verify` without `-q`)
- Code style (Spotless, 4-space indent)
- Generated code prohibition (`java/src/generated/java/`)

---

## Task 10: Verify `.lastmerge` Semantics

The `java/.lastmerge` file currently contains `f4d22d70016c377881d86e4c77f8a3f93746ffae` — this is a commit SHA from the old external `github/copilot-sdk` repo.

**Action needed**: Update `java/.lastmerge` to contain the current monorepo HEAD SHA (or the SHA at which the Java code was last confirmed in sync). This is the "pivot point" — after this, all future `.lastmerge` values will be monorepo SHAs.

To determine the correct SHA: find the monorepo commit that corresponds to the last sync. Since Phase 01 copied everything in, the commit that completed Phase 01 (or the current HEAD of the branch you're working on) is the correct value.

```bash
# Set .lastmerge to current HEAD (the Java code is in sync as of right now)
git rev-parse HEAD > java/.lastmerge
```

---

## Compilation and Verification

After all files are created/modified:

1. **Compile all agentic workflows that were modified**:
   ```bash
   gh aw compile java-reference-impl-sync
   gh aw compile sdk-consistency-review
   gh aw compile issue-triage
   ```

2. **Verify lock files were generated**:
   ```bash
   ls .github/workflows/java-reference-impl-sync.lock.yml
   ls .github/workflows/sdk-consistency-review.lock.yml
   ls .github/workflows/issue-triage.lock.yml
   ```

3. **Run verify-compiled workflow logic locally** (if possible):
   ```bash
   # The verify-compiled.yml workflow checks that .lock.yml matches .md
   # After compilation, both should be in sync
   ```

4. **Sanity-check the scripts are executable**:
   ```bash
   chmod +x .github/scripts/java-reference-impl-sync/*.sh
   chmod +x .github/scripts/java-ci/*.sh
   ```

5. **Verify Java still builds**:
   ```bash
   cd java && mvn clean verify
   ```

---

## File Summary

### Files to CREATE:

| File | Based On |
|------|----------|
| `.github/scripts/java-reference-impl-sync/merge-reference-impl-start.sh` | standalone `merge-reference-impl-start.sh` |
| `.github/scripts/java-reference-impl-sync/merge-reference-impl-diff.sh` | standalone `merge-reference-impl-diff.sh` |
| `.github/scripts/java-reference-impl-sync/merge-reference-impl-finish.sh` | standalone `merge-reference-impl-finish.sh` |
| `.github/scripts/java-reference-impl-sync/sync-cli-version-from-reference-impl.sh` | standalone `sync-cli-version-from-reference-impl.sh` |
| `.github/scripts/java-reference-impl-sync/sync-codegen-version.sh` | standalone `sync-codegen-version.sh` |
| `.github/scripts/java-reference-impl-sync/format-and-test.sh` | standalone `format-and-test.sh` |
| `.github/workflows/java-reference-impl-sync.md` | standalone `reference-impl-sync.md` |
| `.github/workflows/java-reference-impl-sync.lock.yml` | AUTO-GENERATED by `gh aw compile` |
| `.github/prompts/java-agentic-merge-reference-impl.prompt.md` | standalone `agentic-merge-reference-impl.prompt.md` |
| `.github/prompts/java-coding-agent-merge-reference-impl-instructions.md` | standalone `coding-agent-merge-reference-impl-instructions.md` |
| `.github/skills/java-agentic-merge-reference-impl/SKILL.md` | standalone `agentic-merge-reference-impl/SKILL.md` |
| `.github/skills/commit-as-pull-request/SKILL.md` | standalone `commit-as-pull-request/SKILL.md` |
| `.github/prompts/commit-as-pull-request.prompt.md` | standalone `commit-as-pull-request.prompt.md` |
| `.github/scripts/java-ci/parse-repo-info.sh` | standalone `ci/parse-repo-info.sh` |
| `.github/scripts/java-ci/commit-and-push.sh` | standalone `ci/commit-and-push.sh` |
| `.github/scripts/java-ci/sync-after-merge.sh` | standalone `ci/sync-after-merge.sh` |

### Files to MODIFY:

| File | Change |
|------|--------|
| `.github/workflows/sdk-consistency-review.md` | Add `java/**` to paths, update language list |
| `.github/workflows/sdk-consistency-review.lock.yml` | AUTO-REGENERATED by `gh aw compile` |
| `.github/workflows/issue-triage.md` | Add `sdk/java` label |
| `.github/workflows/issue-triage.lock.yml` | AUTO-REGENERATED by `gh aw compile` |
| `java/.lastmerge` | Update to monorepo HEAD SHA (pivot point) |

### Files NOT to touch:

- `java/src/generated/java/**` — auto-generated, forbidden
- `.github/workflows/*.lock.yml` — only regenerated via `gh aw compile`
- `java/pom.xml` — no changes needed (property already exists)

---

## Acceptance Criteria

1. `gh aw compile java-reference-impl-sync` succeeds and produces a `.lock.yml`
2. `gh aw compile sdk-consistency-review` succeeds
3. `gh aw compile issue-triage` succeeds
4. All scripts under `.github/scripts/java-reference-impl-sync/` are executable and have correct shebang lines
5. `java/.lastmerge` contains a valid monorepo commit SHA
6. The `java-reference-impl-sync.md` workflow has `workflow_dispatch` as its ONLY trigger (no schedule)
7. The prompt file references correct monorepo paths (all Java paths prefixed with `java/`)
8. `cd java && mvn clean verify` still passes
9. No references to `https://github.com/github/copilot-sdk.git` remain in any newly created file (the sync is intra-repo now)
10. The `.merge-env` file path is documented and git-ignored (check `.gitignore`)
