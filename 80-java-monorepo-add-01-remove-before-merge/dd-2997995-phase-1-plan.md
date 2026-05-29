# Phase 1 Agent Prompt: Copy Java SDK Source Code into Monorepo

## Instructions

You are working in the `copilot-sdk-00` repository (dest). The source Java SDK code lives at `../copilot-sdk-java-00` (relative to this repo root). Both are local clones:

- **Dest (you are here):** `copilot-sdk-00` — local clone of `https://github.com/github/copilot-sdk`
- **Source:** `../copilot-sdk-java-00` — local clone of `https://github.com/github/copilot-sdk-java`

**Before doing anything else**, read the file `80-java-monorepo-add-01-remove-before-merge/dd-2989727-move-java-to-monorepo-plan.md` in this repository. It contains the full migration plan. You are executing **Phase 1 ONLY** — "Copy Source Code (No Workflows Yet)". Do NOT perform any other phases (Phase 2, 3, 4, 5, or 6).

You are safe to commit directly to the current topic branch. Make fine-grained commits with reasonable commit log messages as you go (e.g., one commit per logical group of files copied, one commit for pom.xml adjustments, one commit for test infrastructure changes).

## Phase 1 Goal

Get all Java source code building and testing in the monorepo under `java/` without any CI/CD workflows.

## Phase 1 Steps

### Step 1: Copy files from source to dest

Copy the following from `../copilot-sdk-java-00/` into `java/` (replacing the existing placeholder `java/README.md`):

| Source path (relative to `../copilot-sdk-java-00/`) | Dest path (relative to repo root)                |
| --------------------------------------------------- | ------------------------------------------------ |
| `src/` (all of it: main, test, generated, site)     | `java/src/`                                      |
| `pom.xml`                                           | `java/pom.xml`                                   |
| `config/` (checkstyle, spotbugs)                    | `java/config/`                                   |
| `scripts/codegen/java.ts`                           | `java/scripts/codegen/java.ts`                   |
| `scripts/codegen/package.json`                      | `java/scripts/codegen/package.json`              |
| `CHANGELOG.md`                                      | `java/CHANGELOG.md`                              |
| `README.md`                                         | `java/README.md` (replaces existing placeholder) |
| `jbang-example.java`                                | `java/jbang-example.java`                        |
| `.lastmerge`                                        | `java/.lastmerge`                                |
| `docs/adr/`                                         | `java/docs/adr/`                                 |
| `mvnw`                                              | `java/mvnw`                                      |
| `mvnw.cmd`                                          | `java/mvnw.cmd`                                  |
| `.mvn/`                                             | `java/.mvn/`                                     |
| `.gitignore`                                        | `java/.gitignore`                                |
| `test` (single file, not a directory)               | `java/test`                                      |

**DO NOT copy:**

- `.githooks/` — already handled separately (strikethrough in plan)
- `instructions/copilot-sdk-java.instructions.md` — already handled separately (strikethrough in plan)
- `.github/` — workflows are Phase 2+, not Phase 1
- `.git/` — never copy git internals
- `target/` — build artifacts, never copy
- `.claude/` — not needed
- `.vscode/` — not needed
- `20260430-*.txt` — log files, not needed
- `CODE_OF_CONDUCT.md`, `CONTRIBUTING.md`, `LICENSE`, `SECURITY.md`, `SUPPORT.md` — these exist at the monorepo root already

### Step 2: Update `pom.xml` paths if needed

The `pom.xml` should be self-contained under `java/`. Review it for any paths that assume it lives at the repository root. Key things to check and fix:

1. **Test harness clone**: The current `pom.xml` likely has a `maven-antrun-plugin` execution that clones `https://github.com/github/copilot-sdk` into `target/copilot-sdk/` to get `test/harness/` and `test/snapshots/`. Since these directories now exist locally in the same repo at `../../test/harness/` and `../../test/snapshots/` (relative to `java/`), **replace the git clone with a local copy or symlink**. The simplest approach: change the antrun execution to copy from `${project.basedir}/../test/` instead of cloning from GitHub.

2. **Any absolute or root-relative paths** that reference the repo root — these should be adjusted to work from `java/` as the working directory.

3. **The `<scm>` section** — update URLs from `github/copilot-sdk-java` to `github/copilot-sdk` and adjust paths if needed.

### Step 3: Verify `mvn clean verify` works from `java/`

Run `cd java && mvn clean verify` and fix any issues. The build must pass. Common issues to expect:

- Test harness path references (from Step 2)
- Any hardcoded paths in test infrastructure that assume repo root = Java project root
- The `E2ETestContext` or `CapiProxy` classes may reference `target/copilot-sdk/test/harness/` — these need to point to `../../test/` (or however the local copy is structured after Step 2)

If tests fail, diagnose and fix. Do NOT skip tests. The goal is a green `mvn clean verify` from `java/`.

### Commit Strategy

Make commits as you go:

1. After copying the source files (Step 1)
2. After updating `pom.xml` and test infrastructure (Step 2)
3. After fixing any build/test issues (Step 3)

Use descriptive commit messages like:

- "Copy Java SDK source files into java/ directory"
- "Update pom.xml to use local test harness instead of git clone"
- "Fix E2E test paths for monorepo layout"

## Constraints

- **DO NOT** create or modify any GitHub Actions workflow files (`.github/workflows/`)
- **DO NOT** modify `.github/copilot-instructions.md`
- **DO NOT** modify the `justfile`
- **DO NOT** modify `CODEOWNERS`
- **DO NOT** modify `dependabot.yaml`
- **DO NOT** modify `copilot-setup-steps.yml`
- **DO NOT** touch any files under `nodejs/`, `python/`, `go/`, `dotnet/`, `rust/`
- **DO NOT** perform Phase 2, 3, 4, 5, or 6 work
- **DO NOT** modify files under `java/src/generated/java/` beyond what was copied — these are auto-generated
- You MAY modify `java/pom.xml`, `java/src/test/java/**`, and `java/src/main/java/**` as needed to get the build passing
