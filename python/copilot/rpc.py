"""Public re-export of the JSON-RPC request/response types.

These types are auto-generated from the Copilot CLI protocol schemas. This
module is the stable public access point so callers can write
``copilot.rpc.SessionUpdateOptionsParams`` without depending on the internal
``copilot._generated`` package layout.
"""

from ._generated.rpc import *  # noqa: F401, F403
from ._generated.rpc import (
    BuiltinToolInputSchemaType as UIElicitationSchemaType,  # noqa: F401
)
from ._generated.rpc import (
    SessionFsReaddirWithTypesEntryType as SessionFSReaddirWithTypesEntryType,  # noqa: F401
)
from ._generated.rpc import __all__ as _generated_all

__all__ = [*_generated_all, "UIElicitationSchemaType"]
