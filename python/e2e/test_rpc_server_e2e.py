"""
E2E coverage for top-level (server-scoped) RPC methods.

Mirrors ``dotnet/test/RpcServerTests.cs`` (snapshot category ``rpc_server``).
"""

from __future__ import annotations

import os
import uuid
from pathlib import Path

import pytest

from copilot import CopilotClient
from copilot.client import SubprocessConfig
from copilot.generated.rpc import (
    AccountGetQuotaRequest,
    MCPDiscoverRequest,
    PingRequest,
    SkillsConfigSetDisabledSkillsRequest,
    SkillsDiscoverRequest,
    ToolsListRequest,
)

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
    ctx.client._config.env["COPILOT_DEBUG_GITHUB_API_URL"] = ctx.proxy_url
    return ctx


def _make_authed_client(ctx: E2ETestContext, token: str) -> CopilotClient:
    env = ctx.get_env()
    env["COPILOT_DEBUG_GITHUB_API_URL"] = ctx.proxy_url
    return CopilotClient(
        SubprocessConfig(
            cli_path=ctx.cli_path,
            cwd=ctx.work_dir,
            env=env,
            github_token=token,
        )
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
        assert result.timestamp >= 0

    async def test_should_call_rpc_models_list_with_typed_result(self, authed_ctx: E2ETestContext):
        token = "rpc-models-token"
        await _configure_user(authed_ctx, token)
        client = _make_authed_client(authed_ctx, token)
        try:
            await client.start()
            result = await client.rpc.models.list()
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
