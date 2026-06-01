"""
E2E coverage for top-level (server-scoped) RPC methods.

Mirrors ``dotnet/test/RpcServerTests.cs`` (snapshot category ``rpc_server``).
"""

from __future__ import annotations

import os
import uuid
from datetime import UTC, datetime
from pathlib import Path

import pytest

from copilot import CopilotClient, RuntimeConnection
from copilot.rpc import (
    AccountGetQuotaRequest,
    ConnectRemoteSessionParams,
    MCPDiscoverRequest,
    ModelsListRequest,
    PingRequest,
    SecretsAddFilterValuesRequest,
    SessionContext,
    SessionFSSetProviderCapabilities,
    SessionFSSetProviderConventions,
    SessionFSSetProviderRequest,
    SessionListFilter,
    SessionMetadata,
    SessionsBulkDeleteRequest,
    SessionsCheckInUseRequest,
    SessionsCloseRequest,
    SessionsEnrichMetadataRequest,
    SessionsFindByPrefixRequest,
    SessionsFindByTaskIDRequest,
    SessionsGetEventFilePathRequest,
    SessionsGetLastForContextRequest,
    SessionsGetPersistedRemoteSteerableRequest,
    SessionsListRequest,
    SessionsLoadDeferredRepoHooksRequest,
    SessionsPruneOldRequest,
    SessionsReleaseLockRequest,
    SessionsReloadPluginHooksRequest,
    SessionsSaveRequest,
    SessionsSetAdditionalPluginsRequest,
    SkillsConfigSetDisabledSkillsRequest,
    SkillsDiscoverRequest,
    ToolsListRequest,
)
from copilot.session import PermissionHandler

from .testharness import E2ETestContext

pytestmark = pytest.mark.asyncio(loop_scope="module")


def _create_skill_directory(work_dir: str, skill_name: str, description: str) -> str:
    skills_dir = Path(work_dir) / "server-rpc-skills" / uuid.uuid4().hex
    skill_subdir = skills_dir / skill_name
    skill_subdir.mkdir(parents=True, exist_ok=True)
    skill_md = (
        f"---\n"
        f"name: {skill_name}\n"
        f"description: {description}\n"
        f"---\n\n"
        f"# {skill_name}\n\n"
        f"This skill is used by RPC E2E tests.\n"
    )
    (skill_subdir / "SKILL.md").write_text(skill_md, encoding="utf-8", newline="\n")
    return str(skills_dir)


@pytest.fixture(scope="module")
async def authed_ctx(ctx: E2ETestContext):
    """Configure proxy to redirect GitHub user lookups so per-token auth works."""
    ctx.client._options.env["COPILOT_DEBUG_GITHUB_API_URL"] = ctx.proxy_url
    return ctx


def _make_authed_client(ctx: E2ETestContext, token: str) -> CopilotClient:
    env = ctx.get_env()
    env["COPILOT_DEBUG_GITHUB_API_URL"] = ctx.proxy_url
    return CopilotClient(
        connection=RuntimeConnection.for_stdio(path=ctx.cli_path),
        working_directory=ctx.work_dir,
        env=env,
        github_token=token,
    )


def _make_client_with_env(ctx: E2ETestContext, env_overrides: dict[str, str]) -> CopilotClient:
    env = ctx.get_env()
    env.update(env_overrides)
    return CopilotClient(
        connection=RuntimeConnection.for_stdio(path=ctx.cli_path),
        working_directory=ctx.work_dir,
        env=env,
        github_token="fake-token-for-e2e-tests",
    )


async def _configure_user(
    ctx: E2ETestContext,
    token: str,
    quota_snapshots: dict | None = None,
):
    payload: dict = {
        "login": "rpc-user",
        "copilot_plan": "individual_pro",
        "endpoints": {
            "api": ctx.proxy_url,
            "telemetry": "https://localhost:1/telemetry",
        },
        "analytics_tracking_id": "rpc-user-tracking-id",
    }
    if quota_snapshots is not None:
        payload["quota_snapshots"] = quota_snapshots
    await ctx.set_copilot_user_by_token(token, payload)


class TestRpcServer:
    async def test_should_call_rpc_ping_with_typed_params_and_result(self, ctx: E2ETestContext):
        await ctx.client.start()
        result = await ctx.client.rpc.ping(PingRequest(message="typed rpc test"))
        assert result.message == "pong: typed rpc test"
        assert result.timestamp is not None

    async def test_should_call_rpc_models_list_with_typed_result(self, authed_ctx: E2ETestContext):
        token = "rpc-models-token"
        await _configure_user(authed_ctx, token)
        client = _make_authed_client(authed_ctx, token)
        try:
            await client.start()
            result = await client.rpc.models.list(ModelsListRequest())
            assert result.models is not None
            assert any(model.id == "claude-sonnet-4.5" for model in result.models)
            assert all((model.name or "").strip() for model in result.models)
        finally:
            try:
                await client.stop()
            except ExceptionGroup:
                # Intentional: shutting down the per-test client can race the
                # CLI's own teardown and surface as an aggregated cancellation
                # error from anyio. We don't want it to fail the test.
                pass

    async def test_should_call_rpc_account_get_quota_when_authenticated(
        self, authed_ctx: E2ETestContext
    ):
        token = "rpc-quota-token"
        await _configure_user(
            authed_ctx,
            token,
            quota_snapshots={
                "chat": {
                    "entitlement": 100,
                    "overage_count": 2,
                    "overage_permitted": True,
                    "percent_remaining": 75,
                    "timestamp_utc": "2026-04-30T00:00:00Z",
                }
            },
        )
        client = _make_authed_client(authed_ctx, token)
        try:
            await client.start()
            result = await client.rpc.account.get_quota(AccountGetQuotaRequest(git_hub_token=token))
            assert "chat" in result.quota_snapshots
            chat_quota = result.quota_snapshots["chat"]
            assert chat_quota.entitlement_requests == 100
            assert chat_quota.used_requests == 25
            assert chat_quota.remaining_percentage == 75
            assert chat_quota.overage == 2
            assert chat_quota.usage_allowed_with_exhausted_quota is True
            assert chat_quota.overage_allowed_with_exhausted_quota is True
            assert chat_quota.reset_date == "2026-04-30T00:00:00Z"
        finally:
            try:
                await client.stop()
            except ExceptionGroup:
                # Intentional: shutting down the per-test client can race the
                # CLI's own teardown and surface as an aggregated cancellation
                # error from anyio. We don't want it to fail the test.
                pass

    async def test_should_call_rpc_tools_list_with_typed_result(self, ctx: E2ETestContext):
        await ctx.client.start()
        result = await ctx.client.rpc.tools.list(ToolsListRequest())
        assert result.tools is not None
        assert len(result.tools) > 0
        assert all((tool.name or "").strip() for tool in result.tools)

    async def test_should_call_rpc_session_fs_set_provider_with_typed_result(
        self, ctx: E2ETestContext
    ):
        client = _make_client_with_env(ctx, {})
        try:
            await client.start()
            result = await client.rpc.session_fs.set_provider(
                SessionFSSetProviderRequest(
                    initial_cwd="/",
                    session_state_path="/session-state",
                    conventions=SessionFSSetProviderConventions.POSIX,
                    capabilities=SessionFSSetProviderCapabilities(sqlite=True),
                )
            )
            assert result.success is True
        finally:
            try:
                await client.stop()
            except ExceptionGroup:
                # Intentional: shutting down the per-test client can race the
                # CLI's own teardown and surface as an aggregated cancellation
                # error from anyio. We don't want it to fail the test.
                pass

    async def test_should_add_secret_filter_values(self, ctx: E2ETestContext):
        client = _make_client_with_env(ctx, {"COPILOT_ENABLE_SECRET_FILTERING": "true"})
        try:
            await client.start()
            secret = f"rpc-secret-{uuid.uuid4().hex}"
            result = await client.rpc.secrets.add_filter_values(
                SecretsAddFilterValuesRequest(values=[secret])
            )
            assert result.ok is True
        finally:
            try:
                await client.stop()
            except ExceptionGroup:
                # Intentional: shutting down the per-test client can race the
                # CLI's own teardown and surface as an aggregated cancellation
                # error from anyio. We don't want it to fail the test.
                pass

    async def test_should_list_find_and_inspect_persisted_session_state(self, ctx: E2ETestContext):
        session_id = str(uuid.uuid4())
        working_directory = Path(ctx.work_dir) / f"server-rpc-list-{uuid.uuid4().hex}"
        working_directory.mkdir(parents=True, exist_ok=True)
        missing_task_id = f"missing-task-{uuid.uuid4().hex}"
        missing_session_id = str(uuid.uuid4())

        session = await ctx.client.create_session(
            session_id=session_id,
            working_directory=str(working_directory),
            on_permission_request=PermissionHandler.approve_all,
        )
        try:
            await session.log("SERVER_RPC_LIST_READY")
            save = await ctx.client.rpc.sessions.save(SessionsSaveRequest(session_id=session_id))
            assert save is not None

            event_path = await ctx.client.rpc.sessions.get_event_file_path(
                SessionsGetEventFilePathRequest(session_id=session_id)
            )
            assert event_path.file_path
            assert os.path.isabs(event_path.file_path)
            assert os.path.basename(event_path.file_path) == "events.jsonl"
            assert session_id.lower() in event_path.file_path.lower()

            listed = await ctx.client.rpc.sessions.list(
                SessionsListRequest(
                    filter=SessionListFilter(cwd=str(working_directory)),
                    metadata_limit=0,
                )
            )
            assert listed.sessions is not None
            assert all(
                item.context is None
                or os.path.normcase(os.path.abspath(item.context.cwd))
                == os.path.normcase(os.path.abspath(str(working_directory)))
                for item in listed.sessions
            )

            by_prefix = await ctx.client.rpc.sessions.find_by_prefix(
                SessionsFindByPrefixRequest(prefix=session_id[:8])
            )
            assert by_prefix.session_id in (None, session_id)

            by_task = await ctx.client.rpc.sessions.find_by_task_id(
                SessionsFindByTaskIDRequest(task_id=missing_task_id)
            )
            assert by_task.session_id is None

            last_for_context = await ctx.client.rpc.sessions.get_last_for_context(
                SessionsGetLastForContextRequest(context=SessionContext(cwd=str(working_directory)))
            )
            assert last_for_context.session_id in (None, session_id)

            sizes = await ctx.client.rpc.sessions.get_sizes()
            assert sizes.sizes is not None
            if session_id in sizes.sizes:
                assert sizes.sizes[session_id] >= 0

            in_use = await ctx.client.rpc.sessions.check_in_use(
                SessionsCheckInUseRequest(session_ids=[session_id, missing_session_id])
            )
            assert missing_session_id not in in_use.in_use

            remote_steerable = await ctx.client.rpc.sessions.get_persisted_remote_steerable(
                SessionsGetPersistedRemoteSteerableRequest(session_id=session_id)
            )
            assert remote_steerable.remote_steerable is None
        finally:
            await session.disconnect()

    async def test_should_enrich_basic_session_metadata(self, ctx: E2ETestContext):
        session_id = str(uuid.uuid4())
        working_directory = Path(ctx.work_dir) / f"server-rpc-enrich-{uuid.uuid4().hex}"
        working_directory.mkdir(parents=True, exist_ok=True)
        session = await ctx.client.create_session(
            session_id=session_id,
            working_directory=str(working_directory),
            on_permission_request=PermissionHandler.approve_all,
        )
        try:
            await session.log("SERVER_RPC_ENRICH_READY")
            await ctx.client.rpc.sessions.save(SessionsSaveRequest(session_id=session_id))

            now = datetime.now(UTC).isoformat()
            result = await ctx.client.rpc.sessions.enrich_metadata(
                SessionsEnrichMetadataRequest(
                    sessions=[
                        SessionMetadata(
                            is_remote=False,
                            modified_time=now,
                            session_id=session_id,
                            start_time=now,
                            name="Basic metadata",
                            context=SessionContext(cwd=str(working_directory)),
                        )
                    ]
                )
            )

            assert len(result.sessions) == 1
            enriched = result.sessions[0]
            assert enriched.session_id == session_id
            assert enriched.is_remote is False
            assert enriched.context is not None
            assert os.path.normcase(os.path.abspath(enriched.context.cwd)) == os.path.normcase(
                os.path.abspath(str(working_directory))
            )
        finally:
            await session.disconnect()

    async def test_should_close_release_prune_and_bulk_delete_persisted_session(
        self, ctx: E2ETestContext
    ):
        session_id = str(uuid.uuid4())
        missing_session_id = str(uuid.uuid4())
        working_directory = Path(ctx.work_dir) / f"server-rpc-delete-{uuid.uuid4().hex}"
        working_directory.mkdir(parents=True, exist_ok=True)

        session = await ctx.client.create_session(
            session_id=session_id,
            working_directory=str(working_directory),
            on_permission_request=PermissionHandler.approve_all,
        )
        await session.log("SERVER_RPC_DELETE_READY")
        await ctx.client.rpc.sessions.save(SessionsSaveRequest(session_id=session_id))
        await ctx.client.rpc.sessions.close(SessionsCloseRequest(session_id=session_id))
        release = await ctx.client.rpc.sessions.release_lock(
            SessionsReleaseLockRequest(session_id=session_id)
        )
        assert release is not None

        prune = await ctx.client.rpc.sessions.prune_old(
            SessionsPruneOldRequest(
                older_than_days=0,
                dry_run=True,
                include_named=True,
                exclude_session_ids=[],
            )
        )
        assert prune.dry_run is True
        assert missing_session_id not in prune.candidates
        assert session_id not in prune.deleted
        assert prune.freed_bytes >= 0

        deleted = await ctx.client.rpc.sessions.bulk_delete(
            SessionsBulkDeleteRequest(session_ids=[session_id, missing_session_id])
        )
        assert session_id in deleted.freed_bytes
        assert deleted.freed_bytes[session_id] >= 0
        if missing_session_id in deleted.freed_bytes:
            assert deleted.freed_bytes[missing_session_id] == 0

        listed = await ctx.client.rpc.sessions.list(SessionsListRequest())
        assert all(item.session_id != session_id for item in listed.sessions)

    async def test_should_report_implemented_error_when_connecting_unknown_remote_session(
        self, ctx: E2ETestContext
    ):
        await ctx.client.start()
        remote_session_id = f"remote-{uuid.uuid4().hex}"
        with pytest.raises(Exception) as excinfo:
            await ctx.client.rpc.sessions.connect(
                ConnectRemoteSessionParams(session_id=remote_session_id)
            )
        text = str(excinfo.value).lower()
        assert "unhandled method sessions.connect" not in text
        assert remote_session_id.lower() in text or "session" in text

    async def test_should_set_additional_plugins_and_reload_deferred_hooks(
        self, ctx: E2ETestContext
    ):
        await ctx.client.start()
        cleared = await ctx.client.rpc.sessions.set_additional_plugins(
            SessionsSetAdditionalPluginsRequest(plugins=[])
        )
        assert cleared is not None

        session_id = str(uuid.uuid4())
        working_directory = Path(ctx.work_dir) / f"server-rpc-hooks-{uuid.uuid4().hex}"
        working_directory.mkdir(parents=True, exist_ok=True)
        session = await ctx.client.create_session(
            session_id=session_id,
            working_directory=str(working_directory),
            on_permission_request=PermissionHandler.approve_all,
            enable_config_discovery=False,
        )
        try:
            reload_result = await ctx.client.rpc.sessions.reload_plugin_hooks(
                SessionsReloadPluginHooksRequest(session_id=session_id, defer_repo_hooks=True)
            )
            assert reload_result is not None

            loaded = await ctx.client.rpc.sessions.load_deferred_repo_hooks(
                SessionsLoadDeferredRepoHooksRequest(session_id=session_id)
            )
            assert loaded.hook_count == 0
            assert loaded.startup_prompts == []
        finally:
            await ctx.client.rpc.sessions.set_additional_plugins(
                SessionsSetAdditionalPluginsRequest(plugins=[])
            )
            await session.disconnect()

    async def test_should_discover_server_mcp_and_skills(self, ctx: E2ETestContext):
        await ctx.client.start()

        skill_name = f"server-rpc-skill-{uuid.uuid4().hex}"
        skill_directory = _create_skill_directory(
            ctx.work_dir,
            skill_name,
            "Skill discovered by server-scoped RPC tests.",
        )

        mcp = await ctx.client.rpc.mcp.discover(MCPDiscoverRequest(working_directory=ctx.work_dir))
        assert mcp.servers is not None

        skills = await ctx.client.rpc.skills.discover(
            SkillsDiscoverRequest(skill_directories=[skill_directory])
        )
        matching = [s for s in skills.skills if s.name == skill_name]
        assert len(matching) == 1
        discovered = matching[0]
        assert discovered.description == "Skill discovered by server-scoped RPC tests."
        assert discovered.enabled is True
        assert discovered.path.endswith(os.path.join(skill_name, "SKILL.md"))

        try:
            await ctx.client.rpc.skills.config.set_disabled_skills(
                SkillsConfigSetDisabledSkillsRequest(disabled_skills=[skill_name])
            )
            disabled = await ctx.client.rpc.skills.discover(
                SkillsDiscoverRequest(skill_directories=[skill_directory])
            )
            disabled_match = [s for s in disabled.skills if s.name == skill_name]
            assert len(disabled_match) == 1
            assert disabled_match[0].enabled is False
        finally:
            await ctx.client.rpc.skills.config.set_disabled_skills(
                SkillsConfigSetDisabledSkillsRequest(disabled_skills=[])
            )
