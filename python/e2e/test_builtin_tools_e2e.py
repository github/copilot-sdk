"""Smoke E2E coverage for Copilot CLI built-in tools."""

from __future__ import annotations

import os
import re
from pathlib import Path

import pytest

from copilot.session import PermissionHandler

from .testharness import E2ETestContext

pytestmark = pytest.mark.asyncio(loop_scope="module")


class TestBuiltinTools:
    async def test_should_capture_exit_code_in_output(self, ctx: E2ETestContext):
        session = await ctx.client.create_session(
            on_permission_request=PermissionHandler.approve_all
        )
        try:
            message = await session.send_and_wait(
                "Run 'echo hello && echo world'. Tell me the exact output."
            )
            content = message.data.content if message else ""
            assert "hello" in content
            assert "world" in content
        finally:
            await session.disconnect()

    @pytest.mark.skipif(
        os.name == "nt",
        reason="The stderr prompt uses bash syntax and is skipped by the TS suite on Windows.",
    )
    async def test_should_capture_stderr_output(self, ctx: E2ETestContext):
        session = await ctx.client.create_session(
            on_permission_request=PermissionHandler.approve_all
        )
        try:
            message = await session.send_and_wait(
                "Run 'echo error_msg >&2; echo ok' and tell me what stderr said. "
                "Reply with just the stderr content."
            )
            assert message is not None
            assert "error_msg" in message.data.content
        finally:
            await session.disconnect()

    async def test_should_read_file_with_line_range(self, ctx: E2ETestContext):
        Path(ctx.work_dir, "lines.txt").write_text(
            "line1\nline2\nline3\nline4\nline5\n", encoding="utf-8", newline="\n"
        )
        session = await ctx.client.create_session(
            on_permission_request=PermissionHandler.approve_all
        )
        try:
            message = await session.send_and_wait(
                "Read lines 2 through 4 of the file 'lines.txt' in this directory. "
                "Tell me what those lines contain."
            )
            content = message.data.content if message else ""
            assert "line2" in content
            assert "line4" in content
        finally:
            await session.disconnect()

    async def test_should_handle_nonexistent_file_gracefully(self, ctx: E2ETestContext):
        session = await ctx.client.create_session(
            on_permission_request=PermissionHandler.approve_all
        )
        try:
            message = await session.send_and_wait(
                "Try to read the file 'does_not_exist.txt'. "
                "If it doesn't exist, say 'FILE_NOT_FOUND'."
            )
            content = message.data.content if message else ""
            assert re.search(
                r"NOT.FOUND|NOT.EXIST|NO.SUCH|FILE_NOT_FOUND|DOES.NOT.EXIST|ERROR",
                content,
                re.IGNORECASE,
            )
        finally:
            await session.disconnect()

    async def test_should_edit_a_file_successfully(self, ctx: E2ETestContext):
        Path(ctx.work_dir, "edit_me.txt").write_text(
            "Hello World\nGoodbye World\n", encoding="utf-8", newline="\n"
        )
        session = await ctx.client.create_session(
            on_permission_request=PermissionHandler.approve_all
        )
        try:
            message = await session.send_and_wait(
                "Edit the file 'edit_me.txt': replace 'Hello World' with "
                "'Hi Universe'. Then read it back and tell me its contents."
            )
            assert message is not None
            assert "Hi Universe" in message.data.content
        finally:
            await session.disconnect()

    async def test_should_create_a_new_file(self, ctx: E2ETestContext):
        session = await ctx.client.create_session(
            on_permission_request=PermissionHandler.approve_all
        )
        try:
            message = await session.send_and_wait(
                "Create a file called 'new_file.txt' with the content "
                "'Created by test'. Then read it back to confirm."
            )
            assert message is not None
            assert "Created by test" in message.data.content
        finally:
            await session.disconnect()

    async def test_should_search_for_patterns_in_files(self, ctx: E2ETestContext):
        Path(ctx.work_dir, "data.txt").write_text(
            "apple\nbanana\napricot\ncherry\n", encoding="utf-8", newline="\n"
        )
        session = await ctx.client.create_session(
            on_permission_request=PermissionHandler.approve_all
        )
        try:
            message = await session.send_and_wait(
                "Search for lines starting with 'ap' in the file 'data.txt'. "
                "Tell me which lines matched."
            )
            content = message.data.content if message else ""
            assert "apple" in content
            assert "apricot" in content
        finally:
            await session.disconnect()

    async def test_should_find_files_by_pattern(self, ctx: E2ETestContext):
        src_dir = Path(ctx.work_dir, "src")
        src_dir.mkdir()
        Path(src_dir, "index.ts").write_text("export const index = 1;", encoding="utf-8")
        Path(ctx.work_dir, "README.md").write_text("# Readme", encoding="utf-8")

        session = await ctx.client.create_session(
            on_permission_request=PermissionHandler.approve_all
        )
        try:
            message = await session.send_and_wait(
                "Find all .ts files in this directory (recursively). List the filenames you found."
            )
            assert message is not None
            assert "index.ts" in message.data.content
        finally:
            await session.disconnect()
