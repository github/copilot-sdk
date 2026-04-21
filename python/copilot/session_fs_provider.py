# --------------------------------------------------------------------------------------------
#  Copyright (c) Microsoft Corporation. All rights reserved.
# --------------------------------------------------------------------------------------------

"""Idiomatic base class for session filesystem providers.

Subclasses override the abstract methods using standard Python patterns:
raise on error, return values directly.  The :func:`create_session_fs_adapter`
function wraps a provider into the generated :class:`SessionFsHandler`
protocol expected by the SDK, converting exceptions into
:class:`SessionFSError` results.

Errors whose ``errno`` matches :data:`errno.ENOENT` are mapped to the
``ENOENT`` error code; all others map to ``UNKNOWN``.
"""

from __future__ import annotations

import abc
import errno
from collections.abc import Sequence
from dataclasses import dataclass
from datetime import datetime

from .generated.rpc import (
    SessionFSError,
    SessionFSErrorCode,
    SessionFSExistsResult,
    SessionFsHandler,
    SessionFSReaddirResult,
    SessionFSReaddirWithTypesEntry,
    SessionFSReaddirWithTypesResult,
    SessionFSReadFileResult,
    SessionFSStatResult,
)


@dataclass
class SessionFsFileInfo:
    """File metadata returned by :meth:`SessionFsProvider.stat`."""

    is_file: bool
    is_directory: bool
    size: int
    mtime: datetime
    birthtime: datetime


class SessionFsProvider(abc.ABC):
    """Abstract base class for session filesystem providers.

    Subclasses implement the abstract methods below using idiomatic Python:
    raise exceptions on errors and return values directly.  Use
    :func:`create_session_fs_adapter` to wrap a provider into the RPC
    handler protocol.
    """

    @abc.abstractmethod
    async def read_file(self, path: str) -> str:
        """Read the full content of a file.  Raise if the file does not exist."""

    @abc.abstractmethod
    async def write_file(self, path: str, content: str, mode: int | None = None) -> None:
        """Write *content* to a file, creating parent directories if needed."""

    @abc.abstractmethod
    async def append_file(self, path: str, content: str, mode: int | None = None) -> None:
        """Append *content* to a file, creating parent directories if needed."""

    @abc.abstractmethod
    async def exists(self, path: str) -> bool:
        """Return whether *path* exists."""

    @abc.abstractmethod
    async def stat(self, path: str) -> SessionFsFileInfo:
        """Return metadata for *path*.  Raise if it does not exist."""

    @abc.abstractmethod
    async def mkdir(self, path: str, recursive: bool, mode: int | None = None) -> None:
        """Create a directory.  If *recursive* is ``True``, create parents."""

    @abc.abstractmethod
    async def readdir(self, path: str) -> list[str]:
        """List entry names in a directory.  Raise if it does not exist."""

    @abc.abstractmethod
    async def readdir_with_types(self, path: str) -> Sequence[SessionFSReaddirWithTypesEntry]:
        """List entries with type info.  Raise if the directory does not exist."""

    @abc.abstractmethod
    async def rm(self, path: str, recursive: bool, force: bool) -> None:
        """Remove a file or directory."""

    @abc.abstractmethod
    async def rename(self, src: str, dest: str) -> None:
        """Rename / move a file or directory."""


def create_session_fs_adapter(provider: SessionFsProvider) -> SessionFsHandler:
    """Wrap a :class:`SessionFsProvider` into a :class:`SessionFsHandler`.

    The adapter catches exceptions thrown by the provider and converts them
    into :class:`SessionFSError` results expected by the runtime.
    """
    return _SessionFsAdapter(provider)


class _SessionFsAdapter:
    """Internal adapter that bridges SessionFsProvider → SessionFsHandler."""

    def __init__(self, provider: SessionFsProvider) -> None:
        self._p = provider

    async def read_file(self, params: object) -> SessionFSReadFileResult:
        try:
            content = await self._p.read_file(params.path)  # type: ignore[attr-defined]
            return SessionFSReadFileResult.from_dict({"content": content})
        except Exception as exc:
            err = _to_session_fs_error(exc)
            return SessionFSReadFileResult.from_dict({"content": "", "error": err.to_dict()})

    async def write_file(self, params: object) -> SessionFSError | None:
        try:
            await self._p.write_file(params.path, params.content, getattr(params, "mode", None))  # type: ignore[attr-defined]
            return None
        except Exception as exc:
            return _to_session_fs_error(exc)

    async def append_file(self, params: object) -> SessionFSError | None:
        try:
            await self._p.append_file(params.path, params.content, getattr(params, "mode", None))  # type: ignore[attr-defined]
            return None
        except Exception as exc:
            return _to_session_fs_error(exc)

    async def exists(self, params: object) -> SessionFSExistsResult:
        try:
            result = await self._p.exists(params.path)  # type: ignore[attr-defined]
            return SessionFSExistsResult.from_dict({"exists": result})
        except Exception:
            return SessionFSExistsResult.from_dict({"exists": False})

    async def stat(self, params: object) -> SessionFSStatResult:
        try:
            info = await self._p.stat(params.path)  # type: ignore[attr-defined]
            return SessionFSStatResult(
                is_file=info.is_file,
                is_directory=info.is_directory,
                size=info.size,
                mtime=info.mtime,
                birthtime=info.birthtime,
            )
        except Exception as exc:
            now = datetime.now(datetime.UTC)  # type: ignore[attr-defined]  # ty doesn't resolve datetime.UTC (added in 3.11)
            err = _to_session_fs_error(exc)
            return SessionFSStatResult(
                is_file=False,
                is_directory=False,
                size=0,
                mtime=now,
                birthtime=now,
                error=err,
            )

    async def mkdir(self, params: object) -> SessionFSError | None:
        try:
            await self._p.mkdir(
                params.path,  # type: ignore[attr-defined]
                getattr(params, "recursive", False),
                getattr(params, "mode", None),
            )
            return None
        except Exception as exc:
            return _to_session_fs_error(exc)

    async def readdir(self, params: object) -> SessionFSReaddirResult:
        try:
            entries = await self._p.readdir(params.path)  # type: ignore[attr-defined]
            return SessionFSReaddirResult.from_dict({"entries": entries})
        except Exception as exc:
            err = _to_session_fs_error(exc)
            return SessionFSReaddirResult.from_dict({"entries": [], "error": err.to_dict()})

    async def readdir_with_types(self, params: object) -> SessionFSReaddirWithTypesResult:
        try:
            entries = await self._p.readdir_with_types(params.path)  # type: ignore[attr-defined]
            return SessionFSReaddirWithTypesResult(entries=list(entries))
        except Exception as exc:
            err = _to_session_fs_error(exc)
            return SessionFSReaddirWithTypesResult.from_dict(
                {"entries": [], "error": err.to_dict()}
            )

    async def rm(self, params: object) -> SessionFSError | None:
        try:
            await self._p.rm(
                params.path,  # type: ignore[attr-defined]
                getattr(params, "recursive", False),
                getattr(params, "force", False),
            )
            return None
        except Exception as exc:
            return _to_session_fs_error(exc)

    async def rename(self, params: object) -> SessionFSError | None:
        try:
            await self._p.rename(params.src, params.dest)  # type: ignore[attr-defined]
            return None
        except Exception as exc:
            return _to_session_fs_error(exc)


def _to_session_fs_error(exc: Exception) -> SessionFSError:
    code = SessionFSErrorCode.ENOENT if _is_enoent(exc) else SessionFSErrorCode.UNKNOWN
    return SessionFSError(code=code, message=str(exc))


def _is_enoent(exc: Exception) -> bool:
    if isinstance(exc, FileNotFoundError):
        return True
    if isinstance(exc, OSError) and exc.errno == errno.ENOENT:
        return True
    return False
