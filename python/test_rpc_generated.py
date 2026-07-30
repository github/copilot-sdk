"""Tests for generated RPC method behavior."""

from unittest.mock import AsyncMock

import pytest

from copilot.generated.rpc import _load_SessionListEntry
from copilot.rpc import (
    CommandsApi,
    CommandsInvokeRequest,
    LocalSessionMetadataValue,
    QueuedCommandHandled,
    QueuedCommandNotHandled,
    RemoteSessionMetadataValue,
    SessionList,
    SlashCommandTextResult,
)


@pytest.mark.asyncio
async def test_commands_invoke_deserializes_slash_command_result():
    client = AsyncMock()
    client.request = AsyncMock(return_value={"kind": "text", "text": "hello", "markdown": True})
    api = CommandsApi(client, "sess-1")

    result = await api.invoke(CommandsInvokeRequest(name="help"))

    assert isinstance(result, SlashCommandTextResult)
    assert result.text == "hello"
    assert result.markdown is True


def test_session_list_entry_decodes_boolean_is_remote_discriminator():
    local_payload = {
        "sessionId": "example-local",
        "startTime": "2026-07-26T10:00:00.000Z",
        "modifiedTime": "2026-07-26T10:05:00.000Z",
        "isRemote": False,
    }
    remote_payload = {
        "sessionId": "example-remote",
        "startTime": "2026-07-26T10:00:00.000Z",
        "modifiedTime": "2026-07-26T10:05:00.000Z",
        "isRemote": True,
        "remoteSessionIds": ["rs-1"],
        "repository": {
            "owner": "github",
            "name": "copilot-sdk",
            "branch": "main",
        },
    }

    local_entry = _load_SessionListEntry(local_payload)
    remote_entry = _load_SessionListEntry(remote_payload)

    assert isinstance(local_entry, LocalSessionMetadataValue)
    assert local_entry.session_id == "example-local"
    assert isinstance(remote_entry, RemoteSessionMetadataValue)
    assert remote_entry.session_id == "example-remote"

    session_list = SessionList.from_dict({"sessions": [local_payload, remote_payload]})
    assert len(session_list.sessions) == 2
    assert isinstance(session_list.sessions[0], LocalSessionMetadataValue)
    assert isinstance(session_list.sessions[1], RemoteSessionMetadataValue)


def test_queued_command_result_round_trips_boolean_handled_discriminator():
    handled = QueuedCommandHandled(stop_processing_queue=True)
    not_handled = QueuedCommandNotHandled()

    assert handled.to_dict() == {"handled": True, "stopProcessingQueue": True}
    assert not_handled.to_dict() == {"handled": False}

    assert QueuedCommandHandled.from_dict({"handled": True, "stopProcessingQueue": True}) == handled
    assert QueuedCommandNotHandled.from_dict({"handled": False}) == not_handled
