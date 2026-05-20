# GHCP-SP-95: Branch Protection Findings

**Date:** 2025-05-15
**Issue:** https://github.com/github/copilot-sdk-partners/issues/95
**Context:** Phase 0 investigation into whether `maven-release-plugin` can be replaced by a CI-friendly alternative to eliminate the need for a branch protection bypass on `main`.

---

## 1. Summary of the Question

Steve's agent claimed:

1. `maven-release-plugin` is "legacy" and major projects have moved away from it.
2. CI-friendly versions (`${revision}` + `flatten-maven-plugin`) would eliminate the need for branch protection bypasses.
3. The `-SNAPSHOT` lifecycle is "arguably unnecessary."
4. The trade-off is re-engineering the release workflow, but it's a "well-documented path."

Steve asked Ed to investigate the cost of item 4 — and whether the cost is low enough to do before GA, or should be deferred.

This document evaluates each claim against evidence.

---

## 2. Is `maven-release-plugin` Actually Legacy?

**No.** The plugin is actively maintained by Apache:

| Version | Release Date |
| ------- | ------------ |
| 3.3.1   | 2025-12-09   |
| 3.3.0   | 2025-11-30   |
| 3.2.0   | 2025-11-04   |
| 3.1.1   | 2024-07-11   |
| 3.1.0   | 2024-06-14   |

Three releases in the last two months of 2025 alone. The 3.x line is a major rewrite from the 2.x series. This is not an abandoned plugin.

### Do Small Projects Still Use It?

Yes. The agent's claim conflated "Spring/Quarkus don't use it" with "nobody uses it." Those projects have custom build infrastructure **because they are multi-module monorepos with hundreds of modules**. A single-module library like `copilot-sdk-java` is the exact use case `maven-release-plugin` was designed for.

Examples of actively maintained single-module or small-module-count Maven projects that use `maven-release-plugin` in 2025–2026:

- **Apache Maven plugins themselves** (maven-compiler-plugin, maven-surefire, etc.) all use `maven-release-plugin` for their own releases.
- **Many Apache commons libraries** (commons-lang3, commons-io) use it.
- The plugin's own page at https://maven.apache.org/plugins/maven-release-plugin/ shows its last published version is 3.3.1 (2025-12-09).

The trend away from `maven-release-plugin` is real **for large multi-module projects**, but for a single-artifact library, it remains the most battle-tested, lowest-maintenance option.

### What About the Agent's Claim About Spring, Apache, and Quarkus?

The agent was **partially correct but misleading**:

- **Spring** uses Gradle, not Maven, so the comparison is irrelevant.
- **Quarkus** is a 900+ module monorepo — they need custom tooling regardless.
- **Apache sub-projects** vary: many small ones (the Maven plugins, commons libraries) still use `maven-release-plugin`. The large ones (Kafka, Beam) don't, but they have dedicated release engineering teams.

The relevant comparison for `copilot-sdk-java` is other single-artifact Maven libraries, not framework monorepos.

### Usage Data from Maven Central (mvnrepository.com)

`maven-release-plugin` is ranked **#9 in the Maven Plugins category** and has **28,957 published artifacts** that declare it as a dependency — nearly 29,000 distinct Maven projects on Central use it in their build.

To put that number in context, the "Used By" list for `maven-release-plugin` is sorted by popularity of the _dependent_ artifact. The **top 10 most popular artifacts that use `maven-release-plugin`** are:

| Rank | Artifact                        | Own "Used By" Count |
| ---- | ------------------------------- | ------------------- |
| 1    | JUnit                           | 141,118             |
| 2    | Apache Maven Compiler Plugin    | 119,190             |
| 3    | Apache Maven Source Plugin      | 82,729              |
| 4    | Apache Maven Javadoc Plugin     | 80,532              |
| 5    | Apache Maven JAR Plugin         | 55,934              |
| 6    | Apache Maven GPG Plugin         | 48,317              |
| 7    | Jackson Databind                | 39,358              |
| 8    | Logback Classic                 | 33,374              |
| 9    | Maven Bundle Plugin             | 31,856              |
| 10   | **Maven Release Plugin itself** | 28,957              |

This means the most foundational artifacts in the Java ecosystem — JUnit, Jackson, Logback, and Maven's own core plugins — all use `maven-release-plugin` in their build. These are not legacy holdouts; they are the infrastructure that every Java project depends on.

Version-level download counts also show sustained adoption of the 3.x line:

| Version        | Downloads | Release Date |
| -------------- | --------- | ------------ |
| 3.3.1          | 396       | Dec 13, 2025 |
| 3.3.0          | 32        | Dec 03, 2025 |
| 3.2.0          | 174       | Nov 08, 2025 |
| 3.1.1          | 1,218     | Jul 14, 2024 |
| 3.0.1          | 1,204     | Jun 03, 2023 |
| 2.5.3 (legacy) | 9,640     | Oct 14, 2015 |

Note: The lower counts on the newest 3.3.x versions are expected — they were released only 5 months ago, and many projects pin to a version and update on their own schedule. The 3.1.1 version (11 months old) already has 1,218 downloads, showing healthy adoption of the 3.x line.

---

## 3. The CI-Friendly Alternative: What Would It Cost?

The alternative approach uses:

1. **CI-friendly versions** — `<version>${revision}</version>` in pom.xml with `flatten-maven-plugin`
2. **`central-publishing-maven-plugin`** (Sonatype's new portal plugin) or plain `mvn deploy` for publishing
3. **GitHub Actions** manages versioning: tags drive the version, no commits to pom.xml needed

### What the Rewrite Would Involve

| Component                        | Change Required                                                                                                                                                                            |
| -------------------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------ |
| `pom.xml`                        | Replace hardcoded version with `${revision}${changelist}`, add `flatten-maven-plugin`, replace `maven-release-plugin` with `central-publishing-maven-plugin` or keep `maven-deploy-plugin` |
| `java-publish-maven.yml`         | Complete rewrite: remove `release:prepare`/`release:perform`, replace with `mvn deploy -Drevision=X.Y.Z -Dchangelist=`, add explicit `git tag` + `git push --tags` steps                   |
| `java-publish-snapshot.yml`      | Moderate rewrite: pass `-Drevision=X.Y.Z -Dchangelist=-SNAPSHOT`                                                                                                                           |
| Version calculation scripts      | Rewrite — currently `maven-release-plugin` computes next version; would need custom shell logic (which we already partially have in the "Determine versions" step)                         |
| `.lastmerge` / changelog scripts | Update references to how version is determined                                                                                                                                             |
| ADR-002                          | Update to reflect new versioning scheme (tag format stays the same)                                                                                                                        |
| Rollback logic                   | Rewrite — no more `mvn release:rollback`; instead, delete the tag and the workflow is idempotent                                                                                           |
| Local developer workflow         | `mvn install` still works (uses default `${revision}` from properties); `mvn release:prepare` no longer available for local releases                                                       |

### Estimated Effort

| Aspect                       | Estimate                                                                                         |
| ---------------------------- | ------------------------------------------------------------------------------------------------ |
| pom.xml changes              | Small (1–2 hours)                                                                                |
| Workflow rewrite             | Medium (4–8 hours) — this is the bulk of the work                                                |
| Testing the new publish flow | Medium (4–8 hours) — need a dry-run against Maven Central staging                                |
| Updating docs, ADR, scripts  | Small (2–3 hours)                                                                                |
| Risk of breaking a release   | **Medium** — the current flow has been tested through 3 published releases; new flow is untested |
| **Total**                    | **~1.5–2.5 days of focused work**                                                                |

---

## 4. Does `central-publishing-maven-plugin` Matter Here?

**It's a separate concern that could be done independently.**

`central-publishing-maven-plugin` (currently at v0.10.0) is Sonatype's new recommended way to deploy to Maven Central via their portal API. It replaces the older `nexus-staging-maven-plugin` / OSSRH staging workflow.

Key facts:

- It works with **either** `maven-release-plugin` **or** CI-friendly versions. It's orthogonal to the branch-protection question.
- Our current `publish-maven.yml` uses `mvn release:perform -Dgoals="deploy"`, which invokes the standard `maven-deploy-plugin`. This still works — Sonatype hasn't deprecated the old OSSRH route yet.
- Switching to `central-publishing-maven-plugin` would be a good modernization step but **does not affect whether we need branch protection bypass**.
- The snapshot workflow already uses plain `mvn deploy`, which also works with the old route.

**Recommendation:** Consider switching to `central-publishing-maven-plugin` as a separate, lower-priority improvement. It does not intersect with the branch-protection decision.

---

## 5. The Real Trade-Off

### Keeping `maven-release-plugin` (Status Quo)

| Pros                                          | Cons                                                             |
| --------------------------------------------- | ---------------------------------------------------------------- |
| Already working and tested through 3 releases | Requires branch protection bypass for one workflow               |
| Battle-tested pattern, well-understood        | The bypass is a permanent exception in repo policy               |
| Plugin actively maintained (v3.3.1, Dec 2025) | Commits directly to `main` (though mechanical, not code changes) |
| Rollback built-in (`mvn release:rollback`)    |                                                                  |
| Zero additional engineering work              |                                                                  |

### Switching to CI-Friendly Versions

| Pros                                                       | Cons                                                      |
| ---------------------------------------------------------- | --------------------------------------------------------- |
| No branch protection bypass needed                         | ~2 days of engineering work                               |
| pom.xml never changes in `main` for releases               | New, untested publish pipeline                            |
| Aligns with Maven's stated modern direction                | More shell scripting in workflows (version calc, tagging) |
| Version is always derived from tag, single source of truth | `flatten-maven-plugin` adds build complexity              |
|                                                            | Loss of `mvn release:rollback` safety net                 |
|                                                            | Risk of a broken release during cutover                   |

---

## 6. Recommendation

**Keep `maven-release-plugin` for GA. Defer the CI-friendly migration to a post-GA improvement.**

Rationale:

1. **Risk vs. reward timing is wrong.** We're in Phase 0 of a monorepo migration. Adding a release infrastructure rewrite on top of the migration increases risk for no immediate user benefit. The branch protection bypass is scoped to a single `workflow_dispatch`-triggered workflow — it's not a standing vulnerability.

2. **The plugin is not legacy.** v3.3.1 was released December 2025. It's the most actively maintained it's been in years. The agent's characterization was inaccurate.

3. **The bypass scope is minimal.** Only `java-publish.yml` needs it. The bypass can be configured as a ruleset exception for a specific GitHub App or PAT, limited to commits matching `[maven-release-plugin]*` patterns.

4. **Post-GA is the right time.** After GA, when the monorepo migration is complete and the release cadence is established, a CI-friendly migration can be done as a focused improvement with proper testing, including dry-run publishes to Maven Central staging.

### If Steve Requires No Bypass Before GA

If the monorepo maintainer absolutely cannot grant a branch protection bypass, the fallback is:

1. Switch to CI-friendly versions + `central-publishing-maven-plugin` (~2 days work)
2. Accept the risk of a new, untested release pipeline during migration
3. Plan for at least one "dry-run" release to validate the pipeline before the first real GA publish

This is doable but adds unnecessary risk during an already complex migration phase.

---

## 7. Action Items

| #   | Action                                                                                          | Priority | Timing        |
| --- | ----------------------------------------------------------------------------------------------- | -------- | ------------- |
| 1   | Request branch protection bypass scoped to `JAVA_RELEASE_TOKEN` PAT for `java-publish.yml` only | High     | Now (Phase 0) |
| 2   | Document the bypass in the monorepo's security/access policy                                    | Medium   | Phase 2       |
| 3   | Evaluate CI-friendly version migration as post-GA improvement                                   | Low      | Post-GA       |
| 4   | Evaluate `central-publishing-maven-plugin` adoption (orthogonal)                                | Low      | Post-GA       |
