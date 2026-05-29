# Phase 03: Publish Workflows — Agent Prompt

## Context

You are working in `copilot-sdk-00`, a local clone of `https://github.com/github/copilot-sdk`. The Java SDK source repository is at `../copilot-sdk-java-00` (a local clone of `https://github.com/github/copilot-sdk-java`).

You are implementing one phase of the work to make it so the `copilot-sdk/java` directory is the new home for what is currently `copilot-sdk-java`, with all source code, workflows and maintenance affordances migrated.

Phase 0 (pre-flight), Phase 1 (copy source code), and Phase 2 (CI workflows) are already complete. The Java source code is already present under `java/` in this repository, `mvn clean verify` passes from that directory, and CI test workflows are operational.

## First Step — Read the Master Plan

Before doing any work, read the file:

```
80-java-monorepo-add-01-remove-before-merge/dd-2989727-move-java-to-monorepo-plan.md
```

This contains the full migration plan. Focus on the "Phase 03: Publish Workflows" section, but also review the naming conventions in §3 and the workflow inventory tables in §5 for context.

**You are executing Phase 03 only. Do NOT perform any other phases (Phase 0, 1, 2, 4, 5, 6, or 7).**

## Working Branch and Commits

You are safe to do all work on the current topic branch. Make fine-grained commits with clear, descriptive commit log messages (e.g., "Fix java-publish-maven.yml to use java/ working directory", "Add java-smoke-test.yml adapted from run-smoke-test.yml"). Do not squash — keep commits granular so they are easy to review.

## Phase 03 Goal

Java can be independently published from the monorepo. This phase creates/updates 5 things:

1. Fix and complete `.github/workflows/java-publish-maven.yml`
2. Fix and complete `.github/workflows/java-publish-snapshot.yml`
3. Create `.github/workflows/java-smoke-test.yml`
4. Migrate `notes.template` to `.github/workflows/notes.template`
5. Migrate `.github/scripts/release/update-changelog.sh` (and its test script)

---

## Task 1: Fix `.github/workflows/java-publish-maven.yml`

**Source to compare against:** `../copilot-sdk-java-00/.github/workflows/publish-maven.yml`

**Current state:** A `java-publish-maven.yml` already exists in the monorepo but has critical bugs — it is missing `working-directory` settings and has file paths that assume the repo root is the Java project root.

### What Must Be Fixed

The existing workflow references `README.md`, `jbang-example.java`, `CHANGELOG.md`, `.lastmerge`, `src/site/markdown/cookbook/`, and runs Maven commands **without** setting a working directory. In the monorepo, all Java files live under `java/`. You must:

1. **Add a `defaults.run.working-directory`** at the job level:
   ```yaml
   defaults:
     run:
       shell: bash
       working-directory: ./java
   ```
   Apply this to **both** the `publish-maven` job and the `github-release` job.

2. **Update file path references in shell scripts** within the `update-docs` step:
   - `.lastmerge` → already correct (relative to working-directory `./java`)
   - `README.md` → already correct (relative to working-directory)
   - `jbang-example.java` → already correct (relative to working-directory)
   - `CHANGELOG.md` → already correct (relative to working-directory)
   - `src/site/markdown/cookbook/` → already correct (relative to working-directory)
   - `./.github/scripts/release/update-changelog.sh` → This path is relative to repo root. Since working-directory is `./java`, change to `../.github/scripts/release/update-changelog.sh` **OR** use an absolute reference `$GITHUB_WORKSPACE/.github/scripts/release/update-changelog.sh`.

3. **The `git add` command** in the `update-docs` step currently does `git add CHANGELOG.md README.md jbang-example.java src/site/markdown/cookbook/`. Because git operates on paths relative to the repo root regardless of shell `cwd`, these must be prefixed with `java/`:
   ```bash
   git add java/CHANGELOG.md java/README.md java/jbang-example.java java/src/site/markdown/cookbook/
   ```

4. **The `notes.template` reference** in the `github-release` job: `envsubst < .github/workflows/notes.template` — this references a file relative to the repo root. With `working-directory: ./java` set, change to `envsubst < $GITHUB_WORKSPACE/.github/workflows/notes.template` or `envsubst < ../.github/workflows/notes.template`.

5. **The `setup-copilot` action reference**: `uses: ./.github/actions/setup-copilot` is fine — `uses:` paths are always relative to the repo root, regardless of `working-directory`.

6. **Secrets naming**: Verify the workflow uses `JAVA_`-prefixed secrets per the plan:
   - `secrets.JAVA_RELEASE_TOKEN` (for checkout token)
   - `secrets.JAVA_GPG_SECRET_KEY`
   - `secrets.JAVA_GPG_PASSPHRASE`
   - `secrets.JAVA_MAVEN_CENTRAL_USERNAME`
   - `secrets.JAVA_MAVEN_CENTRAL_PASSWORD`
   These should already be correct in the existing file but verify.

7. **The `deploy-site` job** at the end triggers `deploy-site.yml`. Since `java-deploy-site.yml` is struck through in the plan (not being migrated), **remove the `deploy-site` job entirely** from this workflow. Site deployment will be handled separately if/when needed.

### Validation

After making changes, mentally trace through each step:
- Does `mvn help:evaluate` run in `./java`? ✅ (via defaults.run.working-directory)
- Does `cat .lastmerge` find `java/.lastmerge`? ✅ (relative to working-directory)
- Does `git add java/CHANGELOG.md` work from `./java` cwd? ✅ (git paths are repo-root-relative)
- Does `mvn -B release:prepare` find `java/pom.xml`? ✅ (via working-directory)

---

## Task 2: Fix `.github/workflows/java-publish-snapshot.yml`

**Source to compare against:** `../copilot-sdk-java-00/.github/workflows/publish-snapshot.yml`

**Current state:** A `java-publish-snapshot.yml` already exists but is missing the `working-directory` setting.

### What Must Be Fixed

1. **Add `defaults.run.working-directory: ./java`** at the job level (same pattern as Task 1).

2. **The `setup-copilot` action reference**: `uses: ./.github/actions/setup-copilot` — this is fine (always repo-root-relative).

3. **Maven commands** (`mvn help:evaluate`, `mvn -B deploy`) — will automatically use `./java` as working directory once defaults are set.

4. **Secrets naming**: Verify uses `JAVA_MAVEN_CENTRAL_USERNAME` and `JAVA_MAVEN_CENTRAL_PASSWORD`.

This fix is straightforward — essentially just adding the `defaults` block.

---

## Task 3: Create `.github/workflows/java-smoke-test.yml`

**Source to adapt from:** `../copilot-sdk-java-00/.github/workflows/run-smoke-test.yml`

**Reference for monorepo patterns:** `.github/workflows/java-sdk-tests.yml` (for trigger/defaults structure)

### Requirements

- **Name:** `Run Java smoke test`

- **Triggers:**
  - `workflow_dispatch`
  - `workflow_call` with `secrets: COPILOT_GITHUB_TOKEN: required: true`

- **Permissions:** `contents: read`

- **Jobs:** Two jobs, same as the source:
  - `smoke-test-jdk17` — JDK 17 smoke test
  - `smoke-test-java25` — JDK 25 smoke test

- **Both jobs must have:**
  ```yaml
  defaults:
    run:
      shell: bash
      working-directory: ./java
  ```

- **Condition:** `if: github.ref == 'refs/heads/main'` on both jobs (same as source)

- **Steps for each job (adapt from source):**
  1. `actions/checkout` (standard, no special options needed)
  2. `actions/setup-java` with appropriate JDK version (`17` or `25`, `microsoft` distribution, `maven` cache)
  3. `uses: ./.github/actions/setup-copilot` (monorepo's setup-copilot installs Copilot CLI via nodejs/)
  4. "Build SDK and install to local repo" — `mvn -DskipTests -Pskip-test-harness clean install`
  5. "Create and run smoke test via Copilot CLI" — adapt the `copilot --yolo` step from the source. **Critical change:** the prompt text says "You are running inside the copilot-sdk-java repository" — change to "You are running inside the copilot-sdk monorepo, in the java/ subdirectory."
  6. "Run smoke test jar" — `cd smoke-test && java -jar ./target/copilot-sdk-smoketest-1.0-SNAPSHOT.jar`

- **Important adaptation for monorepo:** The `copilot --yolo` prompt references `src/test/prompts/PROMPT-smoke-test.md`. Since the working directory is already `./java`, this path remains correct (the file exists at `java/src/test/prompts/PROMPT-smoke-test.md` in the monorepo).

- **The `cd smoke-test` step:** With `working-directory: ./java`, the smoke-test directory will be created at `java/smoke-test/`. The `cd smoke-test` in the "Run smoke test jar" step works relative to working-directory, so no path change needed.

- **Do NOT change** the detailed prompt text content (the SNAPSHOT override instructions, the JDK 25 virtual threads instructions) — those are carefully crafted and correct. Only change the repository description line.

---

## Task 4: Migrate `notes.template`

**Source:** `../copilot-sdk-java-00/.github/workflows/notes.template`

**Destination:** `.github/workflows/notes.template`

Copy the file verbatim. The template uses `${VERSION}`, `${GROUP_ID}`, and `${ARTIFACT_ID}` which are substituted via `envsubst` in the `java-publish-maven.yml` workflow.

No content changes needed — the template is generic enough for the monorepo context. The documentation URLs reference `github.github.io/copilot-sdk-java/` which is still the correct Pages domain for the Java SDK docs (Pages deployment is separate from where the source code lives).

---

## Task 5: Migrate Release Scripts

**Source:** `../copilot-sdk-java-00/.github/scripts/release/`

**Destination:** `.github/scripts/release/`

### Files to copy:
1. `update-changelog.sh` — Copy verbatim. This script is called from `java-publish-maven.yml` at path `./.github/scripts/release/update-changelog.sh` (repo-root-relative).
2. `test-update-changelog.sh` — Copy verbatim. This is the test for the changelog script.

### What to verify after copying:
- The `update-changelog.sh` script uses `${CHANGELOG_FILE:-CHANGELOG.md}` — since the workflow sets `working-directory: ./java`, the script will operate on `java/CHANGELOG.md`. This is correct.
- The script references `https://github.com/github/copilot-sdk/commit/` in its reference-impl-sync URL generation — this is correct for the monorepo context (the Java SDK's `.lastmerge` already stores monorepo commit SHAs after Phase 1).

### Make scripts executable:
After copying, ensure both scripts have the execute bit set:
```bash
chmod +x .github/scripts/release/update-changelog.sh
chmod +x .github/scripts/release/test-update-changelog.sh
```

---

## Checking Your Work

After completing all tasks, perform the following verification for each workflow file:

### For `java-publish-maven.yml` and `java-publish-snapshot.yml`:

Compare the monorepo version against the standalone version (`../copilot-sdk-java-00/.github/workflows/publish-maven.yml` and `../copilot-sdk-java-00/.github/workflows/publish-snapshot.yml`).

Grade yourself on this rubric (you need an A in each category):

1. **Presence/absence**: What steps exist in one but not the other? For any removed step, identify what downstream steps relied on its side effects (e.g., compiled classes, cloned repos, installed packages, env vars set).

2. **Ordering dependencies**: For each step in the monorepo workflow, state what preconditions it assumes (files on disk, compiled artifacts, environment state). Verify that a prior step actually establishes each precondition. Flag any case where a precondition was satisfied in the standalone workflow by a step that's missing or reordered in the monorepo version.

3. **Semantic equivalence**: Where both workflows have a step with the same command (e.g., `mvn -B release:prepare`), confirm it will behave identically given the different prior steps. If a command's behavior depends on prior state (e.g., whether classes exist), flag the difference.

4. **Configuration drift**: Triggers, permissions, action versions/pins, env vars, working directories, matrix strategy.

For any discrepancy found, classify it as:
- (a) intentional adaptation for monorepo context
- (b) potential bug
- (c) needs clarification

### For `java-smoke-test.yml`:

Compare against `../copilot-sdk-java-00/.github/workflows/run-smoke-test.yml` using the same rubric above.

### Known intentional differences (do NOT flag as bugs):

- Secret names changed to `JAVA_` prefix (intentional for monorepo multi-language secret isolation)
- `uses: ./.github/actions/setup-copilot` in monorepo installs CLI via `nodejs/node_modules/@github/copilot/index.js` instead of via a version read from `pom.xml`. This is intentional — the monorepo's setup-copilot uses the Node.js SDK's pinned version as the single source of truth.
- `working-directory: ./java` added (intentional monorepo adaptation)
- `deploy-site` job removed from publish workflow (intentional — struck through in plan)
- Prompt text updated from "copilot-sdk-java repository" to "copilot-sdk monorepo, in the java/ subdirectory" (intentional)

---

## Summary of Expected Commits

1. `Fix java-publish-maven.yml: add working-directory and fix paths for monorepo`
2. `Fix java-publish-snapshot.yml: add working-directory for monorepo`
3. `Add java-smoke-test.yml adapted from run-smoke-test.yml`
4. `Add notes.template for Java release notes`
5. `Add release scripts (update-changelog.sh) for Java publishing`

---

## If You Get Stuck

If a path reference is ambiguous or you're unsure whether `working-directory` affects a particular command (e.g., `git` commands, `uses:` action paths, `envsubst` with file paths), test your understanding against these rules:

- `uses:` (composite action references) — **always repo-root-relative**, unaffected by working-directory
- `run:` shell commands — **affected by working-directory** (the shell `cwd` is set)
- `git add/commit/push` — **paths in git commands are repo-root-relative** regardless of shell cwd
- `$GITHUB_WORKSPACE` — always the repo root checkout path

If something is truly unclear, leave a `# TODO: verify this path in monorepo context` comment and move on.
