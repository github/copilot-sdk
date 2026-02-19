# Config Sample: Session Resume

Demonstrates session persistence and resume with the Copilot SDK. This validates that a destroyed session can be resumed by its ID, retaining full conversation history.

## What Each Sample Does

1. Creates a session with `availableTools: []` and model `gpt-4.1`
2. Sends: _"Remember this: the secret word is PINEAPPLE."_
3. Captures the session ID and destroys the session
4. Resumes the session using the same session ID
5. Sends: _"What was the secret word I told you?"_
6. Prints the response — which should mention **PINEAPPLE**

## Configuration

| Option | Value | Effect |
|--------|-------|--------|
| `availableTools` | `[]` (empty array) | Keeps the session simple with no tools |
| `model` | `"gpt-4.1"` | Uses GPT-4.1 for both the initial and resumed session |

## Run

```bash
./verify.sh
```

Requires the `copilot` binary (auto-detected or set `COPILOT_CLI_PATH`) and `GITHUB_TOKEN`.
