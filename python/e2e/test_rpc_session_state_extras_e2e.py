"""
E2E coverage for additional session-scoped RPC methods.

Mirrors ``dotnet/test/E2E/RpcSessionStateExtrasE2ETests.cs`` (snapshot
category ``rpc_session_state_extras``).
"""

from __future__ import annotations

import contextlib
import json

import pytest

from copilot import CopilotClient, RuntimeConnection
from copilot.rpc import PermissionsSetAllowAllRequest
from copilot.session import PermissionHandler

from .testharness import E2ETestContext

pytestmark = pytest.mark.asyncio(loop_scope="module")


def _make_authed_client(ctx: E2ETestContext, token: str) -> CopilotClient:
    env = ctx.get_env()
    env["COPILOT_DEBUG_GITHUB_API_URL"] = ctx.proxy_url
    return CopilotClient(
        connection=RuntimeConnection.for_stdio(path=ctx.cli_path),
        working_directory=ctx.work_dir,
        env=env,
        github_token=token,
    )


async def _configure_user(ctx: E2ETestContext, token: str) -> None:
    await ctx.set_copilot_user_by_token(
        token,
        {
            "login": "rpc-session-extras-user",
            "copilot_plan": "individual_pro",
            "endpoints": {
                "api": ctx.proxy_url,
                "telemetry": "https://localhost:1/telemetry",
            },
            "analytics_tracking_id": "rpc-session-extras-tracking-id",
        },
    )


async def _stop_client(client: CopilotClient) -> None:
    with contextlib.suppress(ExceptionGroup):
        await client.stop()


class TestRpcSessionStateExtras:
    async def test_should_list_models_for_session(self, ctx: E2ETestContext):
        token = "rpc-session-model-list-token"
        await _configure_user(ctx, token)
        client = _make_authed_client(ctx, token)
        try:
            async with await client.create_session(
                model="claude-sonnet-4.5",
                on_permission_request=PermissionHandler.approve_all,
                github_token=token,
            ) as session:
                result = await session.rpc.model.list()

                assert result.list is not None
                assert len(result.list) > 0
                assert any(
                    "claude-sonnet-4.5" in json.dumps(model, sort_keys=True)
                    for model in result.list
                )
        finally:
            await _stop_client(client)

    async def test_should_report_session_activity_when_idle(self, ctx: E2ETestContext):
        async with await ctx.client.create_session(
            on_permission_request=PermissionHandler.approve_all,
        ) as session:
            activity = await session.rpc.metadata.activity()

            assert activity.has_active_work is False
            assert activity.abortable is False

    async def test_should_get_and_set_allowall_permissions(self, ctx: E2ETestContext):
        async with await ctx.client.create_session(
            on_permission_request=PermissionHandler.approve_all,
        ) as session:
            try:
                initial = await session.rpc.permissions.get_allow_all()
                assert initial.enabled is False

                enable = await session.rpc.permissions.set_allow_all(
                    PermissionsSetAllowAllRequest(enabled=True)
                )
                assert enable.success is True
                assert enable.enabled is True
                assert (await session.rpc.permissions.get_allow_all()).enabled is True

                disable = await session.rpc.permissions.set_allow_all(
                    PermissionsSetAllowAllRequest(enabled=False)
                )
                assert disable.success is True
                assert disable.enabled is False
                assert (await session.rpc.permissions.get_allow_all()).enabled is False
            finally:
                with contextlib.suppress(Exception):
                    await session.rpc.permissions.set_allow_all(
                        PermissionsSetAllowAllRequest(enabled=False)
                    )

    async def test_should_read_empty_sql_todos_for_fresh_session(self, ctx: E2ETestContext):
        async with await ctx.client.create_session(
            on_permission_request=PermissionHandler.approve_all,
        ) as session:
            result = await session.rpc.plan.read_sql_todos()

            assert result.rows is not None
            assert result.rows == []

    async def test_should_get_telemetry_engagement_id(self, ctx: E2ETestContext):
        async with await ctx.client.create_session(
            on_permission_request=PermissionHandler.approve_all,
        ) as session:
            result = await session.rpc.telemetry.get_engagement_id()

            assert result is not None

    async def test_should_get_current_tool_metadata_after_initialization(self, ctx: E2ETestContext):
        async with await ctx.client.create_session(
            on_permission_request=PermissionHandler.approve_all,
        ) as session:
            answer = await session.send_and_wait("What is 2+2?", timeout=60.0)
            assert answer is not None

            result = await session.rpc.tools.get_current_metadata()

            assert result.tools is not None
            assert len(result.tools) > 0
            assert all((tool.name or "").strip() for tool in result.tools)
            assert all(tool.description is not None for tool in result.tools)

    async def test_should_reload_session_plugins(self, ctx: E2ETestContext):
        async with await ctx.client.create_session(
            on_permission_request=PermissionHandler.approve_all,
        ) as session:
            await session.rpc.plugins.reload()

            plugins = await session.rpc.plugins.list()
            assert plugins.plugins is not None
            assert all((plugin.name or "").strip() for plugin in plugins.plugins)
