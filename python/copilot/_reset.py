"""Session reset result types."""

from __future__ import annotations

from dataclasses import dataclass
from typing import Any


@dataclass
class ResetSessionResult:
    """Result returned by :meth:`copilot.session.CopilotSession.reset`."""

    previous_session_id: str
    """The session ID that was closed and replaced."""

    session: Any
    """The fresh session created from the supplied reset configuration."""
