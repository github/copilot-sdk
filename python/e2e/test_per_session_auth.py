"""E2E Per-session GitHub auth tests"""

import pytest

from copilot.session import PermissionHandler

from .testharness import E2ETestContext

pytestmark = pytest.mark.asyncio(loop_scope="module")


@pytest.fixture(scope="module")
async def auth_ctx(ctx: E2ETestContext):
    """Configure per-token user responses on the proxy before tests run."""
    proxy_url = ctx.proxy_url

    # Redirect GitHub API calls to the proxy so per-session auth token
    # resolution (fetchCopilotUser) is intercepted. Must be set before the
    # CLI subprocess is spawned (i.e., before the first create_session call).
    ctx.client._config.env["COPILOT_DEBUG_GITHUB_API_URL"] = proxy_url

    await ctx.set_copilot_user_by_token(
        "token-alice",
        {
            "login": "alice",
            "copilot_plan": "individual_pro",
            "endpoints": {
                "api": proxy_url,
                "telemetry": "https://localhost:1/telemetry",
            },
            "analytics_tracking_id": "alice-tracking-id",
        },
    )

    await ctx.set_copilot_user_by_token(
        "token-bob",
        {
            "login": "bob",
            "copilot_plan": "business",
            "endpoints": {
                "api": proxy_url,
                "telemetry": "https://localhost:1/telemetry",
            },
            "analytics_tracking_id": "bob-tracking-id",
        },
    )

    return ctx


class TestPerSessionAuth:
    async def test_should_create_session_with_github_token_and_check_auth_status(
        self, auth_ctx: E2ETestContext
    ):
        session = await auth_ctx.client.create_session(
            on_permission_request=PermissionHandler.approve_all,
            github_token="token-alice",
        )

        auth_status = await session.rpc.auth.get_status()
        assert auth_status.is_authenticated is True
        assert auth_status.login == "alice"
        assert auth_status.copilot_plan == "individual_pro"

        await session.disconnect()

    async def test_should_isolate_auth_between_sessions_with_different_tokens(
        self, auth_ctx: E2ETestContext
    ):
        session_a = await auth_ctx.client.create_session(
            on_permission_request=PermissionHandler.approve_all,
            github_token="token-alice",
        )
        session_b = await auth_ctx.client.create_session(
            on_permission_request=PermissionHandler.approve_all,
            github_token="token-bob",
        )

        status_a = await session_a.rpc.auth.get_status()
        status_b = await session_b.rpc.auth.get_status()

        assert status_a.is_authenticated is True
        assert status_a.login == "alice"
        assert status_a.copilot_plan == "individual_pro"

        assert status_b.is_authenticated is True
        assert status_b.login == "bob"
        assert status_b.copilot_plan == "business"

        await session_a.disconnect()
        await session_b.disconnect()

    async def test_should_return_unauthenticated_when_no_token_provided(
        self, auth_ctx: E2ETestContext
    ):
        session = await auth_ctx.client.create_session(
            on_permission_request=PermissionHandler.approve_all,
        )

        auth_status = await session.rpc.auth.get_status()
        # Without a per-session token, there is no per-session identity.
        # In CI the process-level fake token may still authenticate globally,
        # so we check login rather than is_authenticated.
        assert auth_status.login is None

        await session.disconnect()

    async def test_should_error_when_creating_session_with_invalid_token(
        self, auth_ctx: E2ETestContext
    ):
        with pytest.raises(Exception):
            await auth_ctx.client.create_session(
                on_permission_request=PermissionHandler.approve_all,
                github_token="invalid-token-12345",
            )
