# Copyright (c) Microsoft Corporation. All rights reserved.

"""Generated prompt RPC contracts; real MCP behavior is covered by Node and .NET E2Es."""

import json
from pathlib import Path
from unittest.mock import AsyncMock

import pytest

from copilot.rpc import MCPPromptsGetRequest, MCPPromptsListRequest, SessionRpc


@pytest.fixture
def prompt_fixtures():
    path = Path(__file__).parent.parent / "test" / "harness" / "mcp-prompt-fixtures.json"
    return json.loads(path.read_text(encoding="utf-8"))


@pytest.mark.asyncio
async def test_prompt_list_dispatch_and_optional_fields(prompt_fixtures):
    client = AsyncMock()
    api = SessionRpc(client, "bound-session")
    client.request.return_value = prompt_fixtures["firstPage"]

    first = await api.mcp.prompts.list(MCPPromptsListRequest(server_name="fixture"))

    client.request.assert_awaited_once_with(
        "session.mcp.prompts.list", {"sessionId": "bound-session", "serverName": "fixture"}
    )
    assert first.to_dict() == prompt_fixtures["firstPage"]
    assert [argument.required for argument in first.prompts[0].arguments] == [True, False, None]

    client.request.reset_mock()
    client.request.return_value = prompt_fixtures["secondPage"]
    second = await api.mcp.prompts.list(
        MCPPromptsListRequest(server_name="fixture", cursor=first.next_cursor)
    )
    client.request.assert_awaited_once_with(
        "session.mcp.prompts.list",
        {"sessionId": "bound-session", "serverName": "fixture", "cursor": first.next_cursor},
    )
    assert second.to_dict() == prompt_fixtures["secondPage"]


@pytest.mark.asyncio
@pytest.mark.parametrize("arguments", [None, {}, {"topic": "日本語", "style": ""}])
async def test_prompt_get_dispatch_and_opaque_json(prompt_fixtures, arguments):
    client = AsyncMock()
    client.request.return_value = prompt_fixtures["richPrompt"]
    api = SessionRpc(client, "bound-session")

    result = await api.mcp.prompts.get(
        MCPPromptsGetRequest(server_name="fixture", prompt_name="rich", arguments=arguments)
    )

    expected = {"sessionId": "bound-session", "serverName": "fixture", "promptName": "rich"}
    if arguments is not None:
        expected["arguments"] = arguments
    client.request.assert_awaited_once_with("session.mcp.prompts.get", expected)
    assert result.to_dict() == prompt_fixtures["richPrompt"]


@pytest.mark.asyncio
async def test_prompt_get_propagates_rpc_error():
    client = AsyncMock()
    client.request.side_effect = RuntimeError("Missing required argument: topic")

    with pytest.raises(RuntimeError, match="Missing required argument: topic"):
        await SessionRpc(client, "bound-session").mcp.prompts.get(
            MCPPromptsGetRequest(server_name="fixture", prompt_name="rich")
        )
