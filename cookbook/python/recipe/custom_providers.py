#!/usr/bin/env python3
"""
Custom Providers (BYOK) - Bring Your Own Key to use custom AI providers.
Run: python custom_providers.py
"""

import asyncio
import os

from copilot import CopilotClient, ProviderConfig
from copilot.types import SessionEventType


# =============================================================================
# Provider Configurations
# =============================================================================


def get_openai_provider():
    """Configure OpenAI as the provider. Requires OPENAI_API_KEY."""
    api_key = os.environ.get("OPENAI_API_KEY")
    if not api_key:
        raise ValueError("OPENAI_API_KEY environment variable not set")

    return ProviderConfig(
        type="openai",
        base_url="https://api.openai.com/v1",
        api_key=api_key,
        wire_api="responses",
    )


def get_azure_openai_provider():
    """Configure Azure OpenAI. Requires AZURE_OPENAI_API_KEY and AZURE_OPENAI_ENDPOINT."""
    api_key = os.environ.get("AZURE_OPENAI_API_KEY")
    endpoint = os.environ.get("AZURE_OPENAI_ENDPOINT")

    if not api_key:
        raise ValueError("AZURE_OPENAI_API_KEY environment variable not set")
    if not endpoint:
        raise ValueError("AZURE_OPENAI_ENDPOINT environment variable not set")

    return ProviderConfig(
        type="azure",
        base_url=endpoint,
        api_key=api_key,
        azure={"api_version": "2024-10-21"},
    )


def get_anthropic_provider():
    """Configure Anthropic Claude. Requires ANTHROPIC_API_KEY."""
    api_key = os.environ.get("ANTHROPIC_API_KEY")
    if not api_key:
        raise ValueError("ANTHROPIC_API_KEY environment variable not set")

    return ProviderConfig(
        type="anthropic",
        base_url="https://api.anthropic.com",
        api_key=api_key,
    )


def get_custom_endpoint_provider():
    """Configure a custom OpenAI-compatible endpoint (local LLM, etc.)."""
    endpoint = os.environ.get("CUSTOM_ENDPOINT", "http://localhost:8080/v1")
    api_key = os.environ.get("CUSTOM_API_KEY", "not-required")

    return ProviderConfig(
        type="openai",
        base_url=endpoint,
        api_key=api_key,
    )


def get_bearer_token_provider():
    """Configure a provider using bearer token authentication."""
    token = os.environ.get("BEARER_TOKEN")
    endpoint = os.environ.get("API_ENDPOINT")

    if not token:
        raise ValueError("BEARER_TOKEN environment variable not set")
    if not endpoint:
        raise ValueError("API_ENDPOINT environment variable not set")

    return ProviderConfig(
        type="openai",
        base_url=endpoint,
        bearer_token=token,
    )


# =============================================================================
# Demo: Using Providers
# =============================================================================


async def demo_with_provider(provider_name, provider_config):
    """Test a custom provider with a simple prompt."""
    print(f"\n--- Testing {provider_name} ---\n")

    client = CopilotClient()

    try:
        await client.start()
        session = await client.create_session({"provider": provider_config})

        response = None

        def handler(event):
            nonlocal response
            if event.type == SessionEventType.ASSISTANT_MESSAGE:
                response = event.data.content

        session.on(handler)

        # Test with a simple prompt
        await session.send_and_wait(
            {"prompt": "Say 'Hello from custom provider' and nothing else."},
            timeout=60.0,
        )

        if response:
            print(f"✓ Response: {response}")
        else:
            print("✗ No response received")

        await session.destroy()

    except Exception as e:
        print(f"✗ Error: {e}")

    finally:
        await client.stop()


async def demo_openai():
    """Demonstrate using OpenAI as the provider."""
    try:
        provider = get_openai_provider()
        await demo_with_provider("OpenAI", provider)
    except ValueError as e:
        print(f"\n⚠️ Skipping OpenAI demo: {e}")


async def demo_azure():
    """Demonstrate using Azure OpenAI as the provider."""
    try:
        provider = get_azure_openai_provider()
        await demo_with_provider("Azure OpenAI", provider)
    except ValueError as e:
        print(f"\n⚠️ Skipping Azure demo: {e}")


async def demo_anthropic():
    """Demonstrate using Anthropic as the provider."""
    try:
        provider = get_anthropic_provider()
        await demo_with_provider("Anthropic Claude", provider)
    except ValueError as e:
        print(f"\n⚠️ Skipping Anthropic demo: {e}")


# =============================================================================
# Provider Switching
# =============================================================================


async def demo_provider_switching():
    """Switch between providers for different tasks."""
    print("\n=== Provider Switching Demo ===\n")

    client = CopilotClient()

    try:
        await client.start()

        available_providers = []
        if os.environ.get("OPENAI_API_KEY"):
            available_providers.append(("OpenAI", get_openai_provider()))
        if os.environ.get("ANTHROPIC_API_KEY"):
            available_providers.append(("Anthropic", get_anthropic_provider()))

        if not available_providers:
            print("No custom providers configured.")
            return

        sessions = {}
        for name, provider in available_providers:
            session = await client.create_session({"provider": provider})
            sessions[name] = session
            print(f"✓ Created session with {name}")

        for name, session in sessions.items():
            response = None

            def handler(event):
                nonlocal response
                if event.type == SessionEventType.ASSISTANT_MESSAGE:
                    response = event.data.content

            session.on(handler)
            await session.send_and_wait(
                {"prompt": f"You are {name}. Say 'Hello!'"},
                timeout=60.0,
            )
            print(f"\n{name}: {response}")
            await session.destroy()

    finally:
        await client.stop()


# =============================================================================
# Fallback Pattern
# =============================================================================


async def demo_fallback_pattern():
    """Fallback pattern - try primary provider, fall back to secondary."""
    print("\n=== Fallback Pattern Demo ===\n")

    providers = []
    if os.environ.get("OPENAI_API_KEY"):
        providers.append(("OpenAI", get_openai_provider()))
    if os.environ.get("ANTHROPIC_API_KEY"):
        providers.append(("Anthropic", get_anthropic_provider()))

    if not providers:
        print("No providers available. Set API keys to test fallback pattern.")
        return

    print(f"Provider priority: {[p[0] for p in providers]}")

    client = CopilotClient()

    try:
        await client.start()

        for name, provider in providers:
            print(f"\nTrying {name}...")
            try:
                session = await client.create_session({"provider": provider})

                response = None

                def handler(event):
                    nonlocal response
                    if event.type == SessionEventType.ASSISTANT_MESSAGE:
                        response = event.data.content

                session.on(handler)
                await session.send_and_wait(
                    {"prompt": "Say 'Success!' and nothing else."},
                    timeout=30.0,
                )
                await session.destroy()

                if response:
                    print(f"✓ {name} succeeded: {response}")
                    break  # Success, no need to try others

            except Exception as e:
                print(f"✗ {name} failed: {e}")
                continue  # Try next provider

        else:
            print("\n✗ All providers failed!")

    finally:
        await client.stop()


# =============================================================================
# Configuration Guide
# =============================================================================


def print_configuration_guide():
    """Print setup instructions for custom providers."""
    print("""
CONFIGURATION GUIDE - Bring Your Own Key (BYOK)

Set environment variables for your provider:

  OpenAI:     export OPENAI_API_KEY="sk-..."
  Azure:      export AZURE_OPENAI_API_KEY="..."
              export AZURE_OPENAI_ENDPOINT="https://your-resource.openai.azure.com"
  Anthropic:  export ANTHROPIC_API_KEY="sk-ant-..."
""")


# =============================================================================
# Main
# =============================================================================


async def main():
    """Run BYOK demonstrations."""
    print("=" * 60)
    print("Custom Providers (BYOK)")
    print("=" * 60)

    print_configuration_guide()

    has_openai = bool(os.environ.get("OPENAI_API_KEY"))
    has_azure = bool(os.environ.get("AZURE_OPENAI_API_KEY"))
    has_anthropic = bool(os.environ.get("ANTHROPIC_API_KEY"))

    print("Detected providers:")
    print(f"  OpenAI:    {'✓' if has_openai else '✗'}")
    print(f"  Azure:     {'✓' if has_azure else '✗'}")
    print(f"  Anthropic: {'✓' if has_anthropic else '✗'}")

    if not any([has_openai, has_azure, has_anthropic]):
        print("\n⚠️ No API keys found. Set environment variables to test.")
        return

    await demo_openai()
    await demo_azure()
    await demo_anthropic()
    await demo_provider_switching()
    await demo_fallback_pattern()

    print("\n" + "=" * 60)
    print("All demos completed!")
    print("=" * 60)


if __name__ == "__main__":
    asyncio.run(main())
