"""Tests for generated RPC method behavior."""

from unittest.mock import AsyncMock

import pytest

from copilot.generated.rpc import (
    CommandsApi,
    CommandsInvokeRequest,
    SlashCommandInvocationResultKind,
)


@pytest.mark.asyncio
async def test_commands_invoke_deserializes_slash_command_result():
    client = AsyncMock()
    client.request = AsyncMock(return_value={"kind": "text", "text": "hello", "markdown": True})
    api = CommandsApi(client, "sess-1")

    result = await api.invoke(CommandsInvokeRequest(name="help"))

    assert result.kind is SlashCommandInvocationResultKind.TEXT
    assert result.text == "hello"
    assert result.markdown is True
