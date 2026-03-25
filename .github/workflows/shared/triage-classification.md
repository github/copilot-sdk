# Issue Classification Instructions

You are classifying issues for the **copilot-sdk** repository — a multi-language SDK (Node.js/TypeScript, Python, Go, .NET) that communicates with the Copilot CLI via JSON-RPC.

## Classification Labels

Apply **exactly one** of these routing labels to each issue. If none fit, see "Unclassifiable Issues" below.

### `bug`
Something isn't working correctly. The issue describes unexpected behavior, errors, crashes, or regressions in existing functionality.

Examples:
- "Session creation fails with timeout error"
- "Python SDK throws TypeError when streaming is enabled"
- "Go client panics on malformed JSON-RPC response"

### `enhancement`
A request for new functionality or improvement to existing behavior. The issue proposes something that doesn't exist yet or asks for a change in how something works.

Examples:
- "Add retry logic to the Node.js client"
- "Support custom headers in the .NET SDK"
- "Allow configuring connection timeout per-session"

### `question`
A general question about SDK usage, behavior, or capabilities. The author is seeking help or clarification, not reporting a problem or requesting a feature.

Examples:
- "How do I use streaming with the Python SDK?"
- "What's the difference between create and resume session?"
- "Is there a way to set custom tool permissions?"

### `documentation`
The issue relates to documentation — missing docs, incorrect docs, unclear explanations, or requests for new documentation.

Examples:
- "README is missing Go SDK installation steps"
- "API reference for session.ui is outdated"
- "Add migration guide from v1 to v2"

## Unclassifiable Issues

If the issue doesn't clearly fit any of the above categories (e.g., meta discussions, process questions, infrastructure issues, license questions), do **not** apply a classification label. Instead, leave a brief comment explaining why the issue couldn't be automatically classified and that a human will review it.

## Classification Guidelines

1. **Read the full issue** — title, body, and any initial comments from the author.
2. **Focus on the author's intent** — what are they trying to communicate? A bug report, a feature request, a question, or a documentation issue?
3. **When in doubt between `bug` and `question`** — if the author is unsure whether something is a bug or they're using the SDK incorrectly, classify as `bug`. It's easier to reclassify later.
4. **When in doubt between `enhancement` and `bug`** — if the author describes behavior they find undesirable but the SDK is working as designed, classify as `enhancement`.
6. **Apply exactly one classification label** — never apply two classification labels to the same issue.
7. **Do not assess validity** — your role is to route the issue, not to judge whether it's valid, reproducible, or a duplicate. Downstream agents handle those determinations.

## Repository Context

The copilot-sdk is a monorepo with four SDK implementations:

- **Node.js/TypeScript** (`nodejs/src/`): The primary/reference implementation
- **Python** (`python/copilot/`): Python SDK with async support
- **Go** (`go/`): Go SDK with OpenTelemetry integration
- **.NET** (`dotnet/src/`): .NET SDK targeting net8.0

Common areas of issues:
- **JSON-RPC client**: Session creation, resumption, event handling
- **Streaming**: Delta events, message completion, reasoning events
- **Tools**: Tool definition, execution, permissions
- **Type generation**: Generated types from `@github/copilot` schema
- **E2E testing**: Test harness, replay proxy, snapshot fixtures
- **UI elicitation**: Confirm, select, input dialogs via session.ui
