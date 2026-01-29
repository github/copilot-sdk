# Persisting Sessions

Save and resume conversations across application restarts.

> **Skill Level:** Beginner to Intermediate
>
> **Runnable Example:** [recipe/persisting_sessions.py](recipe/persisting_sessions.py)
>
> ```bash
> cd recipe && pip install -r requirements.txt
> python persisting_sessions.py
> ```

## Overview

This recipe demonstrates session persistence patterns:

- Basic save and resume functionality
- Custom session IDs for organization
- Infinite sessions that never expire
- Conversation bookmarks and history export
- Session management and cleanup

## Quick Start

```python
import asyncio
from copilot import CopilotClient

async def main():
    client = CopilotClient()
    await client.start()

    # Create a session with a memorable ID
    session = await client.create_session({
        "session_id": "project-discussion-2024"
    })

    # Have a conversation
    await session.send_and_wait({"prompt": "Let's plan a web app architecture"})
    await session.send_and_wait({"prompt": "What database should we use?"})

    # Destroy (but preserve for resuming)
    await session.destroy()

    # Later... resume the session
    resumed = await client.resume_session("project-discussion-2024")
    await resumed.send_and_wait({"prompt": "What were we discussing?"})
    # Session remembers the full conversation context!

    await resumed.destroy()
    await client.stop()

asyncio.run(main())
```

## Session Lifecycle

```
create_session()
      │
      ▼
┌─────────────────────────────────────┐
│          Active Session             │
│  - send() / send_and_wait()         │
│  - Full context maintained          │
│  - on() for event handling          │
└─────────────────────────────────────┘
      │
      ▼ destroy()
┌─────────────────────────────────────┐
│        Resumable Session            │
│  - Persisted to storage             │
│  - Can be listed via list_sessions  │
│  - Context preserved                │
└─────────────────────────────────────┘
      │
      ├──▶ resume_session() ──▶ Active Session
      │
      ▼ delete_session()
┌─────────────────────────────────────┐
│     Permanently Deleted             │
│  - Cannot be recovered              │
│  - All history lost                 │
└─────────────────────────────────────┘
```

## Custom Session IDs

Use meaningful IDs for easy organization:

```python
# User-based sessions
user_session = await client.create_session({
    "session_id": f"user-{user_id}-main"
})

# Project-based sessions
project_session = await client.create_session({
    "session_id": f"project-{project_name}-2024"
})

# Task-based sessions
task_session = await client.create_session({
    "session_id": f"task-{task_id}-review"
})
```

## Infinite Sessions

Sessions that never expire (useful for long-running applications):

```python
infinite_session = await client.create_session({
    "session_id": "persistent-assistant",
    "infinite_sessions": True  # Never expires
})
```

## Conversation History

Access and export conversation history:

```python
# Get all messages
messages = session.get_messages()

for msg in messages:
    print(f"[{msg.role}] {msg.content}")

# Export to JSON
import json

history = [
    {"role": msg.role, "content": msg.content, "id": msg.id}
    for msg in messages
]

with open("conversation.json", "w") as f:
    json.dump(history, f, indent=2)
```

## Session Management

List and manage all sessions:

```python
# List all available sessions
sessions = await client.list_sessions()

for session_info in sessions:
    sid = session_info.get('sessionId', 'unknown')
    modified = session_info.get('modifiedTime', 'N/A')
    summary = session_info.get('summary', 'No summary')

    print(f"Session: {sid}")
    print(f"  Modified: {modified}")
    print(f"  Summary: {summary}")
```

## Safe Resume with Fallback

Handle cases where session may not exist:

```python
async def get_or_create_session(client, session_id, config=None):
    """Resume existing session or create new one."""
    try:
        return await client.resume_session(session_id)
    except RuntimeError:
        return await client.create_session({
            "session_id": session_id,
            **(config or {})
        })

# Usage
session = await get_or_create_session(client, "my-session")
```

## Conversation Bookmarks

Mark important points in a conversation:

```python
class ConversationBookmarks:
    """Track important points in a conversation."""

    def __init__(self):
        self.bookmarks = {}

    def mark(self, session, name, description=""):
        """Mark current position in conversation."""
        messages = session.get_messages()
        self.bookmarks[name] = {
            "message_index": len(messages) - 1,
            "message_id": messages[-1].id if messages else None,
            "description": description
        }

    def get_context_since(self, session, bookmark_name):
        """Get all messages since a bookmark."""
        if bookmark_name not in self.bookmarks:
            return []

        bookmark = self.bookmarks[bookmark_name]
        messages = session.get_messages()
        return messages[bookmark["message_index"] + 1:]


# Usage
bookmarks = ConversationBookmarks()

await session.send_and_wait({"prompt": "Let's start the design"})
bookmarks.mark(session, "design_start", "Beginning of design discussion")

await session.send_and_wait({"prompt": "Now let's discuss implementation"})
bookmarks.mark(session, "implementation_start")

# Later, get context since a bookmark
design_discussion = bookmarks.get_context_since(session, "design_start")
```

## Permanent Deletion

When you're completely done with a session:

```python
# Permanently delete - cannot be recovered
await client.delete_session("old-session-id")

# Cleanup old sessions
sessions = await client.list_sessions()
for session_info in sessions:
    if is_old(session_info):
        await client.delete_session(session_info['sessionId'])
```

## Use Cases

| Use Case | Pattern |
|----------|---------|
| User preferences | One persistent session per user |
| Project discussions | Sessions named by project |
| Audit trails | Export history before deletion |
| Long-running assistants | Infinite sessions |
| Multi-day workflows | Resume with full context |

## Best Practices

1. **Use meaningful session IDs**: Include user, project, or date identifiers
2. **Export before deleting**: Save important conversations to files
3. **Clean up old sessions**: Periodically remove unused sessions
4. **Handle resume failures**: Always wrap `resume_session()` in try-except
5. **Use infinite sessions carefully**: Only for truly persistent assistants

## Complete Example

```bash
python recipe/persisting_sessions.py
```

Demonstrates:
- Basic persistence and resume
- Session management
- Infinite sessions
- Conversation bookmarks and export

## Next Steps

- [Multiple Sessions](multiple-sessions.md): Manage concurrent sessions
- [Error Handling](error-handling.md): Handle persistence errors
- [Custom Tools](custom-tools.md): Add tools to persistent sessions
