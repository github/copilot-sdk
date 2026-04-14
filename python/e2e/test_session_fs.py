"""E2E SessionFs tests mirroring nodejs/test/e2e/session_fs.test.ts."""

from __future__ import annotations

import asyncio
import datetime as dt
import os
import re
from pathlib import Path

import pytest
import pytest_asyncio

from copilot import CopilotClient, SessionFsConfig, define_tool
from copilot.client import ExternalServerConfig, SubprocessConfig
from copilot.generated.rpc import (
    SessionFSExistsResult,
    SessionFSReaddirResult,
    SessionFSReaddirWithTypesResult,
    SessionFSReadFileResult,
    SessionFSStatResult,
)
from copilot.generated.session_events import SessionCompactionCompleteData, SessionEvent
from copilot.session import PermissionHandler

from .testharness import E2ETestContext

pytestmark = pytest.mark.asyncio(loop_scope="module")


SESSION_FS_CONFIG: SessionFsConfig = {
    "initial_cwd": "/",
    "session_state_path": "/session-state",
    "conventions": "posix",
}


@pytest_asyncio.fixture(scope="module", loop_scope="module")
async def session_fs_client(ctx: E2ETestContext):
    github_token = (
        "fake-token-for-e2e-tests" if os.environ.get("GITHUB_ACTIONS") == "true" else None
    )
    client = CopilotClient(
        SubprocessConfig(
            cli_path=ctx.cli_path,
            cwd=ctx.work_dir,
            env=ctx.get_env(),
            github_token=github_token,
            session_fs=SESSION_FS_CONFIG,
        )
    )
    yield client
    try:
        await client.stop()
    except Exception:
        await client.force_stop()


class TestSessionFs:
    async def test_should_route_file_operations_through_the_session_fs_provider(
        self, ctx: E2ETestContext, session_fs_client: CopilotClient
    ):
        provider_root = Path(ctx.work_dir) / "provider"
        session = await session_fs_client.create_session(
            on_permission_request=PermissionHandler.approve_all,
            create_session_fs_handler=create_test_session_fs_handler(provider_root),
        )

        msg = await session.send_and_wait("What is 100 + 200?")
        assert msg is not None
        assert msg.data.content is not None
        assert "300" in msg.data.content
        await session.disconnect()

        events_path = provider_path(
            provider_root, session.session_id, "/session-state/events.jsonl"
        )
        assert "300" in events_path.read_text(encoding="utf-8")

    async def test_should_load_session_data_from_fs_provider_on_resume(
        self, ctx: E2ETestContext, session_fs_client: CopilotClient
    ):
        provider_root = Path(ctx.work_dir) / "provider"
        create_session_fs_handler = create_test_session_fs_handler(provider_root)

        session1 = await session_fs_client.create_session(
            on_permission_request=PermissionHandler.approve_all,
            create_session_fs_handler=create_session_fs_handler,
        )
        session_id = session1.session_id

        msg = await session1.send_and_wait("What is 50 + 50?")
        assert msg is not None
        assert msg.data.content is not None
        assert "100" in msg.data.content
        await session1.disconnect()

        assert provider_path(provider_root, session_id, "/session-state/events.jsonl").exists()

        session2 = await session_fs_client.resume_session(
            session_id,
            on_permission_request=PermissionHandler.approve_all,
            create_session_fs_handler=create_session_fs_handler,
        )

        msg2 = await session2.send_and_wait("What is that times 3?")
        assert msg2 is not None
        assert msg2.data.content is not None
        assert "300" in msg2.data.content
        await session2.disconnect()

    async def test_should_reject_setprovider_when_sessions_already_exist(self, ctx: E2ETestContext):
        github_token = (
            "fake-token-for-e2e-tests" if os.environ.get("GITHUB_ACTIONS") == "true" else None
        )
        client1 = CopilotClient(
            SubprocessConfig(
                cli_path=ctx.cli_path,
                cwd=ctx.work_dir,
                env=ctx.get_env(),
                use_stdio=False,
                github_token=github_token,
            )
        )
        session = None
        client2 = None

        try:
            session = await client1.create_session(
                on_permission_request=PermissionHandler.approve_all,
            )
            actual_port = client1.actual_port
            assert actual_port is not None

            client2 = CopilotClient(
                ExternalServerConfig(
                    url=f"localhost:{actual_port}",
                    session_fs=SESSION_FS_CONFIG,
                )
            )

            with pytest.raises(Exception):
                await client2.start()
        finally:
            if session is not None:
                await session.disconnect()
            if client2 is not None:
                await client2.force_stop()
            await client1.force_stop()

    async def test_should_map_large_output_handling_into_sessionfs(
        self, ctx: E2ETestContext, session_fs_client: CopilotClient
    ):
        provider_root = Path(ctx.work_dir) / "provider"
        supplied_file_content = "x" * 100_000

        @define_tool("get_big_string", description="Returns a large string")
        def get_big_string() -> str:
            return supplied_file_content

        session = await session_fs_client.create_session(
            on_permission_request=PermissionHandler.approve_all,
            create_session_fs_handler=create_test_session_fs_handler(provider_root),
            tools=[get_big_string],
        )

        await session.send_and_wait(
            "Call the get_big_string tool and reply with the word DONE only."
        )

        messages = await session.get_messages()
        tool_result = find_tool_call_result(messages, "get_big_string")
        assert tool_result is not None
        assert "/session-state/temp/" in tool_result
        match = re.search(r"(/session-state/temp/[^\s]+)", tool_result)
        assert match is not None

        temp_file = provider_path(provider_root, session.session_id, match.group(1))
        assert temp_file.read_text(encoding="utf-8") == supplied_file_content

    async def test_should_succeed_with_compaction_while_using_sessionfs(
        self, ctx: E2ETestContext, session_fs_client: CopilotClient
    ):
        provider_root = Path(ctx.work_dir) / "provider"
        session = await session_fs_client.create_session(
            on_permission_request=PermissionHandler.approve_all,
            create_session_fs_handler=create_test_session_fs_handler(provider_root),
        )

        compaction_event = asyncio.Event()
        compaction_success: bool | None = None

        def on_event(event: SessionEvent):
            nonlocal compaction_success
            match event.data:
                case SessionCompactionCompleteData() as data:
                    compaction_success = data.success
                    compaction_event.set()

        session.on(on_event)

        await session.send_and_wait("What is 2+2?")

        events_path = provider_path(
            provider_root, session.session_id, "/session-state/events.jsonl"
        )
        await wait_for_path(events_path)
        assert "checkpointNumber" not in events_path.read_text(encoding="utf-8")

        result = await session.rpc.history.compact()
        await asyncio.wait_for(compaction_event.wait(), timeout=5.0)
        assert result.success is True
        assert compaction_success is True

        await wait_for_content(events_path, "checkpointNumber")


class _SessionFsHandler:
    def __init__(self, provider_root: Path, session_id: str):
        self._provider_root = provider_root
        self._session_id = session_id

    async def read_file(self, params) -> SessionFSReadFileResult:
        content = provider_path(self._provider_root, self._session_id, params.path).read_text(
            encoding="utf-8"
        )
        return SessionFSReadFileResult.from_dict({"content": content})

    async def write_file(self, params) -> None:
        path = provider_path(self._provider_root, self._session_id, params.path)
        path.parent.mkdir(parents=True, exist_ok=True)
        path.write_text(params.content, encoding="utf-8")

    async def append_file(self, params) -> None:
        path = provider_path(self._provider_root, self._session_id, params.path)
        path.parent.mkdir(parents=True, exist_ok=True)
        with path.open("a", encoding="utf-8") as handle:
            handle.write(params.content)

    async def exists(self, params) -> SessionFSExistsResult:
        path = provider_path(self._provider_root, self._session_id, params.path)
        return SessionFSExistsResult.from_dict({"exists": path.exists()})

    async def stat(self, params) -> SessionFSStatResult:
        path = provider_path(self._provider_root, self._session_id, params.path)
        info = path.stat()
        timestamp = dt.datetime.fromtimestamp(info.st_mtime, tz=dt.UTC).isoformat()
        if timestamp.endswith("+00:00"):
            timestamp = f"{timestamp[:-6]}Z"
        return SessionFSStatResult.from_dict(
            {
                "isFile": not path.is_dir(),
                "isDirectory": path.is_dir(),
                "size": info.st_size,
                "mtime": timestamp,
                "birthtime": timestamp,
            }
        )

    async def mkdir(self, params) -> None:
        path = provider_path(self._provider_root, self._session_id, params.path)
        if params.recursive:
            path.mkdir(parents=True, exist_ok=True)
        else:
            path.mkdir()

    async def readdir(self, params) -> SessionFSReaddirResult:
        entries = sorted(
            entry.name
            for entry in provider_path(self._provider_root, self._session_id, params.path).iterdir()
        )
        return SessionFSReaddirResult.from_dict({"entries": entries})

    async def readdir_with_types(self, params) -> SessionFSReaddirWithTypesResult:
        entries = []
        for entry in sorted(
            provider_path(self._provider_root, self._session_id, params.path).iterdir(),
            key=lambda item: item.name,
        ):
            entries.append(
                {
                    "name": entry.name,
                    "type": "directory" if entry.is_dir() else "file",
                }
            )
        return SessionFSReaddirWithTypesResult.from_dict({"entries": entries})

    async def rm(self, params) -> None:
        provider_path(self._provider_root, self._session_id, params.path).unlink()

    async def rename(self, params) -> None:
        src = provider_path(self._provider_root, self._session_id, params.src)
        dest = provider_path(self._provider_root, self._session_id, params.dest)
        dest.parent.mkdir(parents=True, exist_ok=True)
        src.rename(dest)


def create_test_session_fs_handler(provider_root: Path):
    def create_handler(session):
        return _SessionFsHandler(provider_root, session.session_id)

    return create_handler


def provider_path(provider_root: Path, session_id: str, path: str) -> Path:
    return provider_root / session_id / path.lstrip("/")


def find_tool_call_result(messages: list[SessionEvent], tool_name: str) -> str | None:
    for message in messages:
        if (
            message.type.value == "tool.execution_complete"
            and message.data.tool_call_id is not None
        ):
            if find_tool_name(messages, message.data.tool_call_id) == tool_name:
                return message.data.result.content if message.data.result is not None else None
    return None


def find_tool_name(messages: list[SessionEvent], tool_call_id: str) -> str | None:
    for message in messages:
        if (
            message.type.value == "tool.execution_start"
            and message.data.tool_call_id == tool_call_id
        ):
            return message.data.tool_name
    return None


async def wait_for_path(path: Path, timeout: float = 5.0) -> None:
    async def predicate():
        return path.exists()

    await wait_for_predicate(predicate, timeout=timeout)


async def wait_for_content(path: Path, expected: str, timeout: float = 5.0) -> None:
    async def predicate():
        return path.exists() and expected in path.read_text(encoding="utf-8")

    await wait_for_predicate(predicate, timeout=timeout)


async def wait_for_predicate(predicate, timeout: float = 5.0) -> None:
    deadline = asyncio.get_running_loop().time() + timeout
    while asyncio.get_running_loop().time() < deadline:
        if await predicate():
            return
        await asyncio.sleep(0.1)
    raise TimeoutError("timed out waiting for condition")
