---
applyTo: "docs/**"
---

# Copilot SDK docs style guide

This style guide applies to all documentation in the `docs/` directory. These docs are synced to `github/docs-internal` via a normalization pipeline, so they must follow the conventions below to be compatible with docs.github.com.

## Headings

Use **sentence case** for all headings. Capitalize only the first word and proper nouns.

* `## Quick start: Azure AI Foundry` — not `## Quick Start: Azure AI Foundry`
* `# Custom agents and sub-agent orchestration` — not `# Custom Agents & Sub-Agent Orchestration`

Use `and` instead of `&` in headings.

Do not use `**bold**` or `*italic*` markers inside headings. The heading level provides emphasis.

### Proper nouns to always capitalize

* Products/companies: GitHub, Copilot, Azure, OpenAI, Anthropic, Microsoft, Ollama, Slack, Foundry, Kubernetes, Docker
* Languages/frameworks: TypeScript, JavaScript, Python, Java, Node.js, OpenTelemetry, Express
* Platforms: macOS, Linux, Windows
* Protocols/formats: OAuth, JSON-RPC, JSON, YAML, HTTP, TCP, SSE, REST
* Acronyms: MCP, BYOK, MAF, SDK, CLI, API, HMAC, CI/CD, SaaS, ISV, FAQ, LLM, AI, EMU, ID, UI, PNG
* Tools (keep canonical casing): npm, npx, stdio
* Code identifiers in headings: SessionConfig, MessageOptions, TelemetryConfig, ProviderConfig, CopilotClient
* Multi-word proper names: GitHub App, GitHub Actions, GitHub OAuth, Foundry Local, Azure AD, Container Instances

## Callouts

Use GitHub-flavored alert syntax:

```markdown
> [!NOTE]
> This is a note.

> [!TIP]
> This is a tip.

> [!WARNING]
> This is a warning.
```

Never use `> **Note:**` or `> **Tip:**` style callouts.

When a callout applies to a specific language, put the qualifier as bold text in the body:

```markdown
> [!TIP]
> **(Python / Go)** These SDKs use a single `Data` class/struct with all fields optional.
```

## Lists

### Unordered lists

Use `*` (asterisks) for unordered list markers, not `-` (hyphens).

### Ordered lists

Use `1.` for every item in ordered lists, not sequential numbering. This makes reordering easier.

```markdown
1. First step
1. Second step
1. Third step
```

### List item formatting

* Capitalize the first letter of each list item.
* Use periods only if the item is a complete sentence.
* Introduce lists with a descriptive sentence, not vague phrases like "the following" in isolation.

## Em dashes

For list items with a **label and description**, use a colon:

```markdown
* **Ephemeral**: not persisted to disk, not replayed on session resume
```

For em dashes used mid-sentence, use no spaces:

```markdown
The SDK is a transport layer—it sends your prompt to the CLI over JSON-RPC.
```

## Horizontal rules

Do not use `---` as a horizontal rule to visually separate sections in the body of an article. Use headings to separate sections instead. This does not apply to YAML frontmatter delimiters.

## Index.md files

In the docs pipeline, `index.md` files become YAML-only category pages. Rich content (prose, code samples, diagrams) must live in standalone files.

If you are writing a new section with substantive content, create a named file (for example, `choosing-a-setup-path.md`) rather than putting the content in `index.md`.

## Code snippets

* Only modify code block contents when necessary. Keep all examples passing the SDK team's `docs-validate` workflow, rerun validation after changes, and use `docs-validate: skip` or `docs-validate: hidden` markers when appropriate.

## Voice and tone

* Use clear, simple language approachable for a wide range of readers.
* Use active voice whenever possible.
* Avoid idioms, slang, and region-specific phrases.
* Avoid ambiguous modal verbs ("may", "might", "should", "could") when an action is required. Use definitive verbs instead.
* Refer to people as "people" or "users", not "customers."

## Emphasis

* Use **bold** for UI elements and for emphasis, sparingly (no more than five contiguous words).
* Do not bold text that already has other formatting (for example, all-caps placeholders).

## Word choice

| Use | Avoid |
|---|---|
| terminal | shell |
| sign in | log in, login |
| sign up | signup |
| email | e-mail |
| press (a key) | hit, tap |
| repository | repo |
| administrator | admin |
| for example | e.g. |
| and similar | etc. |

## What the pipeline handles

Authors do not need to worry about these — the normalization pipeline handles them automatically:

* YAML frontmatter (added to files for docs.github.com)
* Mermaid diagram → PNG conversion
* Link rewriting for docs.github.com cross-references
* Liquid variable substitution (product names)
* `[AUTOTITLE]` link conversion

## New article template

When creating a new docs article, use this structure:

```markdown
# Article title in sentence case

A one- or two-sentence intro explaining what the reader will learn or accomplish.

## First section

Body text here.

## Second section

Body text here.

## Further reading

* [Link text](./relative-path.md): short description
```
