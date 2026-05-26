# DD-2989727: Move Java SDK into copilot-sdk Monorepo — Plan

## Overall goal

The software that currently resides in https://github.com/github/copilot-sdk-java (the standalone repo) will be merged into https://github.com/github/copilot-sdk (the monorepo) such that the Java specific aspects will reside entirely within the `java` directory and as much of the supporting infrastructure will reside in its natural home, but with `java-` file name prefixes wherever possible.

## Table of Contents

1. [Migration Plan — Phases](#1-migration-plan--phases)
2. [Permissions and Secrets Challenges](#2-permissions-and-secrets-challenges)
3. [Naming Convention Proposal](#3-naming-convention-proposal)
4. [Current Language Separation Assessment](#4-current-language-separation-assessment)5. [Workflow Inventory Tables](#5-workflow-inventory-tables)
5. [Agents, Skills, Prompts, and Supporting Resources Inventory](#6-agents-skills-prompts-and-supporting-resources-inventory)
6. [Pitfalls and Risk Register](#7-pitfalls-and-risk-register)
7. [Post-Migration Verification Checklist](#8-post-migration-verification-checklist)

- [Appendix A: Files to Copy vs. Merge vs. Delete](#appendix-a-files-to-copy-vs-merge-vs-delete)
- [Appendix B: Unique Java Concerns vs Other Languages](#appendix-b-unique-java-concerns-vs-other-languages)
- [Appendix C: Java Smoketest](#appendix-c-java-smoketest)

---

## 1. Migration Plan — Phases

### Phase 00: ✅ Pre-Flight (Before Writing Any Code)

- [✅] **Provision secrets** in `github/copilot-sdk` (see §2A) See https://github.com/github/copilot-sdk-partners/issues/90
- [✅] **Verify CODEOWNERS team** access. See https://github.com/github/copilot-sdk-partners/issues/89
- [✅] **Check Maven Central Trusted Publisher** — can `github/copilot-sdk` publish to `com.github:copilot-sdk-java`? See
- [✅] **Check GitHub Pages** — is it enabled? Can Java docs coexist? See https://github.com/github/copilot-sdk-partners/issues/85
- [✅ ] **Confirm branch protection** — will new required status checks be accepted? See https://github.com/github/copilot-sdk-partners/issues/95 .
- [✅] **Create tracking issue** in `github/copilot-sdk` for this migration. See https://github.co/github/copilot-sdk-partners/issues/80
- [✅] **Define drift-management policy** — instead of a hard freeze, adopt a manual forward-port policy:
  1. Reduce `reference-impl-sync` schedule in `copilot-sdk-java` to weekly (Fridays only)
  2. Perform Phase 1 copy early in the week, right after verifying `copilot-sdk-java` main is clean
  3. After each Friday sync lands, forward-port the diff into `copilot-sdk/java/` (`git diff PREV..NEW` in java repo, applied to monorepo)
  4. Once Phase 1 merges and the monorepo is the source of truth, disable the sync workflow in `copilot-sdk-java` entirely
  - **Rationale:** A hard freeze is unnecessary because (a) there is a single human committer, (b) the only automated commit source is the reference-impl-sync workflow whose schedule is controllable, and (c) any drift is trivially detectable via `git log`. The one constraint: do not trigger a sync while a Phase 1 PR is under active review.

### Phase 01: ✅ Copy Source Code (No Workflows Yet)

**Goal**: Get all Java source code building and testing in the monorepo without any CI/CD.

1. Copy `copilot-sdk-java-00/` contents into `copilot-sdk-00/java/`:
   - `src/` (main, test, generated, site)
   - `pom.xml`
   - `config/` (checkstyle, spotbugs)
   - `scripts/codegen/` → merge `java.ts` into `copilot-sdk-00/scripts/codegen/`
   - `CHANGELOG.md`, `README.md`, `jbang-example.java`
   - `.lastmerge` → `java/.lastmerge`
   - ✅ ~~`.githooks/` → `java/.githooks/`~~
   - `docs/adr/` → `java/docs/adr/`
   - ✅ ~~`instructions/copilot-sdk-java.instructions.md` → `.github/skills/java-coding-skill/SKILL.md` (follows the `rust-coding-skill` pattern); Java repo governance merged into `.github/copilot-instructions.md`~~

2. Update `pom.xml` paths if needed (should be self-contained under `java/`).

3. Verify `mvn clean verify` works from `java/` directory locally. Make necessary changes so the test infrastructure is copied locally, rather than checked out.

### ✅ Phase 02: CI Workflows

**Goal**: Java CI runs on PRs and main pushes within the monorepo.

1. Create `java-sdk-tests.yml` (adapted from `build-test.yml`):
   - Path triggers: `java/**`, `test/**`, `.github/workflows/java-sdk-tests.yml`
   - Uses monorepo's `setup-copilot` action (or create `java/setup-copilot` action)
   - Runs on 3 OS matrix (match other SDKs)

2. Merge Java into `codegen-check.yml`:
   - Add `java/src/generated/**` to path triggers
   - Add a job that runs Java codegen and diffs

3. Create `java-codegen-agentic-fix.md` (adapted from `codegen-agentic-fix.md`):
   - Update paths, remove cross-repo references
   - Compile with `gh aw compile`

4. Merge Java into `copilot-setup-steps.yml`:
   - Add JDK 17 setup step
   - Add Maven cache

5. Update `dependabot.yaml`:
   - Add Maven ecosystem entry for `/java`

### ✅ Phase 03: Publish Workflows

**Goal**: Java can be independently published from the monorepo.

1. Create `java-publish.yml` (adapted from `publish-maven.yml`):
   - All paths updated to `java/` prefix
   - Working directory set to `java/`
   - Uses monorepo secrets
   - **Independent trigger** — not part of the unified `publish.yml`

2. Create `java-publish-snapshot.yml` (adapted from `publish-snapshot.yml`):
   - Similar path/directory updates

3. ~~Create `java-deploy-site.yml` (adapted from `deploy-site.yml`):~~
   ~~- Adjust GitHub Pages setup for coexistence~~
   ~~- May need a subdirectory deployment strategy~~

4. Create `java-smoke-test.yml` (adapted from `run-smoke-test.yml`).

5. Migrate `notes.template` to `java/.github/notes.template` or similar.

### Phase 04: Agentic Workflows and Skills

**Goal**: Agentic automation works for Java within the monorepo.

1. **`reference-impl-sync`** → **`java-reference-impl-sync.md`** — **REWORK** for intra-repo operation:
   - **Trigger**: `schedule` (daily) + `workflow_dispatch` (same as today)
   - **Behavior change**: Instead of cloning `github/copilot-sdk` and comparing commits, it:
     1. Reads `java/.lastmerge` (now a monorepo commit SHA)
     2. Runs `git log <lastmerge-sha>..HEAD -- dotnet/src/ nodejs/src/` to find new reference-impl changes
     3. If changes exist → creates an issue assigned to Copilot agent (same as today)
     4. If no changes → closes stale sync issues (same as today)
   - **Key simplification**: No cross-repo clone, no remote URL handling, no token for external repo access
   - **Compile**: `gh aw compile java-reference-impl-sync.md`

2. **`agentic-merge-reference-impl` skill** — **REWORK** for intra-repo operation:
   - **Current behavior**: Clones `github/copilot-sdk`, checks out the target commit, computes a diff of `dotnet/src/` and `nodejs/src/` against the Java repo's `.lastmerge`, then applies equivalent Java changes.
   - **New behavior**:
     1. Reads `java/.lastmerge` to get the base commit SHA
     2. Computes `git diff <base-sha>..HEAD -- dotnet/src/ nodejs/src/` (all local, no clone needed)
     3. Analyzes the diff to identify what changed semantically (new methods, renamed types, new events, etc.)
     4. Applies equivalent idiomatic Java changes under `java/src/`
     5. Runs `mvn verify` from `java/` to validate
     6. Updates `java/.lastmerge` to the current HEAD SHA
     7. Commits and pushes (via `commit-as-pull-request` skill or direct push)
   - **Scripts to update**: `.github/scripts/reference-impl-sync/` — all 5 scripts assume cross-repo operation:
     - `merge-reference-impl-start.sh` — remove `git clone`, replace with local `git diff`
     - `merge-reference-impl-diff.sh` — simplify to intra-repo diff
     - `merge-reference-impl-finish.sh` — update `java/.lastmerge` with monorepo SHA
     - `sync-cli-version-from-reference-impl.sh` — now reads from local `nodejs/package.json` directly
     - `sync-codegen-version.sh` — now reads from local `scripts/codegen/package.json`
   - **Prompt files to update**:
     - `.github/prompts/agentic-merge-reference-impl.prompt.md` — remove cross-repo instructions, add intra-repo paths
     - `.github/prompts/coding-agent-merge-reference-impl-instructions.md` — same
   - **SKILL.md** — update with new paths and simplified flow
   - Restore the updating of the `readonly-copilot-sdk-ref-impl-version-from-lastmerge-file-updated-by-reference-impl-sync` POM property:
     pin to. It is updated automatically by
     .github/scripts/reference-impl-sync/sync-cli-version-from-reference-impl.sh

3. **`sdk-consistency-review`** — Update:
   - Add `java/**` to path triggers in the `.md` frontmatter
   - Update agent prompt to include Java in the list of SDKs to review

4. **`issue-triage`** — Update:
   - Add `sdk/java` label to the list of per-SDK labels

5. Merge `agentic-workflows.agent.md` — use the monorepo's (newer) version, no action needed.

6. Migrate `documentation-coverage` skill to monorepo's skills directory (as `java-documentation-coverage`).

7. Migrate `commit-as-pull-request` skill (check if monorepo already has equivalent).

### ⌛ Phase 05: Cross-Cutting Updates

1. ✅ Update monorepo `copilot-instructions.md` to include Java section. See https://github.com/github/copilot-sdk/issues/1390 .
2. ✅ Update monorepo `README.md` to list Java as a supported language. See https://github.com/github/awesome-copilot/pull/1811
3. ✅ Update `scenario-builds.yml` to include Java scenarios (if applicable).
4. Handle the package rename requested by Steve Sanderson See https://github.com/github/copilot-sdk-partners/issues/115
5. Update `docs-validation.yml` to include Java code snippets.
6. Update `lsp.json` to add Java LSP config (optional).
7. Add Java to `docs/` getting-started and feature pages.
8. Update `sdk-protocol-version.json` if Java needs it.

### Phase 06: Cutover and Cleanup

1. **Disable CI** in `copilot-sdk-java` (remove or disable workflows).
2. **Archive** `copilot-sdk-java` repo (make read-only).
3. **Update external references**:
   - Maven Central POM `<scm>` URLs
   - README badges pointing to the new repo
   - Javadoc.io configuration
   - Any links in copilot documentation
4. **Remove duplicate resources** that were merged rather than moved.
5. **Run full CI** in monorepo to validate everything.

### Phase 07: Preserve authorship of commits for `pom.xml` and `.java` files

Let's assume

- the only types of files where we want commit authorship preserved are `.java` and `pom.xml` files.

- those types of files in the standalone repo are kept in sync with those types of files in the monorepo so that, during the migration, those types of files in the standalone repo are the "source of truth" for those types of files.

Since the file contents are **identical** at merge time, git will resolve the merge cleanly (both sides "add" the same file with the same content = no conflict). The workflow:

1. Finish all migration work, get PR merged, everything working
2. Then as the final step:

```powershell
# Fresh clone of standalone, filter to only .java and pom.xml, remap to java/
git clone https://github.com/github/copilot-sdk-java /tmp/java-history
cd /tmp/java-history
git filter-repo --path-glob '*.java' --path-glob '**/pom.xml' --to-subdirectory-filter java

# In the monorepo, merge the rewritten history
cd E:\workareas\copilot-sdk-00
git remote add java-history /tmp/java-history
git fetch java-history
git merge java-history/main --allow-unrelated-histories -m "Preserve Java commit history from standalone repo"
git remote remove java-history
```

Because content is identical at merge time, this produces a clean merge commit. After that, `git log --follow java/src/main/java/.../CopilotClient.java` traces back to the original authors.

**One caveat**: if any `.java` or pom.xml files diverge between repos before you do this step, you'll get conflicts on those files. As long as you keep them in sync (your assumption #2), it's safe to do last.

---

## 2. Permissions and Secrets Challenges

### 2A. Secrets That Must Be Provisioned in copilot-sdk

The Java SDK publish workflow requires secrets that **do not currently exist** in the `copilot-sdk` repo:

| Old Secret               | Old Used By                                      | New Secret                    | New Used By                                           | Notes                                                                          |
| ------------------------ | ------------------------------------------------ | ----------------------------- | ----------------------------------------------------- | ------------------------------------------------------------------------------ |
| `RELEASE_TOKEN`          | `publish-maven.yml`                              | `JAVA_RELEASE_TOKEN`          | `java-publish-maven.yml`                              | PAT with `contents:write` for pushing tags/commits during maven-release-plugin |
| `GPG_SECRET_KEY`         | `publish-maven.yml`                              | `JAVA_GPG_SECRET_KEY`         | `java-publish-maven.yml`                              | GPG private key for signing Maven artifacts                                    |
| `GPG_PASSPHRASE`         | `publish-maven.yml`                              | `JAVA_GPG_PASSPHRASE`         | `java-publish-maven.yml`                              | Passphrase for the GPG key                                                     |
| `MAVEN_CENTRAL_USERNAME` | `publish-maven.yml`, `publish-snapshot.yml`      | `JAVA_MAVEN_CENTRAL_USERNAME` | `java-publish-maven.yml`, `java-publish-snapshot.yml` | Sonatype/Maven Central credentials                                             |
| `MAVEN_CENTRAL_PASSWORD` | `publish-maven.yml`, `publish-snapshot.yml`      | `JAVA_MAVEN_CENTRAL_PASSWORD` | `java-publish-maven.yml`, `java-publish-snapshot.yml` | Sonatype/Maven Central credentials                                             |
| `COPILOT_GITHUB_TOKEN`   | `build-test.yml`, `codegen-agentic-fix.lock.yml` | unchanged                     |                                                       | Token for Copilot CLI in CI                                                    |

### 2B. Existing Secrets in copilot-sdk That May Conflict

| Secret                                                 | Used By              | Concern                                                      |
| ------------------------------------------------------ | -------------------- | ------------------------------------------------------------ |
| `CARGO_REGISTRY_TOKEN`                                 | `publish.yml` (Rust) | No conflict                                                  |
| `GH_AW_GITHUB_TOKEN` / `GH_AW_GITHUB_MCP_SERVER_TOKEN` | Agentic workflows    | Likely already present; Java agentic workflows need the same |

### 2C. Permissions / Access to Provision

- [✅] **Repository secrets**: File a ticket to add the 6 Java-specific secrets to `github/copilot-sdk`. See https://github.com/github/copilot-sdk-partners/issues/90
- [✅] **CODEOWNERS team**: ~~Ensure `@github/copilot-sdk-java` team has access to `github/copilot-sdk` and is added to CODEOWNERS for `java/**`.~~ See https://github.com/github/copilot-sdk-partners/issues/89 .
- [⌛] **Maven Central Trusted Publisher**: Currently configured for `github/copilot-sdk-java`. Must be updated to also allow publishing from `github/copilot-sdk` (or create a new namespace mapping). **This is the highest-risk permission issue** — Maven Central's Trusted Publisher setup ties the repository name to the publish flow. See https://github.com/github/copilot-sdk-partners/issues/91
- [✅] **GitHub Pages**: ~~If `deploy-site.yml` moves, check if GitHub Pages is enabled on the monorepo and whether Java docs can coexist with any existing docs deployment.~~ See https://github.com/github/copilot-sdk-partners/issues/85
- [⌛] **Branch protection**: Ensure `main` branch protection rules in copilot-sdk permit the Java CI workflows (merge queues, required status checks, etc.). See https://github.com/github/copilot-sdk-partners/issues/95 .
- [ ] **Copilot coding agent**: Ensure the agent is enabled for `github/copilot-sdk` and the `copilot-setup-steps.yml` is updated to include Java tooling.

---

## 3. Naming Convention Proposal

### Current State

The monorepo already uses a partially consistent pattern:

- **Test workflows**: `{language}-sdk-tests.yml` (e.g., `dotnet-sdk-tests.yml`, `go-sdk-tests.yml`)
- **Cross-language workflows**: descriptive kebab-case names (e.g., `codegen-check.yml`, `publish.yml`)
- **Agentic workflows**: descriptive kebab-case (e.g., `issue-triage.md`, `handle-bug.md`)

### Proposed Convention

**Use kebab-case throughout. Language-specific workflows start with the language name.**

#### Language-specific workflow naming: `{language}-{purpose}.yml`

| Current (copilot-sdk)  | Current (copilot-sdk-java)      | Proposed New Name                                                               |
| ---------------------- | ------------------------------- | ------------------------------------------------------------------------------- |
| `nodejs-sdk-tests.yml` | —                               | `nodejs-sdk-tests.yml` (keep)                                                   |
| `dotnet-sdk-tests.yml` | —                               | `dotnet-sdk-tests.yml` (keep)                                                   |
| `go-sdk-tests.yml`     | —                               | `go-sdk-tests.yml` (keep)                                                       |
| `python-sdk-tests.yml` | —                               | `python-sdk-tests.yml` (keep)                                                   |
| `rust-sdk-tests.yml`   | —                               | `rust-sdk-tests.yml` (keep)                                                     |
| —                      | `build-test.yml`                | **`java-sdk-tests.yml`**                                                        |
| —                      | `publish-maven.yml`             | **`java-publish.yml`**                                                          |
| —                      | `publish-snapshot.yml`          | **`java-publish-snapshot.yml`**                                                 |
| —                      | `deploy-site.yml`               | **`java-deploy-site.yml`**                                                      |
| —                      | `run-smoke-test.yml`            | **`java-smoke-test.yml`**                                                       |
| —                      | `codegen-check.yml`             | **Merge into existing `codegen-check.yml`** (add Java paths + job)              |
| —                      | `codegen-agentic-fix.md`        | **`java-codegen-fix.md`** + `.lock.yml`                                         |
| —                      | `reference-impl-sync.md`        | **`java-reference-impl-sync.md`** + `.lock.yml` (reworked for intra-repo)       |
| —                      | `update-copilot-dependency.yml` | **Merge into existing `update-copilot-dependency.yml`** (add Java codegen step) |
| —                      | `copilot-setup-steps.yml`       | **Merge into existing** (add JDK 17 + Maven setup)                              |
| —                      | `agentics-maintenance.yml`      | Already exists via gh-aw in the monorepo; **do not duplicate**                  |

#### Cross-language workflow naming: `{purpose}.yml` (no language prefix)

Keep existing names: `publish.yml`, `codegen-check.yml`, `scenario-builds.yml`, `docs-validation.yml`, etc.

#### Summary of naming rules

1. **Language-specific** workflows: `{language}-{purpose}.yml` / `.md`
2. **Cross-language** workflows: `{purpose}.yml` / `.md` (no prefix)
3. **Kebab-case** throughout (already the convention)
4. **Agentic workflows**: same pattern but with `.md` extension
5. **Lock files**: auto-generated, always `{name}.lock.yml`

---

## 4. Current Language Separation Assessment

### Are the languages in copilot-sdk-00 already sufficiently separated?

**Mostly yes, with a few cross-cutting concerns:**

#### Well-separated

- **Source code**: Each language lives in its own top-level directory (`nodejs/`, `python/`, `go/`, `dotnet/`, `rust/`). Java will go in `java/`.
- **Test workflows**: Each has its own `{language}-sdk-tests.yml` with path-scoped triggers (only fires on changes to that language's directory + `test/`).
- **Dependabot**: Already per-ecosystem, per-directory entries.

#### Cross-cutting concerns (potential friction points)

| Concern                                       | Current State                                                                   | Impact on Java                                                                                                                                                                                                                       |
| --------------------------------------------- | ------------------------------------------------------------------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------ |
| **Shared test harness** (`test/harness/`)     | Node.js-based replay proxy used by all E2E tests                                | Java already uses this (clones it at build time from `copilot-sdk` repo). When in-repo, can reference it directly — **simpler**.                                                                                                     |
| **Shared test snapshots** (`test/snapshots/`) | YAML snapshot files consumed by all languages                                   | Java can share these — **positive change**.                                                                                                                                                                                          |
| **Unified codegen** (`scripts/codegen/`)      | One `package.json` with generators for TS, C#, Python, Go, Rust                 | Java codegen (`java.ts`) must be **merged in**. The Java codegen currently has its own `package.json` with a direct `@github/copilot` dependency; the monorepo codegen gets it via `nodejs/node_modules`. This needs reconciliation. |
| **`justfile`**                                | Has per-language targets (`format-go`, `test-dotnet`, etc.)                     | Must add `format-java`, `lint-java`, `test-java`, `install-java` targets.                                                                                                                                                            |
| **Unified `publish.yml`**                     | Single workflow publishes all languages with one version number                 | **Java CANNOT join this** — Java has its own versioning scheme (`X.Y.Z-java.N`). Java must keep a separate `java-publish.yml`.                                                                                                       |
| **`sdk-consistency-review`** agentic workflow | Reviews PRs for cross-SDK parity (currently watches nodejs, python, go, dotnet) | Must add `java/` to the path triggers and update the agent prompt to include Java.                                                                                                                                                   |
| **`copilot-setup-steps.yml`**                 | Sets up Node, Python, Go, .NET, Rust                                            | Must add JDK 17 + Maven.                                                                                                                                                                                                             |
| **`copilot-instructions.md`**                 | Monorepo-wide instructions                                                      | Must incorporate Java-specific guidance.                                                                                                                                                                                             |
| **`CODEOWNERS`**                              | Single `* @github/copilot-sdk`                                                  | ~~Must add `java/ @github/copilot-sdk-java` line.~~                                                                                                                                                                                  |
| **`lsp.json`**                                | Configures C# and Go language servers for Copilot agent                         | May want to add Java LSP (jdtls or similar) — **optional**.                                                                                                                                                                          |

### The Big Question: `reference-impl-sync`

Currently, the Java SDK has a scheduled workflow that polls `github/copilot-sdk` for new commits and creates issues for the Copilot agent to port. **This workflow is still needed** when Java lives in the same repo — the primary maintainers of `dotnet/` and `nodejs/` are not Java experts, and changes to those SDKs still need to be detected and ported into `java/`.

What changes is the **mechanism**: instead of polling a remote repository, the workflow watches for commits that land on `main` touching `dotnet/src/` or `nodejs/src/` and compares against `java/.lastmerge` (which now stores a monorepo commit SHA rather than a cross-repo one).

**Recommendation**:

1. **Keep `java/.lastmerge`** — it stores the last monorepo commit SHA whose `dotnet/`/`nodejs/` changes have been ported into Java. This is the anchor for diffing.
2. **Keep `reference-impl-sync` as `java-reference-impl-sync.md`** — reworked for intra-repo operation (see §6 Phase 4 for details).
3. **Keep `agentic-merge-reference-impl` skill** — reworked so that instead of cloning a remote repo, it reads diffs from the local `dotnet/` and `nodejs/` directories relative to the SHA in `java/.lastmerge`.
4. The `sdk-consistency-review` workflow provides an additional safety net on PRs, but is **not a replacement** for the scheduled sync — it only fires on PRs, not when changes land on `main` without Java updates.

---

## 5. Workflow Inventory Tables

### 5A. copilot-sdk-java-00 Workflows (Source)

| YAML File Name                         | Brief Description                                                                                                           | Primary Language       | Complexity |
| -------------------------------------- | --------------------------------------------------------------------------------------------------------------------------- | ---------------------- | ---------- |
| `build-test.yml`                       | Main CI: Spotless, build, Javadoc, `mvn verify`, JaCoCo coverage badges                                                     | Java                   | L          |
| `codegen-check.yml`                    | Re-runs Java codegen, commits regenerated files to PRs, triggers agentic fix on failure                                     | Java                   | M          |
| `codegen-agentic-fix.md` + `.lock.yml` | Agentic: auto-fixes compilation/test failures caused by codegen changes                                                     | Java                   | L          |
| `reference-impl-sync.md` + `.lock.yml` | Agentic: checks for new commits in `github/copilot-sdk`, creates issue for Copilot agent to port                            | Java                   | L          |
| `publish-maven.yml`                    | Publishes release to Maven Central via `maven-release-plugin`, GPG signing, GitHub Release creation                         | Java                   | XL         |
| `publish-snapshot.yml`                 | Publishes SNAPSHOT builds to Maven Central Snapshots on a weekday schedule                                                  | Java                   | M          |
| `deploy-site.yml`                      | Builds/deploys versioned Maven site docs to GitHub Pages                                                                    | Java                   | M          |
| `run-smoke-test.yml`                   | Builds SDK, installs locally, runs Copilot CLI smoke test on JDK 17 + JDK 25 (see [Appendix C](#appendix-c-java-smoketest)) | Java                   | M          |
| `update-copilot-dependency.yml`        | Updates `@github/copilot` npm dep in codegen, re-runs generator, creates PR                                                 | Java                   | M          |
| `copilot-setup-steps.yml`              | Environment setup for Copilot coding agent (JDK 17, Node 22, gh-aw, pre-commit hooks)                                       | Java                   | S          |
| `agentics-maintenance.yml`             | Auto-generated gh-aw maintenance: closes expired discussions/issues/PRs                                                     | Cross-language (infra) | S          |
| `notes.template`                       | Release notes template for Maven Central (not a workflow)                                                                   | Java                   | S          |

### 5B. copilot-sdk-00 Workflows (Target Monorepo)

| YAML File Name                               | Brief Description                                                               | Primary Language       | Complexity |
| -------------------------------------------- | ------------------------------------------------------------------------------- | ---------------------- | ---------- |
| `nodejs-sdk-tests.yml`                       | Build + test Node.js SDK on 3 OS, prettier, ESLint, typecheck, E2E              | Node.js                | L          |
| `dotnet-sdk-tests.yml`                       | Build + test .NET SDK on 3 OS, format check, E2E via replay proxy               | .NET                   | L          |
| `go-sdk-tests.yml`                           | Build + test Go SDK on 3 OS, gofmt, golangci-lint, E2E                          | Go                     | L          |
| `python-sdk-tests.yml`                       | Build + test Python SDK on 3 OS, ruff, ty, E2E via pytest                       | Python                 | L          |
| `rust-sdk-tests.yml`                         | Build + test Rust SDK on 3 OS, nightly fmt, clippy, cargo test                  | Rust                   | L          |
| `codegen-check.yml`                          | Verifies generated files across Node, .NET, Python, Go, Rust                    | Cross-language         | M          |
| `publish.yml`                                | Publishes all SDKs (npm, NuGet, PyPI, Go tags, crates.io) from a single version | Cross-language         | XL         |
| `scenario-builds.yml`                        | Verifies example scenarios build for each language                              | Cross-language         | M          |
| `docs-validation.yml`                        | Extracts and validates code snippets from `docs/`                               | Cross-language         | M          |
| `update-copilot-dependency.yml`              | Updates `@github/copilot` dep, re-runs codegen, opens PR                        | Cross-language         | M          |
| `copilot-setup-steps.yml`                    | Agent env setup: Node, Python, Go, .NET, Rust, just, gh-aw                      | Cross-language         | M          |
| `verify-compiled.yml`                        | Ensures `.lock.yml` files match `.md` sources                                   | Cross-language (infra) | S          |
| `collect-corrections.yml`                    | Collects triage agent feedback                                                  | Cross-language (infra) | S          |
| `corrections-tests.yml`                      | Tests for triage correction scripts                                             | Cross-language (infra) | S          |
| `issue-classification.md` + `.lock.yml`      | Agentic: classifies issues → routes to handle-\* handlers                       | Cross-language         | M          |
| `issue-triage.md` + `.lock.yml`              | Agentic: labels, acknowledges, requests clarification, closes dupes             | Cross-language         | L          |
| `handle-bug.md` + `.lock.yml`                | Agentic: investigates bug issues                                                | Cross-language         | M          |
| `handle-documentation.md` + `.lock.yml`      | Agentic: handles doc-related issues                                             | Cross-language         | S          |
| `handle-enhancement.md` + `.lock.yml`        | Agentic: labels enhancement issues                                              | Cross-language         | S          |
| `handle-question.md` + `.lock.yml`           | Agentic: labels question issues                                                 | Cross-language         | S          |
| `cross-repo-issue-analysis.md` + `.lock.yml` | Agentic: checks if issue root cause is in copilot-agent-runtime                 | Cross-language         | M          |
| `release-changelog.md` + `.lock.yml`         | Agentic: generates release notes, updates CHANGELOG                             | Cross-language         | M          |
| `sdk-consistency-review.md` + `.lock.yml`    | Agentic: reviews PRs for cross-SDK feature parity                               | Cross-language         | L          |

---

## 6. Agents, Skills, Prompts, and Supporting Resources Inventory

### 6A. copilot-sdk-java-00

| Resource                                                         | Location                                     | Purpose                                      | Must Migrate?                                                                            |
| ---------------------------------------------------------------- | -------------------------------------------- | -------------------------------------------- | ---------------------------------------------------------------------------------------- |
| **Agent:** `agentic-workflows.agent.md`                          | `.github/agents/`                            | Dispatcher for gh-aw workflow creation/debug | Yes (merge with monorepo version)                                                        |
| **Skill:** `agentic-merge-reference-impl`                        | `.github/skills/` + `.github/prompts/`       | Merges reference impl changes into Java      | Yes — **must be reworked** (no longer cross-repo)                                        |
| **Skill:** `commit-as-pull-request`                              | `.github/skills/` + `.github/prompts/`       | Creates branch, pushes, opens PR             | Yes (may already exist in monorepo)                                                      |
| **Skill:** `documentation-coverage`                              | `.github/skills/` + `.github/prompts/`       | Assesses Java docs coverage                  | Yes                                                                                      |
| **Prompt:** `coding-agent-merge-reference-impl-instructions.md`  | `.github/prompts/`                           | Instructions for coding agent merge          | Yes                                                                                      |
| **Prompt:** `test-coverage-assessment.prompt.md`                 | `.github/prompts/`                           | Test coverage assessment                     | Yes                                                                                      |
| **Composite Action:** `setup-copilot`                            | `.github/actions/setup-copilot/`             | Sets up Copilot CLI for Java tests           | Yes — **adapt paths**                                                                    |
| **Composite Action:** `test-report`                              | `.github/actions/test-report/`               | Test report generation                       | Yes                                                                                      |
| **Scripts:** `release/`, `ci/`, `build/`, `reference-impl-sync/` | `.github/scripts/`                           | Release, CI, sync automation                 | Yes — **path rewrites**                                                                  |
| **Dependabot:** `dependabot.yml`                                 | `.github/`                                   | Maven + GitHub Actions updates               | Merge into monorepo's `dependabot.yaml`                                                  |
| **CODEOWNERS**                                                   | `.github/`                                   | ~~`@github/copilot-sdk-java`                 | Merge into monorepo's CODEOWNERS~~                                                       |
| **Issue Templates:** bug, documentation, feature, maintenance    | `.github/ISSUE_TEMPLATE/`                    | Issue forms                                  | Assess whether monorepo issue triage covers this                                         |
| **PR Template**                                                  | `.github/pull_request_template.md`           | PR form                                      | Merge or keep per-language                                                               |
| **Release Config**                                               | `.github/release.yml`                        | Auto-generated release notes config          | Merge                                                                                    |
| **copilot-instructions.md**                                      | `.github/`                                   | Agent instructions for Java SDK              | Merge (scoped to `java/`)                                                                |
| **Site templates**                                               | `.github/templates/`                         | HTML/CSS for GitHub Pages                    | Migrate to `java/`                                                                       |
| **Coverage badge script**                                        | `.github/scripts/generate-coverage-badge.sh` | JaCoCo badge generation                      | Migrate                                                                                  |
| **`.lastmerge`**                                                 | repo root                                    | Tracks last merged ref-impl commit           | **This concept changes** — see §6                                                        |
| **`.githooks/pre-commit`**                                       | repo root                                    | Runs `mvn spotless:check`                    | Migrate to `java/.githooks/`                                                             |
| **`instructions/copilot-sdk-java.instructions.md`**              | `instructions/`                              | VS Code copilot instructions                 | Create `.github/skills/java-coding-skill/SKILL.md` (follows `rust-coding-skill` pattern) |

### 6B. copilot-sdk-00

| Resource                                | Location                         | Purpose                                               |
| --------------------------------------- | -------------------------------- | ----------------------------------------------------- |
| **Agent:** `agentic-workflows.agent.md` | `.github/agents/`                | Same dispatcher (newer version with more routing)     |
| **Agent:** `docs-maintenance.agent.md`  | `.github/agents/`                | Docs auditor agent                                    |
| **Skill:** `rust-coding-skill`          | `.github/skills/`                | Rust-specific coding skill                            |
| **Composite Action:** `setup-copilot`   | `.github/actions/setup-copilot/` | Sets up Copilot CLI from nodejs package               |
| **Command:** `triage_feedback.yml`      | `.github/commands/`              | Repository dispatch for triage feedback               |
| **LSP Config:** `lsp.json`              | `.github/`                       | C#, Go language server configs                        |
| **Dependabot:** `dependabot.yaml`       | `.github/`                       | npm, pip, gomod, nuget, github-actions, devcontainers |
| **CODEOWNERS**                          | `.github/`                       | `@github/copilot-sdk`                                 |
| **copilot-instructions.md**             | `.github/`                       | Monorepo-wide agent instructions                      |

---

## 7. Pitfalls and Risk Register

### HIGH RISK

| #   | Risk                                                   | Impact                                                                                                                                | Mitigation                                                                                                                                                                                              |
| --- | ------------------------------------------------------ | ------------------------------------------------------------------------------------------------------------------------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| H1  | **Maven Central Trusted Publisher** repo-name mismatch | Cannot publish Java releases from monorepo                                                                                            | Verify/update Trusted Publisher config in Maven Central **before** migration. If the GAV is bound to `github/copilot-sdk-java`, it must be updated.                                                     |
| H2  | **Unified `publish.yml` version collision**            | All SDKs in monorepo share one version. Java has independent `X.Y.Z-java.N` versions.                                                 | Java must keep a **separate** publish workflow. Do NOT merge into `publish.yml`.                                                                                                                        |
| H3  | **`agentic-merge-reference-impl` breaks**              | The core Java development loop relies on this skill to stay in sync with .NET/Node changes                                            | Must be carefully reworked for intra-repo operation before cutover. Test thoroughly with a dry-run on a feature branch. The skill + its 5 shell scripts + 2 prompt files all assume cross-repo cloning. |
| H4  | **Secret provisioning delay**                          | Can't publish or run full CI until secrets are provisioned                                                                            | Start secret provisioning **immediately** (Phase 0).                                                                                                                                                    |
| H5  | **Test harness path changes**                          | Java E2E tests currently clone `copilot-sdk` at build time to get `test/harness/` and `test/snapshots/`. In-repo, these paths change. | Update `pom.xml` and test infrastructure to reference local `test/` directory instead of cloning. **This simplifies things significantly.**                                                             |

### MEDIUM RISK

| #   | Risk                                    | Impact                                                                                                     | Mitigation                                                                                                                                 |
| --- | --------------------------------------- | ---------------------------------------------------------------------------------------------------------- | ------------------------------------------------------------------------------------------------------------------------------------------ |
| M1  | **Codegen `package.json` merge**        | Java codegen has its own `@github/copilot` dependency; monorepo codegen gets it from `nodejs/node_modules` | Align Java codegen to use the same dependency source. May need to add `generate:java` script to monorepo's `scripts/codegen/package.json`. |
| M2  | ~~**GitHub Pages conflict**~~           | ~~Java deploys versioned docs to Pages. Monorepo may have its own Pages setup.~~                           | ~~Use subdirectory deployment or a separate Pages branch for Java.~~                                                                       |
| M3  | **Branch protection / required checks** | New `java-sdk-tests` check may not be in the required list                                                 | Add to branch protection after first successful run.                                                                                       |
| M4  | **CODEOWNERS team permissions**         | `@github/copilot-sdk-java` team may not have write access to `github/copilot-sdk`                          | Verify team access and add to repo collaborators. See https://github.com/github/copilot-sdk-partners/issues/89                             |
| M5  | **`copilot-setup-steps.yml` bloat**     | Adding JDK + Maven makes agent setup slower for non-Java tasks                                             | Acceptable trade-off; other languages already add their tools. Could consider conditional setup but that's over-engineering.               |
| M6  | **gh-aw version mismatch**              | Java repo uses gh-aw `v0.68.3` setup action pinned at `v0.71.5`; monorepo uses `v0.64.2` reference in docs | Align gh-aw versions. Use the newer version. Recompile all `.lock.yml` files.                                                              |

### LOW RISK

| #   | Risk                                     | Impact                                                        | Mitigation                                                          |
| --- | ---------------------------------------- | ------------------------------------------------------------- | ------------------------------------------------------------------- |
| L1  | **Issue template conflicts**             | Java has custom issue templates; monorepo uses agentic triage | Monorepo agentic triage covers this. Can add Java-specific labels.  |
| L2  | **PR template differences**              | Different PR templates                                        | Use monorepo's template. Java-specific guidance in CONTRIBUTING.md. |
| L3  | **`.githooks` scope**                    | Java pre-commit hook runs `mvn spotless:check` globally       | Scope hook to only run when Java files are changed.                 |
| L4  | **Duplicate `agentics-maintenance.yml`** | Java repo has its own; monorepo will generate one             | The monorepo's gh-aw will handle this automatically. Don't migrate. |

---

## 8. Post-Migration Verification Checklist

### CI/CD

- [ ] `java-sdk-tests.yml` passes on all 3 OS platforms
- [ ] `codegen-check.yml` includes Java and passes
- [ ] `java-codegen-fix.md` compiles and agentic workflow functions
- [ ] `java-publish.yml` can do a dry-run publish
- [ ] `java-publish-snapshot.yml` publishes a SNAPSHOT
- [ ] `java-smoke-test.yml` passes on JDK 17 + JDK 25
- [ ] `java-deploy-site.yml` successfully deploys docs

### Integration

- [ ] `copilot-setup-steps.yml` includes JDK and Maven
- [ ] `dependabot.yaml` includes Maven ecosystem for `java/`
- [✅] `CODEOWNERS` includes `java/` path. See https://github.com/github/copilot-sdk-partners/issues/89
- [ ] `justfile` has all Java targets and `just test` includes Java
- [ ] `sdk-consistency-review` includes `java/` in path triggers
- [ ] `issue-triage` knows about `sdk/java` label

### Code

- [ ] `mvn verify` passes from `java/` directory
- [ ] E2E tests use local `test/harness/` and `test/snapshots/` (no cloning)
- [ ] Java codegen integrated into `scripts/codegen/`
- [ ] `.lastmerge` exists at `java/.lastmerge`

### Documentation

- [ ] Monorepo `README.md` lists Java
- [ ] `copilot-instructions.md` includes Java governance section (build, test, conventions)
- [ ] `.github/skills/java-coding-skill/SKILL.md` exists with Java API patterns and coding rules
- [ ] `java/README.md` links updated to monorepo
- [ ] Maven Central POM `<scm>` URLs updated

### Agentic Sync

- [ ] `java-reference-impl-sync.md` compiles and detects new dotnet/nodejs changes via local `git log`
- [ ] `agentic-merge-reference-impl` skill works intra-repo (no cross-repo clone)
- [ ] `java/.lastmerge` correctly stores monorepo commit SHAs
- [ ] Sync scripts in `.github/scripts/java/reference-impl-sync/` use local paths

### Cleanup

- [ ] `copilot-sdk-java` repo archived
- [ ] No broken links to old repo
- [ ] No duplicate `agentics-maintenance.yml`

---

## Appendix A: Files to Copy vs. Merge vs. Delete

| Source File (copilot-sdk-java-00)                        | Action                                                                  | Target Location (copilot-sdk-00)                              |
| -------------------------------------------------------- | ----------------------------------------------------------------------- | ------------------------------------------------------------- |
| `src/`                                                   | Copy                                                                    | `java/src/`                                                   |
| `config/`                                                | Copy                                                                    | `java/config/`                                                |
| `pom.xml`                                                | Copy + update paths                                                     | `java/pom.xml`                                                |
| `CHANGELOG.md`                                           | Copy                                                                    | `java/CHANGELOG.md`                                           |
| `README.md`                                              | Copy + update links                                                     | `java/README.md`                                              |
| `jbang-example.java`                                     | Copy                                                                    | `java/jbang-example.java`                                     |
| `.lastmerge`                                             | Copy                                                                    | `java/.lastmerge`                                             |
| `.githooks/pre-commit`                                   | Copy + scope to Java changes                                            | `java/.githooks/pre-commit`                                   |
| `docs/adr/`                                              | Copy                                                                    | `java/docs/adr/`                                              |
| `scripts/codegen/java.ts`                                | Copy                                                                    | `java/scripts/codegen/java.ts`                                |
| `scripts/codegen/package.json`                           | Copy (Java keeps its own)                                               | `java/scripts/codegen/package.json`                           |
| `.github/workflows/build-test.yml`                       | **Adapt** → rename                                                      | `.github/workflows/java-sdk-tests.yml`                        |
| `.github/workflows/publish-maven.yml`                    | **Adapt** → rename                                                      | `.github/workflows/java-publish.yml`                          |
| `.github/workflows/publish-snapshot.yml`                 | **Adapt** → rename                                                      | `.github/workflows/java-publish-snapshot.yml`                 |
| `.github/workflows/deploy-site.yml`                      | **Adapt** → rename                                                      | `.github/workflows/java-deploy-site.yml`                      |
| `.github/workflows/run-smoke-test.yml`                   | **Adapt** → rename                                                      | `.github/workflows/java-smoke-test.yml`                       |
| `.github/workflows/codegen-check.yml`                    | **Merge** into existing                                                 | `.github/workflows/codegen-check.yml`                         |
| `.github/workflows/codegen-agentic-fix.md`               | **Adapt** → rename                                                      | `.github/workflows/java-codegen-fix.md`                       |
| `.github/workflows/update-copilot-dependency.yml`        | **Merge** into existing                                                 | `.github/workflows/update-copilot-dependency.yml`             |
| `.github/workflows/copilot-setup-steps.yml`              | **Merge** into existing                                                 | `.github/workflows/copilot-setup-steps.yml`                   |
| `.github/workflows/reference-impl-sync.md` + `.lock.yml` | **Adapt** → rename + rework for intra-repo                              | `.github/workflows/java-reference-impl-sync.md` + `.lock.yml` |
| `.github/workflows/agentics-maintenance.yml`             | **DELETE** (monorepo has its own)                                       | —                                                             |
| `.github/workflows/notes.template`                       | Copy                                                                    | `.github/workflows/java-notes.template`                       |
| `.github/actions/setup-copilot/`                         | **Adapt** or merge                                                      | `.github/actions/java-setup-copilot/` or merge                |
| `.github/actions/test-report/`                           | Copy                                                                    | `.github/actions/java-test-report/`                           |
| `.github/scripts/*`                                      | Copy + update paths                                                     | `.github/scripts/java/` (new subdirectory)                    |
| `.github/skills/agentic-merge-reference-impl/`           | **Rework** for intra-repo (remove cross-repo clone, use local git diff) | `.github/skills/java-merge-reference-impl/`                   |
| `.github/skills/commit-as-pull-request/`                 | Check for duplicates                                                    | `.github/skills/commit-as-pull-request/`                      |
| `.github/skills/documentation-coverage/`                 | Copy                                                                    | `.github/skills/java-documentation-coverage/`                 |
| `.github/prompts/*`                                      | Copy + update                                                           | `.github/prompts/` (prefix with `java-` if needed)            |
| `.github/dependabot.yml`                                 | **Merge** into existing                                                 | `.github/dependabot.yaml`                                     |
| `.github/CODEOWNERS`                                     | **Merge** into existing                                                 | `.github/CODEOWNERS`                                          |
| `.github/copilot-instructions.md`                        | **Merge** into existing                                                 | `.github/copilot-instructions.md`                             |
| `.github/release.yml`                                    | **Merge** into existing                                                 | `.github/release.yml` (if it exists)                          |
| `.github/ISSUE_TEMPLATE/*`                               | Evaluate — likely skip                                                  | —                                                             |
| `.github/pull_request_template.md`                       | Evaluate — likely skip                                                  | —                                                             |
| `.github/templates/`                                     | Copy                                                                    | `java/.github/templates/` or `java/src/site/`                 |
| `instructions/copilot-sdk-java.instructions.md`          | Create skill (follows `rust-coding-skill` pattern)                      | `.github/skills/java-coding-skill/SKILL.md`                   |
| `test` (file, not directory)                             | Copy if needed                                                          | `java/test`                                                   |

## Appendix B: Unique Java Concerns vs Other Languages

| Concern                     | Java                                                | Other Languages                                   | Notes                                                                                                                          |
| --------------------------- | --------------------------------------------------- | ------------------------------------------------- | ------------------------------------------------------------------------------------------------------------------------------ |
| **Build system**            | Maven (`pom.xml`)                                   | npm, pip, go mod, dotnet, cargo                   | Fully self-contained under `java/`                                                                                             |
| **Versioning**              | `X.Y.Z-java.N` (independent)                        | Shared `X.Y.Z` across all others                  | **Must keep separate publish workflow**                                                                                        |
| **Code formatting**         | Spotless (Eclipse formatter)                        | prettier, ruff, gofmt, dotnet format, rustfmt     | Runs only on Java files                                                                                                        |
| **Test framework**          | JUnit + Surefire                                    | Vitest, pytest, go test, xunit, cargo test        | Standard; no conflicts                                                                                                         |
| **E2E test harness**        | Clones `copilot-sdk` at build time                  | References local `test/harness/`                  | **Major simplification** when in-repo                                                                                          |
| **Codegen**                 | Own `java.ts` + own `@github/copilot` dep           | Shared codegen scripts + shared dep               | Needs reconciliation                                                                                                           |
| **CI runner**               | JDK 17 + JDK 25 (smoke test)                        | Node 22, Python 3.12, Go 1.24, .NET 10, Rust 1.94 | Just another tool in `copilot-setup-steps.yml`                                                                                 |
| **Publishing**              | Maven Central (GPG + Sonatype)                      | npm, PyPI, NuGet, crates.io, Go tags              | Completely different mechanism                                                                                                 |
| **Docs hosting**            | ~~GitHub Pages (Maven site)~~                       | ~~Not clear if monorepo has its own~~             | ~~Potential conflict~~                                                                                                         |
| **Reference impl tracking** | `.lastmerge` + scheduled sync + agentic merge skill | N/A (they ARE the reference impl)                 | `.lastmerge` stores monorepo SHA; sync becomes intra-repo but is still needed because Java maintainers ≠ .NET/Node maintainers |

---

## Appendix C: Java Smoketest

### Overview

The Java SDK has an AI-driven smoke test that validates the SDK's Quick Start code actually compiles and runs. The test is **prompt-driven**: the `run-smoke-test.yml` workflow invokes the Copilot CLI (`copilot --yolo`) with a prompt that instructs it to read the repository's `README.md`, extract the Quick Start code and Maven coordinates, generate a standalone Maven project, build it, and run it. Success = exit code 0.

This design intentionally tests the README itself — if the documented code doesn't compile against the published artifact, the smoke test fails rather than silently fixing the code. This catches documentation drift.

### How It Works Today

1. **`src/test/prompts/PROMPT-smoke-test.md`** — The master prompt. It instructs the Copilot CLI to:
   - Read the top-level `README.md`
   - Extract the **"Snapshot Builds"** section (Maven GAV + snapshots repository config)
   - Extract the **"Quick Start"** section (verbatim Java source code)
   - Create a `smoke-test/` Maven project using those extracted values
   - Build with `mvn -U clean package`

2. **`run-smoke-test.yml`** — The workflow. It:
   - Builds the SDK and installs it to the local Maven repo
   - Feeds the prompt to `copilot --yolo` with overrides (use `--no-snapshot-updates`, stop after build)
   - Runs the built jar in a separate deterministic step
   - Has two jobs: `smoke-test-jdk17` and `smoke-test-java25` (the latter also applies virtual thread modifications via `// JDK 25+:` comments)

3. **`build-test.yml`** — Calls `run-smoke-test.yml` as a reusable workflow. The main SDK test suite (`java-sdk` job) depends on the smoke test and only runs if it doesn't fail.

### What Breaks When Moving to the Monorepo

The smoke test prompt (`PROMPT-smoke-test.md`) contains these instructions:

> Read the file `README.md` at the top level of this repository. You will need two sections from it: **"Snapshot Builds"** and **"Quick Start"**

After migration, the **top-level `README.md`** is the monorepo's README (`copilot-sdk-00/README.md`), which does not contain a "Snapshot Builds" section or a "Quick Start" section with Java code. The Java-specific README moves to `java/README.md`.

Additionally, the monorepo's top-level README contains Quick Start code for **other languages** (TypeScript, Python, Go, C#). If the prompt were naively updated to "read the README," the AI agent might extract the wrong language's code.

### Required Changes

#### 1. Update `PROMPT-smoke-test.md` — change the README path

Replace:

```
Read the file `README.md` at the top level of this repository.
```

With:

```
Read the file `java/README.md` in this repository.
```

This is the only structural change needed in the prompt. The section names ("Snapshot Builds" and "Quick Start") remain the same in `java/README.md`.

#### 2. Update `java/README.md` — ensure required sections survive the move

The prompt depends on two specific sections by name:

- **"Snapshot Builds"** — must contain the Maven GAV with `-SNAPSHOT` version and the `central-snapshots` repository XML
- **"Quick Start"** — must contain the verbatim Java source code with `// JDK 25+:` inline comments for virtual thread toggling

When migrating `README.md` → `java/README.md`, these sections and their content must be preserved exactly. The current monorepo placeholder at `java/README.md` has a different Quick Start (different class name `QuickStart` vs `CopilotSDK`, different imports, no `// JDK 25+:` comments, no `System.exit` logic, no usage metrics handling). **The migrated `README.md` from `copilot-sdk-java` must replace the monorepo placeholder**, not the other way around.

#### 3. Update `run-smoke-test.yml` → `java-smoke-test.yml` — working directory

The workflow steps that run `mvn` and reference `src/test/prompts/PROMPT-smoke-test.md` assume the repo root is the Java project root. After migration:

- Add `working-directory: ./java` to the "Build SDK and install to local repo" step
- Update the prompt text from `src/test/prompts/PROMPT-smoke-test.md` to `java/src/test/prompts/PROMPT-smoke-test.md` (or set working directory before invoking `copilot`)
- Update the `cd smoke-test` step to `cd java/smoke-test`
- Update the `uses: ./.github/actions/setup-copilot` reference to point to the monorepo's setup action (or a Java-specific one at `.github/actions/java-setup-copilot/`)

#### 4. Update `build-test.yml` → `java-sdk-tests.yml` — smoke test call

The current `build-test.yml` calls:

```yaml
uses: ./.github/workflows/run-smoke-test.yml
```

After rename, update to:

```yaml
uses: ./.github/workflows/java-smoke-test.yml
```

### Risk Assessment

| Risk                                                 | Severity   | Notes                                                                                                                                                                                                 |
| ---------------------------------------------------- | ---------- | ----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| Prompt reads wrong README                            | **HIGH**   | If `PROMPT-smoke-test.md` still says "top level README," the AI agent reads the monorepo README and fails or extracts wrong-language code                                                             |
| `java/README.md` placeholder overwrites real content | **HIGH**   | The monorepo already has a `java/README.md` with a different Quick Start. Must be replaced with the full Java SDK README during migration                                                             |
| `smoke-test/` directory created at wrong location    | **MEDIUM** | Without `working-directory: ./java`, the smoke test project gets created at the monorepo root instead of under `java/`                                                                                |
| `// JDK 25+:` comments missing from Quick Start      | **MEDIUM** | The JDK 25 smoke test job relies on these comments to toggle virtual thread support. Missing comments → JDK 25 job builds without virtual threads and still passes (silent regression, not a failure) |

### Verification Checklist

- [ ] `PROMPT-smoke-test.md` references `java/README.md`, not `README.md`
- [ ] `java/README.md` contains "Snapshot Builds" and "Quick Start" sections with the full content from `copilot-sdk-java`
- [ ] Quick Start code includes `// JDK 25+:` inline comments and `System.exit` logic
- [ ] `java-smoke-test.yml` uses `working-directory: ./java` for Maven steps
- [ ] `java-smoke-test.yml` references the correct prompt path
- [ ] `java-sdk-tests.yml` calls `java-smoke-test.yml` (not `run-smoke-test.yml`)
- [ ] Smoke test passes locally from `java/` subdirectory before merging
