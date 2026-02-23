# Config Sample: Custom Agents

Demonstrates configuring the Copilot SDK with **custom agent definitions** that restrict which tools an agent can use. This validates:

1. **Agent definition** — The `customAgents` session config accepts agent definitions with name, description, tool lists, and custom prompts.
2. **Tool scoping** — Each custom agent can be restricted to a subset of available tools (e.g. read-only tools like `grep`, `glob`, `view`).
3. **Agent awareness** — The model recognizes and can describe the configured custom agents.

## What Each Sample Does

1. Creates a session with a `customAgents` array containing a "researcher" agent
2. The researcher agent is scoped to read-only tools: `grep`, `glob`, `view`
3. Sends: _"What custom agents are available? Describe the researcher agent and its capabilities."_
4. Prints the response — which should describe the researcher agent and its tool restrictions

## Configuration

| Option | Value | Effect |
|--------|-------|--------|
| `customAgents[0].name` | `"researcher"` | Internal identifier for the agent |
| `customAgents[0].displayName` | `"Research Agent"` | Human-readable name |
| `customAgents[0].description` | Custom text | Describes agent purpose |
| `customAgents[0].tools` | `["grep", "glob", "view"]` | Restricts agent to read-only tools |
| `customAgents[0].prompt` | Custom text | Sets agent behavior instructions |

## Run

```bash
./verify.sh
```

Requires the `copilot` binary (auto-detected or set `COPILOT_CLI_PATH`) and `GITHUB_TOKEN`.
