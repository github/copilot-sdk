import pytest
from unittest.mock import AsyncMock
from copilot import CopilotClient
from copilot.client import SubprocessConfig
from copilot.session import PermissionHandler

@pytest.mark.asyncio
async def test_create_session_passes_persistent_memory():
    """
    Verify that create_session correctly passes the persistent_memory flag to the RPC layer.
    """
    # Mock the JSON-RPC client
    mock_rpc = AsyncMock()
    mock_rpc.request.return_value = {"sessionId": "test-session-id"}
    
    # Initialize the client with a dummy CLI path
    client = CopilotClient(SubprocessConfig(cli_path="dummy-cli"))
    client._client = mock_rpc
    
    # Create a session with persistent_memory=True
    await client.create_session(
        on_permission_request=PermissionHandler.approve_all,
        persistent_memory=True
    )
    
    # Verify the RPC call
    mock_rpc.request.assert_called_once()
    args, _ = mock_rpc.request.call_args
    assert args[0] == "session.create"
    assert args[1]["persistentMemory"] is True

@pytest.mark.asyncio
async def test_create_session_defaults_persistent_memory_to_none():
    """
    Verify that create_session does not pass persistentMemory if not specified.
    """
    mock_rpc = AsyncMock()
    mock_rpc.request.return_value = {"sessionId": "test-session-id"}
    
    client = CopilotClient(SubprocessConfig(cli_path="dummy-cli"))
    client._client = mock_rpc
    
    await client.create_session(
        on_permission_request=PermissionHandler.approve_all
    )
    
    mock_rpc.request.assert_called_once()
    args, _ = mock_rpc.request.call_args
    assert "persistentMemory" not in args[1]
