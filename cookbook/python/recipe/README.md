# Runnable Recipe Examples

This folder contains standalone, executable Python examples for each cookbook recipe. Each file can be run directly as a Python script.

## Prerequisites

- Python 3.9 or later
- Install dependencies (this installs the local SDK in editable mode):

```bash
pip install -r requirements.txt
```

## Running Examples

Each `.py` file is a complete, runnable program with executable permissions:

```bash
python <filename>.py
# or on Unix-like systems:
./<filename>.py
```

### Available Recipes

| Recipe               | Command                          | Description                                |
| -------------------- | -------------------------------- | ------------------------------------------ |
| Error Handling       | `python error_handling.py`       | Demonstrates async error handling patterns |
| Multiple Sessions    | `python multiple_sessions.py`    | Manages multiple independent conversations |
| Managing Local Files | `python managing_local_files.py` | Organizes files using AI grouping          |
| PR Visualization     | `python pr_visualization.py`     | Generates PR age charts                    |
| Persisting Sessions  | `python persisting_sessions.py`  | Save and resume sessions across restarts   |

### Examples with Arguments

**PR Visualization with specific repo:**

```bash
python pr_visualization.py --repo github/copilot-sdk
```

**Managing Local Files (edit the file to change target folder):**

```bash
# Edit the target_folder variable in managing_local_files.py first
python managing_local_files.py
```

## About the SDK API

The Copilot SDK is fully asynchronous. All examples use `asyncio.run()` to run the async main function:

```python
import asyncio
from copilot import CopilotClient

async def main():
    client = CopilotClient()
    await client.start()

    session = await client.create_session()
    await session.send_and_wait({"prompt": "Hello!"})

    await session.destroy()
    await client.stop()

if __name__ == "__main__":
    asyncio.run(main())
```

### Key API Patterns

- **Async methods**: `start()`, `stop()`, `create_session()`, `send()`, `destroy()` all require `await`
- **Configuration dicts**: Pass options as dictionaries, e.g., `{"prompt": "Hello"}`
- **Event objects**: Events have `.type` and `.data` attributes (not dict access)
- **send_and_wait()**: Convenience method that sends and waits for completion

## Local SDK Development

The `requirements.txt` installs the local Copilot SDK using `-e ../../../python` (editable install). This means:

- Changes to the SDK source are immediately available
- No need to publish or install from PyPI
- Perfect for testing and development

If you modify the SDK source, Python will automatically use the updated code (no rebuild needed).

## Python Best Practices

These examples follow Python conventions:

- PEP 8 naming (snake_case for functions and variables)
- Shebang line for direct execution
- Proper exception handling
- Type hints where appropriate
- Module docstrings for documentation
- Async/await patterns

## Virtual Environment (Recommended)

For isolated development:

```bash
# Create virtual environment
python -m venv venv

# Activate it
# Windows:
venv\Scripts\activate
# Unix/macOS:
source venv/bin/activate

# Install dependencies
pip install -r requirements.txt
```

## Learning Resources

- [Python Documentation](https://docs.python.org/3/)
- [Python asyncio](https://docs.python.org/3/library/asyncio.html)
- [PEP 8 Style Guide](https://pep8.org/)
- [GitHub Copilot SDK for Python](../../../python/README.md)
- [Parent Cookbook](../README.md)
