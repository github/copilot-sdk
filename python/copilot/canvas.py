"""
Canvas declarations, provider callbacks, and host-side canvas RPC types.

The Copilot CLI runtime sends inbound canvas JSON-RPC requests to any session
that declares canvases. The SDK forwards every such request to a single
user-supplied :class:`CanvasHandler`; multiplexing across multiple declared
canvases is the implementor's responsibility (for example by switching on
``ctx.canvas_id``).
"""

from __future__ import annotations

from abc import ABC, abstractmethod
from dataclasses import dataclass
from typing import Any

from .generated.rpc import (
    CanvasAction,
    CanvasHostContext,
    CanvasHostContextCapabilities,
    CanvasJsonSchema,
    CanvasProviderCloseRequest,
    CanvasProviderInvokeActionRequest,
    CanvasProviderOpenRequest,
    CanvasProviderOpenResult,
    OpenCanvasInstance,
)

__all__ = [
    "CanvasAction",
    "CanvasActionContext",
    "CanvasDeclaration",
    "CanvasError",
    "CanvasHandler",
    "CanvasHostContext",
    "CanvasHostContextCapabilities",
    "CanvasJsonSchema",
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
    """Declarative metadata for a single canvas, sent on create/resume."""

    id: str
    """Canvas identifier, unique within the declaring connection."""

    display_name: str
    """Human-readable name shown in host UI and canvas pickers."""

    description: str
    """Short description shown to the agent in canvas catalogs."""

    input_schema: CanvasJsonSchema | None = None
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


CanvasOpenResponse = CanvasProviderOpenResult
CanvasOpenContext = CanvasProviderOpenRequest
CanvasActionContext = CanvasProviderInvokeActionRequest
CanvasLifecycleContext = CanvasProviderCloseRequest


class CanvasError(Exception):
    """Structured error returned from canvas handlers."""

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
    """Provider-side canvas lifecycle handler."""

    @abstractmethod
    async def on_open(self, ctx: CanvasOpenContext) -> CanvasOpenResponse:
        """Open a new canvas instance.

        May raise :class:`CanvasError` to surface a structured failure to
        the host.
        """

    async def on_close(self, ctx: CanvasLifecycleContext) -> None:
        """Canvas was closed by the user or agent. Default: no-op."""

    async def on_action(self, ctx: CanvasActionContext) -> Any:
        """Handle a non-lifecycle action declared by the canvas."""
        raise CanvasError.no_handler()
