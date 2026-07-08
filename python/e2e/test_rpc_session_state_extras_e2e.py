"""
E2E coverage for additional session-scoped RPC methods.

Mirrors ``dotnet/test/E2E/RpcSessionStateExtrasE2ETests.cs`` (snapshot
category ``rpc_session_state_extras``).
"""

from __future__ import annotations

import contextlib
import json
import time

import pytest

from copilot import CopilotClient, RuntimeConnection
from copilot.rpc import (
    CompletionsRequestRequest,
    MetadataContextHeaviestMessagesRequest,
    ModelSwitchToRequest,
    NamedProviderConfig,
    PermissionsSetAllowAllRequest,
    ProviderAddRequest,
    ProviderModelConfig,
    ProviderType,
    ProviderWireAPI,
    SessionVisibilityStatus,
    SubagentSettings,
    SubagentSettingsEntry,
    SubagentSettingsEntryContextTier,
    UpdateSubagentSettingsRequest,
    VisibilitySetRequest,
)
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

    async def test_should_add_byok_provider_and_model_at_runtime(self, ctx: E2ETestContext):
        async with await ctx.client.create_session(
            on_permission_request=PermissionHandler.approve_all,
        ) as session:
            provider_name = f"sdk-runtime-provider-{time.time_ns()}"
            model_id = "sdk-runtime-model"
            selection_id = f"{provider_name}/{model_id}"

            added = await session.rpc.provider.add(
                ProviderAddRequest(
                    providers=[
                        NamedProviderConfig(
                            name=provider_name,
                            type=ProviderType.OPENAI,
                            wire_api=ProviderWireAPI.COMPLETIONS,
                            base_url="https://api.example.test/v1",
                            api_key="runtime-provider-secret",
                            headers={"X-SDK-Provider": "runtime"},
                        )
                    ],
                    models=[
                        ProviderModelConfig(
                            provider=provider_name,
                            id=model_id,
                            name="SDK Runtime Model",
                            model_id="claude-sonnet-4.5",
                            wire_model="wire-sdk-runtime-model",
                            max_context_window_tokens=4096,
                            max_prompt_tokens=3072,
                            max_output_tokens=1024,
                        )
                    ],
                )
            )

            assert len(added.models) == 1
            assert selection_id in json.dumps(added.models[0], sort_keys=True)
            assert "SDK Runtime Model" in json.dumps(added.models[0], sort_keys=True)

            listed = await session.rpc.model.list()
            assert any(selection_id in json.dumps(model, sort_keys=True) for model in listed.list)

            switched = await session.rpc.model.switch_to(
                ModelSwitchToRequest(model_id=selection_id)
            )
            assert switched.model_id == selection_id
            assert (await session.rpc.model.get_current()).model_id == selection_id

    async def test_should_return_empty_completions_when_host_does_not_provide_them(
        self, ctx: E2ETestContext
    ):
        async with await ctx.client.create_session(
            on_permission_request=PermissionHandler.approve_all,
        ) as session:
            triggers = await session.rpc.completions.get_trigger_characters()
            assert triggers.trigger_characters == []

            completions = await session.rpc.completions.request(
                CompletionsRequestRequest(text="Use @", offset=5)
            )
            assert completions.items == []

    async def test_should_report_visibility_as_unsynced_for_local_session(
        self, ctx: E2ETestContext
    ):
        async with await ctx.client.create_session(
            on_permission_request=PermissionHandler.approve_all,
        ) as session:
            initial = await session.rpc.visibility.get()
            assert initial.synced is False
            assert initial.status is None
            assert initial.share_url is None

            updated = await session.rpc.visibility.set(
                VisibilitySetRequest(status=SessionVisibilityStatus.REPO)
            )
            assert updated.synced is False
            assert updated.status is None
            assert updated.share_url is None

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

    async def test_should_get_context_attribution_and_heaviest_messages_after_turn(
        self, ctx: E2ETestContext
    ):
        async with await ctx.client.create_session(
            on_permission_request=PermissionHandler.approve_all,
        ) as session:
            answer = await session.send_and_wait("Say CONTEXT_METADATA_OK exactly.", timeout=60.0)
            assert answer is not None
            assert "CONTEXT_METADATA_OK" in (answer.data.content or "")

            attribution = await session.rpc.metadata.get_context_attribution()
            assert attribution.context_attribution is not None
            context_attribution = attribution.context_attribution
            assert context_attribution.total_tokens > 0
            assert len(context_attribution.entries) > 0
            for entry in context_attribution.entries:
                assert entry.id.strip()
                assert entry.kind.strip()
                assert entry.label.strip()
                assert entry.tokens >= 0
                for key in entry.attributes or {}:
                    assert key.strip()

            heaviest = await session.rpc.metadata.get_context_heaviest_messages(
                MetadataContextHeaviestMessagesRequest(limit=2)
            )
            assert heaviest.total_tokens > 0
            assert len(heaviest.messages) <= 2
            for message in heaviest.messages:
                assert message.id.strip()
                assert message.tokens >= 0

    async def test_should_update_and_clear_live_subagent_settings(self, ctx: E2ETestContext):
        async with await ctx.client.create_session(
            on_permission_request=PermissionHandler.approve_all,
        ) as session:
            await session.rpc.tools.update_subagent_settings(
                UpdateSubagentSettingsRequest(
                    subagents=SubagentSettings(
                        {
                            "general-purpose": SubagentSettingsEntry(
                                model="claude-haiku-4.5",
                                effort_level="low",
                                context_tier=SubagentSettingsEntryContextTier.DEFAULT,
                            )
                        }
                    )
                )
            )

            await session.rpc.tools.update_subagent_settings(
                UpdateSubagentSettingsRequest(subagents=None)
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
