"""
Canvas declarations, provider callbacks, and host-side canvas RPC types.

The Copilot CLI runtime sends inbound JSON-RPC requests (``canvas.open``,
``canvas.close``, ``canvas.action.invoke``) to any session that declares
canvases. The SDK forwards every such request to a single user-supplied
:class:`CanvasHandler`; multiplexing across multiple declared canvases is
the implementor's responsibility (e.g. by switching on
:attr:`CanvasOpenContext.canvas_id`).
"""

from __future__ import annotations

from abc import ABC, abstractmethod
from dataclasses import dataclass, field
from typing import Any

from .generated.rpc import CanvasAction, OpenCanvasInstance

__all__ = [
    "CanvasAction",
    "CanvasActionContext",
    "CanvasDeclaration",
    "CanvasError",
    "CanvasHandler",
    "CanvasHostCapabilities",
    "CanvasHostContext",
    "CanvasLifecycleContext",
    "CanvasOpenContext",
    "CanvasOpenResponse",
    "ExtensionInfo",
    "OpenCanvasInstance",
]


@dataclass
class ExtensionInfo:
    """Stable extension identity for session participants that provide canvases.

    Serializes to ``{"source": ..., "name": ...}`` on the wire.
    """

    source: str
    """Extension namespace/source, e.g. ``"github-app"``."""

    name: str
    """Stable provider name within the source namespace."""

    def to_dict(self) -> dict[str, Any]:
        return {"source": self.source, "name": self.name}


@dataclass
class CanvasDeclaration:
    """Declarative metadata for a single canvas, sent on
    ``session.create`` / ``session.resume``.
    """

    id: str
    """Canvas identifier, unique within the declaring connection."""

    display_name: str
    """Human-readable name shown in host UI and canvas pickers."""

    description: str
    """Short, single-sentence description shown to the agent in canvas catalogs."""

    input_schema: dict[str, Any] | None = None
    """JSON Schema for the ``input`` payload accepted by ``canvas.open``."""

    actions: list[CanvasAction] | None = None
    """Agent-callable actions this canvas exposes."""

    def to_dict(self) -> dict[str, Any]:
        result: dict[str, Any] = {
            "id": self.id,
            "displayName": self.display_name,
            "description": self.description,
        }
        if self.input_schema is not None:
            result["inputSchema"] = self.input_schema
        if self.actions is not None:
            result["actions"] = [action.to_dict() for action in self.actions]
        return result


@dataclass
class CanvasOpenResponse:
    """Response returned from :meth:`CanvasHandler.on_open`."""

    url: str | None = None
    """URL the host should render. Optional for canvases with no visual surface."""

    title: str | None = None
    """Provider-supplied title shown in host chrome."""

    status: str | None = None
    """Provider-supplied status text shown in host chrome."""

    def to_dict(self) -> dict[str, Any]:
        result: dict[str, Any] = {}
        if self.url is not None:
            result["url"] = self.url
        if self.title is not None:
            result["title"] = self.title
        if self.status is not None:
            result["status"] = self.status
        return result


@dataclass
class CanvasHostCapabilities:
    """Host capability details passed to canvas provider callbacks."""

    canvases: bool = False
    """Whether the host supports canvas rendering."""

    @staticmethod
    def from_dict(obj: Any) -> CanvasHostCapabilities:
        if not isinstance(obj, dict):
            return CanvasHostCapabilities()
        return CanvasHostCapabilities(canvases=bool(obj.get("canvases", False)))


@dataclass
class CanvasHostContext:
    """Host capabilities passed to canvas provider callbacks."""

    capabilities: CanvasHostCapabilities = field(default_factory=CanvasHostCapabilities)
    """Host capability details."""

    @staticmethod
    def from_dict(obj: Any) -> CanvasHostContext:
        if not isinstance(obj, dict):
            return CanvasHostContext()
        return CanvasHostContext(
            capabilities=CanvasHostCapabilities.from_dict(obj.get("capabilities"))
        )


@dataclass
class CanvasOpenContext:
    """Context handed to :meth:`CanvasHandler.on_open`."""

    session_id: str
    """Session that requested the canvas."""

    extension_id: str
    """Owning provider identifier."""

    canvas_id: str
    """Canvas id from the declaring :class:`CanvasDeclaration`."""

    instance_id: str
    """Stable instance id supplied by the runtime."""

    input: Any
    """Validated input payload."""

    host: CanvasHostContext | None = None
    """Host capabilities supplied by the runtime."""


@dataclass
class CanvasActionContext:
    """Context handed to :meth:`CanvasHandler.on_action`."""

    session_id: str
    """Session that invoked the action."""

    extension_id: str
    """Owning provider identifier."""

    canvas_id: str
    """Canvas id targeted by the action."""

    instance_id: str
    """Instance id targeted by the action."""

    action_name: str
    """Action name from :attr:`CanvasAction.name`."""

    input: Any
    """Validated input payload."""

    host: CanvasHostContext | None = None
    """Host capabilities supplied by the runtime."""


@dataclass
class CanvasLifecycleContext:
    """Context handed to a canvas's close lifecycle hook."""

    session_id: str
    """Session owning the canvas instance."""

    extension_id: str
    """Owning provider identifier."""

    canvas_id: str
    """Canvas id from the declaring :class:`CanvasDeclaration`."""

    instance_id: str
    """Instance id this lifecycle event applies to."""

    host: CanvasHostContext | None = None
    """Host capabilities supplied by the runtime."""


class CanvasError(Exception):
    """Structured error returned from canvas handlers.

    The serialized envelope is ``{"code": ..., "message": ...}``. The SDK
    surfaces this through the JSON-RPC error's ``data`` field while sending
    a standard ``-32603`` (internal error) wire code.
    """

    def __init__(self, code: str, message: str) -> None:
        self.code = code
        self.message = message
        super().__init__(f"{code}: {message}")

    def to_envelope(self) -> dict[str, str]:
        return {"code": self.code, "message": self.message}

    @classmethod
    def no_handler(cls) -> CanvasError:
        """Default error returned when a custom action has no handler."""
        return cls(
            "canvas_action_no_handler",
            "No handler implemented for this canvas action",
        )

    @classmethod
    def handler_unset(cls) -> CanvasError:
        """Error returned when a canvas RPC arrives but no handler is installed."""
        return cls(
            "canvas_handler_unset",
            "No CanvasHandler installed on this session; "
            "install one via SessionConfig.canvas_handler before creating the session.",
        )


class CanvasHandler(ABC):
    """Provider-side canvas lifecycle handler.

    A session installs a single :class:`CanvasHandler` via the
    ``canvas_handler=`` argument to
    :meth:`copilot.CopilotClient.create_session` /
    :meth:`copilot.CopilotClient.resume_session`. The handler receives every
    inbound ``canvas.open`` / ``canvas.close`` / ``canvas.action.invoke``
    JSON-RPC request the runtime issues for this session and decides —
    typically by inspecting :attr:`CanvasOpenContext.canvas_id` — which
    application-side canvas should handle the call.

    The SDK does not maintain a per-canvas registry; multiplexing across
    declared canvases is the implementor's responsibility.
    """

    @abstractmethod
    async def on_open(self, ctx: CanvasOpenContext) -> CanvasOpenResponse:
        """Open a new canvas instance.

        May raise :class:`CanvasError` to surface a structured failure to
        the host.
        """

    async def on_close(self, ctx: CanvasLifecycleContext) -> None:
        """Canvas was closed by the user or agent. Default: no-op."""

    async def on_action(self, ctx: CanvasActionContext) -> Any:
        """Handle a non-lifecycle action declared by the canvas.

        Default raises :meth:`CanvasError.no_handler`.
        """
        raise CanvasError.no_handler()


# ----- Internal helpers for inbound RPC dispatch (not part of the public API). -----


def _open_context_from_params(params: dict[str, Any]) -> CanvasOpenContext:
    return CanvasOpenContext(
        session_id=params["sessionId"],
        extension_id=params["extensionId"],
        canvas_id=params["canvasId"],
        instance_id=params["instanceId"],
        input=params.get("input"),
        host=CanvasHostContext.from_dict(params.get("host")) if params.get("host") else None,
    )


def _lifecycle_context_from_params(params: dict[str, Any]) -> CanvasLifecycleContext:
    return CanvasLifecycleContext(
        session_id=params["sessionId"],
        extension_id=params["extensionId"],
        canvas_id=params["canvasId"],
        instance_id=params["instanceId"],
        host=CanvasHostContext.from_dict(params.get("host")) if params.get("host") else None,
    )


def _action_context_from_params(params: dict[str, Any]) -> CanvasActionContext:
    return CanvasActionContext(
        session_id=params["sessionId"],
        extension_id=params["extensionId"],
        canvas_id=params["canvasId"],
        instance_id=params["instanceId"],
        action_name=params["actionName"],
        input=params.get("input"),
        host=CanvasHostContext.from_dict(params.get("host")) if params.get("host") else None,
    )
