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
    SessionFSReaddirWithTypesEntry,
    SessionFSReaddirWithTypesEntryType,
)
from copilot.generated.session_events import SessionCompactionCompleteData, SessionEvent
from copilot.session import PermissionHandler
from copilot.session_fs_provider import SessionFsFileInfo, SessionFsProvider

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

    async def test_should_write_workspace_metadata_via_sessionfs(
        self, ctx: E2ETestContext, session_fs_client: CopilotClient
    ):
        provider_root = Path(ctx.work_dir) / "provider"
        session = await session_fs_client.create_session(
            on_permission_request=PermissionHandler.approve_all,
            create_session_fs_handler=create_test_session_fs_handler(provider_root),
        )

        msg = await session.send_and_wait("What is 7 * 8?")
        assert msg is not None
        assert msg.data.content is not None
        assert "56" in msg.data.content

        # WorkspaceManager should have created workspace.yaml via sessionFs
        workspace_yaml_path = provider_path(
            provider_root, session.session_id, "/session-state/workspace.yaml"
        )
        await wait_for_path(workspace_yaml_path)
        yaml_content = workspace_yaml_path.read_text(encoding="utf-8")
        assert "id:" in yaml_content

        # Checkpoint index should also exist
        index_path = provider_path(
            provider_root, session.session_id, "/session-state/checkpoints/index.md"
        )
        await wait_for_path(index_path)

        await session.disconnect()

    async def test_should_persist_plan_md_via_sessionfs(
        self, ctx: E2ETestContext, session_fs_client: CopilotClient
    ):
        from copilot.generated.rpc import PlanUpdateRequest

        provider_root = Path(ctx.work_dir) / "provider"
        session = await session_fs_client.create_session(
            on_permission_request=PermissionHandler.approve_all,
            create_session_fs_handler=create_test_session_fs_handler(provider_root),
        )

        # Write a plan via the session RPC
        await session.send_and_wait("What is 2 + 3?")
        await session.rpc.plan.update(PlanUpdateRequest(content="# Test Plan\n\nThis is a test."))

        plan_path = provider_path(provider_root, session.session_id, "/session-state/plan.md")
        await wait_for_path(plan_path)
        content = plan_path.read_text(encoding="utf-8")
        assert "# Test Plan" in content

        await session.disconnect()


class _TestSessionFsProvider(SessionFsProvider):
    def __init__(self, provider_root: Path, session_id: str):
        self._provider_root = provider_root
        self._session_id = session_id

    def _path(self, path: str) -> Path:
        return provider_path(self._provider_root, self._session_id, path)

    async def read_file(self, path: str) -> str:
        return self._path(path).read_text(encoding="utf-8")

    async def write_file(self, path: str, content: str, mode: int | None = None) -> None:
        p = self._path(path)
        p.parent.mkdir(parents=True, exist_ok=True)
        p.write_text(content, encoding="utf-8")

    async def append_file(self, path: str, content: str, mode: int | None = None) -> None:
        p = self._path(path)
        p.parent.mkdir(parents=True, exist_ok=True)
        with p.open("a", encoding="utf-8") as handle:
            handle.write(content)

    async def exists(self, path: str) -> bool:
        return self._path(path).exists()

    async def stat(self, path: str) -> SessionFsFileInfo:
        p = self._path(path)
        info = p.stat()
        timestamp = dt.datetime.fromtimestamp(info.st_mtime, tz=dt.UTC)
        return SessionFsFileInfo(
            is_file=not p.is_dir(),
            is_directory=p.is_dir(),
            size=info.st_size,
            mtime=timestamp,
            birthtime=timestamp,
        )

    async def mkdir(self, path: str, recursive: bool, mode: int | None = None) -> None:
        p = self._path(path)
        if recursive:
            p.mkdir(parents=True, exist_ok=True)
        else:
            p.mkdir()

    async def readdir(self, path: str) -> list[str]:
        return sorted(entry.name for entry in self._path(path).iterdir())

    async def readdir_with_types(self, path: str) -> list[SessionFSReaddirWithTypesEntry]:
        entries = []
        for entry in sorted(self._path(path).iterdir(), key=lambda item: item.name):
            entries.append(
                SessionFSReaddirWithTypesEntry(
                    name=entry.name,
                    type=SessionFSReaddirWithTypesEntryType.DIRECTORY
                    if entry.is_dir()
                    else SessionFSReaddirWithTypesEntryType.FILE,
                )
            )
        return entries

    async def rm(self, path: str, recursive: bool, force: bool) -> None:
        self._path(path).unlink()

    async def rename(self, src: str, dest: str) -> None:
        d = self._path(dest)
        d.parent.mkdir(parents=True, exist_ok=True)
        self._path(src).rename(d)


def create_test_session_fs_handler(provider_root: Path):
    def create_handler(session):
        return _TestSessionFsProvider(provider_root, session.session_id)

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
