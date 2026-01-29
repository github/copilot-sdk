# Managing Local Files

Use Copilot to intelligently organize files based on metadata and content.

> **Skill Level:** Beginner to Advanced
>
> **Runnable Example:** [recipe/managing_local_files.py](recipe/managing_local_files.py)
>
> ```bash
> cd recipe && pip install -r requirements.txt
> python managing_local_files.py
> ```

## Overview

This recipe demonstrates AI-powered file organization:

- Multiple organization strategies (extension, date, size, smart)
- Permission handling for file operations
- Interactive mode for user confirmation
- Dry-run mode for preview without changes

## Quick Start

```python
import asyncio
import os
from copilot import CopilotClient
from copilot.types import SessionEventType

async def main():
    client = CopilotClient()
    await client.start()

    session = await client.create_session()

    # Event handler for visibility
    def handle_event(event):
        if event.type == SessionEventType.ASSISTANT_MESSAGE:
            print(f"\nCopilot: {event.data.content}")
        elif event.type == SessionEventType.TOOL_EXECUTION_START:
            print(f"  → Running: {event.data.tool_name}")
        elif event.type == SessionEventType.TOOL_EXECUTION_COMPLETE:
            print(f"  ✓ Completed")

    session.on(handle_event)

    # Organize files
    target = os.path.expanduser("~/Downloads")

    await session.send_and_wait({
        "prompt": f"""
Analyze the files in "{target}" and organize them into subfolders by type:

1. List all files and their metadata
2. Group by extension (images, documents, videos, etc.)
3. Create appropriate subfolders
4. Move files to their categories

Please confirm before moving any files.
"""
    })

    await session.destroy()
    await client.stop()

asyncio.run(main())
```

## Organization Strategies

### By File Extension

```python
await session.send_and_wait({
    "prompt": f"Organize files in '{folder}' by extension into categories like images, documents, videos"
})

# Result:
# images/   -> .jpg, .png, .gif, .webp
# documents/ -> .pdf, .docx, .txt, .xlsx
# videos/   -> .mp4, .avi, .mov, .mkv
# audio/    -> .mp3, .wav, .flac
# code/     -> .py, .js, .ts, .cpp
```

### By Date

```python
await session.send_and_wait({
    "prompt": f"Organize files in '{folder}' by creation date into monthly folders"
})

# Result:
# 2024-01/  -> files from January 2024
# 2024-02/  -> files from February 2024
```

### By Size

```python
await session.send_and_wait({
    "prompt": f"Organize files in '{folder}' by size: tiny (<1KB), small (<1MB), medium (<100MB), large (>100MB)"
})

# Result:
# tiny-under-1kb/
# small-under-1mb/
# medium-under-100mb/
# large-over-100mb/
```

### Smart Organization

Let AI determine the best organization:

```python
await session.send_and_wait({
    "prompt": f"""
Analyze files in '{folder}' and suggest a logical organization based on:
- File names and content hints
- File types and typical uses
- Date patterns suggesting projects or events

Propose descriptive folder names.
"""
})
```

## Permission Handling

Control what file operations are allowed:

```python
def create_permission_handler(mode="confirm"):
    """Create permission handler for file operations."""
    def handler(event):
        if event.type != "permission.requested":
            return None

        permission = event.data.permission_type
        resource = event.data.resource

        if mode == "allow-all":
            return True
        elif mode == "deny-writes":
            if permission in ["write", "delete", "move"]:
                print(f"Denied: {permission} on {resource}")
                return False
            return True
        elif mode == "confirm":
            print(f"\nPermission requested: {permission}")
            print(f"  Resource: {resource}")
            response = input("  Allow? (y/n): ").lower()
            return response == 'y'

        return False

    return handler

# Usage
session.on(create_permission_handler(mode="confirm"))
```

## Dry-Run Mode

Preview changes without executing:

```python
await session.send_and_wait({
    "prompt": f"""
Analyze files in '{folder}' and show me how you would organize them.
DO NOT move any files - just show me the plan in a table format:

| Current Path | Proposed Folder | Reason |
"""
})
```

## Interactive Mode

Get user confirmation for each action:

```python
async def interactive_organize(session, folder, strategy="extension"):
    """Interactive file organization with confirmations."""

    # Step 1: Analyze
    await session.send_and_wait({
        "prompt": f"List all files in '{folder}' with their metadata (size, date, type)"
    })

    # Step 2: Propose
    await session.send_and_wait({
        "prompt": f"Propose an organization by {strategy}. Show in a table."
    })

    # Step 3: Confirm
    confirm = input("\nProceed with organization? (y/n): ")
    if confirm.lower() != 'y':
        print("Cancelled.")
        return

    # Step 4: Execute
    await session.send_and_wait({
        "prompt": "Execute the proposed organization. Report progress."
    })
```

## File Filtering

Organize specific file types only:

```python
await session.send_and_wait({
    "prompt": f"""
In '{folder}', organize ONLY image files (.jpg, .png, .gif):
- By resolution: small (<500px), medium (<2000px), large (>2000px)
- Skip non-image files
"""
})
```

## Duplicate Handling

Handle files with the same name:

```python
await session.send_and_wait({
    "prompt": f"""
Organize files in '{folder}' by type. When duplicates exist:
- Add a numeric suffix (file_1.txt, file_2.txt)
- Keep the newest version in the main folder
- Report all duplicates found
"""
})
```

## Safety Considerations

| Concern | Solution |
|---------|----------|
| Accidental deletion | Use dry-run first |
| Permission errors | Set up permission handler |
| Duplicate names | Add suffix or skip |
| Important files | Copy instead of move |
| Undo capability | Log all operations |

## Best Practices

1. **Always dry-run first**: Preview changes before executing
2. **Use permission handlers**: Control what operations are allowed
3. **Back up important files**: Copy instead of move for critical data
4. **Log operations**: Keep a record of what was moved where
5. **Confirm before bulk operations**: Especially for delete operations

## Complete Example

```bash
python recipe/managing_local_files.py
```

Demonstrates:
- All organization strategies
- Permission handling
- Interactive mode
- Dry-run preview

## Next Steps

- [Error Handling](error-handling.md): Handle file operation errors
- [Custom Tools](custom-tools.md): Create specialized file tools
- [Multiple Sessions](multiple-sessions.md): Parallel file processing
