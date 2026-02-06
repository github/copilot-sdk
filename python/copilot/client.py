"""
Copilot CLI SDK Client - Main entry point for the Copilot SDK.

This module provides the :class:`CopilotClient` class, which manages the connection
to the Copilot CLI server and provides session management capabilities.

Example:
    >>> from copilot import CopilotClient
    >>>
    >>> async with CopilotClient() as client:
    ...     session = await client.create_session()
    ...     await session.send({"prompt": "Hello!"})
"""

import asyncio
import inspect
import os
import re
import subprocess
import sys
import threading
from dataclasses import asdict, is_dataclass
from pathlib import Path
from typing import Any, Callable, Optional, cast

from .generated.session_events import session_event_from_dict
from .jsonrpc import JsonRpcClient
from .sdk_protocol_version import get_sdk_protocol_version
from .session import CopilotSession
from .types import (
    ConnectionState,
    CopilotClientOptions,
    CustomAgentConfig,
    GetAuthStatusResponse,
    GetStatusResponse,
    ModelInfo,
    PingResponse,
    ProviderConfig,
    ResumeSessionConfig,
    SessionConfig,
    SessionLifecycleEvent,
    SessionLifecycleEventType,
    SessionLifecycleHandler,
    SessionMetadata,
    StopError,
    ToolHandler,
    ToolInvocation,
    ToolResult,
)


def _get_bundled_cli_path() -> Optional[str]:
    """Get the path to the bundled CLI binary, if available."""
    # The binary is bundled in copilot/bin/ within the package
    bin_dir = Path(__file__).parent / "bin"
    if not bin_dir.exists():
        return None

    # Determine binary name based on platform
    if sys.platform == "win32":
        binary_name = "copilot.exe"
    else:
        binary_name = "copilot"

    binary_path = bin_dir / binary_name
    if binary_path.exists():
        return str(binary_path)

    return None


class CopilotClient:
    """
    Main client for interacting with the Copilot CLI.

    The CopilotClient manages the connection to the Copilot CLI server and provides
    methods to create and manage conversation sessions. It can either spawn a CLI
    server process or connect to an existing server.

    The client supports both stdio (default) and TCP transport modes for
    communication with the CLI server.

    Attributes:
        options: The configuration options for the client.

    Example:
        >>> # Create a client with default options (spawns CLI server)
        >>> client = CopilotClient()
        >>> await client.start()
        >>>
        >>> # Create a session and send a message
        >>> session = await client.create_session({"model": "gpt-4"})
        >>> session.on(lambda event: print(event.type))
        >>> await session.send({"prompt": "Hello!"})
        >>>
        >>> # Clean up
        >>> await session.destroy()
        >>> await client.stop()

        >>> # Or connect to an existing server
        >>> client = CopilotClient({"cli_url": "localhost:3000"})
    """

    def __init__(self, options: Optional[CopilotClientOptions] = None):
        """
        Initialize a new CopilotClient.

        Args:
            options: Optional configuration options for the client. If not provided,
                default options are used (spawns CLI server using stdio).

        Raises:
            ValueError: If mutually exclusive options are provided (e.g., cli_url
                with use_stdio or cli_path).

        Example:
            >>> # Default options - spawns CLI server using stdio
            >>> client = CopilotClient()
            >>>
            >>> # Connect to an existing server
            >>> client = CopilotClient({"cli_url": "localhost:3000"})
            >>>
            >>> # Custom CLI path with specific log level
            >>> client = CopilotClient({
            ...     "cli_path": "/usr/local/bin/copilot",
            ...     "log_level": "debug"
            ... })
        """
        opts = options or {}

        # Validate mutually exclusive options
        if opts.get("cli_url") and (opts.get("use_stdio") or opts.get("cli_path")):
            raise ValueError("cli_url is mutually exclusive with use_stdio and cli_path")

        # Validate auth options with external server
        if opts.get("cli_url") and (
            opts.get("github_token") or opts.get("use_logged_in_user") is not None
        ):
            raise ValueError(
                "github_token and use_logged_in_user cannot be used with cli_url "
                "(external server manages its own auth)"
            )

        # Parse cli_url if provided
        self._actual_host: str = "localhost"
        self._is_external_server: bool = False
        if opts.get("cli_url"):
            self._actual_host, actual_port = self._parse_cli_url(opts["cli_url"])
            self._actual_port: Optional[int] = actual_port
            self._is_external_server = True
        else:
            self._actual_port = None

        # Determine CLI path: explicit option > bundled binary
        # Not needed when connecting to external server via cli_url
        if opts.get("cli_url"):
            default_cli_path = ""  # Not used for external server
        elif opts.get("cli_path"):
            default_cli_path = opts["cli_path"]
        else:
            bundled_path = _get_bundled_cli_path()
            if bundled_path:
                default_cli_path = bundled_path
            else:
                raise RuntimeError(
                    "Copilot CLI not found. The bundled CLI binary is not available. "
                    "Ensure you installed a platform-specific wheel, or provide cli_path."
                )

        # Default use_logged_in_user to False when github_token is provided
        github_token = opts.get("github_token")
        use_logged_in_user = opts.get("use_logged_in_user")
        if use_logged_in_user is None:
            use_logged_in_user = False if github_token else True

        self.options: CopilotClientOptions = {
            "cli_path": default_cli_path,
            "cwd": opts.get("cwd", os.getcwd()),
            "port": opts.get("port", 0),
            "use_stdio": False if opts.get("cli_url") else opts.get("use_stdio", True),
            "log_level": opts.get("log_level", "info"),
            "auto_start": opts.get("auto_start", True),
            "auto_restart": opts.get("auto_restart", True),
            "use_logged_in_user": use_logged_in_user,
        }
        if opts.get("cli_url"):
            self.options["cli_url"] = opts["cli_url"]
        if opts.get("env"):
            self.options["env"] = opts["env"]
        if github_token:
            self.options["github_token"] = github_token

        self._process: Optional[subprocess.Popen] = None
        self._client: Optional[JsonRpcClient] = None
        self._state: ConnectionState = "disconnected"
        self._sessions: dict[str, CopilotSession] = {}
        self._sessions_lock = threading.Lock()
        self._models_cache: Optional[list[ModelInfo]] = None
        self._models_cache_lock = asyncio.Lock()
        self._lifecycle_handlers: list[SessionLifecycleHandler] = []
        self._typed_lifecycle_handlers: dict[
            SessionLifecycleEventType, list[SessionLifecycleHandler]
        ] = {}
        self._lifecycle_handlers_lock = threading.Lock()

    def _parse_cli_url(self, url: str) -> tuple[str, int]:
        """
        Parse CLI URL into host and port.

        Supports formats: "host:port", "http://host:port", "https://host:port",
        or just "port".

        Args:
            url: The CLI URL to parse.

        Returns:
            A tuple of (host, port).

        Raises:
            ValueError: If the URL format is invalid or the port is out of range.
        """
        import re

        # Remove protocol if present
        clean_url = re.sub(r"^https?://", "", url)

        # Check if it's just a port number
        if clean_url.isdigit():
            port = int(clean_url)
            if port <= 0 or port > 65535:
                raise ValueError(f"Invalid port in cli_url: {url}")
            return ("localhost", port)

        # Parse host:port format
        parts = clean_url.split(":")
        if len(parts) != 2:
            raise ValueError(f"Invalid cli_url format: {url}")

        host = parts[0] if parts[0] else "localhost"
        try:
            port = int(parts[1])
        except ValueError as e:
            raise ValueError(f"Invalid port in cli_url: {url}") from e

        if port <= 0 or port > 65535:
            raise ValueError(f"Invalid port in cli_url: {url}")

        return (host, port)

    async def start(self) -> None:
        """
        Start the CLI server and establish a connection.

        If connecting to an external server (via cli_url), only establishes the
        connection. Otherwise, spawns the CLI server process and then connects.

        This method is called automatically when creating a session if ``auto_start``
        is True (default).

        Raises:
            RuntimeError: If the server fails to start or the connection fails.

        Example:
            >>> client = CopilotClient({"auto_start": False})
            >>> await client.start()
            >>> # Now ready to create sessions
        """
        if self._state == "connected":
            return

        self._state = "connecting"

        try:
            # Only start CLI server process if not connecting to external server
            if not self._is_external_server:
                await self._start_cli_server()

            # Connect to the server
            await self._connect_to_server()

            # Verify protocol version compatibility
            await self._verify_protocol_version()

            self._state = "connected"
        except Exception:
            self._state = "error"
            raise

    async def stop(self) -> list["StopError"]:
        """
        Stop the CLI server and close all active sessions.

        This method performs graceful cleanup:
        1. Destroys all active sessions
        2. Closes the JSON-RPC connection
        3. Terminates the CLI server process (if spawned by this client)

        Returns:
            A list of StopError objects containing error messages that occurred
            during cleanup. An empty list indicates all cleanup succeeded.

        Example:
            >>> errors = await client.stop()
            >>> if errors:
            ...     for error in errors:
            ...         print(f"Cleanup error: {error.message}")
        """
        errors: list[StopError] = []

        # Atomically take ownership of all sessions and clear the dict
        # so no other thread can access them
        with self._sessions_lock:
            sessions_to_destroy = list(self._sessions.values())
            self._sessions.clear()

        for session in sessions_to_destroy:
            try:
                await session.destroy()
            except Exception as e:
                errors.append(
                    StopError(message=f"Failed to destroy session {session.session_id}: {e}")
                )

        # Close client
        if self._client:
            await self._client.stop()
            self._client = None

        # Clear models cache
        async with self._models_cache_lock:
            self._models_cache = None

        # Kill CLI process
        # Kill CLI process (only if we spawned it)
        if self._process and not self._is_external_server:
            self._process.terminate()
            try:
                self._process.wait(timeout=5)
            except subprocess.TimeoutExpired:
                self._process.kill()
            self._process = None

        self._state = "disconnected"
        if not self._is_external_server:
            self._actual_port = None

        return errors

    async def force_stop(self) -> None:
        """
        Forcefully stop the CLI server without graceful cleanup.

        Use this when :meth:`stop` fails or takes too long. This method:
        - Clears all sessions immediately without destroying them
        - Force closes the connection
        - Kills the CLI process (if spawned by this client)

        Example:
            >>> # If normal stop hangs, force stop
            >>> try:
            ...     await asyncio.wait_for(client.stop(), timeout=5.0)
            ... except asyncio.TimeoutError:
            ...     await client.force_stop()
        """
        # Clear sessions immediately without trying to destroy them
        with self._sessions_lock:
            self._sessions.clear()

        # Force close connection
        if self._client:
            try:
                await self._client.stop()
            except Exception:
                pass  # Ignore errors during force stop
            self._client = None

        # Clear models cache
        async with self._models_cache_lock:
            self._models_cache = None

        # Kill CLI process immediately
        if self._process and not self._is_external_server:
            self._process.kill()
            self._process = None

        self._state = "disconnected"
        if not self._is_external_server:
            self._actual_port = None

    async def create_session(self, config: Optional[SessionConfig] = None) -> CopilotSession:
        """
        Create a new conversation session with the Copilot CLI.

        Sessions maintain conversation state, handle events, and manage tool execution.
        If the client is not connected and ``auto_start`` is enabled, this will
        automatically start the connection.

        Args:
            config: Optional configuration for the session, including model selection,
                custom tools, system messages, and more.

        Returns:
            A :class:`CopilotSession` instance for the new session.

        Raises:
            RuntimeError: If the client is not connected and auto_start is disabled.

        Example:
            >>> # Basic session
            >>> session = await client.create_session()
            >>>
            >>> # Session with model and streaming
            >>> session = await client.create_session({
            ...     "model": "gpt-4",
            ...     "streaming": True
            ... })
        """
        if not self._client:
            if self.options["auto_start"]:
                await self.start()
            else:
                raise RuntimeError("Client not connected. Call start() first.")

        cfg = config or {}

        tool_defs = []
        tools = cfg.get("tools")
        if tools:
            for tool in tools:
                definition = {
                    "name": tool.name,
                    "description": tool.description,
                }
                if tool.parameters:
                    definition["parameters"] = tool.parameters
                tool_defs.append(definition)

        payload: dict[str, Any] = {}
        if cfg.get("model"):
            payload["model"] = cfg["model"]
        if cfg.get("session_id"):
            payload["sessionId"] = cfg["session_id"]
        if cfg.get("reasoning_effort"):
            payload["reasoningEffort"] = cfg["reasoning_effort"]
        if tool_defs:
            payload["tools"] = tool_defs

        # Add system message configuration if provided
        system_message = cfg.get("system_message")
        if system_message:
            payload["systemMessage"] = system_message

        # Add tool filtering options
        available_tools = cfg.get("available_tools")
        if available_tools:
            payload["availableTools"] = available_tools
        excluded_tools = cfg.get("excluded_tools")
        if excluded_tools:
            payload["excludedTools"] = excluded_tools

        # Enable permission request callback if handler provided
        on_permission_request = cfg.get("on_permission_request")
        if on_permission_request:
            payload["requestPermission"] = True

        # Enable user input request callback if handler provided
        on_user_input_request = cfg.get("on_user_input_request")
        if on_user_input_request:
            payload["requestUserInput"] = True

        # Enable hooks callback if any hook handler provided
        hooks = cfg.get("hooks")
        if hooks and any(hooks.values()):
            payload["hooks"] = True

        # Add working directory if provided
        working_directory = cfg.get("working_directory")
        if working_directory:
            payload["workingDirectory"] = working_directory

        # Add streaming option if provided
        streaming = cfg.get("streaming")
        if streaming is not None:
            payload["streaming"] = streaming

        # Add provider configuration if provided
        provider = cfg.get("provider")
        if provider:
            payload["provider"] = self._convert_provider_to_wire_format(provider)

        # Add MCP servers configuration if provided
        mcp_servers = cfg.get("mcp_servers")
        if mcp_servers:
            payload["mcpServers"] = mcp_servers

        # Add custom agents configuration if provided
        custom_agents = cfg.get("custom_agents")
        if custom_agents:
            payload["customAgents"] = [
                self._convert_custom_agent_to_wire_format(agent) for agent in custom_agents
            ]

        # Add config directory override if provided
        config_dir = cfg.get("config_dir")
        if config_dir:
            payload["configDir"] = config_dir

        # Add skill directories configuration if provided
        skill_directories = cfg.get("skill_directories")
        if skill_directories:
            payload["skillDirectories"] = skill_directories

        # Add disabled skills configuration if provided
        disabled_skills = cfg.get("disabled_skills")
        if disabled_skills:
            payload["disabledSkills"] = disabled_skills

        # Add infinite sessions configuration if provided
        infinite_sessions = cfg.get("infinite_sessions")
        if infinite_sessions:
            wire_config: dict[str, Any] = {}
            if "enabled" in infinite_sessions:
                wire_config["enabled"] = infinite_sessions["enabled"]
            if "background_compaction_threshold" in infinite_sessions:
                wire_config["backgroundCompactionThreshold"] = infinite_sessions[
                    "background_compaction_threshold"
                ]
            if "buffer_exhaustion_threshold" in infinite_sessions:
                wire_config["bufferExhaustionThreshold"] = infinite_sessions[
                    "buffer_exhaustion_threshold"
                ]
            payload["infiniteSessions"] = wire_config

        if not self._client:
            raise RuntimeError("Client not connected")
        response = await self._client.request("session.create", payload)

        session_id = response["sessionId"]
        workspace_path = response.get("workspacePath")
        session = CopilotSession(session_id, self._client, workspace_path)
        session._register_tools(tools)
        if on_permission_request:
            session._register_permission_handler(on_permission_request)
        if on_user_input_request:
            session._register_user_input_handler(on_user_input_request)
        if hooks:
            session._register_hooks(hooks)
        with self._sessions_lock:
            self._sessions[session_id] = session

        return session

    async def resume_session(
        self, session_id: str, config: Optional[ResumeSessionConfig] = None
    ) -> CopilotSession:
        """
        Resume an existing conversation session by its ID.

        This allows you to continue a previous conversation, maintaining all
        conversation history. The session must have been previously created
        and not deleted.

        Args:
            session_id: The ID of the session to resume.
            config: Optional configuration for the resumed session.

        Returns:
            A :class:`CopilotSession` instance for the resumed session.

        Raises:
            RuntimeError: If the session does not exist or the client is not connected.

        Example:
            >>> # Resume a previous session
            >>> session = await client.resume_session("session-123")
            >>>
            >>> # Resume with new tools
            >>> session = await client.resume_session("session-123", {
            ...     "tools": [my_new_tool]
            ... })
        """
        if not self._client:
            if self.options["auto_start"]:
                await self.start()
            else:
                raise RuntimeError("Client not connected. Call start() first.")

        cfg = config or {}

        tool_defs = []
        tools = cfg.get("tools")
        if tools:
            for tool in tools:
                definition = {
                    "name": tool.name,
                    "description": tool.description,
                }
                if tool.parameters:
                    definition["parameters"] = tool.parameters
                tool_defs.append(definition)

        payload: dict[str, Any] = {"sessionId": session_id}

        # Add model if provided
        model = cfg.get("model")
        if model:
            payload["model"] = model

        if cfg.get("reasoning_effort"):
            payload["reasoningEffort"] = cfg["reasoning_effort"]
        if tool_defs:
            payload["tools"] = tool_defs

        # Add system message configuration if provided
        system_message = cfg.get("system_message")
        if system_message:
            payload["systemMessage"] = system_message

        # Add available/excluded tools if provided
        available_tools = cfg.get("available_tools")
        if available_tools:
            payload["availableTools"] = available_tools

        excluded_tools = cfg.get("excluded_tools")
        if excluded_tools:
            payload["excludedTools"] = excluded_tools

        provider = cfg.get("provider")
        if provider:
            payload["provider"] = self._convert_provider_to_wire_format(provider)

        # Add streaming option if provided
        streaming = cfg.get("streaming")
        if streaming is not None:
            payload["streaming"] = streaming

        # Enable permission request callback if handler provided
        on_permission_request = cfg.get("on_permission_request")
        if on_permission_request:
            payload["requestPermission"] = True

        # Enable user input request callback if handler provided
        on_user_input_request = cfg.get("on_user_input_request")
        if on_user_input_request:
            payload["requestUserInput"] = True

        # Enable hooks callback if any hook handler provided
        hooks = cfg.get("hooks")
        if hooks and any(hooks.values()):
            payload["hooks"] = True

        # Add working directory if provided
        working_directory = cfg.get("working_directory")
        if working_directory:
            payload["workingDirectory"] = working_directory

        # Add config directory if provided
        config_dir = cfg.get("config_dir")
        if config_dir:
            payload["configDir"] = config_dir

        # Add disable resume flag if provided
        disable_resume = cfg.get("disable_resume")
        if disable_resume:
            payload["disableResume"] = True

        # Add MCP servers configuration if provided
        mcp_servers = cfg.get("mcp_servers")
        if mcp_servers:
            payload["mcpServers"] = mcp_servers

        # Add custom agents configuration if provided
        custom_agents = cfg.get("custom_agents")
        if custom_agents:
            payload["customAgents"] = [
                self._convert_custom_agent_to_wire_format(agent) for agent in custom_agents
            ]

        # Add skill directories configuration if provided
        skill_directories = cfg.get("skill_directories")
        if skill_directories:
            payload["skillDirectories"] = skill_directories

        # Add disabled skills configuration if provided
        disabled_skills = cfg.get("disabled_skills")
        if disabled_skills:
            payload["disabledSkills"] = disabled_skills

        # Add infinite sessions configuration if provided
        infinite_sessions = cfg.get("infinite_sessions")
        if infinite_sessions:
            wire_config: dict[str, Any] = {}
            if "enabled" in infinite_sessions:
                wire_config["enabled"] = infinite_sessions["enabled"]
            if "background_compaction_threshold" in infinite_sessions:
                wire_config["backgroundCompactionThreshold"] = infinite_sessions[
                    "background_compaction_threshold"
                ]
            if "buffer_exhaustion_threshold" in infinite_sessions:
                wire_config["bufferExhaustionThreshold"] = infinite_sessions[
                    "buffer_exhaustion_threshold"
                ]
            payload["infiniteSessions"] = wire_config

        if not self._client:
            raise RuntimeError("Client not connected")
        response = await self._client.request("session.resume", payload)

        resumed_session_id = response["sessionId"]
        workspace_path = response.get("workspacePath")
        session = CopilotSession(resumed_session_id, self._client, workspace_path)
        session._register_tools(cfg.get("tools"))
        if on_permission_request:
            session._register_permission_handler(on_permission_request)
        if on_user_input_request:
            session._register_user_input_handler(on_user_input_request)
        if hooks:
            session._register_hooks(hooks)
        with self._sessions_lock:
            self._sessions[resumed_session_id] = session

        return session

    def get_state(self) -> ConnectionState:
        """
        Get the current connection state of the client.

        Returns:
            The current connection state: "disconnected", "connecting",
            "connected", or "error".

        Example:
            >>> if client.get_state() == "connected":
            ...     session = await client.create_session()
        """
        return self._state

    async def ping(self, message: Optional[str] = None) -> "PingResponse":
        """
        Send a ping request to the server to verify connectivity.

        Args:
            message: Optional message to include in the ping.

        Returns:
            A PingResponse object containing the ping response.

        Raises:
            RuntimeError: If the client is not connected.

        Example:
            >>> response = await client.ping("health check")
            >>> print(f"Server responded at {response.timestamp}")
        """
        if not self._client:
            raise RuntimeError("Client not connected")

        result = await self._client.request("ping", {"message": message})
        return PingResponse.from_dict(result)

    async def get_status(self) -> "GetStatusResponse":
        """
        Get CLI status including version and protocol information.

        Returns:
            A GetStatusResponse object containing version and protocolVersion.

        Raises:
            RuntimeError: If the client is not connected.

        Example:
            >>> status = await client.get_status()
            >>> print(f"CLI version: {status.version}")
        """
        if not self._client:
            raise RuntimeError("Client not connected")

        result = await self._client.request("status.get", {})
        return GetStatusResponse.from_dict(result)

    async def get_auth_status(self) -> "GetAuthStatusResponse":
        """
        Get current authentication status.

        Returns:
            A GetAuthStatusResponse object containing authentication state.

        Raises:
            RuntimeError: If the client is not connected.

        Example:
            >>> auth = await client.get_auth_status()
            >>> if auth.isAuthenticated:
            ...     print(f"Logged in as {auth.login}")
        """
        if not self._client:
            raise RuntimeError("Client not connected")

        result = await self._client.request("auth.getStatus", {})
        return GetAuthStatusResponse.from_dict(result)

    async def list_models(self) -> list["ModelInfo"]:
        """
        List available models with their metadata.

        Results are cached after the first successful call to avoid rate limiting.
        The cache is cleared when the client disconnects.

        Returns:
            A list of ModelInfo objects with model details.

        Raises:
            RuntimeError: If the client is not connected.
            Exception: If not authenticated.

        Example:
            >>> models = await client.list_models()
            >>> for model in models:
            ...     print(f"{model.id}: {model.name}")
        """
        if not self._client:
            raise RuntimeError("Client not connected")

        # Use asyncio lock to prevent race condition with concurrent calls
        async with self._models_cache_lock:
            # Check cache (already inside lock)
            if self._models_cache is not None:
                return list(self._models_cache)  # Return a copy to prevent cache mutation

            # Cache miss - fetch from backend while holding lock
            response = await self._client.request("models.list", {})
            models_data = response.get("models", [])
            models = [ModelInfo.from_dict(model) for model in models_data]

            # Update cache before releasing lock
            self._models_cache = models

            return list(models)  # Return a copy to prevent cache mutation

    async def list_sessions(self) -> list["SessionMetadata"]:
        """
        List all available sessions known to the server.

        Returns metadata about each session including ID, timestamps, and summary.

        Returns:
            A list of SessionMetadata objects.

        Raises:
            RuntimeError: If the client is not connected.

        Example:
            >>> sessions = await client.list_sessions()
            >>> for session in sessions:
            ...     print(f"Session: {session.sessionId}")
        """
        if not self._client:
            raise RuntimeError("Client not connected")

        response = await self._client.request("session.list", {})
        sessions_data = response.get("sessions", [])
        return [SessionMetadata.from_dict(session) for session in sessions_data]

    async def delete_session(self, session_id: str) -> None:
        """
        Delete a session permanently.

        This permanently removes the session and all its conversation history.
        The session cannot be resumed after deletion.

        Args:
            session_id: The ID of the session to delete.

        Raises:
            RuntimeError: If the client is not connected or deletion fails.

        Example:
            >>> await client.delete_session("session-123")
        """
        if not self._client:
            raise RuntimeError("Client not connected")

        response = await self._client.request("session.delete", {"sessionId": session_id})

        success = response.get("success", False)
        if not success:
            error = response.get("error", "Unknown error")
            raise RuntimeError(f"Failed to delete session {session_id}: {error}")

        # Remove from local sessions map if present
        with self._sessions_lock:
            if session_id in self._sessions:
                del self._sessions[session_id]

    async def get_foreground_session_id(self) -> Optional[str]:
        """
        Get the ID of the session currently displayed in the TUI.

        This is only available when connecting to a server running in TUI+server mode
        (--ui-server).

        Returns:
            The session ID, or None if no foreground session is set.

        Raises:
            RuntimeError: If the client is not connected.

        Example:
            >>> session_id = await client.get_foreground_session_id()
            >>> if session_id:
            ...     print(f"TUI is displaying session: {session_id}")
        """
        if not self._client:
            raise RuntimeError("Client not connected")

        response = await self._client.request("session.getForeground", {})
        return response.get("sessionId")

    async def set_foreground_session_id(self, session_id: str) -> None:
        """
        Request the TUI to switch to displaying the specified session.

        This is only available when connecting to a server running in TUI+server mode
        (--ui-server).

        Args:
            session_id: The ID of the session to display in the TUI.

        Raises:
            RuntimeError: If the client is not connected or the operation fails.

        Example:
            >>> await client.set_foreground_session_id("session-123")
        """
        if not self._client:
            raise RuntimeError("Client not connected")

        response = await self._client.request("session.setForeground", {"sessionId": session_id})

        success = response.get("success", False)
        if not success:
            error = response.get("error", "Unknown error")
            raise RuntimeError(f"Failed to set foreground session: {error}")

    def on(
        self,
        event_type_or_handler: SessionLifecycleEventType | SessionLifecycleHandler,
        handler: Optional[SessionLifecycleHandler] = None,
    ) -> Callable[[], None]:
        """
        Subscribe to session lifecycle events.

        Lifecycle events are emitted when sessions are created, deleted, updated,
        or change foreground/background state (in TUI+server mode).

        Can be called in two ways:
        - on(handler): Subscribe to all lifecycle events
        - on(event_type, handler): Subscribe to a specific event type

        Args:
            event_type_or_handler: Either a specific event type to listen for,
                or a handler function for all events.
            handler: Handler function when subscribing to a specific event type.

        Returns:
            A function that, when called, unsubscribes the handler.

        Example:
            >>> # Subscribe to specific event type
            >>> unsubscribe = client.on("session.foreground", lambda e: print(e.sessionId))
            >>>
            >>> # Subscribe to all events
            >>> unsubscribe = client.on(lambda e: print(f"{e.type}: {e.sessionId}"))
            >>>
            >>> # Later, to stop receiving events:
            >>> unsubscribe()
        """
        with self._lifecycle_handlers_lock:
            if callable(event_type_or_handler) and handler is None:
                # Wildcard subscription: on(handler)
                wildcard_handler = event_type_or_handler
                self._lifecycle_handlers.append(wildcard_handler)

                def unsubscribe_wildcard() -> None:
                    with self._lifecycle_handlers_lock:
                        if wildcard_handler in self._lifecycle_handlers:
                            self._lifecycle_handlers.remove(wildcard_handler)

                return unsubscribe_wildcard
            elif isinstance(event_type_or_handler, str) and handler is not None:
                # Typed subscription: on(event_type, handler)
                event_type = cast(SessionLifecycleEventType, event_type_or_handler)
                if event_type not in self._typed_lifecycle_handlers:
                    self._typed_lifecycle_handlers[event_type] = []
                self._typed_lifecycle_handlers[event_type].append(handler)

                def unsubscribe_typed() -> None:
                    with self._lifecycle_handlers_lock:
                        handlers = self._typed_lifecycle_handlers.get(event_type, [])
                        if handler in handlers:
                            handlers.remove(handler)

                return unsubscribe_typed
            else:
                raise ValueError("Invalid arguments: use on(handler) or on(event_type, handler)")

    def _dispatch_lifecycle_event(self, event: SessionLifecycleEvent) -> None:
        """Dispatch a lifecycle event to all registered handlers."""
        with self._lifecycle_handlers_lock:
            # Copy handlers to avoid holding lock during callbacks
            typed_handlers = list(self._typed_lifecycle_handlers.get(event.type, []))
            wildcard_handlers = list(self._lifecycle_handlers)

        # Dispatch to typed handlers
        for handler in typed_handlers:
            try:
                handler(event)
            except Exception:
                pass  # Ignore handler errors

        # Dispatch to wildcard handlers
        for handler in wildcard_handlers:
            try:
                handler(event)
            except Exception:
                pass  # Ignore handler errors

    async def _verify_protocol_version(self) -> None:
        """Verify that the server's protocol version matches the SDK's expected version."""
        expected_version = get_sdk_protocol_version()
        ping_result = await self.ping()
        server_version = ping_result.protocolVersion

        if server_version is None:
            raise RuntimeError(
                f"SDK protocol version mismatch: SDK expects version {expected_version}, "
                f"but server does not report a protocol version. "
                f"Please update your server to ensure compatibility."
            )

        if server_version != expected_version:
            raise RuntimeError(
                f"SDK protocol version mismatch: SDK expects version {expected_version}, "
                f"but server reports version {server_version}. "
                f"Please update your SDK or server to ensure compatibility."
            )

    def _convert_provider_to_wire_format(
        self, provider: ProviderConfig | dict[str, Any]
    ) -> dict[str, Any]:
        """
        Convert provider config from snake_case to camelCase wire format.

        Args:
            provider: The provider configuration in snake_case format.

        Returns:
            The provider configuration in camelCase wire format.
        """
        wire_provider: dict[str, Any] = {"type": provider.get("type")}
        if "base_url" in provider:
            wire_provider["baseUrl"] = provider["base_url"]
        if "api_key" in provider:
            wire_provider["apiKey"] = provider["api_key"]
        if "wire_api" in provider:
            wire_provider["wireApi"] = provider["wire_api"]
        if "bearer_token" in provider:
            wire_provider["bearerToken"] = provider["bearer_token"]
        if "azure" in provider:
            azure = provider["azure"]
            wire_azure: dict[str, Any] = {}
            if "api_version" in azure:
                wire_azure["apiVersion"] = azure["api_version"]
            if wire_azure:
                wire_provider["azure"] = wire_azure
        return wire_provider

    def _convert_custom_agent_to_wire_format(
        self, agent: CustomAgentConfig | dict[str, Any]
    ) -> dict[str, Any]:
        """
        Convert custom agent config from snake_case to camelCase wire format.

        Args:
            agent: The custom agent configuration in snake_case format.

        Returns:
            The custom agent configuration in camelCase wire format.
        """
        wire_agent: dict[str, Any] = {"name": agent.get("name"), "prompt": agent.get("prompt")}
        if "display_name" in agent:
            wire_agent["displayName"] = agent["display_name"]
        if "description" in agent:
            wire_agent["description"] = agent["description"]
        if "tools" in agent:
            wire_agent["tools"] = agent["tools"]
        if "mcp_servers" in agent:
            wire_agent["mcpServers"] = agent["mcp_servers"]
        if "infer" in agent:
            wire_agent["infer"] = agent["infer"]
        return wire_agent

    async def _start_cli_server(self) -> None:
        """
        Start the CLI server process.

        This spawns the CLI server as a subprocess using the configured transport
        mode (stdio or TCP).

        Raises:
            RuntimeError: If the server fails to start or times out.
        """
        cli_path = self.options["cli_path"]

        # Verify CLI exists
        if not os.path.exists(cli_path):
            raise RuntimeError(f"Copilot CLI not found at {cli_path}")

        args = ["--headless", "--log-level", self.options["log_level"]]

        # Add auth-related flags
        if self.options.get("github_token"):
            args.extend(["--auth-token-env", "COPILOT_SDK_AUTH_TOKEN"])
        if not self.options.get("use_logged_in_user", True):
            args.append("--no-auto-login")

        # If cli_path is a .js file, run it with node
        # Note that we can't rely on the shebang as Windows doesn't support it
        if cli_path.endswith(".js"):
            args = ["node", cli_path] + args
        else:
            args = [cli_path] + args

        # Get environment variables
        env = self.options.get("env")
        if env is None:
            env = dict(os.environ)
        else:
            env = dict(env)

        # Set auth token in environment if provided
        if self.options.get("github_token"):
            env["COPILOT_SDK_AUTH_TOKEN"] = self.options["github_token"]

        # Choose transport mode
        if self.options["use_stdio"]:
            args.append("--stdio")
            # Use regular Popen with pipes (buffering=0 for unbuffered)
            self._process = subprocess.Popen(
                args,
                stdin=subprocess.PIPE,
                stdout=subprocess.PIPE,
                stderr=subprocess.PIPE,
                bufsize=0,
                cwd=self.options["cwd"],
                env=env,
            )
        else:
            if self.options["port"] > 0:
                args.extend(["--port", str(self.options["port"])])
            self._process = subprocess.Popen(
                args,
                stdin=subprocess.DEVNULL,
                stdout=subprocess.PIPE,
                stderr=subprocess.PIPE,
                cwd=self.options["cwd"],
                env=env,
            )

        # For stdio mode, we're ready immediately
        if self.options["use_stdio"]:
            return

        # For TCP mode, wait for port announcement
        loop = asyncio.get_event_loop()
        process = self._process  # Capture for closure

        async def read_port():
            if not process or not process.stdout:
                raise RuntimeError("Process not started or stdout not available")
            while True:
                line = await loop.run_in_executor(None, process.stdout.readline)
                if not line:
                    raise RuntimeError("CLI process exited before announcing port")

                line_str = line.decode() if isinstance(line, bytes) else line
                match = re.search(r"listening on port (\d+)", line_str, re.IGNORECASE)
                if match:
                    self._actual_port = int(match.group(1))
                    return

        try:
            await asyncio.wait_for(read_port(), timeout=10.0)
        except asyncio.TimeoutError:
            raise RuntimeError("Timeout waiting for CLI server to start")

    async def _connect_to_server(self) -> None:
        """
        Connect to the CLI server via the configured transport.

        Uses either stdio or TCP based on the client configuration.

        Raises:
            RuntimeError: If the connection fails.
        """
        if self.options["use_stdio"]:
            await self._connect_via_stdio()
        else:
            await self._connect_via_tcp()

    async def _connect_via_stdio(self) -> None:
        """
        Connect to the CLI server via stdio pipes.

        Creates a JSON-RPC client using the CLI process's stdin/stdout.

        Raises:
            RuntimeError: If the CLI process is not started.
        """
        if not self._process:
            raise RuntimeError("CLI process not started")

        # Create JSON-RPC client with the process
        self._client = JsonRpcClient(self._process)

        # Set up notification handler for session events
        # Note: This handler is called from the event loop (thread-safe scheduling)
        def handle_notification(method: str, params: dict):
            if method == "session.event":
                session_id = params["sessionId"]
                event_dict = params["event"]
                # Convert dict to SessionEvent object
                event = session_event_from_dict(event_dict)
                with self._sessions_lock:
                    session = self._sessions.get(session_id)
                if session:
                    session._dispatch_event(event)
            elif method == "session.lifecycle":
                # Handle session lifecycle events
                lifecycle_event = SessionLifecycleEvent.from_dict(params)
                self._dispatch_lifecycle_event(lifecycle_event)

        self._client.set_notification_handler(handle_notification)
        self._client.set_request_handler("tool.call", self._handle_tool_call_request)
        self._client.set_request_handler("permission.request", self._handle_permission_request)
        self._client.set_request_handler("userInput.request", self._handle_user_input_request)
        self._client.set_request_handler("hooks.invoke", self._handle_hooks_invoke)

        # Start listening for messages
        loop = asyncio.get_running_loop()
        self._client.start(loop)

    async def _connect_via_tcp(self) -> None:
        """
        Connect to the CLI server via TCP socket.

        Creates a TCP connection to the server at the configured host and port.

        Raises:
            RuntimeError: If the server port is not available or connection fails.
        """
        if not self._actual_port:
            raise RuntimeError("Server port not available")

        # Create a TCP socket connection with timeout
        import socket

        # Connection timeout constant
        TCP_CONNECTION_TIMEOUT = 10  # seconds

        sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        sock.settimeout(TCP_CONNECTION_TIMEOUT)

        try:
            sock.connect((self._actual_host, self._actual_port))
            sock.settimeout(None)  # Remove timeout after connection
        except OSError as e:
            raise RuntimeError(
                f"Failed to connect to CLI server at {self._actual_host}:{self._actual_port}: {e}"
            )

        # Create a file-like wrapper for the socket
        sock_file = sock.makefile("rwb", buffering=0)

        # Create a mock process object that JsonRpcClient expects
        class SocketWrapper:
            def __init__(self, sock_file, sock_obj):
                self.stdin = sock_file
                self.stdout = sock_file
                self.stderr = None
                self._socket = sock_obj

            def terminate(self):
                try:
                    self._socket.close()
                except OSError:
                    pass

            def kill(self):
                self.terminate()

            def wait(self, timeout=None):
                pass

        self._process = SocketWrapper(sock_file, sock)  # type: ignore
        self._client = JsonRpcClient(self._process)

        # Set up notification handler for session events
        def handle_notification(method: str, params: dict):
            if method == "session.event":
                session_id = params["sessionId"]
                event_dict = params["event"]
                # Convert dict to SessionEvent object
                event = session_event_from_dict(event_dict)
                session = self._sessions.get(session_id)
                if session:
                    session._dispatch_event(event)
            elif method == "session.lifecycle":
                # Handle session lifecycle events
                lifecycle_event = SessionLifecycleEvent.from_dict(params)
                self._dispatch_lifecycle_event(lifecycle_event)

        self._client.set_notification_handler(handle_notification)
        self._client.set_request_handler("tool.call", self._handle_tool_call_request)
        self._client.set_request_handler("permission.request", self._handle_permission_request)
        self._client.set_request_handler("userInput.request", self._handle_user_input_request)
        self._client.set_request_handler("hooks.invoke", self._handle_hooks_invoke)

        # Start listening for messages
        loop = asyncio.get_running_loop()
        self._client.start(loop)

    async def _handle_permission_request(self, params: dict) -> dict:
        """
        Handle a permission request from the CLI server.

        Args:
            params: The permission request parameters from the server.

        Returns:
            A dict containing the permission decision result.

        Raises:
            ValueError: If the request payload is invalid.
        """
        session_id = params.get("sessionId")
        permission_request = params.get("permissionRequest")

        if not session_id or not permission_request:
            raise ValueError("invalid permission request payload")

        with self._sessions_lock:
            session = self._sessions.get(session_id)
        if not session:
            raise ValueError(f"unknown session {session_id}")

        try:
            result = await session._handle_permission_request(permission_request)
            return {"result": result}
        except Exception:  # pylint: disable=broad-except
            # If permission handler fails, deny the permission
            return {
                "result": {
                    "kind": "denied-no-approval-rule-and-could-not-request-from-user",
                }
            }

    async def _handle_user_input_request(self, params: dict) -> dict:
        """
        Handle a user input request from the CLI server.

        Args:
            params: The user input request parameters from the server.

        Returns:
            A dict containing the user's response.

        Raises:
            ValueError: If the request payload is invalid.
        """
        session_id = params.get("sessionId")
        question = params.get("question")

        if not session_id or not question:
            raise ValueError("invalid user input request payload")

        with self._sessions_lock:
            session = self._sessions.get(session_id)
        if not session:
            raise ValueError(f"unknown session {session_id}")

        result = await session._handle_user_input_request(params)
        return {"answer": result["answer"], "wasFreeform": result["wasFreeform"]}

    async def _handle_hooks_invoke(self, params: dict) -> dict:
        """
        Handle a hooks invocation from the CLI server.

        Args:
            params: The hooks invocation parameters from the server.

        Returns:
            A dict containing the hook output.

        Raises:
            ValueError: If the request payload is invalid.
        """
        session_id = params.get("sessionId")
        hook_type = params.get("hookType")
        input_data = params.get("input")

        if not session_id or not hook_type:
            raise ValueError("invalid hooks invoke payload")

        with self._sessions_lock:
            session = self._sessions.get(session_id)
        if not session:
            raise ValueError(f"unknown session {session_id}")

        output = await session._handle_hooks_invoke(hook_type, input_data)
        return {"output": output}

    async def _handle_tool_call_request(self, params: dict) -> dict:
        """
        Handle a tool call request from the CLI server.

        Args:
            params: The tool call parameters from the server.

        Returns:
            A dict containing the tool execution result.

        Raises:
            ValueError: If the request payload is invalid or session is unknown.
        """
        session_id = params.get("sessionId")
        tool_call_id = params.get("toolCallId")
        tool_name = params.get("toolName")

        if not session_id or not tool_call_id or not tool_name:
            raise ValueError("invalid tool call payload")

        with self._sessions_lock:
            session = self._sessions.get(session_id)
        if not session:
            raise ValueError(f"unknown session {session_id}")

        handler = session._get_tool_handler(tool_name)
        if not handler:
            return {"result": self._build_unsupported_tool_result(tool_name)}

        arguments = params.get("arguments")
        result = await self._execute_tool_call(
            session_id,
            tool_call_id,
            tool_name,
            arguments,
            handler,
        )

        return {"result": result}

    async def _execute_tool_call(
        self,
        session_id: str,
        tool_call_id: str,
        tool_name: str,
        arguments: Any,
        handler: ToolHandler,
    ) -> ToolResult:
        """
        Execute a tool call with the given handler.

        Args:
            session_id: The session ID making the tool call.
            tool_call_id: The unique ID for this tool call.
            tool_name: The name of the tool being called.
            arguments: The arguments to pass to the tool handler.
            handler: The tool handler function to execute.

        Returns:
            A ToolResult containing the execution result or error.
        """
        invocation: ToolInvocation = {
            "session_id": session_id,
            "tool_call_id": tool_call_id,
            "tool_name": tool_name,
            "arguments": arguments,
        }

        try:
            result = handler(invocation)
            if inspect.isawaitable(result):
                result = await result
        except Exception as exc:  # pylint: disable=broad-except
            # Don't expose detailed error information to the LLM for security reasons.
            # The actual error is stored in the 'error' field for debugging.
            result = ToolResult(
                textResultForLlm="Invoking this tool produced an error. "
                "Detailed information is not available.",
                resultType="failure",
                error=str(exc),
                toolTelemetry={},
            )

        if result is None:
            result = ToolResult(
                textResultForLlm="Tool returned no result.",
                resultType="failure",
                error="tool returned no result",
                toolTelemetry={},
            )

        return self._normalize_tool_result(result)

    def _normalize_tool_result(self, result: ToolResult) -> ToolResult:
        """
        Normalize a tool result for transmission.

        Converts dataclass instances to dictionaries for JSON serialization.

        Args:
            result: The tool result to normalize.

        Returns:
            The normalized tool result.
        """
        if is_dataclass(result) and not isinstance(result, type):
            return asdict(result)  # type: ignore[arg-type]
        return result

    def _build_unsupported_tool_result(self, tool_name: str) -> ToolResult:
        """
        Build a failure result for an unsupported tool.

        Args:
            tool_name: The name of the unsupported tool.

        Returns:
            A ToolResult indicating the tool is not supported.
        """
        return ToolResult(
            textResultForLlm=f"Tool '{tool_name}' is not supported.",
            resultType="failure",
            error=f"tool '{tool_name}' not supported",
            toolTelemetry={},
        )
