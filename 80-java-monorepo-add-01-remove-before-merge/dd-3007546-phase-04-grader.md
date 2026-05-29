# DD-3007546: Phase 04 Grader — Verify Implementation

You are a grading agent. The implementer claims to have completed Phase 04 of the Java monorepo migration (Agentic Workflows and Skills). Your job is to verify their work meets all requirements.

## Instructions

Run each check below. Report PASS or FAIL for each item. At the end, provide an overall verdict: PASS (all checks green), PARTIAL (some non-critical failures), or FAIL (critical items missing).

---

## Check 1: Files Exist

Verify these files were created. Run:

```bash
test -f .github/scripts/java-reference-impl-sync/merge-reference-impl-start.sh && echo "PASS" || echo "FAIL"
test -f .github/scripts/java-reference-impl-sync/merge-reference-impl-diff.sh && echo "PASS" || echo "FAIL"
test -f .github/scripts/java-reference-impl-sync/merge-reference-impl-finish.sh && echo "PASS" || echo "FAIL"
test -f .github/scripts/java-reference-impl-sync/sync-cli-version-from-reference-impl.sh && echo "PASS" || echo "FAIL"
test -f .github/scripts/java-reference-impl-sync/sync-codegen-version.sh && echo "PASS" || echo "FAIL"
test -f .github/scripts/java-reference-impl-sync/format-and-test.sh && echo "PASS" || echo "FAIL"
test -f .github/workflows/java-reference-impl-sync.md && echo "PASS" || echo "FAIL"
test -f .github/workflows/java-reference-impl-sync.lock.yml && echo "PASS" || echo "FAIL"
test -f .github/prompts/java-agentic-merge-reference-impl.prompt.md && echo "PASS" || echo "FAIL"
test -f .github/prompts/java-coding-agent-merge-reference-impl-instructions.md && echo "PASS" || echo "FAIL"
test -f .github/skills/java-agentic-merge-reference-impl/SKILL.md && echo "PASS" || echo "FAIL"
test -f .github/skills/commit-as-pull-request/SKILL.md && echo "PASS" || echo "FAIL"
test -f .github/prompts/commit-as-pull-request.prompt.md && echo "PASS" || echo "FAIL"
test -f .github/scripts/java-ci/parse-repo-info.sh && echo "PASS" || echo "FAIL"
test -f .github/scripts/java-ci/commit-and-push.sh && echo "PASS" || echo "FAIL"
test -f .github/scripts/java-ci/sync-after-merge.sh && echo "PASS" || echo "FAIL"
```

---

## Check 2: Scripts Are Executable

```bash
for f in .github/scripts/java-reference-impl-sync/*.sh .github/scripts/java-ci/*.sh; do
  if [ -x "$f" ]; then echo "PASS: $f"; else echo "FAIL: $f not executable"; fi
done
```

---

## Check 3: No External Repo References in New Files

The sync is now intra-repo. No new file should clone or reference `https://github.com/github/copilot-sdk.git` for diffing purposes.

```bash
grep -r "git clone.*copilot-sdk" .github/scripts/java-reference-impl-sync/ .github/prompts/java-* .github/workflows/java-reference-impl-sync.md 2>/dev/null
```

- **PASS** if no output (or only in comments explaining the old approach)
- **FAIL** if any active `git clone` of the external repo exists

---

## Check 4: Workflow Trigger Is Dispatch-Only

```bash
grep -A5 "^on:" .github/workflows/java-reference-impl-sync.md | head -10
```

- **PASS** if only `workflow_dispatch:` appears (no `schedule:`)
- **FAIL** if `schedule:` is present

---

## Check 5: Lock File Was Compiled

```bash
# The lock file should exist and contain a content hash
grep -q "content-hash" .github/workflows/java-reference-impl-sync.lock.yml && echo "PASS" || echo "FAIL"
```

Also verify the other recompiled lock files:
```bash
grep -q "content-hash" .github/workflows/sdk-consistency-review.lock.yml && echo "PASS" || echo "FAIL"
grep -q "content-hash" .github/workflows/issue-triage.lock.yml && echo "PASS" || echo "FAIL"
```

---

## Check 6: sdk-consistency-review.md Includes Java

```bash
grep -q "java" .github/workflows/sdk-consistency-review.md && echo "PASS" || echo "FAIL"
grep -q "'java/\*\*'" .github/workflows/sdk-consistency-review.md && echo "PASS: path trigger" || echo "WARN: check path format"
```

---

## Check 7: issue-triage.md Includes sdk/java Label

```bash
grep -q "sdk/java" .github/workflows/issue-triage.md && echo "PASS" || echo "FAIL"
```

---

## Check 8: `.lastmerge` Contains a Monorepo SHA

```bash
LAST_MERGE=$(cat java/.lastmerge | tr -d '[:space:]')
# Verify it's a valid commit in THIS repo
git cat-file -t "$LAST_MERGE" 2>/dev/null && echo "PASS: valid monorepo commit" || echo "FAIL: not a valid local commit"
```

---

## Check 9: Path References in Prompt File

The main prompt file (`.github/prompts/java-agentic-merge-reference-impl.prompt.md`) should reference `java/`-prefixed paths:

```bash
# Should find java/ prefixed paths
grep -c "java/src/" .github/prompts/java-agentic-merge-reference-impl.prompt.md
grep -c "java/pom.xml" .github/prompts/java-agentic-merge-reference-impl.prompt.md
grep -c "java/.lastmerge" .github/prompts/java-agentic-merge-reference-impl.prompt.md
grep -c "java/scripts/codegen" .github/prompts/java-agentic-merge-reference-impl.prompt.md
```

- **PASS** if all return non-zero counts
- **FAIL** if any return 0

Also check for OLD paths that should NOT be present:
```bash
# Should NOT find bare paths (without java/ prefix) for Java-specific resources
grep -n "^\./\.github/scripts/reference-impl-sync/" .github/prompts/java-agentic-merge-reference-impl.prompt.md
```

- **PASS** if no output (paths use the new `java-reference-impl-sync` directory name)
- **FAIL** if old paths found

---

## Check 10: Scripts Reference Correct Paths

Check that the merge scripts use `java/.lastmerge` and `java/pom.xml`:

```bash
grep -l "java/.lastmerge" .github/scripts/java-reference-impl-sync/merge-reference-impl-start.sh && echo "PASS" || echo "FAIL"
grep -l "java/pom.xml" .github/scripts/java-reference-impl-sync/sync-cli-version-from-reference-impl.sh && echo "PASS" || echo "FAIL"
grep -l "java/scripts/codegen" .github/scripts/java-reference-impl-sync/sync-codegen-version.sh && echo "PASS" || echo "FAIL"
```

---

## Check 11: The Diff Script Uses Local Git (No Clone)

```bash
# Should NOT contain git clone
grep -c "git clone" .github/scripts/java-reference-impl-sync/merge-reference-impl-diff.sh
```

- **PASS** if returns 0
- **FAIL** if returns non-zero

```bash
# Should contain local git diff pattern
grep -c 'git diff.*LAST_MERGE' .github/scripts/java-reference-impl-sync/merge-reference-impl-diff.sh || \
grep -c 'git log.*LAST_MERGE' .github/scripts/java-reference-impl-sync/merge-reference-impl-diff.sh
```

- **PASS** if returns non-zero
- **FAIL** if returns 0

---

## Check 12: Java Build Still Passes

```bash
cd java && mvn clean verify
```

- **PASS** if `BUILD SUCCESS`
- **FAIL** if `BUILD FAILURE`

---

## Check 13: ABSOLUTE PROHIBITION Preserved

The prompt file must contain the prohibition against touching generated code:

```bash
grep -c "src/generated/java" .github/prompts/java-agentic-merge-reference-impl.prompt.md
grep -c "ABSOLUTE PROHIBITION" .github/prompts/java-agentic-merge-reference-impl.prompt.md
```

- **PASS** if both return non-zero counts
- **FAIL** if either returns 0

---

## Check 14: Coding Agent Instructions Reference Correct Prompt

```bash
grep -q "java-agentic-merge-reference-impl.prompt.md" .github/prompts/java-coding-agent-merge-reference-impl-instructions.md && echo "PASS" || echo "FAIL"
```

---

## Check 15: Skill SKILL.md References Correct Prompt

```bash
grep -q "java-agentic-merge-reference-impl" .github/skills/java-agentic-merge-reference-impl/SKILL.md && echo "PASS" || echo "FAIL"
```

---

## Check 16: `.merge-env` Is Gitignored

```bash
grep -q "\.merge-env" .gitignore && echo "PASS" || echo "FAIL: .merge-env not in .gitignore"
```

---

## Severity Classification

**Critical (any FAIL = overall FAIL)**:
- Check 1 (core files exist)
- Check 3 (no external clone)
- Check 4 (dispatch-only trigger)
- Check 5 (lock file compiled)
- Check 8 (.lastmerge is valid monorepo SHA)
- Check 9 (correct paths in prompt)
- Check 12 (Java build passes)
- Check 13 (ABSOLUTE PROHIBITION preserved)

**Important (FAIL = PARTIAL)**:
- Check 2 (scripts executable)
- Check 6 (consistency review includes Java)
- Check 7 (issue triage includes sdk/java)
- Check 10 (script paths correct)
- Check 11 (no clone in diff script)
- Check 14 (coding agent instructions)
- Check 15 (skill reference)

**Minor (FAIL = note but still PASS overall)**:
- Check 16 (.merge-env gitignored)

---

## Overall Verdict

Report format:
```
## Results

| # | Check | Result |
|---|-------|--------|
| 1 | Files exist | PASS/FAIL |
| 2 | Scripts executable | PASS/FAIL |
| ... | ... | ... |

## Verdict: PASS / PARTIAL / FAIL

### Issues Found (if any):
- Description of each failure and what needs to be fixed
```
