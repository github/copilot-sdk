---
name: shepherd-task-to-ready
description: "Use this skill to shepherd a child Task issue from 'assigned to Copilot' through CI approval and review-agent feedback resolution, stopping just before marking the PR as **Ready for review**."
---

# Skill: Shepherd Task to Ready for Review

## Purpose

Automate the lifecycle of a child **Task** issue from "assigned to Copilot" through CI passing and review-agent feedback resolution, stopping just before marking the PR as **Ready for review**.

## Inputs

- `TASK_ISSUE`: The issue number (e.g., `1850`) or URL of the child task to shepherd.
- `BASE_BRANCH`: The base branch the task PR should target (default: `upstream/edburns/1810-java-tool-ergonomics-tool-as-lambda`).
- `REPO`: Repository in `OWNER/REPO` format (default: `github/copilot-sdk`).

## Prerequisites

- `gh` CLI authenticated with sufficient permissions (issues, PRs, actions, reviews).
- The task issue already exists and has a clear description of work to do.
- The base branch exists in the repository.

---

## Procedure

### Step 1: Assign the task to @Copilot

Perform three actions **in order** to maximize the chance Copilot uses the correct base branch. This is critical because the issue description references plan files that only exist on `$BASE_BRANCH`, not on `main`.

#### 1a. Prepend a prominent base-branch instruction to the issue body

This must happen **before** assignment to avoid a race condition where Copilot targets `main` instead.

**Idempotency:** If the issue body already starts with `> [!IMPORTANT]`, skip the prepend (it was already done in a prior run).

```bash
# Check if already prepended (idempotency guard)
CURRENT_BODY=$(gh issue view $TASK_ISSUE -R $REPO --json body --jq '.body')
if echo "$CURRENT_BODY" | head -1 | grep -q '^\> \[!IMPORTANT\]'; then
  echo "Base branch instruction already present — skipping prepend."
else
  # Prepend base branch instruction (use --body-file to preserve markdown formatting)
  gh issue view $TASK_ISSUE -R $REPO --json body --jq '.body' > /tmp/issue-body-$TASK_ISSUE.md
  cat > /tmp/issue-body-$TASK_ISSUE-new.md <<HEADER
> [!IMPORTANT]
> ## You MUST branch from \`$BASE_BRANCH\`
> **Do NOT use \`main\` as your base branch.** The plan files and context referenced below exist ONLY on \`$BASE_BRANCH\`.
> - Create your topic branch from: \`$BASE_BRANCH\`
> - Your PR must target: \`$BASE_BRANCH\`
> - The first line of your PR description must be: \`Fixes #$TASK_ISSUE\`

--------

HEADER
  cat /tmp/issue-body-$TASK_ISSUE.md >> /tmp/issue-body-$TASK_ISSUE-new.md
  gh issue edit $TASK_ISSUE -R $REPO --body-file /tmp/issue-body-$TASK_ISSUE-new.md
  rm -f /tmp/issue-body-$TASK_ISSUE.md /tmp/issue-body-$TASK_ISSUE-new.md
fi
```

> **PowerShell equivalent** (when running on Windows):
> ```powershell
> $body = gh issue view $TASK_ISSUE -R $REPO --json body --jq '.body' | Out-String
> if ($body.TrimStart().StartsWith("> [!IMPORTANT]")) {
>     Write-Host "Base branch instruction already present - skipping prepend."
> } else {
>     $instruction = @"
> > [!IMPORTANT]
> > ## You MUST branch from ``$BASE_BRANCH``
> > **Do NOT use ``main`` as your base branch.** The plan files and context referenced below exist ONLY on ``$BASE_BRANCH``.
> > - Create your topic branch from: ``$BASE_BRANCH``
> > - Your PR must target: ``$BASE_BRANCH``
> > - The first line of your PR description must be: ``Fixes #$TASK_ISSUE``
>
> --------
>
> "@
>     $newBody = $instruction + $body
>     $tmpFile = [System.IO.Path]::GetTempFileName()
>     Set-Content -Path $tmpFile -Value $newBody -NoNewline
>     gh issue edit $TASK_ISSUE -R $REPO --body-file $tmpFile
>     Remove-Item $tmpFile
> }
> ```

#### 1b. Assign Copilot

```bash
gh issue edit $TASK_ISSUE --add-assignee "@copilot" -R $REPO
```

#### 1c. Post a reinforcing comment immediately after assignment

This comment acts as a second signal. Copilot reads both the issue body and comments when starting work.

```bash
gh issue comment $TASK_ISSUE -R $REPO --body "@copilot IMPORTANT: You MUST create your branch from \`$BASE_BRANCH\` and target your PR at \`$BASE_BRANCH\`. Do NOT use \`main\`. The plan files referenced in this issue only exist on \`$BASE_BRANCH\`."
```

> **PowerShell equivalent:**
> ```powershell
> gh issue comment $TASK_ISSUE -R $REPO --body "@copilot IMPORTANT: You MUST create your branch from ``$BASE_BRANCH`` and target your PR at ``$BASE_BRANCH``. Do NOT use ``main``. The plan files referenced in this issue only exist on ``$BASE_BRANCH``."
> ```

This triggers Copilot to:
1. Create a topic branch from `$BASE_BRANCH`.
2. Open a draft PR targeting `$BASE_BRANCH`.
3. Push initial commits.

**Fallback (applied in Step 2 after PR is found):** Verify the PR targets `$BASE_BRANCH`. If Copilot ignored the instructions, fix the base and request a rebase — see Step 2.

### Step 2: Find the corresponding PR

Use **all three** of the following strategies (in order) each polling iteration. Copilot often creates PRs whose title or branch name does NOT contain the issue number — it may use a descriptive name instead. Therefore, relying on title/branch regex alone is insufficient.

#### Strategy A: Query the issue timeline for linked PRs

The GitHub timeline API shows PRs linked via "Fixes #N" or the UI link feature. This is the most reliable signal.

```bash
# Query issue timeline for cross-referenced or connected PRs
PR_NUMBER=$(gh api "/repos/$REPO/issues/$TASK_ISSUE/timeline" \
  --jq '.[] | select(.event == "cross-referenced") | select(.source.issue.pull_request != null) | select(.source.issue.state == "open") | .source.issue.number' | head -1)
```

#### Strategy B: Search PR bodies for "Fixes #N" or "#N"

Copilot PRs typically include "Fixes #1876" in the body even when the title is descriptive.

```bash
# Search open PR bodies for the issue number
PR_NUMBER=$(gh pr list -R $REPO --state open --json number,body \
  --jq ".[] | select(.body | test(\"#$TASK_ISSUE\")) | .number" | head -1)
```

#### Strategy C: Match title or branch name (original approach)

```bash
PR_NUMBER=$(gh pr list -R $REPO --state open --json number,title,headRefName \
  --jq ".[] | select((.title | test(\"$TASK_ISSUE\"; \"i\")) or (.headRefName | test(\"$TASK_ISSUE\"))) | .number" | head -1)
```

#### Polling loop

Try all three strategies each iteration. Poll every 30 seconds for up to 15 minutes (Copilot coding agent can take 5-12 minutes to produce a PR).

```bash
TIMEOUT=900
INTERVAL=30
ELAPSED=0

while [ $ELAPSED -lt $TIMEOUT ]; do
  # Strategy A: issue timeline
  PR_NUMBER=$(gh api "/repos/$REPO/issues/$TASK_ISSUE/timeline" \
    --jq '.[] | select(.event == "cross-referenced") | select(.source.issue.pull_request != null) | select(.source.issue.state == "open") | .source.issue.number' 2>/dev/null | head -1)

  # Strategy B: PR body search
  if [ -z "$PR_NUMBER" ]; then
    PR_NUMBER=$(gh pr list -R $REPO --state open --json number,body \
      --jq ".[] | select(.body | test(\"#$TASK_ISSUE\")) | .number" | head -1)
  fi

  # Strategy C: title/branch match
  if [ -z "$PR_NUMBER" ]; then
    PR_NUMBER=$(gh pr list -R $REPO --state open --json number,title,headRefName \
      --jq ".[] | select((.title | test(\"$TASK_ISSUE\"; \"i\")) or (.headRefName | test(\"$TASK_ISSUE\"))) | .number" | head -1)
  fi

  if [ -n "$PR_NUMBER" ]; then
    break
  fi

  sleep $INTERVAL
  ELAPSED=$((ELAPSED + INTERVAL))
done
```

If no PR is found after timeout, report failure and stop.

Once the PR is found, verify and fix the base branch if needed. This is **critical** — if Copilot branched from `main`, its working tree won't contain the plan files referenced in the issue.

```bash
# Check the PR targets the correct base branch
ACTUAL_BASE=$(gh pr view $PR_NUMBER -R $REPO --json baseRefName --jq '.baseRefName')
if [ "$ACTUAL_BASE" != "$BASE_BRANCH" ]; then
  echo "WARNING: PR #$PR_NUMBER targets '$ACTUAL_BASE' instead of '$BASE_BRANCH'. Fixing..."
  gh pr edit $PR_NUMBER -R $REPO --base "$BASE_BRANCH"

  # Tell Copilot to rebase onto the correct base so it picks up the plan files
  gh pr review $PR_NUMBER -R $REPO --request-changes --body "@copilot CRITICAL: Your branch was created from the wrong base. You MUST rebase your branch onto \`$BASE_BRANCH\` immediately. The plan files and context referenced in issue #$TASK_ISSUE only exist on \`$BASE_BRANCH\`. Run: \`git rebase origin/$BASE_BRANCH\` and force-push. Do this BEFORE any other work."
fi
```

### Step 3: Wait for initial commits and workflow trigger

After the PR is created, Copilot pushes commits which trigger workflow runs. These runs require approval because every Copilot push triggers the "Approve workflows to run" gate.

You may be coming to this PR after all the runs have been manually approved. In that case, you need to wait for the runs to complete, then, skip to step 6. Here is how you wait for the runs to complete.

```bash
gh pr checks $PR_NUMBER -R $REPO --watch
```

Otherwise, wait for runs to appear in `action_required` status:

```bash
# Wait for workflow runs needing approval
gh run list -R $REPO --branch "$JTBDTASK_BRANCH" --status action_required \
  --json databaseId,name,status --jq '.[].databaseId'
```

### Steps 4–5: Approve pending workflow runs and wait for completion

Invoke the **`shepherd-task-approve-workflows-and-wait-for-completion`** skill (`.github/skills/shepherd-task-approve-workflows-and-wait-for-completion/SKILL.md`) with:

- `REPO` = `$REPO`
- `JTBDTASK_BRANCH` = the PR's topic branch
- `PR_NUMBER` = `$PR_NUMBER`

This sub-skill approves all `action_required` runs via `gh run rerun` and waits for completion via `gh pr checks --watch`.

### Step 6: Evaluate workflow results

**Note:** Ignore failures from the "Block remove-before-merge paths" / "No remove-before-merge directories" workflow. This failure is expected on feature branches and is not a real problem.

```bash
# Get check results, excluding the expected "Block remove-before-merge paths" failure
RESULTS=$(gh pr checks $PR_NUMBER -R $REPO --json name,state,bucket \
  --jq '.[] | select(.bucket == "fail") | select(.name != "No remove-before-merge directories")')
```

If there are real failures (after excluding the expected one), proceed to Step 7. If all pass, proceed to Step 8.

### Step 7: Request changes from Copilot (iteration loop)

**Max iterations: 20**

When CI fails or review agents flag problems:

#### 7.1: Gather failure details

```bash
# Get failed run IDs
FAILED_RUNS=$(gh run list -R $REPO --branch "$JTBDTASK_BRANCH" \
  --status completed --json databaseId,conclusion,name \
  --jq '.[] | select(.conclusion == "failure") | .databaseId')

# Get logs for failed runs (only failed steps)
for RUN_ID in $FAILED_RUNS; do
  gh run view $RUN_ID -R $REPO --log-failed
done
```

#### 7.2: Gather review agent comments

```bash
# Get review comments on the PR
gh api "/repos/$REPO/pulls/$PR_NUMBER/comments" \
  --jq '.[] | select(.user.type == "Bot") | {user: .user.login, body: .body}'

# Also get issue-level comments (review agents sometimes post there)
gh pr view $PR_NUMBER -R $REPO --comments --json comments \
  --jq '.comments[] | select(.author.login | test("bot|copilot|agent"; "i")) | {author: .author.login, body: .body}'
```

#### 7.3: Compose and submit a "Request changes" review

Analyze the failures and compose a hybrid message: relevant log excerpts plus a short targeted instruction for Copilot.

```bash
# Submit review requesting changes, @mentioning Copilot
gh pr review $PR_NUMBER -R $REPO --request-changes --body "$REVIEW_BODY"
```

The `$REVIEW_BODY` should follow this format:

```
@copilot Please fix the following issues:

## CI Failure: [workflow name]

<relevant log excerpt, trimmed to the essential error>

**Fix:** [Short, specific instruction on what to change]

## Review Comment from [bot name]

> [quoted comment]

**Fix:** [Short, specific instruction on what to change]
```

#### 7.4: Wait for Copilot to push fixes

After submitting the review, wait for new commits on the branch:

```bash
# Get current HEAD SHA
CURRENT_SHA=$(gh pr view $PR_NUMBER -R $REPO --json headRefOid --jq '.headRefOid')

# Poll for new commits (up to 10 minutes)
TIMEOUT=600
INTERVAL=30
ELAPSED=0

while [ $ELAPSED -lt $TIMEOUT ]; do
  NEW_SHA=$(gh pr view $PR_NUMBER -R $REPO --json headRefOid --jq '.headRefOid')
  if [ "$NEW_SHA" != "$CURRENT_SHA" ]; then
    break
  fi
  sleep $INTERVAL
  ELAPSED=$((ELAPSED + INTERVAL))
done
```

#### 7.5: Loop back

Return to **Step 4** (approve workflows) and repeat. Track iteration count. If 20 iterations are exhausted without all checks passing, stop and report:

```
SHEPHERD FAILED: Exhausted 20 iterations on PR #$PR_NUMBER for task #$TASK_ISSUE.
Manual intervention required.
```

### Step 8: Address pre-Ready-for-Review comments

Even when CI passes, review agents (e.g., "Copilot code review", "SDK Consistency Review Agent") may leave comments that should be addressed before marking ready.

#### 8.1: Check for unresolved review comments

```bash
# Get all review comments that haven't been resolved
gh api "/repos/$REPO/pulls/$PR_NUMBER/reviews" \
  --jq '.[] | select(.state == "CHANGES_REQUESTED") | {user: .user.login, body: .body}'

# Get pending review threads
gh api "/repos/$REPO/pulls/$PR_NUMBER/comments" \
  --jq '.[] | select(.user.type == "Bot") | {id: .id, user: .user.login, body: .body, path: .path, line: .line}'
```

#### 8.2: If unresolved comments exist, iterate

Use the same pattern as Step 7: compose a review requesting changes with specific instructions, wait for Copilot to push, approve workflows, and check results. This shares the same 20-iteration budget.

### Step 9: Final status report

When all checks pass and no unresolved review comments remain:

```
SHEPHERD COMPLETE: PR #$PR_NUMBER for task #$TASK_ISSUE is ready to review for marking as **Ready to review**.
All CI checks pass. No unresolved review comments.
Next step: Mark as Ready for Review (use separate skill).
```

---

## Error handling

- **PR not created within 10 minutes**: Report and stop.
- **Copilot doesn't push after review request within 10 minutes**: Report and stop.
- **20 iterations exhausted**: Report and stop.
- **API errors**: Retry up to 3 times with 10-second backoff, then report and stop.

## Notes

- This skill runs in a `copilot --yolo` session on a Dev Box, executing as the authenticated user.
- The skill does NOT mark the PR as "Ready for review" — that is a separate skill.
- The `gh api .../actions/runs/{id}/approve` endpoint is the programmatic equivalent of the "Approve and run" button in the GitHub UI.
- Review comments from bots/agents are treated the same as CI failures for iteration purposes.
- **Do NOT edit any plan/checklist files** (e.g., `1810-ignorance-reduction-for-implementation-plan.md`) to mark tasks as complete. Marking checklist items is outside the scope of this skill.
