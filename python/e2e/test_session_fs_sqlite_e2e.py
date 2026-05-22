"""E2E SessionFs SQLite tests mirroring nodejs/test/e2e/session_fs_sqlite.e2e.test.ts."""

from __future__ import annotations

import datetime as dt
import json
import os
import sqlite3
import tempfile
from pathlib import Path

import pytest
import pytest_asyncio

from copilot import CopilotClient, CopilotClientOptions, RuntimeConnection, SessionFsConfig
from copilot.generated.rpc import (
    SessionFSReaddirWithTypesEntry,
    SessionFSReaddirWithTypesEntryType,
    SessionFSSqliteQueryType,
)
from copilot.session import PermissionHandler
from copilot.session_fs_provider import (
    SessionFsFileInfo,
    SessionFsProvider,
    SessionFsSqliteProvider,
    SessionFsSqliteQueryResult,
)

from .testharness import DEFAULT_GITHUB_TOKEN, E2ETestContext

pytestmark = pytest.mark.asyncio(loop_scope="module")


SESSION_STATE_PATH = (
    "/session-state"
    if os.name == "nt"
    else (Path(tempfile.mkdtemp(prefix="copilot-sessionfs-sqlite-")) / "session-state")
    .resolve()
    .as_posix()
)

SESSION_FS_CONFIG: SessionFsConfig = {
    "initial_working_directory": "/",
    "session_state_path": SESSION_STATE_PATH,
    "conventions": "posix",
    "capabilities": {"sqlite": True},
}


class _InMemorySessionFsSqliteProvider(SessionFsProvider, SessionFsSqliteProvider):
    """In-memory SessionFsProvider with real SQLite for E2E tests."""

    def __init__(self, session_id: str, sqlite_calls: list[dict]):
        self._session_id = session_id
        self._sqlite_calls = sqlite_calls
        self._files: dict[str, str] = {}
        self._dirs: set[str] = {"/"}
        self._db: sqlite3.Connection | None = None

    def _get_or_create_db(self) -> sqlite3.Connection:
        if self._db is None:
            self._db = sqlite3.connect(":memory:")
            self._db.execute("PRAGMA busy_timeout = 5000")
        return self._db

    def _ensure_parent(self, path: str) -> None:
        parts = path.rstrip("/").split("/")
        for i in range(1, len(parts)):
            self._dirs.add("/".join(parts[:i]) or "/")

    async def read_file(self, path: str) -> str:
        if path not in self._files:
            raise FileNotFoundError(path)
        return self._files[path]

    async def write_file(self, path: str, content: str, mode: int | None = None) -> None:
        self._ensure_parent(path)
        self._files[path] = content

    async def append_file(self, path: str, content: str, mode: int | None = None) -> None:
        self._ensure_parent(path)
        self._files[path] = self._files.get(path, "") + content

    async def exists(self, path: str) -> bool:
        return path in self._files or path in self._dirs

    async def stat(self, path: str) -> SessionFsFileInfo:
        now = dt.datetime.now(tz=dt.UTC)
        if path in self._dirs:
            return SessionFsFileInfo(
                is_file=False, is_directory=True, size=0, mtime=now, birthtime=now
            )
        if path in self._files:
            return SessionFsFileInfo(
                is_file=True,
                is_directory=False,
                size=len(self._files[path].encode()),
                mtime=now,
                birthtime=now,
            )
        raise FileNotFoundError(path)

    async def mkdir(self, path: str, recursive: bool, mode: int | None = None) -> None:
        if recursive:
            parts = path.rstrip("/").split("/")
            for i in range(1, len(parts) + 1):
                self._dirs.add("/".join(parts[:i]) or "/")
        else:
            self._dirs.add(path)

    async def readdir(self, path: str) -> list[str]:
        prefix = path.rstrip("/") + "/"
        names: set[str] = set()
        for p in list(self._files.keys()) + list(self._dirs):
            if p.startswith(prefix):
                rest = p[len(prefix) :]
                if rest:
                    names.add(rest.split("/")[0])
        return sorted(names)

    async def readdir_with_types(self, path: str) -> list[SessionFSReaddirWithTypesEntry]:
        prefix = path.rstrip("/") + "/"
        entries: dict[str, SessionFSReaddirWithTypesEntryType] = {}
        for p in self._dirs:
            if p.startswith(prefix):
                rest = p[len(prefix) :]
                if rest:
                    name = rest.split("/")[0]
                    entries[name] = SessionFSReaddirWithTypesEntryType.DIRECTORY
        for p in self._files:
            if p.startswith(prefix):
                rest = p[len(prefix) :]
                if rest:
                    name = rest.split("/")[0]
                    if name not in entries:
                        entries[name] = SessionFSReaddirWithTypesEntryType.FILE
        return [SessionFSReaddirWithTypesEntry(name=n, type=t) for n, t in sorted(entries.items())]

    async def rm(self, path: str, recursive: bool, force: bool) -> None:
        self._files.pop(path, None)
        self._dirs.discard(path)

    async def rename(self, src: str, dest: str) -> None:
        if src in self._files:
            self._ensure_parent(dest)
            self._files[dest] = self._files.pop(src)

    async def sqlite_query(
        self,
        query_type: SessionFSSqliteQueryType,
        query: str,
        params: dict[str, float | str | None] | None = None,
    ) -> SessionFsSqliteQueryResult | None:
        self._sqlite_calls.append(
            {
                "sessionId": self._session_id,
                "queryType": query_type.value,
                "query": query,
            }
        )

        db = self._get_or_create_db()
        trimmed = query.strip()
        if not trimmed:
            return SessionFsSqliteQueryResult(columns=[], rows=[], rows_affected=0)

        if query_type == SessionFSSqliteQueryType.EXEC:
            db.executescript(trimmed)
            db.commit()
            return SessionFsSqliteQueryResult(columns=[], rows=[], rows_affected=0)

        if query_type == SessionFSSqliteQueryType.QUERY:
            cursor = db.execute(trimmed, params or {})
            columns = [desc[0] for desc in cursor.description] if cursor.description else []
            rows = [dict(zip(columns, row)) for row in cursor.fetchall()]
            return SessionFsSqliteQueryResult(columns=columns, rows=rows, rows_affected=0)

        # run (INSERT/UPDATE/DELETE)
        cursor = db.execute(trimmed, params or {})
        db.commit()
        return SessionFsSqliteQueryResult(
            columns=[],
            rows=[],
            rows_affected=cursor.rowcount,
            last_insert_rowid=cursor.lastrowid if cursor.lastrowid else None,
        )

    async def sqlite_exists(self) -> bool:
        return self._db is not None


def _create_sqlite_handler(sqlite_calls: list[dict]):
    def factory(session):
        return _InMemorySessionFsSqliteProvider(session.session_id, sqlite_calls)

    return factory


@pytest_asyncio.fixture(scope="module", loop_scope="module")
async def sqlite_client(ctx: E2ETestContext):
    client = CopilotClient(
        CopilotClientOptions(
            connection=RuntimeConnection.stdio(path=ctx.cli_path),
            working_directory=ctx.work_dir,
            env=ctx.get_env(),
            github_token=DEFAULT_GITHUB_TOKEN,
            session_fs=SESSION_FS_CONFIG,
        )
    )
    yield client
    try:
        await client.stop()
    except Exception:
        await client.force_stop()


class TestSessionFsSqlite:
    async def test_should_route_sql_queries_through_the_sessionfs_sqlite_handler(
        self, sqlite_client: CopilotClient
    ):
        sqlite_calls: list[dict] = []
        session = await sqlite_client.create_session(
            on_permission_request=PermissionHandler.approve_all,
            create_session_fs_handler=_create_sqlite_handler(sqlite_calls),
        )

        await session.send_and_wait(
            'Use the sql tool to create a table called "items" with columns '
            "id (TEXT PRIMARY KEY) and name (TEXT). "
            'Then insert a row with id "a1" and name "Widget".'
        )

        session_calls = [c for c in sqlite_calls if c["sessionId"] == session.session_id]
        assert len(session_calls) > 0
        assert any("CREATE TABLE" in c["query"].upper() for c in session_calls)
        assert any("INSERT" in c["query"].upper() for c in session_calls)

        assert any(c["queryType"] == "exec" for c in session_calls)
        assert any(c["queryType"] == "run" for c in session_calls)

        await session.disconnect()

    async def test_should_allow_subagents_to_use_sql_tool_via_inherited_sessionfs(
        self, sqlite_client: CopilotClient
    ):
        sqlite_calls: list[dict] = []
        providers: dict[str, _InMemorySessionFsSqliteProvider] = {}

        def handler_factory(session):
            provider = _InMemorySessionFsSqliteProvider(session.session_id, sqlite_calls)
            providers[session.session_id] = provider
            return provider

        session = await sqlite_client.create_session(
            on_permission_request=PermissionHandler.approve_all,
            create_session_fs_handler=handler_factory,
        )

        await session.send_and_wait(
            "Use the task tool to ask a task agent to do the following: "
            "Use the sql tool to run this query: INSERT INTO todos "
            "(id, title, status) VALUES ('subagent-test', 'Created by subagent', 'done')"
        )

        await session.disconnect()

        session_calls = [c for c in sqlite_calls if c["sessionId"] == session.session_id]
        insert_calls = [c for c in session_calls if "INSERT" in c["query"].upper()]
        assert len(insert_calls) > 0

        # Read events.jsonl from in-memory FS
        provider = providers[session.session_id]
        events_path = f"{SESSION_STATE_PATH}/events.jsonl"
        content = await provider.read_file(events_path)
        lines = [line for line in content.split("\n") if line.strip()]
        parsed = [json.loads(line) for line in lines]
        sql_tool_events = [
            e
            for e in parsed
            if e.get("type") == "tool.execution_start"
            and e.get("data", {}).get("toolName") == "sql"
        ]
        assert len(sql_tool_events) > 0
        assert all(e.get("agentId") for e in sql_tool_events)
