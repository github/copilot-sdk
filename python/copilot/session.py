"""
Copilot Session - represents a single conversation session with the Copilot CLI.

This module provides the CopilotSession class for managing individual
conversation sessions with the Copilot CLI.
"""

import asyncio
import inspect
import threading
from typing import Any, Callable, Optional

from .generated.session_events import SessionEvent, SessionEventType, session_event_from_dict
from .types import (
    MessageOptions,
    PermissionHandler,
    SessionHooks,
    Tool,
    ToolHandler,
    UserInputHandler,
    UserInputRequest,
    UserInputResponse,
)
from .types import (
    SessionEvent as SessionEventTypeAlias,
)


class CopilotSession:
    """
    Represents a single conversation session with the Copilot CLI.

    A session maintains conversation state, handles events, and manages tool execution.
    Sessions are created via :meth:`CopilotClient.create_session` or resumed via
    :meth:`CopilotClient.resume_session`.

    The session provides methods to send messages, subscribe to events, retrieve
    conversation history, and manage the session lifecycle.

    Attributes:
        session_id: The unique identifier for this session.

    Example:
        >>> async with await client.create_session() as session:
        ...     # Subscribe to events
        ...     unsubscribe = session.on(lambda event: print(event.type))
        ...
        ...     # Send a message
        ...     await session.send({"prompt": "Hello, world!"})
        ...
        ...     # Clean up
        ...     unsubscribe()
    """

    def __init__(self, session_id: str, client: Any, workspace_path: Optional[str] = None):
        """
        Initialize a new CopilotSession.

        Note:
            This constructor is internal. Use :meth:`CopilotClient.create_session`
            to create sessions.

        Args:
            session_id: The unique identifier for this session.
            client: The internal client connection to the Copilot CLI.
            workspace_path: Path to the session workspace directory
                (when infinite sessions enabled).
        """
        self.session_id = session_id
        self._client = client
        self._workspace_path = workspace_path
        self._event_handlers: set[Callable[[SessionEvent], None]] = set()
        self._event_handlers_lock = threading.Lock()
        self._tool_handlers: dict[str, ToolHandler] = {}
        self._tool_handlers_lock = threading.Lock()
        self._permission_handler: Optional[PermissionHandler] = None
        self._permission_handler_lock = threading.Lock()
        self._user_input_handler: Optional[UserInputHandler] = None
        self._user_input_handler_lock = threading.Lock()
        self._hooks: Optional[SessionHooks] = None
        self._hooks_lock = threading.Lock()

    @property
    def workspace_path(self) -> Optional[str]:
        """
        Path to the session workspace directory when infinite sessions are enabled.

        Contains checkpoints/, plan.md, and files/ subdirectories.
        None if infinite sessions are disabled.
        """
        return self._workspace_path

    async def send(self, options: MessageOptions) -> str:
        """
        Send a message to this session and wait for the response.

        The message is processed asynchronously. Subscribe to events via :meth:`on`
        to receive streaming responses and other session events.

        Args:
            options: Message options including the prompt and optional attachments.
                Must contain a "prompt" key with the message text. Can optionally
                include "attachments" and "mode" keys.

        Returns:
            The message ID of the response, which can be used to correlate events.

        Raises:
            Exception: If the session has been destroyed or the connection fails.

        Example:
            >>> message_id = await session.send({
            ...     "prompt": "Explain this code",
            ...     "attachments": [{"type": "file", "path": "./src/main.py"}]
            ... })
        """
        response = await self._client.request(
            "session.send",
            {
                "sessionId": self.session_id,
                "prompt": options["prompt"],
                "attachments": options.get("attachments"),
                "mode": options.get("mode"),
            },
        )
        return response["messageId"]

    async def send_and_wait(
        self, options: MessageOptions, timeout: Optional[float] = None
    ) -> Optional[SessionEvent]:
        """
        Send a message to this session and wait until the session becomes idle.

        This is a convenience method that combines :meth:`send` with waiting for
        the session.idle event. Use this when you want to block until the assistant
        has finished processing the message.

        Events are still delivered to handlers registered via :meth:`on` while waiting.

        Args:
            options: Message options including the prompt and optional attachments.
            timeout: Timeout in seconds (default: 60). Controls how long to wait;
                does not abort in-flight agent work.

        Returns:
            The final assistant message event, or None if none was received.

        Raises:
            asyncio.TimeoutError: If the timeout is reached before session becomes idle.
            Exception: If the session has been destroyed or the connection fails.

        Example:
            >>> response = await session.send_and_wait({"prompt": "What is 2+2?"})
            >>> if response:
            ...     print(response.data.content)
        """
        effective_timeout = timeout if timeout is not None else 60.0

        idle_event = asyncio.Event()
        error_event: Optional[Exception] = None
        last_assistant_message: Optional[SessionEvent] = None

        def handler(event: SessionEventTypeAlias) -> None:
            nonlocal last_assistant_message, error_event
            if event.type == SessionEventType.ASSISTANT_MESSAGE:
                last_assistant_message = event
            elif event.type == SessionEventType.SESSION_IDLE:
                idle_event.set()
            elif event.type == SessionEventType.SESSION_ERROR:
                error_event = Exception(
                    f"Session error: {getattr(event.data, 'message', str(event.data))}"
                )
                idle_event.set()

        unsubscribe = self.on(handler)
        try:
            await self.send(options)
            await asyncio.wait_for(idle_event.wait(), timeout=effective_timeout)
            if error_event:
                raise error_event
            return last_assistant_message
        except asyncio.TimeoutError:
            raise asyncio.TimeoutError(
                f"Timeout after {effective_timeout}s waiting for session.idle"
            )
        finally:
            unsubscribe()

    def on(self, handler: Callable[[SessionEvent], None]) -> Callable[[], None]:
        """
        Subscribe to events from this session.

        Events include assistant messages, tool executions, errors, and session
        state changes. Multiple handlers can be registered and will all receive
        events.

        Args:
            handler: A callback function that receives session events. The function
                takes a single :class:`SessionEvent` argument and returns None.

        Returns:
            A function that, when called, unsubscribes the handler.

        Example:
            >>> def handle_event(event):
            ...     if event.type == "assistant.message":
            ...         print(f"Assistant: {event.data.content}")
            ...     elif event.type == "session.error":
            ...         print(f"Error: {event.data.message}")
            ...
            >>> unsubscribe = session.on(handle_event)
            ...
            >>> # Later, to stop receiving events:
            >>> unsubscribe()
        """
        with self._event_handlers_lock:
            self._event_handlers.add(handler)

        def unsubscribe():
            with self._event_handlers_lock:
                self._event_handlers.discard(handler)

        return unsubscribe

    def _dispatch_event(self, event: SessionEvent) -> None:
        """
        Dispatch an event to all registered handlers.

        Note:
            This method is internal and should not be called directly.

        Args:
            event: The session event to dispatch to all handlers.
        """
        with self._event_handlers_lock:
            handlers = list(self._event_handlers)

        for handler in handlers:
            try:
                handler(event)
            except Exception as e:
                print(f"Error in session event handler: {e}")

    def _register_tools(self, tools: Optional[list[Tool]]) -> None:
        """
        Register custom tool handlers for this session.

        Tools allow the assistant to execute custom functions. When the assistant
        invokes a tool, the corresponding handler is called with the tool arguments.

        Note:
            This method is internal. Tools are typically registered when creating
            a session via :meth:`CopilotClient.create_session`.

        Args:
            tools: A list of Tool objects with their handlers, or None to clear
                all registered tools.
        """
        with self._tool_handlers_lock:
            self._tool_handlers.clear()
            if not tools:
                return
            for tool in tools:
                if not tool.name or not tool.handler:
                    continue
                self._tool_handlers[tool.name] = tool.handler

    def _get_tool_handler(self, name: str) -> Optional[ToolHandler]:
        """
        Retrieve a registered tool handler by name.

        Note:
            This method is internal and should not be called directly.

        Args:
            name: The name of the tool to retrieve.

        Returns:
            The tool handler if found, or None if no handler is registered
            for the given name.
        """
        with self._tool_handlers_lock:
            return self._tool_handlers.get(name)

    def _register_permission_handler(self, handler: Optional[PermissionHandler]) -> None:
        """
        Register a handler for permission requests.

        When the assistant needs permission to perform certain actions (e.g.,
        file operations), this handler is called to approve or deny the request.

        Note:
            This method is internal. Permission handlers are typically registered
            when creating a session via :meth:`CopilotClient.create_session`.

        Args:
            handler: The permission handler function, or None to remove the handler.
        """
        with self._permission_handler_lock:
            self._permission_handler = handler

    async def _handle_permission_request(self, request: dict) -> dict:
        """
        Handle a permission request from the Copilot CLI.

        Note:
            This method is internal and should not be called directly.

        Args:
            request: The permission request data from the CLI.

        Returns:
            A dictionary containing the permission decision with a "kind" key.
        """
        with self._permission_handler_lock:
            handler = self._permission_handler

        if not handler:
            # No handler registered, deny permission
            return {"kind": "denied-no-approval-rule-and-could-not-request-from-user"}

        try:
            result = handler(request, {"session_id": self.session_id})
            if inspect.isawaitable(result):
                result = await result
            return result
        except Exception:  # pylint: disable=broad-except
            # Handler failed, deny permission
            return {"kind": "denied-no-approval-rule-and-could-not-request-from-user"}

    def _register_user_input_handler(self, handler: Optional[UserInputHandler]) -> None:
        """
        Register a handler for user input requests.

        When the agent needs input from the user (via ask_user tool),
        this handler is called to provide the response.

        Note:
            This method is internal. User input handlers are typically registered
            when creating a session via :meth:`CopilotClient.create_session`.

        Args:
            handler: The user input handler function, or None to remove the handler.
        """
        with self._user_input_handler_lock:
            self._user_input_handler = handler

    async def _handle_user_input_request(self, request: dict) -> UserInputResponse:
        """
        Handle a user input request from the Copilot CLI.

        Note:
            This method is internal and should not be called directly.

        Args:
            request: The user input request data from the CLI.

        Returns:
            A dictionary containing the user's response.
        """
        with self._user_input_handler_lock:
            handler = self._user_input_handler

        if not handler:
            raise RuntimeError("User input requested but no handler registered")

        try:
            result = handler(
                UserInputRequest(
                    question=request.get("question", ""),
                    choices=request.get("choices"),
                    allowFreeform=request.get("allowFreeform", True),
                ),
                {"session_id": self.session_id},
            )
            if inspect.isawaitable(result):
                result = await result
            return result
        except Exception:
            raise

    def _register_hooks(self, hooks: Optional[SessionHooks]) -> None:
        """
        Register hook handlers for session lifecycle events.

        Hooks allow custom logic to be executed at various points during
        the session lifecycle (before/after tool use, session start/end, etc.).

        Note:
            This method is internal. Hooks are typically registered
            when creating a session via :meth:`CopilotClient.create_session`.

        Args:
            hooks: The hooks configuration object, or None to remove all hooks.
        """
        with self._hooks_lock:
            self._hooks = hooks

    async def _handle_hooks_invoke(self, hook_type: str, input_data: Any) -> Any:
        """
        Handle a hooks invocation from the Copilot CLI.

        Note:
            This method is internal and should not be called directly.

        Args:
            hook_type: The type of hook being invoked.
            input_data: The input data for the hook.

        Returns:
            The hook output, or None if no handler is registered.
        """
        with self._hooks_lock:
            hooks = self._hooks

        if not hooks:
            return None

        handler_map = {
            "preToolUse": hooks.get("on_pre_tool_use"),
            "postToolUse": hooks.get("on_post_tool_use"),
            "userPromptSubmitted": hooks.get("on_user_prompt_submitted"),
            "sessionStart": hooks.get("on_session_start"),
            "sessionEnd": hooks.get("on_session_end"),
            "errorOccurred": hooks.get("on_error_occurred"),
        }

        handler = handler_map.get(hook_type)
        if not handler:
            return None

        try:
            result = handler(input_data, {"session_id": self.session_id})
            if inspect.isawaitable(result):
                result = await result
            return result
        except Exception:  # pylint: disable=broad-except
            # Hook failed, return None
            return None

    async def get_messages(self) -> list[SessionEvent]:
        """
        Retrieve all events and messages from this session's history.

        This returns the complete conversation history including user messages,
        assistant responses, tool executions, and other session events.

        Returns:
            A list of all session events in chronological order.

        Raises:
            Exception: If the session has been destroyed or the connection fails.

        Example:
            >>> events = await session.get_messages()
            >>> for event in events:
            ...     if event.type == "assistant.message":
            ...         print(f"Assistant: {event.data.content}")
        """
        response = await self._client.request("session.getMessages", {"sessionId": self.session_id})
        # Convert dict events to SessionEvent objects
        events_dicts = response["events"]
        return [session_event_from_dict(event_dict) for event_dict in events_dicts]

    async def destroy(self) -> None:
        """
        Destroy this session and release all associated resources.

        After calling this method, the session can no longer be used. All event
        handlers and tool handlers are cleared. To continue the conversation,
        use :meth:`CopilotClient.resume_session` with the session ID.

        Raises:
            Exception: If the connection fails.

        Example:
            >>> # Clean up when done
            >>> await session.destroy()
        """
        await self._client.request("session.destroy", {"sessionId": self.session_id})
        with self._event_handlers_lock:
            self._event_handlers.clear()
        with self._tool_handlers_lock:
            self._tool_handlers.clear()
        with self._permission_handler_lock:
            self._permission_handler = None

    async def abort(self) -> None:
        """
        Abort the currently processing message in this session.

        Use this to cancel a long-running request. The session remains valid
        and can continue to be used for new messages.

        Raises:
            Exception: If the session has been destroyed or the connection fails.

        Example:
            >>> import asyncio
            >>>
            >>> # Start a long-running request
            >>> task = asyncio.create_task(
            ...     session.send({"prompt": "Write a very long story..."})
            ... )
            >>>
            >>> # Abort after 5 seconds
            >>> await asyncio.sleep(5)
            >>> await session.abort()
        """
        await self._client.request("session.abort", {"sessionId": self.session_id})
