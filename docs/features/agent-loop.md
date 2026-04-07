# The Agent Loop

How the Copilot CLI processes a user message end-to-end: from prompt to `session.idle`.

## Architecture

```mermaid
graph LR
    App["Your App"] -->|send prompt| SDK["SDK Session"]
    SDK -->|JSON-RPC| CLI["Copilot CLI"]
    CLI -->|API calls| LLM["LLM"]
    LLM -->|response| CLI
    CLI -->|events| SDK
    SDK -->|events| App
```

The **SDK** is a transport layer — it sends your prompt to the **Copilot CLI** over JSON-RPC and surfaces events back to your app. The **CLI** is the orchestrator that runs the agentic tool-use loop, making one or more LLM API calls until the task is done.

## The Tool-Use Loop

When you call `session.send({ prompt })`, the CLI enters a loop:

```mermaid
flowchart TD
    A["User prompt"] --> B["LLM API call\n(= one turn)"]
    B --> C{"toolRequests\nin response?"}
    C -->|Yes| D["Execute tools\nCollect results"]
    D -->|"Results fed back\nas next turn input"| B
    C -->|No| E["Final text\nresponse"]
    E --> F(["session.idle"])

    style B fill:#1a1a2e,stroke:#58a6ff,color:#c9d1d9
    style D fill:#1a1a2e,stroke:#3fb950,color:#c9d1d9
    style F fill:#0d1117,stroke:#f0883e,color:#f0883e
```

The model sees the **full conversation history** on each call — system prompt, user message, and all prior tool calls and results.

**Key insight:** Each iteration of this loop is exactly one LLM API call, visible as one `assistant.turn_start` / `assistant.turn_end` pair in the event log. There are no hidden calls.

## Turns — What They Are

A **turn** is a single LLM API call and its consequences:

1. The CLI sends the conversation history to the LLM
2. The LLM responds (possibly with tool requests)
3. If tools were requested, the CLI executes them
4. `assistant.turn_end` is emitted

A single user message typically results in **multiple turns**. For example, a question like "how does X work in this codebase?" might produce:

| Turn | What the model does | toolRequests? |
|------|-------------------|---------------|
| 1 | Calls `grep` and `glob` to search the codebase | ✅ Yes |
| 2 | Reads specific files based on search results | ✅ Yes |
| 3 | Reads more files for deeper context | ✅ Yes |
| 4 | Produces the final text answer | ❌ No → loop ends |

The model decides on each turn whether to request more tools or produce a final answer. Each call sees the **full accumulated context** (all prior tool calls and results), so it can make an informed decision about whether it has enough information.

## Event Flow for a Multi-Turn Interaction

```mermaid
flowchart TD
    send["session.send({ prompt: &quot;Fix the bug in auth.ts&quot; })"]

    subgraph Turn1 ["Turn 1"]
        t1s["assistant.turn_start"]
        t1m["assistant.message (toolRequests)"]
        t1ts["tool.execution_start (read_file)"]
        t1tc["tool.execution_complete"]
        t1e["assistant.turn_end"]
        t1s --> t1m --> t1ts --> t1tc --> t1e
    end

    subgraph Turn2 ["Turn 2 — auto-triggered by CLI"]
        t2s["assistant.turn_start"]
        t2m["assistant.message (toolRequests)"]
        t2ts["tool.execution_start (edit_file)"]
        t2tc["tool.execution_complete"]
        t2e["assistant.turn_end"]
        t2s --> t2m --> t2ts --> t2tc --> t2e
    end

    subgraph Turn3 ["Turn 3"]
        t3s["assistant.turn_start"]
        t3m["assistant.message (no toolRequests)\n&quot;Done, here's what I changed&quot;"]
        t3e["assistant.turn_end"]
        t3s --> t3m --> t3e
    end

    idle(["session.idle — ready for next message"])

    send --> Turn1 --> Turn2 --> Turn3 --> idle
```

## Who Triggers Each Turn?

| Actor | Responsibility |
|-------|---------------|
| **Your app** | Sends the initial prompt via `session.send()` |
| **Copilot CLI** | Runs the tool-use loop — executes tools and feeds results back to the LLM for the next turn |
| **LLM** | Decides whether to request tools (continue looping) or produce a final response (stop) |
| **SDK** | Passes events through; does not control the loop |

The CLI is purely mechanical: "model asked for tools → execute → call model again." The **model** is the decision-maker for when to stop.

## `session.idle` vs `session.task_complete`

These are two different completion signals with very different guarantees:

### `session.idle`

- **Always emitted** when the tool-use loop ends
- **Ephemeral** — not persisted to disk, not replayed on session resume
- Means: "the agent has stopped processing and is ready for the next message"
- **Use this** as your reliable "done" signal

The SDK's `sendAndWait()` method waits for this event:

```typescript
// Blocks until session.idle fires
const response = await session.sendAndWait({ prompt: "Fix the bug" });
```

### `session.task_complete`

- **Optionally emitted** — requires the model to explicitly signal it
- **Persisted** — saved to the session event log on disk
- Means: "the agent considers the overall task fulfilled"
- Carries an optional `summary` field

```typescript
session.on("session.task_complete", (event) => {
    console.log("Task done:", event.data.summary);
});
```

### Autopilot mode: the CLI nudges for `task_complete`

In **autopilot mode** (headless/autonomous operation), the CLI actively tracks whether the model has called `task_complete`. If the tool-use loop ends without it, the CLI injects a synthetic user message nudging the model:

> *"You have not yet marked the task as complete using the task_complete tool. If you were planning, stop planning and start implementing. You aren't done until you have fully completed the task."*

This effectively restarts the tool-use loop — the model sees the nudge as a new user message and continues working. The nudge also instructs the model **not** to call `task_complete` prematurely:

- Don't call it if you have open questions — make decisions and keep working
- Don't call it if you hit an error — try to resolve it
- Don't call it if there are remaining steps — complete them first

This creates a **two-level completion mechanism** in autopilot:
1. The model calls `task_complete` with a summary → CLI emits `session.task_complete` → done
2. The model stops without calling it → CLI nudges → model continues or calls `task_complete`

### Why `task_complete` might not appear

In **interactive mode** (normal chat), the CLI does not nudge for `task_complete`. The model may skip it entirely. Common reasons:

- **Conversational Q&A**: The model answers a question and simply stops — there's no discrete "task" to complete
- **Model discretion**: The model produces a final text response without calling the task-complete signal
- **Interrupted sessions**: The session ends before the model reaches a completion point

The CLI emits `session.idle` regardless, because it's a mechanical signal (the loop ended), not a semantic one (the model thinks it's done).

### Which should you use?

| Use case | Signal |
|----------|--------|
| "Wait for the agent to finish processing" | `session.idle` ✅ |
| "Know when a coding task is done" | `session.task_complete` (best-effort) |
| "Timeout/error handling" | `session.idle` + `session.error` ✅ |

## Counting LLM Calls

The number of `assistant.turn_start` / `assistant.turn_end` pairs in the event log equals the total number of LLM API calls made. There are no hidden calls for planning, evaluation, or completion checking.

To inspect turn count for a session:

```bash
# Count turns in a session's event log
grep -c "assistant.turn_start" ~/.copilot/session-state/<sessionId>/events.jsonl
```

## Further Reading

- [Streaming Events Reference](./streaming-events.md) — Full field-level reference for every event type
- [Session Persistence](./session-persistence.md) — How sessions are saved and resumed
- [Hooks](./hooks.md) — Intercepting events in the loop (permissions, tools)
