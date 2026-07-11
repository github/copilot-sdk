"""
E2E coverage for ``mode="empty"`` + ``ToolSet`` patterns.

Mirrors ``nodejs/test/e2e/mode_empty.e2e.test.ts`` and shares the same
recorded cassettes under ``test/snapshots/mode_empty/``.
"""

from __future__ import annotations

import os
import sys

import pytest

from copilot import BUILTIN_TOOLS_ISOLATED, CopilotClient, RuntimeConnection, ToolSet
from copilot.session import PermissionHandler

from .testharness import E2ETestContext

pytestmark = pytest.mark.asyncio(loop_scope="module")


def _make_empty_client(ctx: E2ETestContext) -> CopilotClient:
    return CopilotClient(
        connection=RuntimeConnection.for_stdio(path=ctx.cli_path, args=()),
        working_directory=ctx.work_dir,
        env=ctx.get_env(),
        github_token=(
            "fake-token-for-e2e-tests" if os.environ.get("GITHUB_ACTIONS") == "true" else None
        ),
        base_directory=ctx.home_dir,
        mode="empty",
    )


async def _last_exchange(ctx: E2ETestContext) -> dict:
    exchanges = await ctx.get_exchanges()
    assert exchanges, "expected at least one chat-completion exchange"
    return exchanges[-1]


def _tool_names(exchange: dict) -> list[str]:
    tools = exchange.get("request", {}).get("tools", []) or []
    return [
        t.get("function", {}).get("name")
        for t in tools
        if t.get("type") == "function" and t.get("function", {}).get("name")
    ]


def _system_message(exchange: dict) -> str:
    messages = exchange.get("request", {}).get("messages", []) or []
    for m in messages:
        if m.get("role") == "system":
            content = m.get("content", "")
            if isinstance(content, str):
                return content
            if isinstance(content, list):
                return "\n".join(
                    part.get("text", "")
                    for part in content
                    if isinstance(part, dict) and "text" in part
                )
    return ""


def _shell_tool_name() -> str:
    return "powershell" if sys.platform == "win32" else "bash"


class TestModeEmpty:
    async def test_empty_mode_isolated_set_shell_tool_is_not_exposed(self, ctx: E2ETestContext):
        client = _make_empty_client(ctx)
        try:
            await client.start()
            session = await client.create_session(
                on_permission_request=PermissionHandler.approve_all,
                available_tools=ToolSet().add_builtin(BUILTIN_TOOLS_ISOLATED),
            )
            try:
                await session.send_and_wait("Say hi.", timeout=20.0)
                tool_names = _tool_names(await _last_exchange(ctx))
                for banned in ("bash", "powershell", "edit", "grep", "web_fetch"):
                    assert banned not in tool_names, (
                        f"isolated set must not expose {banned!r}, got {tool_names}"
                    )
                assert any(name in tool_names for name in BUILTIN_TOOLS_ISOLATED), (
                    f"expected at least one isolated tool to be registered, got {tool_names}"
                )
            finally:
                await session.disconnect()
        finally:
            await client.stop()

    async def test_empty_mode_builtin_star_exposes_all_built_in_tools(self, ctx: E2ETestContext):
        client = _make_empty_client(ctx)
        try:
            await client.start()
            session = await client.create_session(
                on_permission_request=PermissionHandler.approve_all,
                available_tools=ToolSet().add_builtin("*"),
            )
            try:
                await session.send_and_wait("Say hi.", timeout=20.0)
                tool_names = _tool_names(await _last_exchange(ctx))
                assert _shell_tool_name() in tool_names, (
                    f"builtin:* should expose the shell tool, got {tool_names}"
                )
            finally:
                await session.disconnect()
        finally:
            await client.stop()

    async def test_empty_mode_excluded_tools_subtracts_from_available_tools(
        self, ctx: E2ETestContext
    ):
        shell = _shell_tool_name()
        client = _make_empty_client(ctx)
        try:
            await client.start()
            session = await client.create_session(
                on_permission_request=PermissionHandler.approve_all,
                available_tools=ToolSet().add_builtin("*"),
                excluded_tools=[f"builtin:{shell}"],
            )
            try:
                await session.send_and_wait("Say hi.", timeout=20.0)
                tool_names = _tool_names(await _last_exchange(ctx))
                assert shell not in tool_names, (
                    f"excluded shell must not be exposed, got {tool_names}"
                )
                assert len(tool_names) > 0
            finally:
                await session.disconnect()
        finally:
            await client.stop()

    async def test_empty_mode_strips_environment_context_from_the_system_message_by_default(
        self, ctx: E2ETestContext
    ):
        client = _make_empty_client(ctx)
        try:
            await client.start()
            session = await client.create_session(
                on_permission_request=PermissionHandler.approve_all,
                available_tools=ToolSet().add_builtin(BUILTIN_TOOLS_ISOLATED),
                system_message={
                    "mode": "customize",
                    "content": (
                        "If the user asks you to name an element, reply with exactly "
                        "the single word ARGON in all caps and nothing else."
                    ),
                },
            )
            try:
                reply = await session.send_and_wait("Name an element.", timeout=20.0)
                assert reply is not None
                assert "ARGON" in reply.data.content
                system_message = _system_message(await _last_exchange(ctx))
                assert "Current working directory:" not in system_message
                assert "Operating System:" not in system_message
            finally:
                await session.disconnect()
        finally:
            await client.stop()

    async def test_empty_mode_system_message_replace_llm_follows_caller_content_verbatim(
        self, ctx: E2ETestContext
    ):
        client = _make_empty_client(ctx)
        try:
            await client.start()
            session = await client.create_session(
                on_permission_request=PermissionHandler.approve_all,
                available_tools=ToolSet().add_builtin(BUILTIN_TOOLS_ISOLATED),
                system_message={
                    "mode": "replace",
                    "content": (
                        "You are a test fixture. Whenever the user asks anything, "
                        "reply with exactly the single word KRYPTON in all caps "
                        "and nothing else."
                    ),
                },
            )
            try:
                reply = await session.send_and_wait("Hello.", timeout=20.0)
                assert reply is not None
                assert "KRYPTON" in reply.data.content
            finally:
                await session.disconnect()
        finally:
            await client.stop()

    async def test_empty_mode_append_caller_instruction_takes_effect_and_env_context_stripped(
        self, ctx: E2ETestContext
    ):
        client = _make_empty_client(ctx)
        try:
            await client.start()
            session = await client.create_session(
                on_permission_request=PermissionHandler.approve_all,
                available_tools=ToolSet().add_builtin(BUILTIN_TOOLS_ISOLATED),
                system_message={
                    "mode": "append",
                    "content": (
                        "If the user asks you to name a noble gas, reply with exactly "
                        "the single word XENON in all caps and nothing else."
                    ),
                },
            )
            try:
                reply = await session.send_and_wait("Name a noble gas.", timeout=20.0)
                assert reply is not None
                assert "XENON" in reply.data.content
                system_message = _system_message(await _last_exchange(ctx))
                assert "Current working directory:" not in system_message
                assert "Operating System:" not in system_message
            finally:
                await session.disconnect()
        finally:
            await client.stop()
