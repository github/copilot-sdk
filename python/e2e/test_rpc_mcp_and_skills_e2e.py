"""
E2E coverage for session-scoped MCP, skills, plugins, and extensions RPCs.

Mirrors ``dotnet/test/RpcMcpAndSkillsTests.cs`` (snapshot category
``rpc_mcp_and_skills``).
"""

from __future__ import annotations

import asyncio
import os
import time
import uuid
from pathlib import Path

import pytest
import pytest_asyncio

from copilot.generated.rpc import (
    ExtensionsDisableRequest,
    ExtensionsEnableRequest,
    MCPAppsCallToolRequest,
    MCPAppsDiagnoseRequest,
    MCPAppsDisplayMode,
    MCPAppsHostContextDetailsPlatform,
    MCPAppsListToolsRequest,
    MCPAppsReadResourceRequest,
    MCPAppsSetHostContextDetails,
    MCPAppsSetHostContextRequest,
    MCPCancelSamplingExecutionParams,
    MCPDisableRequest,
    MCPEnableRequest,
    MCPExecuteSamplingParams,
    MCPRemoveGitHubResult,
    MCPSamplingExecutionAction,
    McpServerStatus,
    MCPSetEnvValueModeDetails,
    MCPSetEnvValueModeParams,
    SkillsDisableRequest,
    SkillsEnableRequest,
    Theme,
)
from copilot.session import PermissionHandler

from .testharness import E2ETestContext

pytestmark = pytest.mark.asyncio(loop_scope="module")

TEST_MCP_SERVER = str(
    (Path(__file__).parents[2] / "test" / "harness" / "test-mcp-server.mjs").resolve()
)
TEST_HARNESS_DIR = str((Path(__file__).parents[2] / "test" / "harness").resolve())


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


def _test_mcp_servers(*server_names: str) -> dict:
    return {
        server_name: {
            "command": "node",
            "args": [TEST_MCP_SERVER],
            "tools": ["*"],
            "working_directory": TEST_HARNESS_DIR,
        }
        for server_name in server_names
    }


async def _wait_for_mcp_server_status(
    session, server_name: str, expected_status: McpServerStatus = McpServerStatus.CONNECTED
) -> None:
    deadline = time.monotonic() + 60
    last_status = "<not listed>"

    while time.monotonic() < deadline:
        result = await session.rpc.mcp.list()
        server = next((s for s in result.servers if s.name == server_name), None)
        if server is not None and server.status == expected_status:
            return
        last_status = server.status if server is not None else "<not listed>"
        await asyncio.sleep(0.2)

    raise AssertionError(
        f"{server_name} did not reach {expected_status.value}; last status was {last_status}"
    )


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


async def _assert_implemented_failure(awaitable, method: str) -> None:
    with pytest.raises(Exception) as excinfo:
        _ = await awaitable
    assert f"unhandled method {method}".lower() not in str(excinfo.value).lower()


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

    async def test_should_ensure_skills_loaded_and_report_no_invoked_skills_for_fresh_session(
        self, ctx: E2ETestContext
    ):
        skill_name = f"ensure-rpc-skill-{uuid.uuid4().hex}"
        skills_dir = _create_skill_directory(
            ctx.work_dir, skill_name, "Skill loaded explicitly by RPC."
        )

        session = await ctx.client.create_session(
            on_permission_request=PermissionHandler.approve_all,
            skill_directories=[skills_dir],
        )
        try:
            await session.rpc.skills.ensure_loaded()
            listed = await session.rpc.skills.list()
            _assert_skill(listed.skills, skill_name, enabled=True)

            invoked = await session.rpc.skills.get_invoked()
            assert invoked.skills == []
        finally:
            await session.disconnect()

    async def test_should_list_mcp_servers_with_configured_server(self, ctx: E2ETestContext):
        server_name = "rpc-list-mcp-server"
        session = await ctx.client.create_session(
            on_permission_request=PermissionHandler.approve_all,
            mcp_servers=_test_mcp_servers(server_name),
        )
        try:
            await _wait_for_mcp_server_status(session, server_name)
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

    async def test_should_set_mcp_env_mode_remove_github_and_cancel_missing_sampling(
        self, ctx: E2ETestContext
    ):
        session = await ctx.client.create_session(
            on_permission_request=PermissionHandler.approve_all,
        )
        try:
            indirect = await session.rpc.mcp.set_env_value_mode(
                MCPSetEnvValueModeParams(mode=MCPSetEnvValueModeDetails.INDIRECT)
            )
            assert indirect.mode == MCPSetEnvValueModeDetails.INDIRECT

            direct = await session.rpc.mcp.set_env_value_mode(
                MCPSetEnvValueModeParams(mode=MCPSetEnvValueModeDetails.DIRECT)
            )
            assert direct.mode == MCPSetEnvValueModeDetails.DIRECT

            removed = await session.rpc.mcp.remove_git_hub()
            assert isinstance(removed, MCPRemoveGitHubResult)
            assert removed.removed in (True, False)

            cancelled = await session.rpc.mcp.cancel_sampling_execution(
                MCPCancelSamplingExecutionParams(request_id="missing-sampling-request")
            )
            assert cancelled.cancelled is False
        finally:
            await session.disconnect()

    async def test_should_report_failure_or_implemented_error_for_missing_mcp_sampling(
        self, ctx: E2ETestContext
    ):
        session = await ctx.client.create_session(
            on_permission_request=PermissionHandler.approve_all,
        )
        try:
            try:
                result = await session.rpc.mcp.execute_sampling(
                    MCPExecuteSamplingParams(
                        mcp_request_id="mcp-sampling-e2e",
                        request={
                            "messages": [
                                {
                                    "role": "user",
                                    "content": {"type": "text", "text": "hello"},
                                }
                            ],
                            "maxTokens": 16,
                        },
                        request_id=f"sampling-{uuid.uuid4().hex}",
                        server_name="missing-server",
                    )
                )
            except Exception as exc:
                assert "unhandled method session.mcp.executesampling" not in str(exc).lower()
            else:
                assert result.action == MCPSamplingExecutionAction.FAILURE
                assert result.error
        finally:
            await session.disconnect()

    async def test_should_round_trip_mcp_apps_host_context_and_diagnose_shape(
        self, ctx: E2ETestContext
    ):
        session = await ctx.client.create_session(
            on_permission_request=PermissionHandler.approve_all,
        )
        try:
            await session.rpc.mcp.apps.set_host_context(
                MCPAppsSetHostContextRequest(
                    context=MCPAppsSetHostContextDetails(
                        available_display_modes=[
                            MCPAppsDisplayMode.INLINE,
                            MCPAppsDisplayMode.FULLSCREEN,
                        ],
                        display_mode=MCPAppsDisplayMode.INLINE,
                        locale="en-US",
                        platform=MCPAppsHostContextDetailsPlatform.DESKTOP,
                        theme=Theme.DARK,
                        time_zone="Etc/UTC",
                        user_agent="python-sdk-e2e",
                    )
                )
            )

            host_context = await session.rpc.mcp.apps.get_host_context()
            assert host_context.context.display_mode == MCPAppsDisplayMode.INLINE
            assert host_context.context.locale == "en-US"
            assert host_context.context.platform == MCPAppsHostContextDetailsPlatform.DESKTOP
            assert host_context.context.theme == Theme.DARK
            assert host_context.context.time_zone == "Etc/UTC"
            assert host_context.context.user_agent == "python-sdk-e2e"
            assert MCPAppsDisplayMode.FULLSCREEN in (
                host_context.context.available_display_modes or []
            )

            diagnose = await session.rpc.mcp.apps.diagnose(
                MCPAppsDiagnoseRequest(server_name="missing-mcp-app-server")
            )
            assert diagnose.capability.advertised in (True, False)
            assert diagnose.capability.feature_flag_enabled in (True, False)
            assert diagnose.capability.session_has_mcp_apps in (True, False)
            assert diagnose.server.connected is False
            assert diagnose.server.tool_count >= 0
            assert diagnose.server.tools_with_ui_meta >= 0
            assert diagnose.server.sample_tool_names is not None
        finally:
            await session.disconnect()

    async def test_should_report_implemented_errors_for_mcp_apps_without_capability(
        self, ctx: E2ETestContext
    ):
        session = await ctx.client.create_session(
            on_permission_request=PermissionHandler.approve_all,
        )
        try:
            await _assert_implemented_failure(
                session.rpc.mcp.apps.list_tools(
                    MCPAppsListToolsRequest(
                        origin_server_name="missing-server",
                        server_name="missing-server",
                    )
                ),
                "session.mcp.apps.listTools",
            )
            await _assert_implemented_failure(
                session.rpc.mcp.apps.call_tool(
                    MCPAppsCallToolRequest(
                        origin_server_name="missing-server",
                        server_name="missing-server",
                        tool_name="missing-tool",
                        arguments={},
                    )
                ),
                "session.mcp.apps.callTool",
            )
            await _assert_implemented_failure(
                session.rpc.mcp.apps.read_resource(
                    MCPAppsReadResourceRequest(
                        server_name="missing-server",
                        uri="ui://missing/resource.html",
                    )
                ),
                "session.mcp.apps.readResource",
            )
        finally:
            await session.disconnect()
