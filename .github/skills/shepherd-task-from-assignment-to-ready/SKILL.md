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

First, prepend an instruction to the issue body telling Copilot which base branch to use. This must happen **before** assignment to avoid a race condition where Copilot targets `main` instead.

```bash
# Prepend base branch instruction to issue body
CURRENT_BODY=$(gh issue view $TASK_ISSUE -R $REPO --json body --jq '.body')
INSTRUCTION="**Base branch:** Create your PR targeting \`$BASE_BRANCH\` (not \`main\`).\n\n\n\n\n--------\n\n"
gh issue edit $TASK_ISSUE -R $REPO --body "${INSTRUCTION}${CURRENT_BODY}"
```

Then assign:

```bash
gh issue edit $TASK_ISSUE --add-assignee "@copilot" -R $REPO
```

This triggers Copilot to:
1. Create a topic branch from `$BASE_BRANCH`.
2. Open a draft PR targeting `$BASE_BRANCH`.
3. Push initial commits.

**Fallback:** After the PR is created (Step 2), verify it targets `$BASE_BRANCH`. If Copilot ignored the instruction, fix the base:

```bash
gh pr edit $PR_NUMBER -R $REPO --base "$BASE_BRANCH"
```

### Step 2: Find the corresponding PR

Look for the corresponding PR created and mentioned in the issue. The issue will often have text like "linked a pull request that will close this issue". First, try to find the PR this way. If you find it, let `PR_NUMBER` be that PR number.

If that doesn't work, wait for Copilot to create the PR.

Poll until a PR exists with the task issue linked or with a head branch referencing the issue number.

```bash
# Poll every 30 seconds for up to 10 minutes
TIMEOUT=600
INTERVAL=30
ELAPSED=0

while [ $ELAPSED -lt $TIMEOUT ]; do
  PR_NUMBER=$(gh pr list -R $REPO --state open --json number,title,headRefName \
    --jq ".[] | select(.title | test(\"$TASK_ISSUE\"; \"i\")) | .number" | head -1)
  
  if [ -n "$PR_NUMBER" ]; then
    break
  fi
  
  sleep $INTERVAL
  ELAPSED=$((ELAPSED + INTERVAL))
done
```

If no PR is found after timeout, report failure and stop.

Once the PR is found, verify and fix the base branch if needed:

```bash
# Check the PR targets the correct base branch
ACTUAL_BASE=$(gh pr view $PR_NUMBER -R $REPO --json baseRefName --jq '.baseRefName')
if [ "$ACTUAL_BASE" != "$BASE_BRANCH" ]; then
  gh pr edit $PR_NUMBER -R $REPO --base "$BASE_BRANCH"
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

### Step 4: Approve pending workflow runs

For each run in `action_required` status on the PR's branch, re-run it. The correct mechanism is `gh run rerun` (the `POST .../actions/runs/{id}/approve` endpoint is for fork PRs only and will return HTTP 403 here).

```bash
# Get all action_required runs for the PR branch
PENDING_RUNS=$(gh run list -R $REPO --branch "$JTBDTASK_BRANCH" \
  --json databaseId,conclusion --jq '.[] | select(.conclusion == "action_required") | .databaseId')

for RUN_ID in $PENDING_RUNS; do
  gh run rerun $RUN_ID -R $REPO
done
```

### Step 5: Wait for workflow runs to complete

```bash
# Watch all runs on the branch until they complete
# Use gh pr checks with --watch for convenience
gh pr checks $PR_NUMBER -R $REPO --watch --fail-fast
```

Alternatively, poll with:

```bash
gh run list -R $REPO --branch "$JTBDTASK_BRANCH" \
  --json databaseId,status,conclusion,name \
  --jq '.[] | select(.status != "completed")'
```

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
