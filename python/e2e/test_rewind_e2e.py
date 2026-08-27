"""E2E coverage for rewinding tracked files and conversation history."""

from __future__ import annotations

import asyncio
import os
from pathlib import Path

import pytest

from copilot.rpc import (
    HistoryPreviewRewindRequest,
    HistoryRewindMode,
    HistoryRewindOutcome,
    HistoryRewindRequest,
)
from copilot.session import PermissionHandler

from .testharness import E2ETestContext

pytestmark = pytest.mark.asyncio(loop_scope="module")

FILE_NAME = "rewind-sdk.txt"
FILE_CONTENT = "SDK rewind content"


def _same_path(left: str | Path, right: str | Path) -> bool:
    return os.path.normcase(os.path.abspath(left)) == os.path.normcase(os.path.abspath(right))


class TestRewind:
    async def test_should_restore_tracked_file_and_conversation(self, ctx: E2ETestContext):
        file_path = Path(ctx.work_dir) / FILE_NAME
        session = await ctx.client.create_session(
            model="claude-sonnet-4.5",
            enable_file_change_tracking=True,
            on_permission_request=PermissionHandler.approve_all,
        )

        try:
            response = await session.send_and_wait(
                f"Use the create tool to create {FILE_NAME} containing exactly {FILE_CONTENT}. "
                "After the tool succeeds, reply with exactly SDK_REWIND_DONE."
            )

            assert response is not None
            assert response.data.content == "SDK_REWIND_DONE"
            assert file_path.read_text(encoding="utf-8") == FILE_CONTENT

            # File change capture settles asynchronously after the turn completes, so a
            # rewind point can briefly report zero restorable files. Poll until the
            # capture lands instead of sampling once; the assertions below still run if
            # it never does.
            rewind_points = await session.rpc.history.list_rewind_points()
            deadline = asyncio.get_running_loop().time() + 30
            while asyncio.get_running_loop().time() < deadline and not (
                rewind_points.unavailable_reason is None
                and rewind_points.points
                and rewind_points.points[0].can_restore_files
            ):
                await asyncio.sleep(0.1)
                rewind_points = await session.rpc.history.list_rewind_points()

            assert rewind_points.unavailable_reason is None
            assert rewind_points.file_change_tracking_enabled
            assert len(rewind_points.points) == 1
            rewind_point = rewind_points.points[0]
            assert rewind_point.can_restore_files
            assert rewind_point.file_count == 1

            preview = await session.rpc.history.preview_rewind(
                HistoryPreviewRewindRequest(event_id=rewind_point.event_id)
            )
            assert preview.available
            assert len(preview.files) == 1
            assert _same_path(preview.files[0].path, file_path)

            rewind = await session.rpc.history.rewind(
                HistoryRewindRequest(
                    event_id=rewind_point.event_id,
                    mode=HistoryRewindMode.CONVERSATION_AND_FILES,
                )
            )
            assert rewind.outcome == HistoryRewindOutcome.SUCCESS
            assert rewind.events_removed is not None and rewind.events_removed > 0
            assert len(rewind.restored_files) == 1
            assert _same_path(rewind.restored_files[0], file_path)
            assert not file_path.exists()

            events = await session.get_events()
            assert all(str(event.id) != rewind_point.event_id for event in events)
        finally:
            await session.disconnect()
