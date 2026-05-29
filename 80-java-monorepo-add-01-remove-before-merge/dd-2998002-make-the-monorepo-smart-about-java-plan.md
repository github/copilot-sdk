# DD-2998002: Make the Monorepo Smart About Java

## Goal

Make the `copilot-sdk` monorepo's Copilot configuration aware of Java so that AI coding agents receive Java-specific guidance when editing Java files. This involves two deliverables:

1. **Create `.github/skills/java-coding-skill/SKILL.md`** — a new skill containing Java SDK API patterns and coding rules (sourced from the standalone Java repo's `instructions/copilot-sdk-java.instructions.md`).
2. **Add a Java section to `.github/copilot-instructions.md`** — concise Java governance (build commands, architecture, test conventions) that parallels the existing Node, Python, Go, .NET entries.

## Context

- The monorepo uses **skills** for language-specific coding guidance (not `instructions/` directories).
- The only existing language-specific skill is `rust-coding-skill` at `.github/skills/rust-coding-skill/SKILL.md`.
- No other language (Node, Python, Go, .NET) has a dedicated skill — only Rust does.
- The Java SDK source will live under `java/` in the monorepo (per the Phase 1 migration plan).
- The standalone Java repo has two Copilot configuration files:
  - `instructions/copilot-sdk-java.instructions.md` (~757 lines) — API usage patterns, coding rules, examples
  - `.github/copilot-instructions.md` (~260 lines) — repo governance: build commands, architecture, testing conventions, boundaries, security

## Execution Context

- You are running from the **monorepo root** (`copilot-sdk-00/`).
- The standalone Java SDK repo is available at **`../copilot-sdk-java-00/`** (a sibling directory).
- All files you create or edit are in the monorepo (current directory). The Java repo is read-only — you only read source files from it.

## Non-Goals

- Do NOT copy `instructions/` as a directory to `java/instructions/` — that doesn't match the monorepo convention.
- Do NOT create skills for other languages — only Java is being added.
- Do NOT modify any Java source code, tests, or build files.
- Do NOT modify any existing skills (e.g., `rust-coding-skill`).
- Do NOT modify any files in `../copilot-sdk-java-00/` — it is a read-only source.

---

## Checklist

### Step 1: Read the existing `rust-coding-skill` to understand the pattern

- [ ] Read `.github/skills/rust-coding-skill/SKILL.md` to understand the YAML frontmatter structure and content organization.

The frontmatter uses exactly these fields:

```yaml
---
name: rust-coding-skill
description: "Use this skill whenever editing `*.rs` files in the `rust/` SDK in order to write idiomatic, efficient, well-structured Rust code"
---
```

### Step 2: Read the Java instructions source file

- [ ] Read the full content of the Java instructions file at **`../copilot-sdk-java-00/instructions/copilot-sdk-java.instructions.md`**.
  - **Fallback** (if the file is not found at that path): The structure is described in [Appendix A](#appendix-a-java-instructions-source-content) below.

### Step 3: Create `.github/skills/java-coding-skill/SKILL.md`

- [ ] Create the directory `.github/skills/java-coding-skill/`
- [ ] Create `.github/skills/java-coding-skill/SKILL.md` with:
  - YAML frontmatter (see template below)
  - Body content adapted from the Java instructions file

**YAML frontmatter** — use exactly this:

```yaml
---
name: java-coding-skill
description: "Use this skill whenever editing `*.java` files in the `java/` SDK in order to write idiomatic, well-structured Java code for the Copilot SDK"
---
```

**Body content** — take the full content of `../copilot-sdk-java-00/instructions/copilot-sdk-java.instructions.md` (everything after its YAML frontmatter) and make these adaptations:

1. **Remove the old YAML frontmatter** (`applyTo`, `description`, `name` fields from the instructions file). Replace it with the new frontmatter above.
2. **Add a title line** after the frontmatter: `# Java Coding Skill`
3. **Update paths to reflect monorepo layout**:
   - References to `src/` → `java/src/`
   - References to `pom.xml` → `java/pom.xml`
   - References to `config/` → `java/config/`
   - References to `scripts/codegen/` → `scripts/codegen/` (codegen lives at monorepo root)
   - References to `target/` → `java/target/`
   - References to `.lastmerge` → `java/.lastmerge`
   - References to `.githooks/` → `java/.githooks/`
   - References to `src/site/` → `java/src/site/`
   - References to `src/generated/java/` → `java/src/generated/java/`
4. **Keep all code examples unchanged** — they show API usage, not file paths.
5. **Keep all sections** — Core Principles, Installation, Client Initialization, Session Management, Event Handling, Streaming, Custom Tools, Permission Handling, User Input, System Message, File Attachments, Message Delivery, Send and Wait, Multiple Sessions, BYOK, Session Lifecycle, Error Handling, Connectivity Testing, Status/Auth, Resource Cleanup, Best Practices, Common Patterns.
6. **Do NOT add content that isn't in the source** — no new sections, no commentary.

### Step 4: Read the monorepo's existing `.github/copilot-instructions.md`

- [ ] Read `.github/copilot-instructions.md` to understand the current structure and where Java should be added.

The current file has these sections:

- Big picture 🔧
- Most important files to read first 📚
- Developer workflows ▶️ (per-language subsection)
- Testing & E2E tips ⚙️
- Project-specific conventions & patterns ✅
- Integration & environment notes ⚠️
- Where to add new code or tests 🧭

### Step 5: Read the Java repo's `.github/copilot-instructions.md`

- [ ] Read the Java repo's governance file at **`../copilot-sdk-java-00/.github/copilot-instructions.md`** to extract the content that needs to be merged.
  - **Fallback** (if the file is not found at that path): The content to merge is provided in [Appendix B](#appendix-b-java-governance-content-to-merge) below.

### Step 6: Add Java to `.github/copilot-instructions.md`

- [ ] Make the following additions to the monorepo's `.github/copilot-instructions.md`:

**6a. Update "Big picture" section:**

Change:

```
The repo implements language SDKs (Node/TS, Python, Go, .NET) that speak to the **Copilot CLI**
```

To:

```
The repo implements language SDKs (Node/TS, Python, Go, .NET, Java) that speak to the **Copilot CLI**
```

And add Java's CLI URL option to the typical flow line. Current:

```
(Node: `cliUrl`, Go: `CLIUrl`, .NET: `CliUrl`, Python: `cli_url`)
```

Updated:

```
(Node: `cliUrl`, Go: `CLIUrl`, .NET: `CliUrl`, Python: `cli_url`, Java: `cliUrl`)
```

**6b. Update "Most important files to read first" section:**

Add:

```
- Java: `java/README.md`, `java/pom.xml`
```

**6c. Update "Developer workflows" per-language section:**

Add a Java entry after the .NET entry:

```
  - Java: `cd java && mvn clean verify` (full build + tests), `mvn spotless:apply` (format code before commit)
  - **Java testing note:** Always use `mvn verify` without `-q` and without piping through `grep`. Never add `InternalsVisibleTo` equivalent — tests must only access public APIs.
```

**6d. Update "Testing & E2E tips" section:**

Add after the existing E2E description:

```
- Java E2E tests use `E2ETestContext` which manages a `CapiProxy` (Node.js replaying proxy). The harness is cloned during Maven's `generate-test-resources` phase to `java/target/copilot-sdk/`.
```

**6e. Update "Where to add new code or tests" section:**

Add Java to the lists:

- SDK code line: add `java/src/main/java`
- Unit tests line: add `java/src/test/java`
- E2E tests line: add `java/src/test/java/**/e2e/`
- Generated types line: add `java/src/generated/java`

**6f. Update "Integration & environment notes" section:**

Add Java's CLI URL option. Current:

```
(Node: `cliUrl`, Go: `CLIUrl`, .NET: `CliUrl`, Python: `cli_url`)
```

Updated:

```
(Node: `cliUrl`, Go: `CLIUrl`, .NET: `CliUrl`, Python: `cli_url`, Java: `cliUrl`)
```

Add environment note:

```
- Java requires JDK 17+ and Maven 3.9+. Java E2E tests also require Node.js (for the replay proxy).
```

### Step 7: Verify

- [ ] Confirm `.github/skills/java-coding-skill/SKILL.md` exists and has valid YAML frontmatter with `name` and `description` fields.
- [ ] Confirm `.github/copilot-instructions.md` mentions Java in all per-language lists (Big picture, Developer workflows, Most important files, Where to add code, Integration notes).
- [ ] Confirm no files were created under `java/instructions/` — that pattern is NOT used in this monorepo.
- [ ] Confirm `.github/skills/rust-coding-skill/` was NOT modified.

---

## Appendix A: Java Instructions Source Content

The source file is `instructions/copilot-sdk-java.instructions.md` from the `copilot-sdk-java` repository. Its YAML frontmatter is:

```yaml
---
applyTo: "**.java, **/pom.xml"
description: "This file provides guidance on building Java applications using GitHub Copilot SDK for Java."
name: "GitHub Copilot SDK Java Instructions"
---
```

The body contains these sections (in order):

1. Core Principles
2. Installation (Maven, Gradle)
3. Client Initialization (Basic, Options, Manual Server Control)
4. Session Management (Creating, Config Options, Resuming, Operations)
5. Event Handling (Subscription, Type-Safe, Unsubscribing, Event Types, Error Handling)
6. Streaming Responses (Enabling, Handling Events)
7. Custom Tools (Defining, Type-Safe Args, Overriding Built-In, Return Types, Execution Flow)
8. Permission Handling (Required Handler)
9. User Input Handling
10. System Message Customization (Append Mode, Replace Mode)
11. File Attachments
12. Message Delivery Modes
13. Convenience: Send and Wait
14. Multiple Sessions
15. Bring Your Own Key (BYOK)
16. Session Lifecycle Management (Listing, Deleting, Connection State, Lifecycle Events)
17. Error Handling (Standard Exceptions, Session Error Events)
18. Connectivity Testing
19. Status and Authentication
20. Resource Cleanup (Automatic, Manual)
21. Best Practices (12 items)
22. Common Patterns (Simple Query-Response, Event-Driven, Multi-Turn, Complex Tools, Session Hooks)

The file is ~757 lines. The agent MUST read the full file from the source repo or the monorepo copy — do not truncate or summarize.

## Appendix B: Java Governance Content to Merge

The source file is `.github/copilot-instructions.md` from the `copilot-sdk-java` repository. Key content to extract and merge into the monorepo's `copilot-instructions.md`:

**Build & Test Commands** (merge into Developer workflows):

- `mvn clean verify` — full build + tests
- `mvn test -Dtest=ClassName` — single test class
- `mvn test -Dtest=ClassName#method` — single test method
- `mvn spotless:apply` — format code (required before commit)
- `mvn spotless:check` — check formatting only
- `mvn clean package -DskipTests` — build without tests
- AI agent testing rule: always use `mvn verify` without `-q`, never pipe through `grep`

**Architecture** (reference from skill, don't duplicate):

- CopilotClient, CopilotSession, JsonRpcClient
- Package structure: `com.github.copilot.sdk`, `.json`, `.generated`

**Key Conventions** (merge selectively — most belongs in the skill):

- Reference implementation merging pattern (keep in governance — it's repo-level policy)
- Code style: 4-space indent, Spotless, Checkstyle (keep in governance)
- Pre-commit hooks (keep in governance)

**Boundaries and Restrictions** (keep in governance):

- Do not edit `src/generated/java/` (auto-generated)
- Do not modify test snapshots in `target/copilot-sdk/test/snapshots/`
- Must run `gh aw compile` after editing agentic workflow `.md` files

**Security Guidelines** (keep in governance):

- Never commit secrets
- Use try-with-resources, StandardCharsets.UTF_8
- Review dependencies for vulnerabilities

**NOTE**: The governance content that goes into `copilot-instructions.md` should be CONCISE — just enough for an agent to know how to build, test, and follow repo rules. The detailed API patterns and coding examples belong in the skill file, not in governance.
