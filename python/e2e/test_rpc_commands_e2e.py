"""E2E coverage for session.commands RPC methods."""

from __future__ import annotations

import pytest

from copilot.rpc import (
    CommandsInvokeRequest,
    CommandsListRequest,
    CommandsRespondToQueuedCommandRequest,
    ExecuteCommandParams,
    QueuedCommandHandled,
    SlashCommandKind,
    SlashCommandTextResult,
)
from copilot.session import CommandContext, CommandDefinition, PermissionHandler

from .testharness import E2ETestContext

pytestmark = pytest.mark.asyncio(loop_scope="module")


class TestRpcCommands:
    async def test_should_list_builtin_and_client_commands(self, ctx: E2ETestContext):
        session = await ctx.client.create_session(
            on_permission_request=PermissionHandler.approve_all,
            commands=[
                CommandDefinition(
                    name="deploy",
                    description="Deploy the app",
                    handler=lambda _: None,
                )
            ],
        )
        try:
            commands = await session.rpc.commands.list(CommandsListRequest())
            by_name = {command.name: command for command in commands.commands}

            builtins = [
                command for command in commands.commands if command.kind == SlashCommandKind.BUILTIN
            ]
            assert builtins
            if "model" in by_name:
                assert by_name["model"].kind == SlashCommandKind.BUILTIN
            if "compact" in by_name:
                assert by_name["compact"].kind == SlashCommandKind.BUILTIN

            assert "deploy" in by_name
            assert by_name["deploy"].kind == SlashCommandKind.CLIENT
            assert by_name["deploy"].description == "Deploy the app"
        finally:
            await session.disconnect()

    async def test_should_invoke_builtin_model_command(self, ctx: E2ETestContext):
        session = await ctx.client.create_session(
            on_permission_request=PermissionHandler.approve_all,
        )
        try:
            result = await session.rpc.commands.invoke(CommandsInvokeRequest(name="model"))
            assert result is not None
            if isinstance(result, SlashCommandTextResult):
                assert result.text.strip()
            else:
                assert getattr(result, "kind", None) in {
                    "agent-prompt",
                    "completed",
                    "select-subcommand",
                    "text",
                }
        finally:
            await session.disconnect()

    async def test_should_execute_registered_command_with_arguments(self, ctx: E2ETestContext):
        calls: list[CommandContext] = []

        def deploy(context: CommandContext) -> None:
            calls.append(context)

        session = await ctx.client.create_session(
            on_permission_request=PermissionHandler.approve_all,
            commands=[
                CommandDefinition(
                    name="deploy",
                    description="Deploy the app",
                    handler=deploy,
                )
            ],
        )
        try:
            result = await session.rpc.commands.execute(
                ExecuteCommandParams(command_name="deploy", args="production")
            )
            assert result.error is None
            assert len(calls) == 1
            assert calls[0].session_id == session.session_id
            assert calls[0].command_name == "deploy"
            assert calls[0].args == "production"
            assert calls[0].command == "/deploy production"
        finally:
            await session.disconnect()

    async def test_should_return_false_for_unknown_queued_command_response(
        self, ctx: E2ETestContext
    ):
        session = await ctx.client.create_session(
            on_permission_request=PermissionHandler.approve_all,
        )
        try:
            result = await session.rpc.commands.respond_to_queued_command(
                CommandsRespondToQueuedCommandRequest(
                    request_id="missing-queued-command",
                    result=QueuedCommandHandled(stop_processing_queue=True),
                )
            )
            assert result.success is False
        finally:
            await session.disconnect()
