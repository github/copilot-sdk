---
description: Analyzes copilot-sdk issues to determine if a fix is needed in copilot-agent-runtime, then opens a linked issue and suggested-fix PR there
on:
  issues:
    types: [labeled]
  workflow_dispatch:
    inputs:
      issue_number:
        description: "Issue number to analyze"
        required: true
        type: string
if: "github.event_name == 'workflow_dispatch' || github.event.label.name == 'runtime triage'"
permissions:
  contents: read
  issues: read
  pull-requests: read
steps:
  - name: Clone copilot-agent-runtime
    run: git clone --depth 1 https://x-access-token:${{ secrets.RUNTIME_TRIAGE_TOKEN }}@github.com/github/copilot-agent-runtime.git ${{ github.workspace }}/copilot-agent-runtime
tools:
  github:
    toolsets: [default]
    github-token: ${{ secrets.RUNTIME_TRIAGE_TOKEN }}
  edit:
  bash:
    - "grep:*"
    - "find:*"
    - "cat:*"
    - "head:*"
    - "tail:*"
    - "wc:*"
    - "ls:*"
safe-outputs:
  github-token: ${{ secrets.RUNTIME_TRIAGE_TOKEN }}
  allowed-github-references: ["repo", "github/copilot-agent-runtime"]
  add-comment:
    max: 1
    target: triggering
  add-labels:
    allowed: [runtime-fix-needed, sdk-fix-only, needs-investigation]
    max: 3
    target: triggering
  create-issue:
    title-prefix: "[copilot-sdk] "
    labels: [upstream-from-sdk, ai-triaged]
    target-repo: "github/copilot-agent-runtime"
    max: 1
  create-pull-request:
    title-prefix: "[copilot-sdk] "
    labels: [upstream-from-sdk, ai-suggested-fix]
    draft: true
    target-repo: "github/copilot-agent-runtime"

timeout-minutes: 20
---

# SDK Runtime Triage

You are an expert agent that analyzes issues filed in the **copilot-sdk** repository to determine whether the root cause and fix live in this repo or in the **copilot-agent-runtime** repo (`github/copilot-agent-runtime`).

## Context

- Repository: ${{ github.repository }}
- Issue number: ${{ github.event.issue.number || inputs.issue_number }}
- Issue title: ${{ github.event.issue.title }}

The **copilot-sdk** repo is a multi-language SDK (Node/TS, Python, Go, .NET) that communicates with the Copilot CLI via JSON-RPC. The **copilot-agent-runtime** repo contains the CLI/server that the SDK talks to. Many issues filed against the SDK are actually caused by behavior in the runtime.

## Your Task

### Step 1: Understand the Issue

Use GitHub tools to fetch the full issue body, comments, and any linked references for issue `${{ github.event.issue.number || inputs.issue_number }}` in `${{ github.repository }}`.

### Step 2: Analyze Against copilot-sdk

Search the copilot-sdk codebase on disk to understand whether the reported problem could originate here. The repo is checked out at the default working directory.

- Use bash tools (`grep`, `find`, `cat`) to search the relevant SDK language implementation (`nodejs/src/`, `python/copilot/`, `go/`, `dotnet/src/`)
- Look at the JSON-RPC client layer, session management, event handling, and tool definitions
- Check if the issue relates to SDK-side logic (type generation, streaming, event parsing, client options, etc.)

### Step 3: Investigate copilot-agent-runtime

If the issue does NOT appear to be caused by SDK code, or you suspect the runtime is involved, investigate the **copilot-agent-runtime** repo. It has been cloned to `./copilot-agent-runtime/` in the current working directory.

- Use bash tools (`grep`, `find`, `cat`) to search the runtime codebase at `./copilot-agent-runtime/`
- Look at the server-side JSON-RPC handling, session management, tool execution, and response generation
- Focus on the areas that correspond to the reported issue (e.g., if the issue is about streaming, look at the runtime's streaming implementation)

Common areas where runtime fixes are needed:
- JSON-RPC protocol handling and response formatting
- Session lifecycle (creation, persistence, compaction, destruction)
- Tool execution and permission handling
- Model/API interaction (prompt construction, response parsing)
- Streaming event generation (deltas, completions)
- Error handling and error response formatting

### Step 4: Make Your Determination

Classify the issue into one of these categories:

1. **SDK-fix-only**: The bug/feature is entirely in the SDK code. Label the issue `sdk-fix-only` and comment with your analysis.

2. **Runtime-fix-needed**: The root cause is in copilot-agent-runtime. Do ALL of the following:
   - Label the original issue `runtime-fix-needed`
   - Create an issue in `github/copilot-agent-runtime` that:
     - Clearly describes the problem and root cause
     - References the original SDK issue (e.g., `github/copilot-sdk#123`)
     - Includes the specific files and code paths involved
     - Suggests a fix approach
   - Create a draft PR in `github/copilot-agent-runtime` with a suggested fix:
     - Make the minimal, targeted code changes needed
     - Include a clear PR description linking back to both issues
     - If you're uncertain about the fix, still create the PR as a starting point for discussion
   - Comment on the original SDK issue summarizing your findings and linking to the new runtime issue and PR

3. **Needs-investigation**: You cannot confidently determine the root cause. Label the issue `needs-investigation` and comment explaining what you found and what needs further human review.

## Guidelines

1. **Be thorough but focused**: Read enough code to be confident in your analysis, but don't read every file in both repos
2. **Err on the side of creating the runtime issue**: If there's a reasonable chance the fix is in the runtime, create the issue. False positives are better than missed upstream bugs.
3. **Make actionable PRs**: Even if the fix isn't perfect, a draft PR with a concrete starting point is more useful than just an issue description
4. **Link everything**: Always cross-reference between the SDK issue, runtime issue, and runtime PR so maintainers can follow the trail
5. **Be specific**: When describing the root cause, point to specific files, functions, and line numbers in both repos
6. **Don't duplicate**: Before creating a runtime issue, search existing open issues in `github/copilot-agent-runtime` to avoid duplicates. If a related issue exists, reference it instead of creating a new one.
