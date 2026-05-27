"""
E2E coverage for ``mode="empty"`` + ``ToolSet`` patterns.

The runtime is mode-agnostic — these tests verify the SDK's translation
reaches the runtime correctly by inspecting the resulting CapiProxy
chat-completion request (the LLM only sees tools the runtime exposed
for the session) and end-to-end behavior.

Mirrors ``nodejs/test/e2e/mode_empty.e2e.test.ts`` and shares the same
recorded cassettes under ``test/snapshots/mode_empty/``.
"""

from __future__ import annotations

import asyncio
import contextlib
import os
import sys
from collections.abc import AsyncIterator

import pytest
import pytest_asyncio

from copilot import BuiltInTools, CopilotClient, RuntimeConnection, ToolSet
from copilot.session import PermissionHandler

from .testharness import E2ETestContext


pytestmark = pytest.mark.asyncio(loop_scope="module")


@pytest_asyncio.fixture(scope="module", loop_scope="module")
async def ctx(request) -> AsyncIterator[E2ETestContext]:
    """Module-scoped harness; we build a per-test empty-mode client below."""
    context = E2ETestContext()
    await context.setup()
    yield context
    any_failed = request.session.stash.get("any_test_failed", False)
    await context.teardown(test_failed=any_failed)


@contextlib.asynccontextmanager
async def empty_mode_client(ctx: E2ETestContext) -> AsyncIterator[CopilotClient]:
    """Construct a Copilot client wired to the harness proxy in ``mode="empty"``."""
    client = CopilotClient(
        connection=RuntimeConnection.for_stdio(
            path=ctx.cli_path,
            args=(),
        ),
        working_directory=ctx.work_dir,
        env=ctx.get_env(),
        github_token="fake-token-for-e2e-tests",
        base_directory=ctx.home_dir,
        mode="empty",
    )
    try:
        yield client
    finally:
        with contextlib.suppress(Exception):
            await client.stop()


async def _tools_exposed_to_llm(ctx: E2ETestContext) -> list[str]:
    exchanges = await ctx.wait_for_exchanges(minimum_count=1, timeout=10.0)
    tools = exchanges[-1].get("request", {}).get("tools", []) or []
    return [
        t.get("function", {}).get("name")
        for t in tools
        if t.get("type") == "function" and t.get("function", {}).get("name")
    ]


async def _system_message_to_llm(ctx: E2ETestContext) -> str:
    exchanges = await ctx.wait_for_exchanges(minimum_count=1, timeout=10.0)
    messages = exchanges[-1].get("request", {}).get("messages", []) or []
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
    async def test_empty_mode_isolated_set_shell_tool_is_not_exposed(
        self, ctx: E2ETestContext
    ):
        async with empty_mode_client(ctx) as client:
            session = await client.create_session(
                on_permission_request=PermissionHandler.approve_all,
                available_tools=ToolSet().add_built_in(BuiltInTools.ISOLATED),
            )
            try:
                with contextlib.suppress(Exception):
                    await session.send(prompt="Say hi.")
                    # Give the agent a moment to issue the chat completion.
                    await asyncio.sleep(0.1)

                tool_names = await _tools_exposed_to_llm(ctx)
                for banned in ("bash", "powershell", "edit", "grep", "web_fetch"):
                    assert banned not in tool_names, (
                        f"isolated set must not expose {banned!r}, got {tool_names}"
                    )
                assert any(name in tool_names for name in BuiltInTools.ISOLATED), (
                    f"expected at least one isolated tool to be registered, got {tool_names}"
                )
            finally:
                await session.disconnect()

    async def test_empty_mode_builtin_star_exposes_all_built_in_tools(
        self, ctx: E2ETestContext
    ):
        async with empty_mode_client(ctx) as client:
            session = await client.create_session(
                on_permission_request=PermissionHandler.approve_all,
                available_tools=ToolSet().add_built_in("*"),
            )
            try:
                with contextlib.suppress(Exception):
                    await session.send(prompt="Say hi.")
                    await asyncio.sleep(0.1)

                tool_names = await _tools_exposed_to_llm(ctx)
                assert _shell_tool_name() in tool_names, (
                    f"builtin:* should expose the shell tool, got {tool_names}"
                )
            finally:
                await session.disconnect()

    async def test_empty_mode_excluded_tools_subtracts_from_available_tools(
        self, ctx: E2ETestContext
    ):
        shell = _shell_tool_name()
        async with empty_mode_client(ctx) as client:
            session = await client.create_session(
                on_permission_request=PermissionHandler.approve_all,
                available_tools=ToolSet().add_built_in("*"),
                excluded_tools=[f"builtin:{shell}"],
            )
            try:
                with contextlib.suppress(Exception):
                    await session.send(prompt="Say hi.")
                    await asyncio.sleep(0.1)

                tool_names = await _tools_exposed_to_llm(ctx)
                assert shell not in tool_names, (
                    f"excluded shell must not be exposed, got {tool_names}"
                )
                assert len(tool_names) > 0
            finally:
                await session.disconnect()

    async def test_empty_mode_strips_environment_context_from_the_system_message_by_default(
        self, ctx: E2ETestContext
    ):
        async with empty_mode_client(ctx) as client:
            session = await client.create_session(
                on_permission_request=PermissionHandler.approve_all,
                available_tools=ToolSet().add_built_in(BuiltInTools.ISOLATED),
                system_message={
                    "mode": "customize",
                    "content": (
                        "If the user asks you to name an element, reply with exactly "
                        "the single word ARGON in all caps and nothing else."
                    ),
                },
            )
            try:
                reply = await session.send_and_wait(prompt="Name an element.")
                assert reply is not None
                assert "ARGON" in reply.data.content

                system_message = await _system_message_to_llm(ctx)
                assert "Current working directory:" not in system_message
                assert "Operating System:" not in system_message
            finally:
                await session.disconnect()

    async def test_empty_mode_system_message_replace_llm_follows_caller_content_verbatim(
        self, ctx: E2ETestContext
    ):
        async with empty_mode_client(ctx) as client:
            session = await client.create_session(
                on_permission_request=PermissionHandler.approve_all,
                available_tools=ToolSet().add_built_in(BuiltInTools.ISOLATED),
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
                reply = await session.send_and_wait(prompt="Hello.")
                assert reply is not None
                assert "KRYPTON" in reply.data.content
            finally:
                await session.disconnect()

    async def test_empty_mode_append_caller_instruction_takes_effect_and_env_context_stripped(
        self, ctx: E2ETestContext
    ):
        async with empty_mode_client(ctx) as client:
            session = await client.create_session(
                on_permission_request=PermissionHandler.approve_all,
                available_tools=ToolSet().add_built_in(BuiltInTools.ISOLATED),
                system_message={
                    "mode": "append",
                    "content": (
                        "If the user asks you to name a noble gas, reply with exactly "
                        "the single word XENON in all caps and nothing else."
                    ),
                },
            )
            try:
                reply = await session.send_and_wait(prompt="Name a noble gas.")
                assert reply is not None
                assert "XENON" in reply.data.content

                system_message = await _system_message_to_llm(ctx)
                assert "Current working directory:" not in system_message
                assert "Operating System:" not in system_message
            finally:
                await session.disconnect()
