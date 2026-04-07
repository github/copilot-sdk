"""E2E tests for SessionFs virtual filesystem support."""

from __future__ import annotations

import os
import re
import shutil
import tempfile
from pathlib import Path
from typing import Any

import pytest
import pytest_asyncio

from copilot import CopilotClient, SessionFsConfig, SessionFsHandler
from copilot.client import SubprocessConfig
from copilot.session import CopilotSession, PermissionHandler

from .testharness import E2ETestContext

pytestmark = pytest.mark.asyncio(loop_scope="module")


class InMemoryFS:
    """Simple in memory filesystem for testing."""

    def __init__(self):
        self._files: dict[str, str] = {}
        self._dirs: set[str] = {"/"}

    def _ensure_parents(self, path: str) -> None:
        parts = path.split("/")
        for i in range(1, len(parts) - 1):
            self._dirs.add("/".join(parts[: i + 1]))

    def read_file(self, path: str) -> str:
        if path not in self._files:
            raise FileNotFoundError(f"File not found: {path}")
        return self._files[path]

    def write_file(self, path: str, content: str) -> None:
        self._ensure_parents(path)
        self._files[path] = content

    def append_file(self, path: str, content: str) -> None:
        self._ensure_parents(path)
        self._files[path] = self._files.get(path, "") + content

    def exists(self, path: str) -> bool:
        p = path.rstrip("/") or "/"
        return p in self._files or p in self._dirs

    def mkdir(self, path: str, recursive: bool = False) -> None:
        if recursive:
            self._ensure_parents(path + "/x")
        self._dirs.add(path.rstrip("/"))

    def readdir(self, path: str) -> list[str]:
        prefix = path if path.endswith("/") else path + "/"
        entries: set[str] = set()
        for key in list(self._files.keys()) + list(self._dirs):
            if key.startswith(prefix) and len(key) > len(prefix):
                rest = key[len(prefix) :]
                slash = rest.find("/")
                entries.add(rest[:slash] if slash >= 0 else rest)
        return sorted(entries)

    def remove(self, path: str) -> None:
        p = path.rstrip("/") or "/"
        self._files.pop(p, None)
        self._dirs.discard(p)

    def rename(self, src: str, dest: str) -> None:
        if src in self._files:
            self._ensure_parents(dest)
            self._files[dest] = self._files.pop(src)


class InMemorySessionFsHandler(SessionFsHandler):
    """SessionFs handler backed by an in memory filesystem."""

    def __init__(self, session_id: str, fs: InMemoryFS):
        self._session_id = session_id
        self._fs = fs

    def _sp(self, path: str) -> str:
        if path.startswith("/"):
            return f"/{self._session_id}{path}"
        return f"/{self._session_id}/{path}"

    async def read_file(self, *, session_id: str, path: str) -> dict[str, Any]:
        return {"content": self._fs.read_file(self._sp(path))}

    async def write_file(
        self, *, session_id: str, path: str, content: str, mode: int | None = None
    ) -> None:
        self._fs.write_file(self._sp(path), content)

    async def append_file(
        self, *, session_id: str, path: str, content: str, mode: int | None = None
    ) -> None:
        self._fs.append_file(self._sp(path), content)

    async def exists(self, *, session_id: str, path: str) -> dict[str, Any]:
        return {"exists": self._fs.exists(self._sp(path))}

    async def stat(self, *, session_id: str, path: str) -> dict[str, Any]:
        p = self._sp(path)
        if p in self._fs._files:
            content = self._fs._files[p]
            return {
                "isFile": True,
                "isDirectory": False,
                "size": len(content),
                "mtime": "2026-01-01T00:00:00.000Z",
                "birthtime": "2026-01-01T00:00:00.000Z",
            }
        if p.rstrip("/") in self._fs._dirs:
            return {
                "isFile": False,
                "isDirectory": True,
                "size": 0,
                "mtime": "2026-01-01T00:00:00.000Z",
                "birthtime": "2026-01-01T00:00:00.000Z",
            }
        raise FileNotFoundError(f"Path not found: {path}")

    async def mkdir(
        self,
        *,
        session_id: str,
        path: str,
        recursive: bool | None = None,
        mode: int | None = None,
    ) -> None:
        self._fs.mkdir(self._sp(path), recursive=bool(recursive))

    async def readdir(self, *, session_id: str, path: str) -> dict[str, Any]:
        return {"entries": self._fs.readdir(self._sp(path))}

    async def readdir_with_types(self, *, session_id: str, path: str) -> dict[str, Any]:
        p = self._sp(path)
        names = self._fs.readdir(p)
        prefix = p if p.endswith("/") else p + "/"
        entries = []
        for name in names:
            full = prefix + name
            is_dir = full in self._fs._dirs or any(
                k.startswith(full + "/") for k in self._fs._files
            )
            entries.append({"name": name, "type": "directory" if is_dir else "file"})
        return {"entries": entries}

    async def rm(
        self,
        *,
        session_id: str,
        path: str,
        recursive: bool | None = None,
        force: bool | None = None,
    ) -> None:
        self._fs.remove(self._sp(path))

    async def rename(self, *, session_id: str, src: str, dest: str) -> None:
        self._fs.rename(self._sp(src), self._sp(dest))


# Shared in memory filesystem for all tests in this module
_shared_fs = InMemoryFS()

SESSION_FS_CONFIG = SessionFsConfig(
    initial_cwd="/",
    session_state_path="/session-state",
    conventions="posix",
)


def _make_handler(session: CopilotSession) -> SessionFsHandler:
    return InMemorySessionFsHandler(session.session_id, _shared_fs)


@pytest_asyncio.fixture(scope="module", loop_scope="module")
async def ctx(request):
    """Custom context that creates a CopilotClient with SessionFs enabled."""
    context = E2ETestContext()
    # Override setup to inject session_fs config
    context.cli_path = context.cli_path or str(
        (
            Path(__file__).parents[2]
            / "nodejs"
            / "node_modules"
            / "@github"
            / "copilot"
            / "index.js"
        ).resolve()
    )
    env_cli = os.environ.get("COPILOT_CLI_PATH")
    if env_cli and Path(env_cli).exists():
        context.cli_path = str(Path(env_cli).resolve())
    else:
        base = Path(__file__).parents[2]
        cli = base / "nodejs" / "node_modules" / "@github" / "copilot" / "index.js"
        if cli.exists():
            context.cli_path = str(cli.resolve())
        else:
            pytest.skip("CLI not found")

    context.home_dir = tempfile.mkdtemp(prefix="copilot-test-config-")
    context.work_dir = tempfile.mkdtemp(prefix="copilot-test-work-")

    from .testharness.proxy import CapiProxy

    context._proxy = CapiProxy()
    context.proxy_url = await context._proxy.start()

    github_token = (
        "fake-token-for-e2e-tests" if os.environ.get("GITHUB_ACTIONS") == "true" else None
    )
    env = os.environ.copy()
    env.update(
        {
            "COPILOT_API_URL": context.proxy_url,
            "XDG_CONFIG_HOME": context.home_dir,
            "XDG_STATE_HOME": context.home_dir,
        }
    )
    context._client = CopilotClient(
        SubprocessConfig(
            cli_path=context.cli_path,
            cwd=context.work_dir,
            env=env,
            github_token=github_token,
        ),
        session_fs=SESSION_FS_CONFIG,
    )

    yield context
    any_failed = request.session.stash.get("any_test_failed", False)
    await context.teardown(test_failed=any_failed)


@pytest_asyncio.fixture(autouse=True, loop_scope="module")
async def configure_test(request, ctx):
    """Configure the proxy for each test using session_fs snapshot dir."""
    test_name = request.node.name
    if test_name.startswith("test_"):
        test_name = test_name[5:]
    sanitized = re.sub(r"[^a-zA-Z0-9]", "_", test_name).lower()

    snapshots_dir = Path(__file__).parents[2] / "test" / "snapshots"
    snapshot_path = snapshots_dir / "session_fs" / f"{sanitized}.yaml"

    await ctx._proxy.configure(str(snapshot_path.resolve()), ctx.work_dir)

    # Clean temp dirs between tests
    for item in Path(ctx.home_dir).iterdir():
        if item.is_dir():
            shutil.rmtree(item, ignore_errors=True)
        else:
            item.unlink(missing_ok=True)
    yield


class TestSessionFs:
    async def test_should_route_file_operations_through_the_session_fs_provider(
        self, ctx: E2ETestContext
    ):
        session = await ctx.client.create_session(
            on_permission_request=PermissionHandler.approve_all,
            create_session_fs_handler=_make_handler,
        )

        msg = await session.send_and_wait("What is 100 + 200?")
        assert msg is not None
        assert "300" in msg.data.content
        await session.disconnect()

        events_path = f"/{session.session_id}/session-state/events.jsonl"
        content = _shared_fs.read_file(events_path)
        assert "300" in content

    async def test_should_load_session_data_from_fs_provider_on_resume(self, ctx: E2ETestContext):
        session1 = await ctx.client.create_session(
            on_permission_request=PermissionHandler.approve_all,
            create_session_fs_handler=_make_handler,
        )
        session_id = session1.session_id

        msg = await session1.send_and_wait("What is 50 + 50?")
        assert msg is not None
        assert "100" in msg.data.content
        await session1.disconnect()

        events_path = f"/{session_id}/session-state/events.jsonl"
        assert _shared_fs.exists(events_path)

        session2 = await ctx.client.resume_session(
            session_id,
            on_permission_request=PermissionHandler.approve_all,
            create_session_fs_handler=_make_handler,
        )

        msg2 = await session2.send_and_wait("What is that times 3?")
        await session2.disconnect()
        assert msg2 is not None
        assert "300" in msg2.data.content
