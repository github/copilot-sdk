# Custom Providers (BYOK)

Configure custom model providers with your own API keys.

> **Skill Level:** Advanced
>
> **Runnable Example:** [recipe/custom_providers.py](recipe/custom_providers.py)
>
> ```bash
> cd recipe && pip install -r requirements.txt
> python custom_providers.py
> ```

## Overview

This recipe covers Bring Your Own Key (BYOK) patterns:

- OpenAI API configuration
- Azure OpenAI integration
- Anthropic Claude configuration
- Custom endpoints
- Provider fallback patterns

## Quick Start

```python
import asyncio
import os
from copilot import CopilotClient, ProviderConfig

async def main():
    # Configure OpenAI provider
    provider = ProviderConfig(
        type="openai",
        api_key=os.environ["OPENAI_API_KEY"],
        model="gpt-4o"
    )

    client = CopilotClient()
    await client.start()

    session = await client.create_session({
        "provider": provider
    })

    await session.send_and_wait({
        "prompt": "Hello from custom provider!"
    })

    await session.destroy()
    await client.stop()

asyncio.run(main())
```

## Provider Configurations

### OpenAI

```python
from copilot import ProviderConfig

openai_provider = ProviderConfig(
    type="openai",
    api_key=os.environ["OPENAI_API_KEY"],
    model="gpt-4o",  # or "gpt-4-turbo", "gpt-3.5-turbo"
    base_url=None  # Optional: custom endpoint
)
```

### Azure OpenAI

```python
azure_provider = ProviderConfig(
    type="azure",
    api_key=os.environ["AZURE_OPENAI_API_KEY"],
    base_url=os.environ["AZURE_OPENAI_ENDPOINT"],  # e.g., "https://your-resource.openai.azure.com"
    model="gpt-4o",  # Your deployment name
    api_version="2024-02-15-preview"  # Optional
)
```

### Anthropic Claude

```python
anthropic_provider = ProviderConfig(
    type="anthropic",
    api_key=os.environ["ANTHROPIC_API_KEY"],
    model="claude-sonnet-4-20250514"  # or "claude-3-opus-20240229"
)
```

### Custom Endpoint

```python
custom_provider = ProviderConfig(
    type="openai",  # Use OpenAI-compatible format
    api_key=os.environ["CUSTOM_API_KEY"],
    base_url="https://your-custom-endpoint.com/v1",
    model="your-model-name"
)
```

## Helper Functions

Create provider helpers for cleaner code:

```python
def get_openai_provider(model="gpt-4o"):
    """Get OpenAI provider configuration."""
    api_key = os.environ.get("OPENAI_API_KEY")
    if not api_key:
        raise ValueError("OPENAI_API_KEY not set")

    return ProviderConfig(
        type="openai",
        api_key=api_key,
        model=model
    )


def get_azure_provider(deployment_name="gpt-4o"):
    """Get Azure OpenAI provider configuration."""
    api_key = os.environ.get("AZURE_OPENAI_API_KEY")
    endpoint = os.environ.get("AZURE_OPENAI_ENDPOINT")

    if not api_key or not endpoint:
        raise ValueError("AZURE_OPENAI_API_KEY and AZURE_OPENAI_ENDPOINT required")

    return ProviderConfig(
        type="azure",
        api_key=api_key,
        base_url=endpoint,
        model=deployment_name
    )


def get_anthropic_provider(model="claude-sonnet-4-20250514"):
    """Get Anthropic provider configuration."""
    api_key = os.environ.get("ANTHROPIC_API_KEY")
    if not api_key:
        raise ValueError("ANTHROPIC_API_KEY not set")

    return ProviderConfig(
        type="anthropic",
        api_key=api_key,
        model=model
    )
```

## Provider Selection

Choose providers based on task:

```python
async def select_provider_for_task(task_type):
    """Select the best provider for a given task."""
    providers = {
        "code": get_openai_provider("gpt-4o"),
        "creative": get_anthropic_provider("claude-sonnet-4-20250514"),
        "fast": get_openai_provider("gpt-4o-mini"),
        "enterprise": get_azure_provider("gpt-4o"),
    }

    return providers.get(task_type, providers["code"])


# Usage
client = CopilotClient()
await client.start()

provider = await select_provider_for_task("code")
session = await client.create_session({"provider": provider})
```

## Provider Fallback

Implement fallback when primary provider fails:

```python
async def create_session_with_fallback(client, primary, fallbacks):
    """Create session with provider fallback."""
    providers = [primary] + fallbacks

    for i, provider in enumerate(providers):
        try:
            session = await client.create_session({"provider": provider})

            # Test the connection
            await session.send_and_wait({
                "prompt": "ping"
            }, timeout=10.0)

            print(f"Using provider {i + 1}: {provider.type}")
            return session

        except Exception as e:
            print(f"Provider {i + 1} failed: {e}")
            if i < len(providers) - 1:
                print("Trying next provider...")
            continue

    raise RuntimeError("All providers failed")


# Usage
session = await create_session_with_fallback(
    client,
    primary=get_openai_provider(),
    fallbacks=[
        get_azure_provider(),
        get_anthropic_provider()
    ]
)
```

## Multiple Providers

Use different providers in the same application:

```python
async def multi_provider_demo():
    """Demonstrate using multiple providers."""
    client = CopilotClient()
    await client.start()

    # OpenAI for code tasks
    code_session = await client.create_session({
        "session_id": "code-assistant",
        "provider": get_openai_provider("gpt-4o")
    })

    # Claude for analysis
    analysis_session = await client.create_session({
        "session_id": "analysis-assistant",
        "provider": get_anthropic_provider()
    })

    # Use each for their strengths
    await code_session.send_and_wait({
        "prompt": "Write a Python function to parse JSON"
    })

    await analysis_session.send_and_wait({
        "prompt": "Analyze the security implications of parsing untrusted JSON"
    })

    await code_session.destroy()
    await analysis_session.destroy()
    await client.stop()
```

## Environment Setup

Recommended environment variables:

```bash
# .env file
OPENAI_API_KEY=sk-...
AZURE_OPENAI_API_KEY=...
AZURE_OPENAI_ENDPOINT=https://your-resource.openai.azure.com
ANTHROPIC_API_KEY=sk-ant-...
```

Load with python-dotenv:

```python
from dotenv import load_dotenv
load_dotenv()
```

## Provider Comparison

| Provider | Best For | Models |
|----------|----------|--------|
| OpenAI | General use, code | gpt-4o, gpt-4o-mini |
| Azure OpenAI | Enterprise, compliance | Same as OpenAI |
| Anthropic | Analysis, safety | claude-3-opus, claude-sonnet-4 |

## Error Handling

Handle provider-specific errors:

```python
async def safe_provider_request(session, prompt):
    """Handle provider-specific errors."""
    try:
        await session.send_and_wait({"prompt": prompt})

    except TimeoutError:
        print("Request timed out - provider may be slow")

    except Exception as e:
        error_msg = str(e).lower()

        if "rate limit" in error_msg:
            print("Rate limited - waiting before retry")
            await asyncio.sleep(60)

        elif "invalid api key" in error_msg:
            print("Invalid API key - check configuration")

        elif "model not found" in error_msg:
            print("Model not available - check model name")

        else:
            print(f"Provider error: {e}")

        raise
```

## Best Practices

1. **Secure API keys**: Use environment variables, never commit keys
2. **Implement fallbacks**: Have backup providers ready
3. **Match provider to task**: Use the right model for the job
4. **Handle rate limits**: Implement retry with backoff
5. **Monitor costs**: Different providers have different pricing

## Complete Example

```bash
# Set environment variables first
export OPENAI_API_KEY=sk-...

python recipe/custom_providers.py
```

Demonstrates:
- Multiple provider configurations
- Provider selection
- Fallback patterns
- Multi-provider applications

## Next Steps

- [Custom Agents](custom-agents.md): Use providers with custom agents
- [Error Handling](error-handling.md): Handle provider errors
- [MCP Servers](mcp-servers.md): Combine providers with MCP tools
