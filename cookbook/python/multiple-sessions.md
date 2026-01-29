# Working with Multiple Sessions

Manage multiple independent conversations simultaneously for multi-user or multi-task applications.

> **Skill Level:** Beginner to Intermediate
>
> **Runnable Example:** [recipe/multiple_sessions.py](recipe/multiple_sessions.py)
>
> ```bash
> cd recipe && pip install -r requirements.txt
> python multiple_sessions.py
> ```

## Overview

This recipe covers managing multiple conversation sessions:

- Creating independent sessions with isolated contexts
- Using different models for different sessions
- Parallel execution of multiple requests
- Custom session IDs for tracking
- Session pool pattern for scalable applications

## Quick Start

```python
import asyncio
from copilot import CopilotClient

async def main():
    client = CopilotClient()
    await client.start()

    # Create multiple independent sessions
    session1 = await client.create_session()
    session2 = await client.create_session()
    session3 = await client.create_session({"model": "claude-sonnet-4"})

    # Each session has its own context
    await session1.send_and_wait({"prompt": "You're helping with Python"})
    await session2.send_and_wait({"prompt": "You're helping with TypeScript"})
    await session3.send_and_wait({"prompt": "You're helping with Go"})

    # Context-aware follow-ups
    await session1.send_and_wait({"prompt": "How do I create a virtual environment?"})
    await session2.send_and_wait({"prompt": "How do I set up tsconfig?"})
    await session3.send_and_wait({"prompt": "How do I initialize a module?"})

    # Clean up
    await session1.destroy()
    await session2.destroy()
    await session3.destroy()
    await client.stop()

asyncio.run(main())
```

## Custom Session IDs

Use meaningful IDs for easier tracking and management:

```python
# Create sessions with custom IDs
user_session = await client.create_session({
    "session_id": "user-123-chat"
})

support_session = await client.create_session({
    "session_id": "support-ticket-456"
})

print(user_session.session_id)  # "user-123-chat"
```

## Parallel Execution

Execute requests across multiple sessions concurrently:

```python
import asyncio
from copilot import CopilotClient
from copilot.types import SessionEventType

async def parallel_requests():
    client = CopilotClient()
    await client.start()

    # Create sessions
    topics = ["Python", "JavaScript", "Rust"]
    sessions = [await client.create_session() for _ in topics]

    # Collect responses
    results = {}

    def make_handler(topic, results_dict):
        def handler(event):
            if event.type == SessionEventType.ASSISTANT_MESSAGE:
                results_dict[topic] = event.data.content
        return handler

    # Set up handlers
    for session, topic in zip(sessions, topics):
        session.on(make_handler(topic, results))

    # Execute all requests in parallel
    await asyncio.gather(*[
        session.send_and_wait({"prompt": f"What is {topic}? One sentence."})
        for session, topic in zip(sessions, topics)
    ])

    # All results available now
    for topic, response in results.items():
        print(f"{topic}: {response}")

    # Cleanup
    for session in sessions:
        await session.destroy()
    await client.stop()
```

## Session Listing

View all active sessions:

```python
# List all sessions
sessions = await client.list_sessions()

for session_info in sessions:
    print(f"Session: {session_info['sessionId']}")
    print(f"  Modified: {session_info['modifiedTime']}")
    if session_info.get('summary'):
        print(f"  Summary: {session_info['summary']}")
```

## Session Pool Pattern

For high-throughput applications, use a session pool:

```python
class SessionPool:
    """Reusable pool of sessions for concurrent requests."""

    def __init__(self, client, size=5):
        self.client = client
        self.size = size
        self._available = asyncio.Queue()
        self._all_sessions = []

    async def initialize(self):
        """Create the pool of sessions."""
        for i in range(self.size):
            session = await self.client.create_session({
                "session_id": f"pool-{i}"
            })
            self._all_sessions.append(session)
            await self._available.put(session)

    async def acquire(self, timeout=30.0):
        """Get a session from the pool."""
        return await asyncio.wait_for(
            self._available.get(),
            timeout=timeout
        )

    async def release(self, session):
        """Return a session to the pool."""
        await self._available.put(session)

    async def close(self):
        """Destroy all sessions."""
        for session in self._all_sessions:
            await session.destroy()


# Usage
pool = SessionPool(client, size=5)
await pool.initialize()

# Process requests concurrently
session = await pool.acquire()
try:
    await session.send_and_wait({"prompt": "Question"})
finally:
    await pool.release(session)

await pool.close()
```

## Use Cases

### Multi-User Chat Application

```python
# One session per user
async def get_or_create_session(client, user_id):
    session_id = f"user-{user_id}"
    try:
        return await client.resume_session(session_id)
    except RuntimeError:
        return await client.create_session({"session_id": session_id})
```

### Multi-Task Workflows

```python
# Different sessions for different tasks
planning = await client.create_session({"session_id": "task-planning"})
coding = await client.create_session({"session_id": "task-coding"})
review = await client.create_session({"session_id": "task-review"})
```

### A/B Testing Models

```python
# Compare responses from different models
gpt_session = await client.create_session({"model": "gpt-5"})
claude_session = await client.create_session({"model": "claude-sonnet-4"})

# Same prompt, different models
prompt = {"prompt": "Explain quantum computing in one paragraph."}
await asyncio.gather(
    gpt_session.send_and_wait(prompt),
    claude_session.send_and_wait(prompt)
)
```

## Session Lifecycle

```
                    create_session()
                          │
                          ▼
    ┌─────────────────────────────────────┐
    │           Active Session            │
    │  - send() / send_and_wait()         │
    │  - on() for events                  │
    │  - get_messages() for history       │
    └─────────────────────────────────────┘
                          │
              ┌───────────┴───────────┐
              ▼                       ▼
         destroy()               resume_session()
              │                       │
              ▼                       │
    ┌─────────────┐                   │
    │  Destroyed  │ ◄─────────────────┘
    │(persisted)  │
    └─────────────┘
              │
              ▼
       delete_session()
              │
              ▼
    ┌─────────────┐
    │   Deleted   │
    │(permanent)  │
    └─────────────┘
```

## Best Practices

1. **Use meaningful session IDs**: Include user, task, or date identifiers
2. **Clean up sessions**: Always call `destroy()` when done
3. **Limit concurrent sessions**: Use session pools for high volume
4. **Handle session not found**: Wrap `resume_session()` in try-except

## Complete Example

```bash
python recipe/multiple_sessions.py
```

Demonstrates:
- Basic multiple sessions
- Parallel execution
- Custom session IDs
- Session pool pattern

## Next Steps

- [Persisting Sessions](persisting-sessions.md): Save and resume across restarts
- [Error Handling](error-handling.md): Handle session errors gracefully
- [Streaming Responses](streaming-responses.md): Real-time response handling
