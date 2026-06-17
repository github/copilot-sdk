"""Public re-export of the JSON-RPC request/response types.

These types are auto-generated from the Copilot CLI protocol schemas. This
module is the stable public access point so callers can write
``copilot.rpc.SessionUpdateOptionsParams`` without depending on the internal
``copilot.generated`` package layout.
"""

from .generated.rpc import *  # noqa: F401, F403
from .generated.rpc import (
    SessionFsReaddirWithTypesEntryType as SessionFSReaddirWithTypesEntryType,  # noqa: F401
)
from .generated.rpc import __all__  # noqa: F401
