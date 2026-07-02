#!/usr/bin/env bash
#
# shepherd-task.sh — Shepherds a child Task issue end-to-end:
# from Copilot assignment through merge.
#
# Orchestrates two phases by launching separate `copilot --yolo` sessions.
# Between phases, verifies state using gh CLI (not copilot exit codes).
#
# Usage: ./shepherd-task.sh <TASK_ISSUE> <BASE_BRANCH> <REPO>

set -euo pipefail

TASK_ISSUE="${1:?Usage: $0 <TASK_ISSUE> <BASE_BRANCH> <REPO>}"
BASE_BRANCH="${2:?Usage: $0 <TASK_ISSUE> <BASE_BRANCH> <REPO>}"
REPO="${3:?Usage: $0 <TASK_ISSUE> <BASE_BRANCH> <REPO>}"

# --- Helpers ---

status()  { echo -e "\033[36m[shepherd-task] $*\033[0m"; }
fail()    { echo -e "\033[31m[shepherd-task] FAILED: $*\033[0m"; exit 1; }
ok()      { echo -e "\033[32m[shepherd-task] $*\033[0m"; }

# Find the PR linked to the task issue using three strategies.
find_linked_pr() {
    local pr_number=""

    # Strategy A: Issue timeline for cross-referenced PRs
    pr_number=$(gh api "/repos/$REPO/issues/$TASK_ISSUE/timeline" \
        --jq '.[] | select(.event == "cross-referenced") | select(.source.issue.pull_request != null) | select(.source.issue.state == "open") | .source.issue.number' 2>/dev/null | head -1)

    if [[ -n "$pr_number" ]]; then echo "$pr_number"; return 0; fi

    # Strategy B: Search PR bodies for the issue number
    pr_number=$(gh pr list -R "$REPO" --state open --json number,body \
        --jq ".[] | select(.body | test(\"#$TASK_ISSUE\")) | .number" 2>/dev/null | head -1)

    if [[ -n "$pr_number" ]]; then echo "$pr_number"; return 0; fi

    # Strategy C: Title or branch name match
    pr_number=$(gh pr list -R "$REPO" --state open --json number,title,headRefName \
        --jq ".[] | select((.title | test(\"$TASK_ISSUE\"; \"i\")) or (.headRefName | test(\"$TASK_ISSUE\"))) | .number" 2>/dev/null | head -1)

    if [[ -n "$pr_number" ]]; then echo "$pr_number"; return 0; fi

    return 1
}

# Verify all CI checks pass (excluding expected failure).
ci_passing() {
    local pr_number="$1"
    local failures
    failures=$(gh pr checks "$pr_number" -R "$REPO" --json name,state,bucket \
        --jq '.[] | select(.bucket == "fail") | select(.name != "No remove-before-merge directories") | .name' 2>/dev/null)

    [[ -z "$failures" ]]
}

# Check for unresolved bot review comments.
no_unresolved_reviews() {
    local pr_number="$1"
    local unresolved
    unresolved=$(gh api graphql -F number="$pr_number" -f query='
    query($number: Int!) {
      repository(owner: "github", name: "copilot-sdk") {
        pullRequest(number: $number) {
          reviewThreads(first: 100) {
            nodes { isResolved comments(first: 1) { nodes { author { login } } } }
          }
        }
      }
    }' --jq '.data.repository.pullRequest.reviewThreads.nodes[] | select(.isResolved == false) | .comments.nodes[0].author.login' 2>/dev/null)

    [[ -z "$unresolved" ]]
}

# =============================================================================
# PHASE 1: Assignment to Ready for Review
# =============================================================================

status "Phase 1: Launching copilot --yolo for task #$TASK_ISSUE"

PHASE1_PROMPT="Invoke skill \`shepherd-task-to-ready\` with these inputs:

- TASK_ISSUE: $TASK_ISSUE
- BASE_BRANCH: $BASE_BRANCH
- REPO: $REPO"

status "Phase 1 prompt:"
echo "$PHASE1_PROMPT"
echo "$PHASE1_PROMPT" | copilot --yolo

status "Phase 1: copilot exited. Verifying state..."

# --- Verify Phase 1 outcome ---
PR_NUMBER=$(find_linked_pr) || fail "No open PR found linked to issue #$TASK_ISSUE after Phase 1."
status "Found PR #$PR_NUMBER"

# Verify base branch
ACTUAL_BASE=$(gh pr view "$PR_NUMBER" -R "$REPO" --json baseRefName --jq '.baseRefName')
if [[ "$ACTUAL_BASE" != "$BASE_BRANCH" ]]; then
    status "PR base is '$ACTUAL_BASE', fixing to '$BASE_BRANCH'..."
    gh pr edit "$PR_NUMBER" -R "$REPO" --base "$BASE_BRANCH"
fi

# Verify CI passing
ci_passing "$PR_NUMBER" || fail "CI checks not passing on PR #$PR_NUMBER after Phase 1."

# Verify no unresolved reviews
no_unresolved_reviews "$PR_NUMBER" || fail "Unresolved review comments remain on PR #$PR_NUMBER after Phase 1."

ok "Phase 1 VERIFIED: PR #$PR_NUMBER is ready. CI passing, no unresolved comments."

# =============================================================================
# PHASE 2: Ready for Review to Merged
# =============================================================================

status "Phase 2: Launching copilot --yolo for PR #$PR_NUMBER"

PHASE2_PROMPT="Invoke skill \`shepherd-task-from-ready-to-merged-to-base\` with these inputs:

- TASK_ISSUE: $TASK_ISSUE
- BASE_BRANCH: $BASE_BRANCH
- REPO: $REPO
- PR_NUMBER: $PR_NUMBER"

status "Phase 2 prompt:"
echo "$PHASE2_PROMPT"
echo "$PHASE2_PROMPT" | copilot --yolo

status "Phase 2: copilot exited. Verifying state..."

# --- Verify Phase 2 outcome ---
PR_STATE=$(gh pr view "$PR_NUMBER" -R "$REPO" --json state --jq '.state')
if [[ "$PR_STATE" != "MERGED" ]]; then
    fail "PR #$PR_NUMBER is in state '$PR_STATE', expected MERGED."
fi

# Verify merged into correct branch (strip remote prefix for comparison)
MERGED_BASE=$(gh pr view "$PR_NUMBER" -R "$REPO" --json baseRefName --jq '.baseRefName')
EXPECTED_BASE="${BASE_BRANCH#*/}"
if [[ "$MERGED_BASE" != "$EXPECTED_BASE" ]]; then
    fail "PR #$PR_NUMBER was merged into '$MERGED_BASE', expected '$EXPECTED_BASE'."
fi

# Verify issue is closed
ISSUE_STATE=$(gh issue view "$TASK_ISSUE" -R "$REPO" --json state --jq '.state')
if [[ "$ISSUE_STATE" != "CLOSED" ]]; then
    status "Issue #$TASK_ISSUE still open, closing..."
    gh issue close "$TASK_ISSUE" -R "$REPO"
fi

ok "SHEPHERD TASK COMPLETE: Task #$TASK_ISSUE has been fully shepherded."
ok "PR #$PR_NUMBER merged to $BASE_BRANCH."
exit 0
