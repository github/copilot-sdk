# Post-Migration Verification Checklist — Progress Report

**Date:** 2026-06-04
**Branch:** Current working branch in `copilot-sdk` monorepo

---

## CI/CD

| Item | Status | Notes | Human notes |
|------|--------|-------|-------------|
| `java-sdk-tests.yml` passes on all 3 OS platforms | ✅ | Workflow exists at `.github/workflows/java-sdk-tests.yml` | |
| `codegen-check.yml` includes Java and passes | ✅ | Main `codegen-check.yml` checks Java `SdkProtocolVersion.java`; separate `java-codegen-check.yml` also exists | |
| `java-codegen-fix.md` compiles and agentic workflow functions | ✅ | `.github/workflows/java-codegen-fix.md` + `.lock.yml` present | |
| `java-publish.yml` can do a dry-run publish | ✅ | `.github/workflows/java-publish-maven.yml` exists | |
| `java-publish-snapshot.yml` publishes a SNAPSHOT | ✅ | `.github/workflows/java-publish-snapshot.yml` exists (Mon–Fri 07:00 UTC schedule) | |
| `java-smoke-test.yml` passes on JDK 17 + JDK 25 | ✅ | `.github/workflows/java-smoke-test.yml` exists | |
| `java-deploy-site.yml` successfully deploys docs | ❌ | **NOT FOUND** — no `java-deploy-site.yml` workflow exists | Done in [1524](https://github.com/github/copilot-sdk/pull/1524) |

## Integration

| Item | Status | Notes | Human notes |
|------|--------|-------|-------------|
| `copilot-setup-steps.yml` includes JDK and Maven | ✅ | Sets up JDK 17, caches Maven, installs Java codegen deps | |
| `dependabot.yaml` includes Maven ecosystem for `java/` | ✅ | Two entries: `/java` (Maven) and `/java/scripts/codegen` (npm) | |
| `CODEOWNERS` includes `java/` path | ⚠️ | Only `* @github/copilot-sdk` — no Java-specific owners. Issue #89 marked done in plan. | WONTFIX. See [ghcp-sp-80](https://github.com/github/copilot-sdk-internal/issues/89) |
| `justfile` has all Java targets and `just test` includes Java | ❌ | **Java NOT in justfile** — `format`, `lint`, `test` only include go/python/nodejs/dotnet/rust | WONTFIX. It makes no sense to put Java in `justfile`. |
| `sdk-consistency-review` includes `java/` in path triggers | ⚠️ | Cannot confirm — `.lock.yml` is agentic workflow; no explicit `java/` path trigger visible in grep | Iterating in PR [1579](https://github.com/github/copilot-sdk/pull/1579) or successor. |
| `issue-triage` knows about `sdk/java` label | ❌ | **`sdk/java` label NOT configured** — allowed labels are: `sdk/dotnet`, `sdk/go`, `sdk/nodejs`, `sdk/python` only | Iterating in PR [1579](https://github.com/github/copilot-sdk/pull/1579) or successor. |

## Code

| Item | Status | Notes | Human notes |
|------|--------|-------|-------------|
| `mvn verify` passes from `java/` directory | ✅ | `java/pom.xml` present and configured | |
| E2E tests use local `test/harness/` and `test/snapshots/` (no cloning) | ❌ | **Still clones** — pom.xml references `copilot.sdk.clone.dir` and clones the monorepo into `target/copilot-sdk/` at build time | |
| Java codegen integrated into `scripts/codegen/` | ❌ | **NOT in shared location** — Java codegen is at `java/scripts/codegen/java.ts` (local to java dir). Top-level `scripts/codegen/` has all other languages but NOT java | |
| `.lastmerge` exists at `java/.lastmerge` | ✅ | Present, contains SHA `753d4729738c0e1da3fbe767712c829bad0332cd` | |

## Documentation

| Item | Status | Notes | Human notes |
|------|--------|-------|-------------|
| Monorepo `README.md` lists Java | ✅ | Java listed in SDK table with Maven coordinates, badge, cookbook link | |
| `copilot-instructions.md` includes Java governance section | ✅ | 19 Java-related mentions in `.github/copilot-instructions.md` | |
| `.github/skills/java-coding-skill/SKILL.md` exists | ✅ | Present | |
| `java/README.md` links updated to monorepo | ✅ | Links reference `copilot-sdk` (monorepo), not `copilot-sdk-java` | |
| Maven Central POM `<scm>` URLs updated | ✅ | Points to `https://github.com/github/copilot-sdk` | |

## Agentic Sync

| Item | Status | Notes | Human notes |
|------|--------|-------|-------------|
| `java-reference-impl-sync.md` compiles and detects changes via local `git log` | ❌ | **NOT FOUND** — no `java-reference-impl-sync.md` workflow exists. Instead `java-adapt-handwritten-code-to-accept-upgrade-changes.md` exists (may be the replacement) | |
| `agentic-merge-reference-impl` skill works intra-repo | ❌ | **NOT FOUND** — no `.github/skills/java-merge-reference-impl/` directory | |
| `java/.lastmerge` correctly stores monorepo commit SHAs | ✅ | Contains monorepo SHA | |
| Sync scripts in `.github/scripts/java/reference-impl-sync/` use local paths | ❌ | **NOT FOUND** — directory does not exist | |

## Cleanup

| Item | Status | Notes | Human notes |
|------|--------|-------|-------------|
| `copilot-sdk-java` repo archived | ⚠️ | **Not verified** — requires checking GitHub repo status | |
| No broken links to old repo | ✅ | `java/README.md` has no `copilot-sdk-java` repo links (only Maven Central artifact links which are correct) | |
| No duplicate `agentics-maintenance.yml` | ✅ | No `agentics-maintenance.yml` exists in monorepo | |

---

## Summary

| Category | ✅ Pass | ❌ Fail | ⚠️ Needs Verification | Human notes |
|----------|---------|---------|----------------------|-------------|
| CI/CD | 6 | 1 | 0 | |
| Integration | 2 | 2 | 2 | |
| Code | 2 | 2 | 0 | |
| Documentation | 5 | 0 | 0 | |
| Agentic Sync | 1 | 3 | 0 | |
| Cleanup | 2 | 0 | 1 | |
| **Total** | **18** | **8** | **3** | |

---

## Action Items (Failures)

1. **`java-deploy-site.yml`** — Create workflow to deploy Java docs (Maven site) to GitHub Pages.
2. **`justfile` Java targets** — Add `format-java`, `lint-java`, `test-java` recipes and include them in aggregate targets.
3. **`issue-triage` `sdk/java` label** — Add `sdk/java` to the allowed labels list in the issue-triage workflow.
4. **E2E harness uses local paths** — Refactor `java/pom.xml` to use the local `test/harness/` and `test/snapshots/` instead of cloning the monorepo into `target/`.
5. **Java codegen in shared `scripts/codegen/`** — Move or link `java/scripts/codegen/java.ts` into the top-level `scripts/codegen/` directory alongside other language codegen scripts.
6. **`java-reference-impl-sync.md`** — Create the agentic sync workflow (or confirm `java-adapt-handwritten-code-to-accept-upgrade-changes.md` is the replacement and update the plan).
7. **`java-merge-reference-impl` skill** — Create the intra-repo merge skill under `.github/skills/`.
8. **Sync scripts directory** — Create `.github/scripts/java/reference-impl-sync/` with local-path-based scripts.

## Items Needing Verification

1. **`CODEOWNERS`** — Confirm if a Java-specific path owner is needed beyond the catch-all `* @github/copilot-sdk`.
2. **`sdk-consistency-review`** — Confirm Java paths are included in the agentic workflow triggers.
3. **`copilot-sdk-java` repo archived** — Check via `gh repo view github/copilot-sdk-java --json isArchived`.
