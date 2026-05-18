# GitHub Copilot SDK Cookbook — Java

This folder hosts short, practical recipes for using the GitHub Copilot SDK with Java. Each recipe is concise, copy‑pasteable, and points to fuller examples and tests.

## Prerequisites

All cookbook examples can be run directly using [JBang](https://www.jbang.dev/), which allows you to run Java code without a full project setup.

**Install JBang:**

```bash
# macOS (using Homebrew)
brew install jbangdev/tap/jbang

# Linux/macOS (using curl)
curl -Ls https://sh.jbang.dev | bash -s - app setup

# Windows (using Scoop)
scoop install jbang
```

For other installation methods, see the [JBang installation guide](https://www.jbang.dev/download/).

## Recipes

- [Error Handling](error-handling.md): Handle errors gracefully including connection failures, timeouts, and cleanup.
- [Multiple Sessions](multiple-sessions.md): Manage multiple independent conversations simultaneously.
- [Managing Local Files](managing-local-files.md): Organize files by metadata using AI-powered grouping strategies.
- [PR Visualization](pr-visualization.md): Generate interactive PR age charts using GitHub MCP Server.
- [Persisting Sessions](persisting-sessions.md): Save and resume sessions across restarts.

## Contributing

Add a new recipe by creating a markdown file in this folder and linking it above. Follow repository guidance in the [main README](https://github.com/github/copilot-sdk-java#contributing).

## Status

These recipes are complete, practical examples and can be used directly or adapted for your own projects.
