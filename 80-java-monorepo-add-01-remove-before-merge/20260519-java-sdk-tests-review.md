### 16:23 Prompt

Evaluate `java-sdk-tests.yml` and show me the differences with the file from which it was derived, `copilot-sdk-java-00/.github/workflows/build-test.yml`.


### 16:25 Response

Read [](file:///c%3A/Users/edburns/workareas/copilot-sdk-00/.github/workflows/java-sdk-tests.yml)

Read [](file:///c%3A/Users/edburns/workareas/copilot-sdk-java-00/.github/workflows/build-test.yml#1-1), lines 1 to 150

Here's a comparison of the key differences:

## Structural Differences

| Aspect | build-test.yml (source) | java-sdk-tests.yml (new) |
|--------|--------------------------|---------------------------|
| **Name** | "Build & Test" | "Java SDK Tests" |
| **Schedule trigger** | Weekly cron (Sundays 00:00 UTC) | Removed |
| **Push paths** | `paths-ignore` (README, LICENSE, .github) | No path filter on push to main |
| **PR paths** | `paths-ignore` pattern | `paths` include pattern (`java/**`, `test/**`, workflow file) with doc exclusions |
| **OS matrix** | `ubuntu-latest` only | 3-OS matrix (`ubuntu`, `macos`, `windows`) |
| **Working directory** | None (repo root = java root) | `working-directory: ./java` |
| **Smoke test job** | Yes (calls `run-smoke-test.yml`, gates main job) | Removed (deferred to Phase 3) |
| **Permissions** | `contents: write`, `checks: write`, `pull-requests: write` | `contents: read` only |

## Removed Features (intentionally per plan)

- **Smoke test job** — Phase 3 concern
- **JaCoCo badge generation** + PR creation — not needed in monorepo CI
- **`validate reference-impl-sync completeness` step** — Phase 4 concern
- **Upload test results for site generation** — not needed in monorepo CI
- **`COPILOT_GITHUB_TOKEN` / `COPILOT_CLI_PATH` env vars** — not used
- **Clone-based test harness setup** (`cd target/copilot-sdk/nodejs && npm ci`) — replaced by local harness install

## Added/Changed Features

- **3-OS matrix** — matches other monorepo SDK test workflows
- **Explicit Node.js setup** + `npm ci` on nodejs and harness — replaces the clone-based approach since harness is now local
- **Path-scoped PR triggers** — only fires when java, test, or the workflow itself changes
- **Spotless/Javadoc gated to Linux only** (`if: runner.os == 'Linux'`) — avoids redundant formatting checks on all 3 OS
- **Action versions use tags** (`@v6.0.2`) instead of pinned commit SHAs — less secure but more readable; the source used full SHA pins

## Potential Concerns

1. **No SHA-pinned actions** — The source pinned actions to exact commit SHAs (e.g., `actions/checkout@de0fac2e...`). The new file uses version tags (`@v6.0.2`, `@v5`, `@v6`). This is less secure against supply-chain attacks but matches the style used in some monorepo workflows.

2. **Missing `persist-credentials: false`** on checkout — the source had this explicitly set for security.

3. **No `COPILOT_GITHUB_TOKEN`** — The source passed this to `mvn verify` for E2E tests that talk to a live Copilot CLI. If E2E tests in the monorepo still need a real token (vs. only the replay proxy), this would cause test failures. However, since the monorepo E2E tests use the replay proxy from harness, this is likely fine.

4. **`distribution: "microsoft"`** for JDK — both files use this, which is consistent.

5. **Push trigger has no path filter** — PRs are path-filtered, but pushes to `main` will run on every push regardless of which files changed. Other monorepo workflows (e.g., `dotnet-sdk-tests.yml`) may or may not have this same pattern — worth checking for consistency.

### 16:25 Response

Concerns

1. I want a push path filter. Should only run on channges to `/java`. 

2. For the 3-OS matrix, make it only for `ubuntu-latest`.

3. For permissions, make it have the same as `build-tests.yml`.

4. Yes, I do want the Jacoco badge creation. Copy that entire mechanism from `copilot-sdk-java-00`.

5. You stated

   `COPILOT_GITHUB_TOKEN / COPILOT_CLI_PATH env vars — not used` 
   
   Why is this? Looking at `build-tests.yml` those things are clearly in there:

   ```yml
   - name: Run Java SDK tests
     env:
       CI: "true"
       COPILOT_GITHUB_TOKEN: ${{ secrets.COPILOT_GITHUB_TOKEN }}
       COPILOT_CLI_PATH: ${{ steps.setup-copilot.outputs.path }}
     run: mvn verify
   ```
   
   Surely we need them on the `copilot-sdk-00` side as well. There are Java tests that don't just use the relay proxy.

6. Consider this review comment:

   > No SHA-pinned actions — The source pinned actions to exact commit SHAs (e.g., actions/checkout@de0fac2e...). The new file uses version tags (@v6.0.2, @v5, @v6). This is less secure against supply-chain attacks but matches the style used in some monorepo workflows.
   
   I want the pinned actions.

7. Consider this review comment:

   > Missing persist-credentials: false on checkout — the source had this explicitly set for security.
   
   I want this set here as well.
