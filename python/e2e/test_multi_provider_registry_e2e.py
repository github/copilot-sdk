"""E2E tests for the experimental multi-provider BYOK registry.

Validates that several named providers, several models per provider, and custom
agents bound to those provider-qualified models can coexist in one session, be
launched, and route inference to the configured provider with the configured
wire model and headers.
"""

import pytest

from copilot.session import PermissionHandler

from .testharness import E2ETestContext

pytestmark = pytest.mark.asyncio(loop_scope="module")


def _normalize_headers(headers) -> dict[str, str]:
    if isinstance(headers, list):
        flat: dict[str, str] = {}
        for entry in headers:
            if isinstance(entry, dict):
                key = entry.get("name") or entry.get("key")
                value = entry.get("value")
                if key is not None:
                    flat[str(key).lower()] = str(value)
        return flat
    if isinstance(headers, dict):
        flat = {}
        for key, value in headers.items():
            if isinstance(value, list):
                flat[str(key).lower()] = ", ".join(str(v) for v in value)
            else:
                flat[str(key).lower()] = str(value)
        return flat
    return {}


# A heterogeneous registry: two providers of different types, with multiple
# models each. Provider-qualified selection ids are alpha/sonnet, alpha/haiku,
# beta/opus, beta/haiku.
REGISTRY_PROVIDERS = [
    {
        "name": "alpha",
        "type": "openai",
        "wire_api": "completions",
        "base_url": "https://alpha.example.test/v1",
        "api_key": "alpha-secret",
        "headers": {"X-Provider": "alpha"},
    },
    {
        "name": "beta",
        "type": "anthropic",
        "base_url": "https://beta.example.test",
        "bearer_token": "beta-bearer",
        "headers": {"X-Provider": "beta"},
    },
]
REGISTRY_MODELS = [
    {"id": "sonnet", "provider": "alpha", "wire_model": "byok-gpt-4o", "max_prompt_tokens": 111111},
    {"id": "haiku", "provider": "alpha", "wire_model": "byok-gpt-4o-mini"},
    {"id": "opus", "provider": "beta", "wire_model": "byok-claude-3-opus"},
    {"id": "haiku", "provider": "beta", "wire_model": "byok-claude-3-haiku"},
]
REGISTRY_AGENTS = [
    {"name": "orchestrator", "display_name": "Orchestrator", "description": "Top-level planner.", "prompt": "Plan and delegate.", "model": "alpha/sonnet"},
    {"name": "researcher", "display_name": "Researcher", "description": "Deep research subagent.", "prompt": "Research thoroughly.", "model": "beta/opus"},
    {"name": "fast-helper", "display_name": "Fast Helper", "description": "Quick subagent.", "prompt": "Answer quickly.", "model": "alpha/haiku"},
    {"name": "summarizer", "display_name": "Summarizer", "description": "Summarizing subagent.", "prompt": "Summarize.", "model": "beta/haiku"},
]


class TestMultiProviderRegistry:
    async def test_should_register_multiple_providers_with_custom_agents_bound_to_their_models(
        self, ctx: E2ETestContext
    ):
        session = await ctx.client.create_session(
            on_permission_request=PermissionHandler.approve_all,
            providers=REGISTRY_PROVIDERS,
            models=REGISTRY_MODELS,
            custom_agents=REGISTRY_AGENTS,
        )

        try:
            result = await session.rpc.agent.list()

            # All four custom agents coexist in a single session.
            assert result.agents is not None
            assert len(result.agents) == 4

            # Each agent is bound to its configured provider-qualified BYOK model.
            by_name = {agent.name: agent for agent in result.agents}
            assert by_name["orchestrator"].model == "alpha/sonnet"
            assert by_name["researcher"].model == "beta/opus"
            assert by_name["fast-helper"].model == "alpha/haiku"
            assert by_name["summarizer"].model == "beta/haiku"

            # Models from BOTH providers are represented, proving the two
            # providers and their models coexist within the same session.
            bound_models = [agent.model or "" for agent in result.agents]
            assert any(m.startswith("alpha/") for m in bound_models)
            assert any(m.startswith("beta/") for m in bound_models)
        finally:
            await session.disconnect()

    async def _assert_routing(
        self,
        ctx: E2ETestContext,
        selection_id: str,
        expected_wire_model: str,
        expected_provider_header: str,
    ):
        # Two OpenAI-compatible providers, both pointed at the replay proxy so
        # their /chat/completions traffic is captured. They are distinguished on
        # the wire by their per-provider X-Provider header. "alpha" carries two
        # models (multiple models per provider); "delta" carries one.
        providers = [
            {
                "name": "alpha",
                "type": "openai",
                "wire_api": "completions",
                "base_url": ctx.proxy_url,
                "api_key": "alpha-secret",
                "headers": {"X-Provider": "alpha"},
            },
            {
                "name": "delta",
                "type": "openai",
                "wire_api": "completions",
                "base_url": ctx.proxy_url,
                "api_key": "delta-secret",
                "headers": {"X-Provider": "delta"},
            },
        ]
        models = [
            {"id": "sonnet", "provider": "alpha", "wire_model": "byok-gpt-4o"},
            {"id": "haiku", "provider": "alpha", "wire_model": "byok-gpt-4o-mini"},
            {"id": "turbo", "provider": "delta", "wire_model": "byok-gpt-4-turbo"},
        ]

        session = await ctx.client.create_session(
            on_permission_request=PermissionHandler.approve_all,
            model=selection_id,
            providers=providers,
            models=models,
        )

        try:
            await session.send_and_wait("What is 5+5?")

            exchanges = await ctx.get_exchanges()
            assert len(exchanges) == 1
            exchange = exchanges[0]

            # The wire model sent to the provider is the selected model's
            # wire_model, not its provider-qualified selection id.
            assert exchange["request"]["model"] == expected_wire_model

            # The request carried the owning provider's custom header, proving
            # the turn was dispatched against the correct provider connection.
            headers = _normalize_headers(exchange.get("requestHeaders"))
            assert headers.get("x-provider") == expected_provider_header

            # The provider's API key was applied as an Authorization header.
            assert headers.get("authorization")
        finally:
            await session.disconnect()

    async def test_should_route_alpha_sonnet_turn_to_its_provider_and_wire_model(
        self, ctx: E2ETestContext
    ):
        await self._assert_routing(ctx, "alpha/sonnet", "byok-gpt-4o", "alpha")

    async def test_should_route_alpha_haiku_turn_to_its_provider_and_wire_model(
        self, ctx: E2ETestContext
    ):
        await self._assert_routing(ctx, "alpha/haiku", "byok-gpt-4o-mini", "alpha")

    async def test_should_route_delta_turbo_turn_to_its_provider_and_wire_model(
        self, ctx: E2ETestContext
    ):
        await self._assert_routing(ctx, "delta/turbo", "byok-gpt-4-turbo", "delta")
