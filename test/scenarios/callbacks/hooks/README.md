# configs/hooks — Session Lifecycle Hooks

Demonstrates all SDK session lifecycle hooks firing during a typical prompt–tool–response cycle.

## Hooks Tested

| Hook | When It Fires | Purpose |
|------|---------------|---------|
| `onSessionStart` | Session is created | Initialize logging, metrics, or state |
| `onSessionEnd` | Session is destroyed | Clean up resources, flush logs |
| `onPreToolUse` | Before a tool executes | Approve/deny tool calls, audit usage |
| `onPostToolUse` | After a tool executes | Log results, collect metrics |
| `onUserPromptSubmitted` | User sends a prompt | Transform, validate, or log prompts |
| `onErrorOccurred` | An error is raised | Centralized error handling |

## What This Scenario Does

1. Creates a session with **all** lifecycle hooks registered.
2. Each hook appends its name to a log list when invoked.
3. Sends a prompt that triggers tool use (glob file listing).
4. Prints the model's response followed by the hook execution log showing which hooks fired and in what order.

## Run

```bash
# TypeScript
cd typescript && npm install && npm run build && node dist/index.js

# Python
cd python && pip install -r requirements.txt && python3 main.py

# Go
cd go && go run .
```

## Verify All

```bash
./verify.sh
```
