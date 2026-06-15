"""E2E coverage for session.todos_changed and SQL todo dependency reads."""

from __future__ import annotations

import pytest

from copilot.session import PermissionHandler

from .testharness import E2ETestContext

pytestmark = pytest.mark.asyncio(loop_scope="module")


PROMPT = """Use the sql tool to execute exactly these statements, in order, with no extra rows:
1. INSERT INTO todos (id, title, status) VALUES ('alpha', 'First todo', 'pending');
2. INSERT INTO todos (id, title, status) VALUES ('beta', 'Second todo', 'done');
3. INSERT INTO todo_deps (todo_id, depends_on) VALUES ('beta', 'alpha');
Then stop. Do not insert any other rows or create any other tables."""


def _event_type_value(event) -> str:
    return getattr(event.type, "value", event.type)


class TestSessionTodosChanged:
    async def test_fires_session_todos_changed_and_exposes_rows_and_dependencies(
        self, ctx: E2ETestContext
    ):
        async with await ctx.client.create_session(
            on_permission_request=PermissionHandler.approve_all,
        ) as session:
            events = []
            unsubscribe = session.on(events.append)
            try:
                await session.send_and_wait(PROMPT, timeout=120.0)
            finally:
                unsubscribe()

            todos_events = [
                event for event in events if _event_type_value(event) == "session.todos_changed"
            ]
            assert len(todos_events) >= 1

            result = await session.rpc.plan.read_sql_todos_with_dependencies()
            ids = sorted(row.id for row in result.rows if row.id)
            assert ids == ["alpha", "beta"]

            assert any(
                dependency.todo_id == "beta" and dependency.depends_on == "alpha"
                for dependency in result.dependencies
            )
