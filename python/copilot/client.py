"""
Copilot CLI SDK Client - Main entry point for the Copilot SDK.

This module provides the :class:`CopilotClient` class, which manages the connection
to the Copilot CLI server and provides session management capabilities.

Example:
    >>> from copilot import CopilotClient
    >>>
    >>> async with CopilotClient() as client:
    ...     session = await client.create_session()
    ...     await session.send("Hello!")
"""

import asyncio
import inspect
import os
import re
import shutil
import subprocess
import sys
import threading
import uuid
from collections.abc import Awaitable, Callable
from pathlib import Path
from typing import Any, cast, overload

from ._jsonrpc import JsonRpcClient, ProcessExitedError
from ._sdk_protocol_version import get_sdk_protocol_version
from ._telemetry import get_trace_context, trace_context
from .generated.rpc import ServerRpc
from .generated.session_events import PermissionRequest, session_event_from_dict
from .session import CopilotSession
from .types import (
    ConnectionState,
    CustomAgentConfig,
    ExternalServerConfig,
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
    SessionListFilter,
    SessionMetadata,
    StopError,
    SubprocessConfig,
    ToolInvocation,
    ToolResult,
)

HandlerUnsubcribe = Callable[[], None]

NO_RESULT_PERMISSION_V2_ERROR = (
    "Permission handlers cannot return 'no-result' when connected to a protocol v2 server."
)

# Minimum protocol version this SDK can communicate with.
# Servers reporting a version below this are rejected.
MIN_PROTOCOL_VERSION = 2


def _get_bundled_cli_path() -> str | None:
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

    Example:
        >>> # Create a client with default options (spawns CLI server)
        >>> client = CopilotClient()
        >>> await client.start()
        >>>
        >>> # Create a session and send a message
        >>> session = await client.create_session({
        ...     "on_permission_request": PermissionHandler.approve_all,
        ...     "model": "gpt-4",
        ... })
        >>> session.on(lambda event: print(event.type))
        >>> await session.send("Hello!")
        >>>
        >>> # Clean up
        >>> await session.disconnect()
        >>> await client.stop()

        >>> # Or connect to an existing server
        >>> client = CopilotClient(ExternalServerConfig(url="localhost:3000"))
    """

    def __init__(
        self,
        config: SubprocessConfig | ExternalServerConfig | None = None,
        *,
        auto_start: bool = True,
        on_list_models: Callable[[], list[ModelInfo] | Awaitable[list[ModelInfo]]] | None = None,
    ):
        """
        Initialize a new CopilotClient.

        Args:
            config: Connection configuration. Pass a :class:`SubprocessConfig` to
                spawn a local CLI process, or an :class:`ExternalServerConfig` to
                connect to an existing server. Defaults to ``SubprocessConfig()``.
            auto_start: Automatically start the connection on first use
                (default: ``True``).
            on_list_models: Custom handler for :meth:`list_models`. When provided,
                the handler is called instead of querying the CLI server.

        Example:
            >>> # Default — spawns CLI server using stdio
            >>> client = CopilotClient()
            >>>
            >>> # Connect to an existing server
            >>> client = CopilotClient(ExternalServerConfig(url="localhost:3000"))
            >>>
            >>> # Custom CLI path with specific log level
            >>> client = CopilotClient(SubprocessConfig(
            ...     cli_path="/usr/local/bin/copilot",
            ...     log_level="debug",
            ... ))
        """
        if config is None:
            config = SubprocessConfig()

        self._config: SubprocessConfig | ExternalServerConfig = config
        self._auto_start = auto_start
        self._on_list_models = on_list_models

        # Resolve connection-mode-specific state
        self._actual_host: str = "localhost"
        self._is_external_server: bool = isinstance(config, ExternalServerConfig)

        if isinstance(config, ExternalServerConfig):
            self._actual_host, actual_port = self._parse_cli_url(config.url)
            self._actual_port: int | None = actual_port
        else:
            self._actual_port = None

            # Resolve CLI path: explicit > bundled binary
            if config.cli_path is None:
                bundled_path = _get_bundled_cli_path()
                if bundled_path:
                    config.cli_path = bundled_path
                else:
                    raise RuntimeError(
                        "Copilot CLI not found. The bundled CLI binary is not available. "
                        "Ensure you installed a platform-specific wheel, or provide cli_path."
                    )

            # Resolve use_logged_in_user default
            if config.use_logged_in_user is None:
                config.use_logged_in_user = not bool(config.github_token)

        self._process: subprocess.Popen | None = None
        self._client: JsonRpcClient | None = None
        self._state: ConnectionState = "disconnected"
        self._sessions: dict[str, CopilotSession] = {}
        self._sessions_lock = threading.Lock()
        self._models_cache: list[ModelInfo] | None = None
        self._models_cache_lock = asyncio.Lock()
        self._lifecycle_handlers: list[SessionLifecycleHandler] = []
        self._typed_lifecycle_handlers: dict[
            SessionLifecycleEventType, list[SessionLifecycleHandler]
        ] = {}
        self._lifecycle_handlers_lock = threading.Lock()
        self._rpc: ServerRpc | None = None
        self._negotiated_protocol_version: int | None = None

    @property
    def rpc(self) -> ServerRpc:
        """Typed server-scoped RPC methods."""
        if self._rpc is None:
            raise RuntimeError("Client is not connected. Call start() first.")
        return self._rpc

    @property
    def actual_port(self) -> int | None:
        """The actual TCP port the CLI server is listening on, if using TCP transport.

        Useful for multi-client scenarios where a second client needs to connect
        to the same server. Only available after :meth:`start` completes and
        only when not using stdio transport.
        """
        return self._actual_port

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

        If connecting to an external server (via :class:`ExternalServerConfig`),
        only establishes the connection. Otherwise, spawns the CLI server process
        and then connects.

        This method is called automatically when creating a session if ``auto_start``
        is True (default).

        Raises:
            RuntimeError: If the server fails to start or the connection fails.

        Example:
            >>> client = CopilotClient(auto_start=False)
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
        except ProcessExitedError as e:
            # Process exited with error - reraise as RuntimeError with stderr
            self._state = "error"
            raise RuntimeError(str(e)) from None
        except Exception as e:
            self._state = "error"
            # Check if process exited and capture any remaining stderr
            if self._process and hasattr(self._process, "poll"):
                return_code = self._process.poll()
                if return_code is not None and self._client:
                    stderr_output = self._client.get_stderr_output()
                    if stderr_output:
                        raise RuntimeError(
                            f"CLI process exited with code {return_code}\nstderr: {stderr_output}"
                        ) from e
            raise

    async def stop(self) -> None:
        """
        Stop the CLI server and close all active sessions.

        This method performs graceful cleanup:
        1. Closes all active sessions (releases in-memory resources)
        2. Closes the JSON-RPC connection
        3. Terminates the CLI server process (if spawned by this client)

        Note: session data on disk is preserved, so sessions can be resumed
        later. To permanently remove session data before stopping, call
        :meth:`delete_session` for each session first.

        Raises:
            ExceptionGroup[StopError]: If any errors occurred during cleanup.

        Example:
            >>> try:
            ...     await client.stop()
            ... except* StopError as eg:
            ...     for error in eg.exceptions:
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
                await session.disconnect()
            except Exception as e:
                errors.append(
                    StopError(message=f"Failed to disconnect session {session.session_id}: {e}")
                )

        # Close client
        if self._client:
            await self._client.stop()
            self._client = None
        self._rpc = None

        # Clear models cache
        async with self._models_cache_lock:
            self._models_cache = None

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

        if errors:
            raise ExceptionGroup("errors during CopilotClient.stop()", errors)

    async def force_stop(self) -> None:
        """
        Forcefully stop the CLI server without graceful cleanup.

        Use this when :meth:`stop` fails or takes too long. This method:
        - Clears all sessions immediately without destroying them
        - Force closes the connection (closes the underlying transport)
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

        # Close the transport first to signal the server immediately.
        # For external servers (TCP), this closes the socket.
        # For spawned processes (stdio), this kills the process.
        if self._process:
            try:
                if self._is_external_server:
                    self._process.terminate()  # closes the TCP socket
                else:
                    self._process.kill()
                    self._process = None
            except Exception:
                pass

        # Then clean up the JSON-RPC client
        if self._client:
            try:
                await self._client.stop()
            except Exception:
                pass  # Ignore errors during force stop
            self._client = None
        self._rpc = None

        # Clear models cache
        async with self._models_cache_lock:
            self._models_cache = None

        self._state = "disconnected"
        if not self._is_external_server:
            self._actual_port = None

    async def create_session(self, config: SessionConfig) -> CopilotSession:
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
            >>> config = {"on_permission_request": PermissionHandler.approve_all}
            >>> session = await client.create_session(config)
            >>>
            >>> # Session with model and streaming
            >>> session = await client.create_session({
            ...     "on_permission_request": PermissionHandler.approve_all,
            ...     "model": "gpt-4",
            ...     "streaming": True
            ... })
        """
        if not self._client:
            if self._auto_start:
                await self.start()
            else:
                raise RuntimeError("Client not connected. Call start() first.")

        cfg = config

        if not cfg.get("on_permission_request"):
            raise ValueError(
                "An on_permission_request handler is required when creating a session. "
                "For example, to allow all permissions, use "
                '{"on_permission_request": PermissionHandler.approve_all}.'
            )

        tool_defs = []
        tools = cfg.get("tools")
        if tools:
            for tool in tools:
                definition: dict[str, Any] = {
                    "name": tool.name,
                    "description": tool.description,
                }
                if tool.parameters:
                    definition["parameters"] = tool.parameters
                if tool.overrides_built_in_tool:
                    definition["overridesBuiltInTool"] = True
                if tool.skip_permission:
                    definition["skipPermission"] = True
                tool_defs.append(definition)

        payload: dict[str, Any] = {}
        if cfg.get("model"):
            payload["model"] = cfg["model"]
        if cfg.get("client_name"):
            payload["clientName"] = cfg["client_name"]
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
        if available_tools is not None:
            payload["availableTools"] = available_tools
        excluded_tools = cfg.get("excluded_tools")
        if excluded_tools is not None:
            payload["excludedTools"] = excluded_tools

        # Always enable permission request callback (deny by default if no handler provided)
        on_permission_request = cfg.get("on_permission_request")
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
        payload["envValueMode"] = "direct"

        # Add custom agents configuration if provided
        custom_agents = cfg.get("custom_agents")
        if custom_agents:
            payload["customAgents"] = [
                self._convert_custom_agent_to_wire_format(agent) for agent in custom_agents
            ]

        # Add agent selection if provided
        agent = cfg.get("agent")
        if agent:
            payload["agent"] = agent

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

        session_id = cfg.get("session_id") or str(uuid.uuid4())
        payload["sessionId"] = session_id

        # Propagate W3C Trace Context to CLI if OpenTelemetry is active
        trace_ctx = get_trace_context()
        payload.update(trace_ctx)

        # Create and register the session before issuing the RPC so that
        # events emitted by the CLI (e.g. session.start) are not dropped.
        session = CopilotSession(session_id, self._client, None)
        session._register_tools(tools)
        session._register_permission_handler(on_permission_request)
        if on_user_input_request:
            session._register_user_input_handler(on_user_input_request)
        if hooks:
            session._register_hooks(hooks)
        on_event = cfg.get("on_event")
        if on_event:
            session.on(on_event)
        with self._sessions_lock:
            self._sessions[session_id] = session

        try:
            response = await self._client.request("session.create", payload)
            session._workspace_path = response.get("workspacePath")
        except BaseException:
            with self._sessions_lock:
                self._sessions.pop(session_id, None)
            raise

        return session

    async def resume_session(self, session_id: str, config: ResumeSessionConfig) -> CopilotSession:
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
            >>> config = {"on_permission_request": PermissionHandler.approve_all}
            >>> session = await client.resume_session("session-123", config)
            >>>
            >>> # Resume with new tools
            >>> session = await client.resume_session("session-123", {
            ...     "on_permission_request": PermissionHandler.approve_all,
            ...     "tools": [my_new_tool]
            ... })
        """
        if not self._client:
            if self._auto_start:
                await self.start()
            else:
                raise RuntimeError("Client not connected. Call start() first.")

        cfg = config

        if not cfg.get("on_permission_request"):
            raise ValueError(
                "An on_permission_request handler is required when resuming a session. "
                "For example, to allow all permissions, use "
                '{"on_permission_request": PermissionHandler.approve_all}.'
            )

        tool_defs = []
        tools = cfg.get("tools")
        if tools:
            for tool in tools:
                definition: dict[str, Any] = {
                    "name": tool.name,
                    "description": tool.description,
                }
                if tool.parameters:
                    definition["parameters"] = tool.parameters
                if tool.overrides_built_in_tool:
                    definition["overridesBuiltInTool"] = True
                if tool.skip_permission:
                    definition["skipPermission"] = True
                tool_defs.append(definition)

        payload: dict[str, Any] = {"sessionId": session_id}

        # Add client name if provided
        client_name = cfg.get("client_name")
        if client_name:
            payload["clientName"] = client_name

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
        if available_tools is not None:
            payload["availableTools"] = available_tools

        excluded_tools = cfg.get("excluded_tools")
        if excluded_tools is not None:
            payload["excludedTools"] = excluded_tools

        provider = cfg.get("provider")
        if provider:
            payload["provider"] = self._convert_provider_to_wire_format(provider)

        # Add streaming option if provided
        streaming = cfg.get("streaming")
        if streaming is not None:
            payload["streaming"] = streaming

        # Always enable permission request callback (deny by default if no handler provided)
        on_permission_request = cfg.get("on_permission_request")
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
        payload["envValueMode"] = "direct"

        # Add custom agents configuration if provided
        custom_agents = cfg.get("custom_agents")
        if custom_agents:
            payload["customAgents"] = [
                self._convert_custom_agent_to_wire_format(agent) for agent in custom_agents
            ]

        # Add agent selection if provided
        agent = cfg.get("agent")
        if agent:
            payload["agent"] = agent

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

        # Propagate W3C Trace Context to CLI if OpenTelemetry is active
        trace_ctx = get_trace_context()
        payload.update(trace_ctx)

        # Create and register the session before issuing the RPC so that
        # events emitted by the CLI (e.g. session.start) are not dropped.
        session = CopilotSession(session_id, self._client, None)
        session._register_tools(cfg.get("tools"))
        session._register_permission_handler(on_permission_request)
        if on_user_input_request:
            session._register_user_input_handler(on_user_input_request)
        if hooks:
            session._register_hooks(hooks)
        on_event = cfg.get("on_event")
        if on_event:
            session.on(on_event)
        with self._sessions_lock:
            self._sessions[session_id] = session

        try:
            response = await self._client.request("session.resume", payload)
            session._workspace_path = response.get("workspacePath")
        except BaseException:
            with self._sessions_lock:
                self._sessions.pop(session_id, None)
            raise

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

    async def ping(self, message: str | None = None) -> "PingResponse":
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

        If a custom ``on_list_models`` handler was provided in the client options,
        it is called instead of querying the CLI server. The handler may be sync
        or async.

        Returns:
            A list of ModelInfo objects with model details.

        Raises:
            RuntimeError: If the client is not connected (when no custom handler is set).
            Exception: If not authenticated.

        Example:
            >>> models = await client.list_models()
            >>> for model in models:
            ...     print(f"{model.id}: {model.name}")
        """
        # Use asyncio lock to prevent race condition with concurrent calls
        async with self._models_cache_lock:
            # Check cache (already inside lock)
            if self._models_cache is not None:
                return list(self._models_cache)  # Return a copy to prevent cache mutation

            if self._on_list_models:
                # Use custom handler instead of CLI RPC
                result = self._on_list_models()
                if inspect.isawaitable(result):
                    models = await result
                else:
                    models = result
            else:
                if not self._client:
                    raise RuntimeError("Client not connected")

                # Cache miss - fetch from backend while holding lock
                response = await self._client.request("models.list", {})
                models_data = response.get("models", [])
                models = [ModelInfo.from_dict(model) for model in models_data]

            # Update cache before releasing lock (copy to prevent external mutation)
            self._models_cache = list(models)

            return list(models)  # Return a copy to prevent cache mutation

    async def list_sessions(
        self, filter: "SessionListFilter | None" = None
    ) -> list["SessionMetadata"]:
        """
        List all available sessions known to the server.

        Returns metadata about each session including ID, timestamps, and summary.

        Args:
            filter: Optional filter to narrow down the list of sessions by cwd, git root,
                repository, or branch.

        Returns:
            A list of SessionMetadata objects.

        Raises:
            RuntimeError: If the client is not connected.

        Example:
            >>> sessions = await client.list_sessions()
            >>> for session in sessions:
            ...     print(f"Session: {session.sessionId}")
            >>> # Filter sessions by repository
            >>> from copilot import SessionListFilter
            >>> filtered = await client.list_sessions(SessionListFilter(repository="owner/repo"))
        """
        if not self._client:
            raise RuntimeError("Client not connected")

        payload: dict = {}
        if filter is not None:
            payload["filter"] = filter.to_dict()

        response = await self._client.request("session.list", payload)
        sessions_data = response.get("sessions", [])
        return [SessionMetadata.from_dict(session) for session in sessions_data]

    async def delete_session(self, session_id: str) -> None:
        """
        Permanently delete a session and all its data from disk, including
        conversation history, planning state, and artifacts.

        Unlike :meth:`CopilotSession.disconnect`, which only releases in-memory
        resources and preserves session data for later resumption, this method
        is irreversible. The session cannot be resumed after deletion.

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

    async def get_last_session_id(self) -> str | None:
        """
        Get the ID of the most recently updated session.

        This is useful for resuming the last conversation when the session ID
        was not stored.

        Returns:
            The session ID, or None if no sessions exist.

        Raises:
            RuntimeError: If the client is not connected.

        Example:
            >>> last_id = await client.get_last_session_id()
            >>> if last_id:
            ...     config = {"on_permission_request": PermissionHandler.approve_all}
            ...     session = await client.resume_session(last_id, config)
        """
        if not self._client:
            raise RuntimeError("Client not connected")

        response = await self._client.request("session.getLastId", {})
        return response.get("sessionId")

    async def get_foreground_session_id(self) -> str | None:
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

    @overload
    def on(self, handler: SessionLifecycleHandler, /) -> HandlerUnsubcribe: ...

    @overload
    def on(
        self, event_type: SessionLifecycleEventType, /, handler: SessionLifecycleHandler
    ) -> HandlerUnsubcribe: ...

    def on(
        self,
        event_type_or_handler: SessionLifecycleEventType | SessionLifecycleHandler,
        /,
        handler: SessionLifecycleHandler | None = None,
    ) -> HandlerUnsubcribe:
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
        """Verify that the server's protocol version is within the supported range
        and store the negotiated version."""
        max_version = get_sdk_protocol_version()
        ping_result = await self.ping()
        server_version = ping_result.protocolVersion

        if server_version is None:
            raise RuntimeError(
                "SDK protocol version mismatch: "
                f"SDK supports versions {MIN_PROTOCOL_VERSION}-{max_version}"
                ", but server does not report a protocol version. "
                "Please update your server to ensure compatibility."
            )

        if server_version < MIN_PROTOCOL_VERSION or server_version > max_version:
            raise RuntimeError(
                "SDK protocol version mismatch: "
                f"SDK supports versions {MIN_PROTOCOL_VERSION}-{max_version}"
                f", but server reports version {server_version}. "
                "Please update your SDK or server to ensure compatibility."
            )

        self._negotiated_protocol_version = server_version

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
        assert isinstance(self._config, SubprocessConfig)
        cfg = self._config

        cli_path = cfg.cli_path
        assert cli_path is not None  # resolved in __init__

        # Verify CLI exists
        if not os.path.exists(cli_path):
            original_path = cli_path
            if (cli_path := shutil.which(cli_path)) is None:
                raise RuntimeError(f"Copilot CLI not found at {original_path}")

        # Start with user-provided cli_args, then add SDK-managed args
        args = list(cfg.cli_args) + [
            "--headless",
            "--no-auto-update",
            "--log-level",
            cfg.log_level,
        ]

        # Add auth-related flags
        if cfg.github_token:
            args.extend(["--auth-token-env", "COPILOT_SDK_AUTH_TOKEN"])
        if not cfg.use_logged_in_user:
            args.append("--no-auto-login")

        # If cli_path is a .js file, run it with node
        # Note that we can't rely on the shebang as Windows doesn't support it
        if cli_path.endswith(".js"):
            args = ["node", cli_path] + args
        else:
            args = [cli_path] + args

        # Get environment variables
        if cfg.env is None:
            env = dict(os.environ)
        else:
            env = dict(cfg.env)

        # Set auth token in environment if provided
        if cfg.github_token:
            env["COPILOT_SDK_AUTH_TOKEN"] = cfg.github_token

        # Set OpenTelemetry environment variables if telemetry config is provided
        telemetry = cfg.telemetry
        if telemetry is not None:
            env["COPILOT_OTEL_ENABLED"] = "true"
            if "otlp_endpoint" in telemetry:
                env["OTEL_EXPORTER_OTLP_ENDPOINT"] = telemetry["otlp_endpoint"]
            if "file_path" in telemetry:
                env["COPILOT_OTEL_FILE_EXPORTER_PATH"] = telemetry["file_path"]
            if "exporter_type" in telemetry:
                env["COPILOT_OTEL_EXPORTER_TYPE"] = telemetry["exporter_type"]
            if "source_name" in telemetry:
                env["COPILOT_OTEL_SOURCE_NAME"] = telemetry["source_name"]
            if "capture_content" in telemetry:
                env["OTEL_INSTRUMENTATION_GENAI_CAPTURE_MESSAGE_CONTENT"] = str(
                    telemetry["capture_content"]
                ).lower()

        # On Windows, hide the console window to avoid distracting users in GUI apps
        creationflags = subprocess.CREATE_NO_WINDOW if sys.platform == "win32" else 0

        cwd = cfg.cwd or os.getcwd()

        # Choose transport mode
        if cfg.use_stdio:
            args.append("--stdio")
            # Use regular Popen with pipes (buffering=0 for unbuffered)
            self._process = subprocess.Popen(
                args,
                stdin=subprocess.PIPE,
                stdout=subprocess.PIPE,
                stderr=subprocess.PIPE,
                bufsize=0,
                cwd=cwd,
                env=env,
                creationflags=creationflags,
            )
        else:
            if cfg.port > 0:
                args.extend(["--port", str(cfg.port)])
            self._process = subprocess.Popen(
                args,
                stdin=subprocess.DEVNULL,
                stdout=subprocess.PIPE,
                stderr=subprocess.PIPE,
                cwd=cwd,
                env=env,
                creationflags=creationflags,
            )

        # For stdio mode, we're ready immediately
        if cfg.use_stdio:
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
        except TimeoutError:
            raise RuntimeError("Timeout waiting for CLI server to start")

    async def _connect_to_server(self) -> None:
        """
        Connect to the CLI server via the configured transport.

        Uses either stdio or TCP based on the client configuration.

        Raises:
            RuntimeError: If the connection fails.
        """
        use_stdio = isinstance(self._config, SubprocessConfig) and self._config.use_stdio
        if use_stdio:
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
        self._client.on_close = lambda: setattr(self, "_state", "disconnected")
        self._rpc = ServerRpc(self._client)

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
        # Protocol v3 servers send tool calls / permission requests as broadcast events.
        # Protocol v2 servers use the older tool.call / permission.request RPC model.
        # We always register v2 adapters because handlers are set up before version
        # negotiation; a v3 server will simply never send these requests.
        self._client.set_request_handler("tool.call", self._handle_tool_call_request_v2)
        self._client.set_request_handler("permission.request", self._handle_permission_request_v2)
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
        self._client.on_close = lambda: setattr(self, "_state", "disconnected")
        self._rpc = ServerRpc(self._client)

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
        # Protocol v3 servers send tool calls / permission requests as broadcast events.
        # Protocol v2 servers use the older tool.call / permission.request RPC model.
        # We always register v2 adapters; a v3 server will simply never send these requests.
        self._client.set_request_handler("tool.call", self._handle_tool_call_request_v2)
        self._client.set_request_handler("permission.request", self._handle_permission_request_v2)
        self._client.set_request_handler("userInput.request", self._handle_user_input_request)
        self._client.set_request_handler("hooks.invoke", self._handle_hooks_invoke)

        # Start listening for messages
        loop = asyncio.get_running_loop()
        self._client.start(loop)

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

    # ========================================================================
    # Protocol v2 backward-compatibility adapters
    # ========================================================================

    async def _handle_tool_call_request_v2(self, params: dict) -> dict:
        """Handle a v2-style tool.call RPC request from the server."""
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
            return {
                "result": {
                    "textResultForLlm": (
                        f"Tool '{tool_name}' is not supported by this client instance."
                    ),
                    "resultType": "failure",
                    "error": f"tool '{tool_name}' not supported",
                    "toolTelemetry": {},
                }
            }

        arguments = params.get("arguments")
        invocation = ToolInvocation(
            session_id=session_id,
            tool_call_id=tool_call_id,
            tool_name=tool_name,
            arguments=arguments,
        )

        tp = params.get("traceparent")
        ts = params.get("tracestate")

        try:
            with trace_context(tp, ts):
                result = handler(invocation)
                if inspect.isawaitable(result):
                    result = await result

            tool_result: ToolResult = result  # type: ignore[assignment]
            return {
                "result": {
                    "textResultForLlm": tool_result.text_result_for_llm,
                    "resultType": tool_result.result_type,
                    "error": tool_result.error,
                    "toolTelemetry": tool_result.tool_telemetry or {},
                }
            }
        except Exception as exc:
            return {
                "result": {
                    "textResultForLlm": (
                        "Invoking this tool produced an error."
                        " Detailed information is not available."
                    ),
                    "resultType": "failure",
                    "error": str(exc),
                    "toolTelemetry": {},
                }
            }

    async def _handle_permission_request_v2(self, params: dict) -> dict:
        """Handle a v2-style permission.request RPC request from the server."""
        session_id = params.get("sessionId")
        permission_request = params.get("permissionRequest")

        if not session_id or not permission_request:
            raise ValueError("invalid permission request payload")

        with self._sessions_lock:
            session = self._sessions.get(session_id)
        if not session:
            raise ValueError(f"unknown session {session_id}")

        try:
            perm_request = PermissionRequest.from_dict(permission_request)
            result = await session._handle_permission_request(perm_request)
            if result.kind == "no-result":
                raise ValueError(NO_RESULT_PERMISSION_V2_ERROR)
            result_payload: dict = {"kind": result.kind}
            if result.rules is not None:
                result_payload["rules"] = result.rules
            if result.feedback is not None:
                result_payload["feedback"] = result.feedback
            if result.message is not None:
                result_payload["message"] = result.message
            if result.path is not None:
                result_payload["path"] = result.path
            return {"result": result_payload}
        except ValueError as exc:
            if str(exc) == NO_RESULT_PERMISSION_V2_ERROR:
                raise
            return {
                "result": {
                    "kind": "denied-no-approval-rule-and-could-not-request-from-user",
                }
            }
        except Exception:  # pylint: disable=broad-except
            return {
                "result": {
                    "kind": "denied-no-approval-rule-and-could-not-request-from-user",
                }
            }
