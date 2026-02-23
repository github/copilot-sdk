# Config Sample: Skills (SKILL.md Discovery)

Demonstrates configuring the Copilot SDK with **skill directories** that contain `SKILL.md` files. The agent discovers and uses skills defined in these markdown files at runtime.

## What This Tests

1. **Skill discovery** — Setting `skillDirectories` points the agent to directories containing `SKILL.md` files that define available skills.
2. **Skill execution** — The agent reads the skill definition and follows its instructions when prompted to use the skill.
3. **SKILL.md format** — Skills are defined as markdown files with a name, description, and usage instructions.

## SKILL.md Format

A `SKILL.md` file is a markdown document placed in a named directory under a skills root:

```
sample-skills/
└── greeting/
    └── SKILL.md      # Defines the "greeting" skill
```

The file contains:
- **Title** (`# skill-name`) — The skill's identifier
- **Description** — What the skill does
- **Usage** — Instructions the agent follows when the skill is invoked

## What Each Sample Does

1. Creates a session with `skillDirectories` pointing to `sample-skills/`
2. Sends: _"Use the greeting skill to greet someone named Alice."_
3. The agent discovers the greeting skill from `SKILL.md` and generates a personalized greeting
4. Prints the response and confirms skill directory configuration

## Configuration

| Option | Value | Effect |
|--------|-------|--------|
| `skillDirectories` | `["path/to/sample-skills"]` | Points the agent to directories containing skill definitions |

## Run

```bash
./verify.sh
```

Requires the `copilot` binary (auto-detected or set `COPILOT_CLI_PATH`) and `GITHUB_TOKEN`.
