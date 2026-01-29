# GitHub Copilot SDK Cookbook — Python

Practical recipes for the GitHub Copilot SDK with Python. Each recipe is self-contained and ready to run.

## Installation **Copilot CLI:**

  Refer to the [Getting Started guide](../docs/getting-started.md#prerequisites) for installation instructions.

## 📚 Recipes

| Recipe | Level | Description |
| -------- | ------- | ------------- |
| [Error Handling](error-handling.md) | Beginner | Exceptions, retries, graceful shutdown |
| [Multiple Sessions](multiple-sessions.md) | Beginner | Managing independent conversations |
| [Persisting Sessions](persisting-sessions.md) | Beginner | Save and resume sessions |
| [Managing Local Files](managing-local-files.md) | Intermediate | AI-powered file organization |
| [Streaming Responses](streaming-responses.md) | Intermediate | Real-time response streaming |
| [Custom Tools](custom-tools.md) | Intermediate | Extend Copilot with custom tools |
| [PR Visualization](pr-visualization.md) | Intermediate | Generate PR analytics charts |
| [Custom Providers](custom-providers.md) | Advanced | BYOK for custom AI providers |
| [MCP Servers](mcp-servers.md) | Advanced | Model Context Protocol integration |
| [Custom Agents](custom-agents.md) | Advanced | Build specialized assistants |

## 🚀 Quick Start

```bash
cd cookbook/python/recipe
pip install -r requirements.txt

# Run any recipe
python error_handling.py
python pr_visualization.py --repo github/copilot-sdk
```

## 📦 Requirements

- Python 3.9+ (supports up to 3.14)
- Copilot CLI installed and authenticated

## 🔧 Troubleshooting

| Issue | Solution |
| ------- | ---------- |
| `FileNotFoundError: Copilot CLI not found` | Install the Copilot CLI |
| `ConnectionError` | Check network and CLI status |
| `TimeoutError` | Increase timeout in `send_and_wait()` |

Enable debug logging:

```python
client = CopilotClient({"log_level": "debug"})
```

## 📝 Contributing

1. Add a Python file in `recipe/`
2. Add a matching `.md` file here
3. Update this README

See [CONTRIBUTING.md](../../CONTRIBUTING.md) for details.
