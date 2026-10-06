"""Skill provider unit tests."""

import asyncio
import io
import json
from collections.abc import Awaitable, Callable
from typing import Any
from unittest.mock import AsyncMock, Mock

import pytest

from copilot import (
    CloudSessionOptions,
    CopilotClient,
    RuntimeConnection,
    SkillProvider,
    SkillProviderDescriptor,
)
from copilot._jsonrpc import JsonRpcClient, JsonRpcError
from copilot.session import CopilotSession


class _SyncSkillProvider:
    def __init__(
        self,
        *,
        skills: list[SkillProviderDescriptor] | None = None,
        markdown: dict[str, str | None] | None = None,
    ) -> None:
        self.skills = skills
        self.markdown = markdown or {}
        self.list_calls = 0
        self.read_calls: list[str] = []

    def list_skills(self) -> list[SkillProviderDescriptor] | None:
        self.list_calls += 1
        return self.skills

    def read_skill(self, name: str) -> str | None:
        self.read_calls.append(name)
        return self.markdown.get(name)


class _AsyncSkillProvider:
    def __init__(
        self,
        *,
        skills: list[SkillProviderDescriptor] | None = None,
        markdown: dict[str, str | None] | None = None,
    ) -> None:
        self._sync = _SyncSkillProvider(skills=skills, markdown=markdown)

    @property
    def list_calls(self) -> int:
        return self._sync.list_calls

    @property
    def read_calls(self) -> list[str]:
        return self._sync.read_calls

    async def list_skills(self) -> list[SkillProviderDescriptor] | None:
        await asyncio.sleep(0)
        return self._sync.list_skills()

    async def read_skill(self, name: str) -> str | None:
        await asyncio.sleep(0)
        return self._sync.read_skill(name)


class _FailingSkillProvider:
    def list_skills(self) -> list[SkillProviderDescriptor]:
        raise RuntimeError("secret-list-token")

    def read_skill(self, name: str) -> str:
        raise RuntimeError("secret-read-token")


class _WriteOnlyProcess:
    def __init__(self) -> None:
        self.stdin = io.BytesIO()
        self.stdout = io.BytesIO()
        self.stderr = None

    def poll(self) -> None:
        return None


def _descriptor(name: str, description: str = "Test skill") -> SkillProviderDescriptor:
    return SkillProviderDescriptor(name=name, description=description)


def _decode_jsonrpc_output(process: _WriteOnlyProcess) -> dict[str, Any]:
    _header, payload = process.stdin.getvalue().split(b"\r\n\r\n", 1)
    return json.loads(payload)


async def _assert_no_provider_error(session: CopilotSession, provider: _SyncSkillProvider) -> None:
    list_calls = provider.list_calls
    read_calls = list(provider.read_calls)
    with pytest.raises(JsonRpcError) as exc_info:
        await session._handle_skill_provider_list()
    assert exc_info.value.code == -32603
    assert exc_info.value.message == f"No skill provider for session: {session.session_id}"
    assert exc_info.value.data is None
    assert provider.list_calls == list_calls
    assert provider.read_calls == read_calls


def _install_fake_client(
    client: CopilotClient,
    handler: Callable[[str, dict, dict[str, Any]], Awaitable[dict[str, Any]]],
) -> None:
    async def request(method: str, params: dict, **kwargs: Any) -> dict[str, Any]:
        return await handler(method, params, kwargs)

    client._client = Mock()
    client._client.request = request


async def _default_request(method: str, params: dict, kwargs: dict[str, Any]) -> dict[str, Any]:
    if method == "session.create":
        result = {"sessionId": params.get("sessionId") or "created-session", "workspacePath": None}
        callback = kwargs.get("on_response_inline")
        if callback is not None:
            callback(result)
        return result
    if method == "session.resume":
        return {"sessionId": params["sessionId"], "workspacePath": None}
    if method == "session.options.update":
        return {"success": True}
    return {}


def test_skill_provider_is_runtime_checkable_and_exported() -> None:
    assert isinstance(_SyncSkillProvider(), SkillProvider)
    assert SkillProviderDescriptor(name="demo", description="Demo").to_dict() == {
        "description": "Demo",
        "name": "demo",
    }


@pytest.mark.asyncio
async def test_create_and_resume_send_skill_provider_flag_only_when_configured() -> None:
    client = CopilotClient(connection=RuntimeConnection.for_uri("localhost:1234"))
    captured: list[tuple[str, dict]] = []

    async def request(method: str, params: dict, kwargs: dict[str, Any]) -> dict[str, Any]:
        captured.append((method, dict(params)))
        return await _default_request(method, params, kwargs)

    _install_fake_client(client, request)
    provider = _SyncSkillProvider(skills=[_descriptor("provided")])

    await client.create_session(session_id="create-with-provider", skill_provider=provider)
    await client.resume_session("resume-with-provider", skill_provider=provider)
    await client.create_session(session_id="create-without-provider")
    await client.resume_session("resume-without-provider")

    payloads = {(method, params["sessionId"]): params for method, params in captured}
    assert payloads[("session.create", "create-with-provider")]["hasSkillProvider"] is True
    assert payloads[("session.resume", "resume-with-provider")]["hasSkillProvider"] is True
    assert "hasSkillProvider" not in payloads[("session.create", "create-without-provider")]
    assert "hasSkillProvider" not in payloads[("session.resume", "resume-without-provider")]


@pytest.mark.asyncio
async def test_empty_mode_keeps_skill_provider_dormant_by_default() -> None:
    client = CopilotClient(connection=RuntimeConnection.for_uri("localhost:1234"), mode="empty")
    captured: list[tuple[str, dict]] = []

    async def request(method: str, params: dict, kwargs: dict[str, Any]) -> dict[str, Any]:
        captured.append((method, dict(params)))
        return await _default_request(method, params, kwargs)

    _install_fake_client(client, request)
    provider = _SyncSkillProvider(skills=[_descriptor("provided")])

    await client.create_session(
        session_id="empty-create",
        available_tools=[],
        skill_provider=provider,
    )
    await client.resume_session(
        "empty-resume",
        available_tools=[],
        skill_provider=provider,
    )

    payloads = {(method, params["sessionId"]): params for method, params in captured}
    assert payloads[("session.create", "empty-create")]["enableSkills"] is False
    assert payloads[("session.resume", "empty-resume")]["enableSkills"] is False
    assert payloads[("session.create", "empty-create")]["hasSkillProvider"] is True
    assert payloads[("session.resume", "empty-resume")]["hasSkillProvider"] is True


@pytest.mark.asyncio
async def test_cloud_create_rejects_skill_provider_before_connecting() -> None:
    client = CopilotClient(connection=RuntimeConnection.for_uri("localhost:1234"))
    start_mock = AsyncMock()
    setattr(client, "start", start_mock)
    provider = _SyncSkillProvider(skills=[_descriptor("cloud")])

    with pytest.raises(ValueError, match="Skill providers are not supported for cloud sessions\\."):
        await client.create_session(
            cloud=CloudSessionOptions(),
            skill_provider=provider,
        )

    start_mock.assert_not_awaited()
    assert provider.list_calls == 0
    assert provider.read_calls == []


@pytest.mark.asyncio
async def test_early_skill_provider_callbacks_are_served_during_create_and_resume() -> None:
    client = CopilotClient(connection=RuntimeConnection.for_uri("localhost:1234"))
    observed: list[dict] = []
    provider = _SyncSkillProvider(
        skills=[_descriptor("early")],
        markdown={"early": "early skill body"},
    )

    async def request(method: str, params: dict, kwargs: dict[str, Any]) -> dict[str, Any]:
        if method in ("session.create", "session.resume"):
            observed.append(
                await client._handle_skill_provider_list({"sessionId": params["sessionId"]})
            )
            observed.append(
                await client._handle_skill_provider_read(
                    {"sessionId": params["sessionId"], "name": "early"}
                )
            )
        return await _default_request(method, params, kwargs)

    _install_fake_client(client, request)

    await client.create_session(session_id="early-create", skill_provider=provider)
    await client.resume_session("early-resume", skill_provider=provider)

    assert observed == [
        {"skills": [{"description": "Test skill", "name": "early"}]},
        {"markdown": "early skill body"},
        {"skills": [{"description": "Test skill", "name": "early"}]},
        {"markdown": "early skill body"},
    ]


@pytest.mark.parametrize("provider_cls", [_SyncSkillProvider, _AsyncSkillProvider])
@pytest.mark.asyncio
async def test_skill_provider_dispatch_serializes_descriptors_and_reads_markdown(
    provider_cls: type[_SyncSkillProvider] | type[_AsyncSkillProvider],
) -> None:
    client = CopilotClient(connection=RuntimeConnection.for_uri("localhost:1234"))
    provider = provider_cls(
        skills=[
            _descriptor("alpha", "Alpha skill"),
            SkillProviderDescriptor(
                name="beta",
                description="Beta skill",
                argument_hint="ARG",
                disable_model_invocation=True,
                user_invocable=False,
            ),
        ],
        markdown={"alpha": "# Alpha"},
    )
    session = CopilotSession("session-1", Mock())
    session._register_skill_provider(provider)
    client._sessions["session-1"] = session

    listed = await client._handle_skill_provider_list({"sessionId": "session-1"})
    read = await client._handle_skill_provider_read({"sessionId": "session-1", "name": "alpha"})

    assert listed == {
        "skills": [
            {"description": "Alpha skill", "name": "alpha"},
            {
                "argumentHint": "ARG",
                "description": "Beta skill",
                "disableModelInvocation": True,
                "name": "beta",
                "userInvocable": False,
            },
        ]
    }
    assert read == {"markdown": "# Alpha"}


@pytest.mark.asyncio
async def test_skill_provider_null_list_becomes_empty_catalog() -> None:
    session = CopilotSession("session-1", Mock())
    session._register_skill_provider(_SyncSkillProvider(skills=None))

    assert await session._handle_skill_provider_list() == {"skills": []}


@pytest.mark.asyncio
async def test_skill_provider_read_none_returns_null_markdown() -> None:
    session = CopilotSession("session-1", Mock())
    session._register_skill_provider(_SyncSkillProvider(markdown={"missing": None}))

    assert await session._handle_skill_provider_read("missing") == {"markdown": None}


@pytest.mark.asyncio
async def test_skill_provider_failures_are_generic_and_do_not_leak_provider_errors() -> None:
    session = CopilotSession("session-1", Mock())
    session._register_skill_provider(_FailingSkillProvider())

    with pytest.raises(JsonRpcError) as list_error:
        await session._handle_skill_provider_list()
    with pytest.raises(JsonRpcError) as read_error:
        await session._handle_skill_provider_read("secret")

    assert list_error.value.code == -32603
    assert list_error.value.message == "Skill provider listSkills failed"
    assert list_error.value.data is None
    assert "secret-list-token" not in str(list_error.value)
    assert read_error.value.code == -32603
    assert read_error.value.message == "Skill provider readSkill failed"
    assert read_error.value.data is None
    assert "secret-read-token" not in str(read_error.value)


@pytest.mark.asyncio
async def test_skill_provider_failures_are_logged_with_session_id(
    caplog: pytest.LogCaptureFixture,
) -> None:
    session = CopilotSession("session-1", Mock())
    session._register_skill_provider(_FailingSkillProvider())

    with caplog.at_level("WARNING", logger="copilot.session"):
        with pytest.raises(JsonRpcError):
            await session._handle_skill_provider_list()
        with pytest.raises(JsonRpcError):
            await session._handle_skill_provider_read("secret")

    records = [r for r in caplog.records if r.getMessage().startswith("Skill provider")]
    assert [r.getMessage() for r in records] == [
        "Skill provider listSkills failed",
        "Skill provider readSkill failed",
    ]
    assert all(getattr(r, "session_id", None) == "session-1" for r in records)
    assert [str(r.exc_info[1]) for r in records if r.exc_info] == [
        "secret-list-token",
        "secret-read-token",
    ]


class _SelfCancellingSkillProvider:
    async def list_skills(self) -> list[SkillProviderDescriptor]:
        raise asyncio.CancelledError

    async def read_skill(self, name: str) -> str:
        raise asyncio.CancelledError


@pytest.mark.asyncio
async def test_skill_provider_cancelled_error_raised_by_provider_is_a_failure() -> None:
    session = CopilotSession("session-1", Mock())
    session._register_skill_provider(_SelfCancellingSkillProvider())

    with pytest.raises(JsonRpcError) as list_error:
        await session._handle_skill_provider_list()
    with pytest.raises(JsonRpcError) as read_error:
        await session._handle_skill_provider_read("cancelled")

    assert list_error.value.message == "Skill provider listSkills failed"
    assert read_error.value.message == "Skill provider readSkill failed"


class _BlockingSkillProvider:
    def __init__(self) -> None:
        self.entered = asyncio.Event()

    async def list_skills(self) -> list[SkillProviderDescriptor]:
        self.entered.set()
        await asyncio.Event().wait()
        return []

    async def read_skill(self, name: str) -> str:
        return ""


@pytest.mark.asyncio
async def test_skill_provider_dispatch_cancellation_propagates() -> None:
    session = CopilotSession("session-1", Mock())
    provider = _BlockingSkillProvider()
    session._register_skill_provider(provider)

    task = asyncio.create_task(session._handle_skill_provider_list())
    await provider.entered.wait()
    task.cancel()

    with pytest.raises(asyncio.CancelledError):
        await task


class _JsonRpcErrorSkillProvider:
    def list_skills(self) -> list[SkillProviderDescriptor]:
        raise JsonRpcError(-32000, "secret-list-token", data={"code": "provider_secret"})

    def read_skill(self, name: str) -> str:
        raise JsonRpcError(-32000, "secret-read-token", data={"code": "provider_secret"})


@pytest.mark.asyncio
async def test_skill_provider_json_rpc_errors_from_provider_are_not_forwarded() -> None:
    session = CopilotSession("session-1", Mock())
    session._register_skill_provider(_JsonRpcErrorSkillProvider())

    with pytest.raises(JsonRpcError) as list_error:
        await session._handle_skill_provider_list()
    with pytest.raises(JsonRpcError) as read_error:
        await session._handle_skill_provider_read("secret")

    assert (list_error.value.code, list_error.value.message, list_error.value.data) == (
        -32603,
        "Skill provider listSkills failed",
        None,
    )
    assert (read_error.value.code, read_error.value.message, read_error.value.data) == (
        -32603,
        "Skill provider readSkill failed",
        None,
    )


@pytest.mark.asyncio
async def test_skill_provider_unknown_session_and_no_provider_return_generic_errors() -> None:
    client = CopilotClient(connection=RuntimeConnection.for_uri("localhost:1234"))

    with pytest.raises(JsonRpcError) as unknown_error:
        await client._handle_skill_provider_list({"sessionId": "missing-session"})
    assert (unknown_error.value.code, unknown_error.value.message, unknown_error.value.data) == (
        -32603,
        "Session not found: missing-session",
        None,
    )

    client._sessions["session-1"] = CopilotSession("session-1", Mock())
    with pytest.raises(JsonRpcError) as no_provider_error:
        await client._handle_skill_provider_read({"sessionId": "session-1", "name": "demo"})
    assert (
        no_provider_error.value.code,
        no_provider_error.value.message,
        no_provider_error.value.data,
    ) == (
        -32603,
        "No skill provider for session: session-1",
        None,
    )


@pytest.mark.parametrize("method", ["list", "read"])
@pytest.mark.asyncio
async def test_skill_provider_invalid_params_are_json_rpc_invalid_params(method: str) -> None:
    client = CopilotClient(connection=RuntimeConnection.for_uri("localhost:1234"))
    params = {"sessionId": "session-1"} if method == "read" else {}
    handler = (
        client._handle_skill_provider_read
        if method == "read"
        else client._handle_skill_provider_list
    )

    with pytest.raises(JsonRpcError) as exc_info:
        await handler(params)

    assert exc_info.value.code == -32602
    assert exc_info.value.message == "Invalid params"
    assert exc_info.value.data is None


@pytest.mark.asyncio
async def test_skill_provider_read_none_is_written_to_json_rpc_result() -> None:
    sdk_client = CopilotClient(connection=RuntimeConnection.for_uri("localhost:1234"))
    process = _WriteOnlyProcess()
    rpc_client = JsonRpcClient(process)
    sdk_client._client = rpc_client
    sdk_client._register_session_scoped_request_handlers()
    session = CopilotSession("session-1", rpc_client)
    session._register_skill_provider(_SyncSkillProvider(markdown={"missing": None}))
    sdk_client._sessions["session-1"] = session

    await rpc_client._dispatch_request(
        {
            "jsonrpc": "2.0",
            "id": 1,
            "method": "skillProvider.read",
            "params": {"sessionId": "session-1", "name": "missing"},
        },
        rpc_client.request_handlers["skillProvider.read"],
        None,
    )

    response = _decode_jsonrpc_output(process)
    assert response == {
        "jsonrpc": "2.0",
        "id": 1,
        "result": {"markdown": None},
    }


@pytest.mark.parametrize("method", ["list", "read"])
@pytest.mark.asyncio
async def test_skill_provider_task_is_cancelled_by_runtime_cancel_request(
    method: str, caplog: pytest.LogCaptureFixture
) -> None:
    entered = asyncio.Event()
    cancelled = asyncio.Event()

    class BlockingProvider:
        async def _block(self) -> Any:
            entered.set()
            try:
                await asyncio.Event().wait()
            except asyncio.CancelledError:
                cancelled.set()
                raise

        async def list_skills(self) -> list[SkillProviderDescriptor]:
            return await self._block()

        async def read_skill(self, name: str) -> str | None:
            return await self._block()

    sdk_client = CopilotClient(connection=RuntimeConnection.for_uri("localhost:1234"))
    process = _WriteOnlyProcess()
    rpc_client = JsonRpcClient(process)
    rpc_client._loop = asyncio.get_running_loop()
    sdk_client._client = rpc_client
    sdk_client._register_session_scoped_request_handlers()
    session = CopilotSession("session-1", rpc_client)
    session._register_skill_provider(BlockingProvider())
    sdk_client._sessions["session-1"] = session
    params = {"sessionId": "session-1", "name": "demo"}

    with caplog.at_level("WARNING", logger="copilot.session"):
        rpc_client._handle_request(
            {"jsonrpc": "2.0", "id": 7, "method": f"skillProvider.{method}", "params": params}
        )
        await asyncio.wait_for(entered.wait(), timeout=5)
        rpc_client._handle_cancel_request({"id": 7})
        await asyncio.wait_for(cancelled.wait(), timeout=5)
        # The response is written before dispatch cleanup retires the cancellation entry.
        async with asyncio.timeout(5):
            while not process.stdin.getvalue() or 7 in rpc_client._incoming_request_cancellations:
                await asyncio.sleep(0.01)

    assert _decode_jsonrpc_output(process) == {
        "jsonrpc": "2.0",
        "id": 7,
        "error": {"code": -32800, "message": "Request cancelled", "data": None},
    }
    assert not [r for r in caplog.records if r.getMessage().startswith("Skill provider")]
    assert rpc_client._incoming_request_cancellations == {}


@pytest.mark.parametrize("teardown", ["disconnect", "mark", "force_stop", "delete", "close"])
@pytest.mark.asyncio
async def test_skill_provider_is_cleared_on_teardown_paths(teardown: str) -> None:
    provider = _SyncSkillProvider(skills=[_descriptor("demo")])
    client = CopilotClient(connection=RuntimeConnection.for_uri("localhost:1234"))
    session = CopilotSession("session-1", Mock())
    session._register_skill_provider(provider)

    if teardown == "disconnect":
        session._client.request = AsyncMock(return_value={"success": True})
        await session.disconnect()
    elif teardown == "mark":
        session._mark_disconnected()
    elif teardown == "force_stop":
        client._sessions["session-1"] = session
        client._client = Mock()
        client._client.stop = AsyncMock()
        await client.force_stop()
    elif teardown == "delete":
        client._sessions["session-1"] = session
        client._client = Mock()
        client._client.request = AsyncMock(return_value={"success": True})
        await client.delete_session("session-1")
    else:
        client._sessions["session-1"] = session
        client._client = Mock(_loop=asyncio.get_running_loop())
        client._handle_connection_close()
        await asyncio.sleep(0)

    await _assert_no_provider_error(session, provider)


@pytest.mark.parametrize("operation", ["create", "resume"])
@pytest.mark.asyncio
async def test_skill_provider_is_cleared_after_failed_create_or_resume(operation: str) -> None:
    client = CopilotClient(connection=RuntimeConnection.for_uri("localhost:1234"))
    provider = _SyncSkillProvider(skills=[_descriptor("demo")])
    captured_session: CopilotSession | None = None

    async def request(method: str, params: dict, _kwargs: dict[str, Any]) -> dict[str, Any]:
        nonlocal captured_session
        if method == f"session.{operation}":
            captured_session = client._sessions[params["sessionId"]]
            raise RuntimeError("admission failed")
        return {}

    _install_fake_client(client, request)

    with pytest.raises(RuntimeError, match="admission failed"):
        if operation == "create":
            await client.create_session(session_id="failed-session", skill_provider=provider)
        else:
            await client.resume_session("failed-session", skill_provider=provider)

    assert "failed-session" not in client._sessions
    assert captured_session is not None
    await _assert_no_provider_error(captured_session, provider)


@pytest.mark.asyncio
async def test_failed_resume_restores_the_resident_skill_provider() -> None:
    client = CopilotClient(connection=RuntimeConnection.for_uri("localhost:1234"))
    resident = _SyncSkillProvider(skills=[_descriptor("demo")])
    replacement = _SyncSkillProvider(skills=[_descriptor("demo")])

    async def request(method: str, params: dict, kwargs: dict[str, Any]) -> dict[str, Any]:
        if method == "session.resume":
            raise RuntimeError("resume failed")
        return await _default_request(method, params, kwargs)

    _install_fake_client(client, request)
    session = await client.create_session(session_id="session-1", skill_provider=resident)

    with pytest.raises(RuntimeError, match="resume failed"):
        await client.resume_session("session-1", skill_provider=replacement)

    assert client._sessions["session-1"] is session
    assert await client._handle_skill_provider_list({"sessionId": "session-1"}) == {
        "skills": [{"name": "demo", "description": "Test skill"}]
    }
    assert (resident.list_calls, replacement.list_calls) == (1, 0)


@pytest.mark.asyncio
async def test_resume_rebinds_or_unbinds_skill_provider() -> None:
    client = CopilotClient(connection=RuntimeConnection.for_uri("localhost:1234"))
    provider_a = _SyncSkillProvider(
        skills=[_descriptor("demo")],
        markdown={"demo": "from A"},
    )
    provider_b = _SyncSkillProvider(
        skills=[_descriptor("demo")],
        markdown={"demo": "from B"},
    )
    _install_fake_client(client, _default_request)

    session = CopilotSession("session-1", Mock())
    session._register_skill_provider(provider_a)
    client._sessions["session-1"] = session

    await client.resume_session("session-1", skill_provider=provider_b)
    assert await client._handle_skill_provider_read({"sessionId": "session-1", "name": "demo"}) == {
        "markdown": "from B"
    }
    assert provider_a.read_calls == []

    await client.resume_session("session-1")
    with pytest.raises(JsonRpcError) as exc_info:
        await client._handle_skill_provider_list({"sessionId": "session-1"})
    assert (exc_info.value.code, exc_info.value.message, exc_info.value.data) == (
        -32603,
        "No skill provider for session: session-1",
        None,
    )
    assert provider_b.list_calls == 0
