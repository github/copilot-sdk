# GitHub Copilot SDK Cookbook

This cookbook collects small, focused recipes showing how to accomplish common tasks with the GitHub Copilot SDK across languages. Each recipe is intentionally short and practical, with copy‑pasteable snippets and pointers to fuller examples and tests.

## Recipes by Language

### .NET (C#)

- [Error Handling](dotnet/cookbook/error-handling.md): Handle errors gracefully including connection failures, timeouts, and cleanup.
- [Multiple Sessions](dotnet/cookbook/multiple-sessions.md): Manage multiple independent conversations simultaneously.
- [Managing Local Files](dotnet/cookbook/managing-local-files.md): Organize files by metadata using AI-powered grouping strategies.
- [PR Visualization](dotnet/cookbook/pr-visualization.md): Generate interactive PR age charts using GitHub MCP Server.
- [Persisting Sessions](dotnet/cookbook/persisting-sessions.md): Save and resume sessions across restarts.

### Node.js / TypeScript

- [Error Handling](nodejs/cookbook/error-handling.md): Handle errors gracefully including connection failures, timeouts, and cleanup.
- [Multiple Sessions](nodejs/cookbook/multiple-sessions.md): Manage multiple independent conversations simultaneously.
- [Managing Local Files](nodejs/cookbook/managing-local-files.md): Organize files by metadata using AI-powered grouping strategies.
- [PR Visualization](nodejs/cookbook/pr-visialisation.md): Generate interactive PR age charts using GitHub MCP Server.
- [Persisting Sessions](nodejs/cookbook/persisting-sessions.md): Save and resume sessions across restarts.

### Python

- [Error Handling](python/cookbook/error-handling.md): Handle errors gracefully including connection failures, timeouts, and cleanup.
- [Multiple Sessions](python/cookbook/multiple-sessions.md): Manage multiple independent conversations simultaneously.
- [Managing Local Files](python/cookbook/managing-local-files.md): Organize files by metadata using AI-powered grouping strategies.
- [PR Visualization](python/cookbook/pr-visualization.md): Generate interactive PR age charts using GitHub MCP Server.
- [Persisting Sessions](python/cookbook/persisting-sessions.md): Save and resume sessions across restarts.

### Go

- [Error Handling](go/cookbook/error-handling.md): Handle errors gracefully including connection failures, timeouts, and cleanup.
- [Multiple Sessions](go/cookbook/multiple-sessions.md): Manage multiple independent conversations simultaneously.
- [Managing Local Files](go/cookbook/managing-local-files.md): Organize files by metadata using AI-powered grouping strategies.
- [PR Visualization](go/cookbook/pr-visualization.md): Generate interactive PR age charts using GitHub MCP Server.
- [Persisting Sessions](go/cookbook/persisting-sessions.md): Save and resume sessions across restarts.

## How to Use

- Browse your language section above and open the recipe links
- Each recipe includes runnable examples in a `recipe/` subfolder with language-specific tooling
- See existing examples and tests for working references:
  - Node.js examples: `nodejs/examples/basic-example.ts`
  - E2E tests: `go/e2e`, `python/e2e`, `nodejs/test/e2e`, `dotnet/test/Harness`

## Running Examples

### .NET

```bash
cd dotnet/cookbook/recipe
dotnet run <filename>.cs
```

### Node.js

```bash
cd nodejs/cookbook/recipe
npm install
npx tsx <filename>.ts
```

### Python

```bash
cd python/cookbook/recipe
pip install -r requirements.txt
python <filename>.py
```

### Go

```bash
cd go/cookbook/recipe
go run <filename>.go
```

## Contributing

- Propose or add a new recipe by creating a markdown file in your language's `cookbook/` folder and a runnable example in `recipe/`
- Follow repository guidance in [CONTRIBUTING.md](CONTRIBUTING.md)

## Status

Cookbook structure is complete with 4 recipes across all 4 supported languages. Each recipe includes both markdown documentation and runnable examples.
