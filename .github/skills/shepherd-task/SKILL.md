---
name: shepherd-task
description: "Use this skill to shepherd a child Task issue end-to-end: from assignment to Copilot, through CI and review, to merged into the specified base branch."
---

# Skill: Shepherd Task (End-to-End)

## Purpose

Orchestrates the full lifecycle of a child **Task** issue from assignment through merge. This skill simply invokes the two phase skills in sequence:

1. `shepherd-task-from-assignment-to-ready` — assigns to Copilot, waits for PR, approves workflows, iterates until CI passes and no review-agent comments remain.
2. `shepherd-task-from-ready-to-merged-to-base` — marks Ready for Review, addresses Copilot code review comments locally, iterates until clean, and merges to the base branch.

## Inputs

- `TASK_ISSUE`: The issue number (e.g., `1841`) or URL of the child task to shepherd.
- `BASE_BRANCH`: The base branch the task PR should target (e.g., `edburns/1810-java-tool-ergonomics-tool-as-lambda`).
- `REPO`: Repository in `OWNER/REPO` format (default: `github/copilot-sdk`).

## Prerequisites

- `gh` CLI authenticated with sufficient permissions (issues, PRs, actions, reviews).
- The task issue already exists and has a clear description of work to do.
- The base branch exists in the repository.

---

## Procedure

### Phase 1: Assignment to Ready for Review

Invoke the skill defined in `.github/skills/shepherd-task-from-assignment-to-ready/SKILL.md` with the same inputs:

- `TASK_ISSUE`: as provided
- `BASE_BRANCH`: as provided
- `REPO`: as provided

**If Phase 1 fails** (reports `SHEPHERD FAILED`), stop and propagate the failure. Do NOT proceed to Phase 2.

**If Phase 1 succeeds** (reports `SHEPHERD COMPLETE`), proceed to the context compaction step below.

### Context Compaction (between phases)

Before starting Phase 2, compact the conversation to free context window space. Run:

```
/compact Retain only: TASK_ISSUE=$TASK_ISSUE, PR_NUMBER (from Phase 1), BASE_BRANCH=$BASE_BRANCH, REPO=$REPO, branch name for the PR, and that Phase 1 completed successfully. Discard all polling output, CI logs, and intermediate step details.
```

Once compaction is complete, proceed to Phase 2.

### Phase 2: Ready for Review to Merged

Only if Phase 1 completed successfully, invoke the skill defined in `.github/skills/shepherd-task-from-ready-to-merged-to-base/SKILL.md` with the same inputs:

- `TASK_ISSUE`: as provided
- `BASE_BRANCH`: as provided
- `REPO`: as provided
- `PR_NUMBER`: carried over from Phase 1 (the PR that was created and shepherded)

### Final Status

On success:

```
SHEPHERD TASK COMPLETE: Task #$TASK_ISSUE has been fully shepherded.
PR merged to $BASE_BRANCH.
```

On failure in either phase:

```
SHEPHERD TASK FAILED: Task #$TASK_ISSUE failed during [Phase 1|Phase 2].
[Error details from the failed phase]
```

---

## Notes

- This skill runs in a `copilot --yolo` session on a Dev Box, executing as the authenticated user.
- The `PR_NUMBER` is determined during Phase 1 and passed implicitly to Phase 2.
- **Do edit the plan/checklist files** (e.g., `1810-ignorance-reduction-for-implementation-plan.md`) to mark the task as complete.
