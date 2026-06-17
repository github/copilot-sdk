"""E2E coverage for session.todos_changed and SQL todo dependency reads."""

from __future__ import annotations

import asyncio

import pytest

from copilot.session import PermissionHandler

from .testharness import E2ETestContext, get_next_event_of_type

pytestmark = pytest.mark.asyncio(loop_scope="module")


PROMPT = (
    "Use the sql tool exactly once to execute all three of the following statements "
    "together, in this exact order, in a single sql tool call (a single query string "
    "containing all three statements):\n"
    "1. INSERT INTO todos (id, title, status) VALUES ('alpha', 'First todo', 'pending');\n"
    "2. INSERT INTO todos (id, title, status) VALUES ('beta', 'Second todo', 'done');\n"
    "3. INSERT INTO todo_deps (todo_id, depends_on) VALUES ('beta', 'alpha');\n"
    "Then stop. Do not insert any other rows or create any other tables."
)


class TestSessionTodosChanged:
    async def test_fires_session_todos_changed_and_exposes_rows_and_dependencies(
        self, ctx: E2ETestContext
    ):
        async with await ctx.client.create_session(
            on_permission_request=PermissionHandler.approve_all,
        ) as session:
            todos_changed = asyncio.create_task(
                get_next_event_of_type(session, "session.todos_changed", timeout=120.0)
            )
            await session.send_and_wait(PROMPT, timeout=120.0)
            await todos_changed

            result = await session.rpc.plan.read_sql_todos_with_dependencies()
            ids = sorted(row.id for row in result.rows if row.id)
            assert ids == ["alpha", "beta"]

            assert any(
                dependency.todo_id == "beta" and dependency.depends_on == "alpha"
                for dependency in result.dependencies
            )
