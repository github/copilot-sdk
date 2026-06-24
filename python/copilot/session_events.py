"""Public re-export of the session event types.

These types are auto-generated from the Copilot CLI session-events schema. This
module is the stable public access point so callers can write
``copilot.session_events.AssistantMessageData`` without depending on the
internal ``copilot.generated`` package layout.
"""

from .generated.session_events import *  # noqa: F401, F403
from .generated.session_events import __all__  # noqa: F401
