"""Host user hook defaults must be sent on every initial create/resume request."""

import json
from unittest.mock import AsyncMock, Mock

import pytest

from copilot import CopilotClient, RuntimeConnection
from copilot._jsonrpc import JsonRpcError


def setup_client(mode):
    client = CopilotClient(mode=mode, connection=RuntimeConnection.for_uri("http://localhost:1234"))
    captured = []

    async def request(method, params, **kwargs):
        captured.append((method, json.loads(json.dumps(params))))
        return {"sessionId": params.get("sessionId", "session-id"), "success": True}

    client._client = Mock(request=AsyncMock(side_effect=request))
    return client, captured


async def test_host_user_hooks_rejects_protocol_3_without_fallback():
    client, _ = setup_client("copilot-cli")
    client._client.request = AsyncMock(
        return_value={"ok": True, "protocolVersion": 3, "version": "test"}
    )
    with pytest.raises(RuntimeError, match="SDK protocol version mismatch"):
        await client._verify_protocol_version()
    client._client.request.assert_awaited_once()
    assert client._client.request.call_args.args[0] == "connect"


async def test_host_user_hooks_does_not_probe_ping_when_connect_is_unsupported():
    client, _ = setup_client("copilot-cli")
    error = JsonRpcError(-32601, "Unhandled method connect")
    client._client.request = AsyncMock(side_effect=error)
    with pytest.raises(JsonRpcError) as raised:
        await client._verify_protocol_version()
    assert raised.value is error
    client._client.request.assert_awaited_once()
    assert client._client.request.call_args.args[0] == "connect"


@pytest.mark.parametrize("mode", ["empty", "copilot-cli"])
@pytest.mark.parametrize("supplied", [None, False, True])
@pytest.mark.parametrize("operation", ["create", "resume"])
async def test_host_user_hooks_matrix(mode, supplied, operation):
    client, captured = setup_client(mode)
    callback = Mock(return_value=None)
    config = {
        "available_tools": [],
        "enable_host_user_hooks": supplied,
        "hooks": {"on_session_start": callback},
    }
    if operation == "create":
        session = await client.create_session(**config)
    else:
        session = await client.resume_session("existing-enabled-session", **config)
    requests = [params for method, params in captured if method == f"session.{operation}"]
    assert len(requests) == 1
    expected = supplied if supplied is not None else mode != "empty"
    assert requests[0]["enableHostUserHooks"] is expected
    assert requests[0]["hooks"] is True
    await session._handle_hooks_invoke("sessionStart", {})
    callback.assert_called_once()
    assert config["enable_host_user_hooks"] is supplied


async def test_host_user_hooks_reused_config_uses_current_mode():
    config = {"available_tools": []}
    for mode in ["empty", "copilot-cli"]:
        client, captured = setup_client(mode)
        session = await client.create_session(**config)
        await client.resume_session(session.session_id, **config)
        for method, params in captured:
            if method in ("session.create", "session.resume"):
                assert params["enableHostUserHooks"] is (mode != "empty")
        assert config == {"available_tools": []}


@pytest.mark.parametrize("mode", ["empty", "copilot-cli"])
async def test_host_user_hooks_resume_does_not_inherit_saved_value(mode):
    client, captured = setup_client(mode)
    session = await client.create_session(available_tools=[], enable_host_user_hooks=True)
    config = {"available_tools": []} if mode == "empty" else {}
    await client.resume_session(session.session_id, **config)
    payload = next(params for method, params in captured if method == "session.resume")
    assert payload["enableHostUserHooks"] is (mode != "empty")
