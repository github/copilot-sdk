import asyncio
import sys
import os
from unittest.mock import MagicMock, AsyncMock, patch

# Add current directory to path
sys.path.insert(0, os.path.abspath("."))

import copilot.client
from copilot.client import CopilotClient
from copilot.session import PermissionHandler

async def test_create_session_passes_persistent_memory():
    """
    Verify that create_session correctly passes the persistent_memory flag to the RPC layer.
    """
    # Mock the JSON-RPC client
    mock_rpc = AsyncMock()
    mock_rpc.request.return_value = {"sessionId": "test-session-id"}
    
    # Initialize the client with mock init
    with patch("copilot.client.CopilotClient.__init__", return_value=None):
        client = CopilotClient()
        client._client = mock_rpc
        client._auto_start = False
        client._session_fs_config = None
        client._create_session_fs_handler = None
        client._skill_directories = []
        client._disabled_skills = []
        client._mcp_servers = {}
        client._custom_agents = []
        client._default_agent = None
        client._agent = None
        client._config_dir = None
        client._enable_config_discovery = False
        client._infinite_sessions = None
        client._on_event = None
        client._on_elicitation_request = None
        
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

async def test_create_session_defaults_persistent_memory_to_none():
    """
    Verify that create_session does not pass persistentMemory if not specified.
    """
    mock_rpc = AsyncMock()
    mock_rpc.request.return_value = {"sessionId": "test-session-id"}
    
    with patch("copilot.client.CopilotClient.__init__", return_value=None):
        client = CopilotClient()
        client._client = mock_rpc
        client._auto_start = False
        client._session_fs_config = None
        client._create_session_fs_handler = None
        client._skill_directories = []
        client._disabled_skills = []
        client._mcp_servers = {}
        client._custom_agents = []
        client._default_agent = None
        client._agent = None
        client._config_dir = None
        client._enable_config_discovery = False
        client._infinite_sessions = None
        client._on_event = None
        client._on_elicitation_request = None
        
        await client.create_session(
            on_permission_request=PermissionHandler.approve_all
        )
    
    mock_rpc.request.assert_called_once()
    args, _ = mock_rpc.request.call_args
    assert "persistentMemory" not in args[1]

if __name__ == "__main__":
    # Manually run tests
    try:
        asyncio.run(test_create_session_passes_persistent_memory())
        asyncio.run(test_create_session_defaults_persistent_memory_to_none())
        print("Copilot SDK persistent_memory tests passed!")
    except Exception as e:
        print(f"Test failed: {e}")
        import traceback
        traceback.print_exc()
        sys.exit(1)
