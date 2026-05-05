"""
E2E coverage for session-scoped MCP, skills, plugins, and extensions RPCs.

Mirrors ``dotnet/test/RpcMcpAndSkillsTests.cs`` (snapshot category
``rpc_mcp_and_skills``).
"""

from __future__ import annotations

import os
import uuid
from pathlib import Path

import pytest
import pytest_asyncio

from copilot.generated.rpc import (
    ExtensionsDisableRequest,
    ExtensionsEnableRequest,
    MCPDisableRequest,
    MCPEnableRequest,
    SkillsDisableRequest,
    SkillsEnableRequest,
)
from copilot.session import PermissionHandler

from .testharness import E2ETestContext

pytestmark = pytest.mark.asyncio(loop_scope="module")


# --yolo auto-approves extension permission gates at the CLI level,
# preventing breakage from new gates (e.g., extension-permission-access).
@pytest_asyncio.fixture(scope="module", loop_scope="module")
async def ctx(request):
    """Module-scoped context with --yolo for extension test hardening."""
    context = E2ETestContext()
    await context.setup(cli_args=["--yolo"])
    yield context
    any_failed = request.session.stash.get("any_test_failed", False)
    await context.teardown(test_failed=any_failed)


def _create_skill(skills_dir: Path, skill_name: str, description: str) -> None:
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


def _create_skill_directory(work_dir: str, skill_name: str, description: str) -> str:
    skills_dir = Path(work_dir) / "session-rpc-skills" / uuid.uuid4().hex
    skills_dir.mkdir(parents=True, exist_ok=True)
    _create_skill(skills_dir, skill_name, description)
    return str(skills_dir)


def _assert_skill(skills, skill_name: str, *, enabled: bool):
    matching = [s for s in skills if s.name == skill_name]
    assert len(matching) == 1, f"Expected exactly one skill named {skill_name!r}"
    skill = matching[0]
    assert skill.enabled is enabled
    assert skill.path is not None
    assert skill.path.endswith(os.path.join(skill_name, "SKILL.md"))
    return skill


async def _assert_failure(awaitable, expected: str) -> None:
    with pytest.raises(Exception) as excinfo:
        _ = await awaitable
    assert expected.lower() in str(excinfo.value).lower()


class TestRpcMcpAndSkills:
    async def test_should_list_and_toggle_session_skills(self, ctx: E2ETestContext):
        skill_name = f"session-rpc-skill-{uuid.uuid4().hex}"
        skills_dir = _create_skill_directory(
            ctx.work_dir, skill_name, "Session skill controlled by RPC."
        )

        session = await ctx.client.create_session(
            on_permission_request=PermissionHandler.approve_all,
            skill_directories=[skills_dir],
            disabled_skills=[skill_name],
        )
        try:
            disabled = await session.rpc.skills.list()
            _assert_skill(disabled.skills, skill_name, enabled=False)

            await session.rpc.skills.enable(SkillsEnableRequest(name=skill_name))
            enabled = await session.rpc.skills.list()
            _assert_skill(enabled.skills, skill_name, enabled=True)

            await session.rpc.skills.disable(SkillsDisableRequest(name=skill_name))
            disabled_again = await session.rpc.skills.list()
            _assert_skill(disabled_again.skills, skill_name, enabled=False)
        finally:
            await session.disconnect()

    async def test_should_reload_session_skills(self, ctx: E2ETestContext):
        skills_dir = Path(ctx.work_dir) / "reloadable-rpc-skills" / uuid.uuid4().hex
        skills_dir.mkdir(parents=True, exist_ok=True)
        skill_name = f"reload-rpc-skill-{uuid.uuid4().hex}"

        session = await ctx.client.create_session(
            on_permission_request=PermissionHandler.approve_all,
            skill_directories=[str(skills_dir)],
        )
        try:
            before = await session.rpc.skills.list()
            assert all(s.name != skill_name for s in before.skills)

            _create_skill(skills_dir, skill_name, "Skill added after session creation.")
            await session.rpc.skills.reload()

            after = await session.rpc.skills.list()
            reloaded = _assert_skill(after.skills, skill_name, enabled=True)
            assert reloaded.description == "Skill added after session creation."
        finally:
            await session.disconnect()

    async def test_should_list_mcp_servers_with_configured_server(self, ctx: E2ETestContext):
        server_name = "rpc-list-mcp-server"
        session = await ctx.client.create_session(
            on_permission_request=PermissionHandler.approve_all,
            mcp_servers={
                server_name: {
                    "command": "echo",
                    "args": ["rpc-list-mcp-server"],
                    "tools": ["*"],
                }
            },
        )
        try:
            result = await session.rpc.mcp.list()
            matching = [s for s in result.servers if s.name == server_name]
            assert len(matching) == 1
            assert matching[0].status is not None
        finally:
            await session.disconnect()

    async def test_should_list_plugins(self, ctx: E2ETestContext):
        session = await ctx.client.create_session(
            on_permission_request=PermissionHandler.approve_all,
        )
        try:
            result = await session.rpc.plugins.list()
            assert result.plugins is not None
            assert all((p.name or "").strip() for p in result.plugins)
        finally:
            await session.disconnect()

    async def test_should_list_extensions(self, ctx: E2ETestContext):
        session = await ctx.client.create_session(
            on_permission_request=PermissionHandler.approve_all,
        )
        try:
            result = await session.rpc.extensions.list()
            assert result.extensions is not None
            for extension in result.extensions:
                assert (extension.id or "").strip()
                assert (extension.name or "").strip()
        finally:
            await session.disconnect()

    async def test_should_report_error_when_mcp_host_is_not_initialized(self, ctx: E2ETestContext):
        session = await ctx.client.create_session(
            on_permission_request=PermissionHandler.approve_all,
        )
        try:
            await _assert_failure(
                session.rpc.mcp.enable(MCPEnableRequest(server_name="missing-server")),
                "No MCP host initialized",
            )
            await _assert_failure(
                session.rpc.mcp.disable(MCPDisableRequest(server_name="missing-server")),
                "No MCP host initialized",
            )
            await _assert_failure(
                session.rpc.mcp.reload(),
                "MCP config reload not available",
            )
        finally:
            await session.disconnect()

    async def test_should_report_error_when_extensions_are_not_available(self, ctx: E2ETestContext):
        session = await ctx.client.create_session(
            on_permission_request=PermissionHandler.approve_all,
        )
        try:
            await _assert_failure(
                session.rpc.extensions.enable(ExtensionsEnableRequest(id="missing-extension")),
                "Extensions not available",
            )
            await _assert_failure(
                session.rpc.extensions.disable(ExtensionsDisableRequest(id="missing-extension")),
                "Extensions not available",
            )
            await _assert_failure(
                session.rpc.extensions.reload(),
                "Extensions not available",
            )
        finally:
            await session.disconnect()
