# Copyright (c) Microsoft Corporation. All rights reserved.

"""Experimental, connection-owned in-process AHP hosting."""

from __future__ import annotations

import asyncio
import copy
import inspect
import logging
import types
from collections.abc import Awaitable, Callable
from dataclasses import dataclass, field
from typing import TYPE_CHECKING, Any, Union, get_args, get_origin, get_type_hints, is_typeddict
from uuid import uuid4
from weakref import WeakKeyDictionary

from .generated.rpc import (
    HostExitedNotification,
    HostGitHubEnvironmentOptions,
    HostListSessionsResult,
    HostLocalServerOptions,
    HostPublishSessionResult,
    HostStartResult,
)

if TYPE_CHECKING:
    from ._jsonrpc import JsonRpcClient
    from .session import CopilotSession

logger = logging.getLogger(__name__)
AhpHostExit = HostExitedNotification


class _HandoffEnded(RuntimeError):
    pass


@dataclass(frozen=True)
class AhpSessionCreateRequest:
    """Preserve ``config`` when calling ``client.create_session(**config)``.

    Add application prompts, tools and callbacks. ``cancellation_event`` is set
    when participation ends; the SDK still releases any session returned late.
    """

    config: dict[str, Any]
    cancellation_event: asyncio.Event


@dataclass(frozen=True)
class AhpSessionResumeRequest:
    """Resume ``session_id`` with ``**config``, or return its retained original."""

    session_id: str
    config: dict[str, Any]
    cancellation_event: asyncio.Event


@dataclass(frozen=True)
class AhpHost:
    """An experimental AHP listener owned by the client's current connection.

    Disposal joins runtime cleanup without destroying application sessions.
    Reconnecting the client does not transfer this handle to its new connection.
    """

    host_id: str
    url: str | None
    token: str | None
    pid: int | None
    environment_id: str | None
    _rpc: JsonRpcClient = field(repr=False, compare=False)

    async def dispose(self) -> None:
        """Stop and join the listener, including on repeated/concurrent calls."""
        await self._rpc.request("host.dispose", {"hostId": self.host_id})

    async def publish_session(self, session_id: str) -> HostPublishSessionResult:
        """Publish an attached session into the durable compute-scoped catalog."""
        result = await self._rpc.request(
            "host.publishSession", {"hostId": self.host_id, "sessionId": session_id}
        )
        return HostPublishSessionResult.from_dict(result)

    async def list_sessions(self) -> HostListSessionsResult:
        """List all live and dormant catalog sessions advertised by this host."""
        result = await self._rpc.request("host.listSessions", {"hostId": self.host_id})
        return HostListSessionsResult.from_dict(result)

    async def __aenter__(self) -> AhpHost:
        return self

    async def __aexit__(self, *_: object) -> None:
        await self.dispose()


@dataclass(frozen=True)
class AhpHostOptions:
    """Local callbacks and transport settings for experimental AHP hosting.

    Select ``local_server``, ``github_environment``, or both. An empty local
    server selects loopback defaults. GitHub-only hosting has no local URL.
    Factories must return the exact requested session from the owning client.
    Release callbacks receive that same object once per handoff, including late
    results. The SDK never disconnects or destroys it on the application's behalf.
    ``compute_id`` selects a durable catalog across transports; omitted local-only
    identities are persisted by the runtime. It must agree with the GitHub
    environment's compute identity when both are supplied.
    """

    local_server: HostLocalServerOptions | None = None
    github_environment: HostGitHubEnvironmentOptions | None = None
    create_session: Callable[[AhpSessionCreateRequest], Awaitable[CopilotSession]] | None = None
    resume_session: Callable[[AhpSessionResumeRequest], Awaitable[CopilotSession]] | None = None
    on_session_released: Callable[[CopilotSession], Awaitable[None] | None] | None = None
    on_exit: Callable[[AhpHostExit], Awaitable[None] | None] | None = None
    compute_id: str | None = None


@dataclass
class _Handoff:
    host_id: str
    session_id: str
    options: AhpHostOptions
    cancellation: asyncio.Event = field(default_factory=asyncio.Event)
    session: CopilotSession | None = None
    released: bool = False
    configs: WeakKeyDictionary[CopilotSession, dict[str, Any]] = field(
        default_factory=WeakKeyDictionary
    )


def _config_key(name: str, hints: dict[str, Any]) -> str:
    # SDK names normalize acronyms differently from wire names (OAuth, GitHub).
    return next((field for field in hints if field.replace("_", "").lower() == name.lower()), name)


def _config_value(value: Any, annotation: Any) -> Any:
    """Translate typed config fields, preserving arbitrary dictionary keys."""
    origin = get_origin(annotation)
    if origin in (Union, types.UnionType):
        variants = get_args(annotation)
        if isinstance(value, dict):
            for variant in variants:
                if is_typeddict(variant) and all(
                    _config_key(key, get_type_hints(variant)) in get_type_hints(variant)
                    for key in value
                ):
                    return _config_value(value, variant)
            for variant in variants:
                if get_origin(variant) is dict:
                    return _config_value(value, variant)
        for variant in variants:
            if value is not None and variant is not type(None):
                return _config_value(value, variant)
    if isinstance(value, dict):
        if is_typeddict(annotation):
            hints = get_type_hints(annotation)
            result = {}
            for key, item in value.items():
                name = _config_key(key, hints)
                if name not in hints:
                    raise ValueError(f"Unsupported AHP session setting: {key}")
                result[name] = _config_value(item, hints[name])
            return result
        if origin is dict:
            return {
                key: _config_value(item, get_args(annotation)[1]) for key, item in value.items()
            }
        decoder = getattr(annotation, "from_dict", None)
        if callable(decoder):
            return decoder(value)
    if isinstance(value, list) and origin is list:
        return [_config_value(item, get_args(annotation)[0]) for item in value]
    return copy.deepcopy(value)


def _session_config(config: dict[str, Any], method: Callable[..., Any]) -> dict[str, Any]:
    hints = get_type_hints(method)
    result = {}
    for key, value in config.items():
        name = "config_directory" if key == "configDir" else _config_key(key, hints)
        if name not in hints:
            raise ValueError(f"Unsupported AHP session setting: {key}")
        result[name] = _config_value(value, hints[name])
    return result


def _contains(actual: Any, expected: Any) -> bool:
    if isinstance(expected, dict):
        return isinstance(actual, dict) and all(
            key in actual and _contains(actual[key], value) for key, value in expected.items()
        )
    return type(actual) is type(expected) and actual == expected


class _AhpHostManager:
    def __init__(
        self,
        get_session: Callable[[str], CopilotSession | None],
        create_session: Callable[..., Any],
        resume_session: Callable[..., Any],
    ) -> None:
        self._get_session = get_session
        self._create_session = create_session
        self._resume_session = resume_session
        self._hosts: dict[str, AhpHostOptions] = {}
        self._handoffs: dict[str, _Handoff] = {}
        self._tasks: set[asyncio.Task[Any]] = set()

    def capture(self, session: CopilotSession, config: dict[str, Any]) -> None:
        for entry in self._handoffs.values():
            if entry.session_id == session.session_id:
                entry.configs[session] = copy.deepcopy(config)

    async def start(self, rpc: JsonRpcClient, options: AhpHostOptions) -> AhpHost:
        if options.local_server is None and options.github_environment is None:
            raise ValueError("At least one of local_server or github_environment is required")
        host_id = str(uuid4())
        self._hosts[host_id] = options
        params = {
            "hostId": host_id,
            "computeId": options.compute_id,
            "localServer": (
                options.local_server.to_dict() if options.local_server is not None else None
            ),
            "githubEnvironment": (
                options.github_environment.to_dict()
                if options.github_environment is not None
                else None
            ),
            "sessionFactory": True if options.create_session else None,
            "resumeFactory": True if options.resume_session else None,
        }
        startup = asyncio.create_task(
            rpc.request(
                "host.start", {key: value for key, value in params.items() if value is not None}
            )
        )
        try:
            info = HostStartResult.from_dict(await asyncio.shield(startup))
            return AhpHost(info.host_id, info.url, info.token, info.pid, info.environment_id, rpc)
        except asyncio.CancelledError:
            # Settle startup before disposing: the runtime may not have registered
            # the host yet, and canceling the local RPC does not cancel that work.
            async def cleanup() -> None:
                try:
                    try:
                        await startup
                    except Exception:
                        return
                    await rpc.request("host.dispose", {"hostId": host_id})
                except Exception:
                    logger.exception("AHP canceled startup cleanup failed")
                finally:
                    self._release_host(host_id)

            task = asyncio.create_task(cleanup())
            self._tasks.add(task)
            task.add_done_callback(self._tasks.discard)
            raise
        except BaseException:
            self._release_host(host_id)
            raise

    def notification(self, method: str, params: dict[str, Any]) -> bool:
        if method == "host.sessionReleased":
            entry = self._handoffs.get(params["handoffId"])
            if entry is not None and entry.host_id == params["hostId"]:
                self._release(params["handoffId"])
            return True
        if method == "host.exited":
            self._exit(HostExitedNotification.from_dict(params))
            return True
        return False

    def disconnect(self) -> None:
        for host_id in list(self._hosts):
            self._exit(
                HostExitedNotification.from_dict(
                    {
                        "hostId": host_id,
                        "reason": "ownerDisconnected",
                        "error": (
                            "Owner connection closed; runtime cleanup cannot be acknowledged "
                            "on this connection."
                        ),
                    }
                )
            )

    def _exit(self, event: AhpHostExit) -> None:
        options = self._release_host(event.host_id)
        if options is not None and options.on_exit is not None:
            self._notify(options.on_exit, event, "AHP host exit callback failed")

    def _release_host(self, host_id: str) -> AhpHostOptions | None:
        options = self._hosts.pop(host_id, None)
        for handoff_id, entry in list(self._handoffs.items()):
            if entry.host_id == host_id:
                self._release(handoff_id)
        return options

    def _release(self, handoff_id: str) -> None:
        entry = self._handoffs.pop(handoff_id, None)
        if entry is None:
            return
        entry.released = True
        entry.cancellation.set()
        entry.configs.clear()
        self._notify_released(entry)

    def _notify_released(self, entry: _Handoff) -> None:
        session, entry.session = entry.session, None
        if session is not None:
            if entry.options.on_session_released is not None:
                self._notify(
                    entry.options.on_session_released,
                    session,
                    "AHP session release callback failed",
                )

    def _notify(self, callback: Callable[..., Any], value: Any, message: str) -> None:
        async def invoke() -> None:
            try:
                result = callback(value)
                if inspect.isawaitable(result):
                    await result
            except Exception:
                logger.exception(message)

        task = asyncio.create_task(invoke())
        self._tasks.add(task)
        task.add_done_callback(self._tasks.discard)

    async def materialize(self, params: dict[str, Any]) -> dict[str, str]:
        host_id, handoff_id = params["hostId"], params["handoffId"]
        options = self._hosts.get(host_id)
        resume = params.get("resume", False)
        if options is None or handoff_id in self._handoffs:
            raise ValueError("AHP session factory is unavailable or handoff already exists")
        if not (options.resume_session if resume else options.create_session):
            raise ValueError("AHP session factory is unavailable")
        expected = copy.deepcopy(params["config"])
        for public, wire in {
            "suppressResumeEvent": "disableResume",
            "enableExperimentalMode": "isExperimentalMode",
            "enableMcpApps": "requestMcpApps",
        }.items():
            if public in expected:
                expected[wire] = expected.pop(public)
        session_id = expected.get("sessionId")
        if not isinstance(session_id, str) or not session_id:
            raise ValueError("AHP session handoff requires sessionId")
        entry = _Handoff(host_id, session_id, options)
        self._handoffs[handoff_id] = entry

        async def run() -> dict[str, str]:
            try:
                method = self._resume_session if resume else self._create_session
                config = _session_config(params["config"], method)
                if resume:
                    config.pop("session_id")
                    assert options.resume_session is not None
                    session = await options.resume_session(
                        AhpSessionResumeRequest(session_id, config, entry.cancellation)
                    )
                else:
                    assert options.create_session is not None
                    session = await options.create_session(
                        AhpSessionCreateRequest(config, entry.cancellation)
                    )
                entry.session = session
                if entry.released:
                    self._notify_released(entry)
                    raise _HandoffEnded("AHP session handoff ended")
                if session.session_id != session_id or self._get_session(session_id) is not session:
                    raise ValueError(
                        "AHP callback must return the requested session from this client"
                    )
                actual = entry.configs.pop(session, None)
                if (not resume or actual is not None) and not _contains(actual, expected):
                    raise ValueError(
                        "AHP callback must preserve the supplied session configuration"
                    )
                return {"sessionId": session_id}
            except asyncio.CancelledError as exc:
                self._release(handoff_id)
                raise RuntimeError("AHP session factory was cancelled") from exc
            except BaseException:
                self._release(handoff_id)
                raise

        materialized = asyncio.create_task(run())
        self._tasks.add(materialized)

        def completed(task: asyncio.Task[Any]) -> None:
            self._tasks.discard(task)
            if not task.cancelled():
                failure = task.exception()
                if (
                    entry.released
                    and failure is not None
                    and not isinstance(failure, _HandoffEnded)
                ):
                    logger.error(
                        "AHP session factory failed",
                        exc_info=(type(failure), failure, failure.__traceback__),
                    )

        materialized.add_done_callback(completed)
        cancelled = asyncio.create_task(entry.cancellation.wait())
        try:
            await asyncio.wait([materialized, cancelled], return_when=asyncio.FIRST_COMPLETED)
            if materialized.done():
                return materialized.result()
            if entry.released:
                raise _HandoffEnded("AHP session handoff ended")
            return await materialized
        except BaseException:
            self._release(handoff_id)
            raise
        finally:
            cancelled.cancel()
