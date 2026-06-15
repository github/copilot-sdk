"""E2E tests for session.provider.getEndpoint."""

# session.provider.getEndpoint is gated behind COPILOT_ALLOW_GET_PROVIDER_ENDPOINT;
# the harness env passed to the CLI subprocess opts in for this test file.

import re

import pytest

from copilot.client import CopilotClient, RuntimeConnection
from copilot.generated.rpc import ProviderEndpointType, ProviderEndpointWireApi
from copilot.session import PermissionHandler

from .testharness import E2ETestContext

pytestmark = pytest.mark.asyncio(loop_scope="module")


@pytest.fixture(scope="module")
async def provider_ctx(ctx: E2ETestContext):
    env = {**ctx.get_env(), "COPILOT_ALLOW_GET_PROVIDER_ENDPOINT": "true"}
    client = CopilotClient(
        connection=RuntimeConnection.for_stdio(path=ctx.cli_path),
        working_directory=ctx.work_dir,
        env=env,
        github_token=env["GITHUB_TOKEN"],
    )
    try:
        yield ctx, client
    finally:
        await client.stop()


class TestProviderEndpoint:
    async def test_returns_byok_provider_endpoint_when_custom_provider_is_configured(
        self, provider_ctx: tuple[E2ETestContext, CopilotClient]
    ):
        _, client = provider_ctx
        session = await client.create_session(
            on_permission_request=PermissionHandler.approve_all,
            provider={
                "type": "openai",
                "wire_api": "completions",
                "base_url": "https://api.example.test/v1",
                "api_key": "byok-secret",
                "headers": {"X-Custom-Header": "byok-yes"},
            },
        )

        try:
            endpoint = await session.rpc.provider.get_endpoint()

            assert endpoint.type == ProviderEndpointType.OPENAI
            assert endpoint.wire_api == ProviderEndpointWireApi.COMPLETIONS
            assert endpoint.base_url == "https://api.example.test/v1"
            assert endpoint.api_key == "byok-secret"
            assert endpoint.headers["X-Custom-Header"] == "byok-yes"
            # BYOK sessions never issue a CAPI session token.
            assert endpoint.session_token is None
        finally:
            try:
                await session.disconnect()
            except Exception:
                pass  # disconnect may fail since the BYOK provider URL is fake

    async def test_returns_capi_provider_endpoint_for_oauth_authenticated_session(
        self, provider_ctx: tuple[E2ETestContext, CopilotClient]
    ):
        _, client = provider_ctx
        session = await client.create_session(
            on_permission_request=PermissionHandler.approve_all,
        )

        try:
            endpoint = await session.rpc.provider.get_endpoint()

            assert endpoint.type in (
                ProviderEndpointType.OPENAI,
                ProviderEndpointType.AZURE,
                ProviderEndpointType.ANTHROPIC,
            )
            # wire_api is omitted for anthropic; otherwise one of the OpenAI shapes.
            if endpoint.type != ProviderEndpointType.ANTHROPIC:
                assert endpoint.wire_api in (
                    ProviderEndpointWireApi.COMPLETIONS,
                    ProviderEndpointWireApi.RESPONSES,
                )

            # CAPI baseUrl is the (proxy) Copilot API URL injected by the harness.
            assert re.match(r"^https?://", endpoint.base_url)

            # For CAPI OAuth sessions the api_key is the resolved GitHub bearer.
            assert isinstance(endpoint.api_key, str)
            assert len(endpoint.api_key) > 0

            # Standard CAPI headers must be present, and Authorization is
            # surfaced as the runtime sends it (`Bearer <api_key>`).
            assert isinstance(endpoint.headers["Copilot-Integration-Id"], str)
            assert re.search(r"Copilot", endpoint.headers["User-Agent"], re.IGNORECASE)
            assert isinstance(endpoint.headers["X-GitHub-Api-Version"], str)
            assert re.search(r"[0-9a-f-]{8,}", endpoint.headers["X-Interaction-Id"])
            assert endpoint.headers["Authorization"] == f"Bearer {endpoint.api_key}"

            # When the omit-model_id path returned an auto-mode session token,
            # it must use the documented header name. The harness may have a
            # non-auto model selected, in which case the field is simply
            # omitted.
            if endpoint.session_token is not None:
                assert endpoint.session_token.header == "Copilot-Session-Token"
                assert len(endpoint.session_token.token) > 0
                # When provided, expires_at should be a parseable ISO timestamp.
                if endpoint.session_token.expires_at is not None:
                    from datetime import datetime

                    datetime.fromisoformat(endpoint.session_token.expires_at.replace("Z", "+00:00"))
        finally:
            await session.disconnect()
