"""E2E coverage for workspace checkpoint, diff, and large-paste RPCs."""

from __future__ import annotations

import subprocess
import uuid
from pathlib import Path

import pytest

from copilot.generated.rpc import (
    WorkspaceDiffFileChangeType,
    WorkspaceDiffMode,
    WorkspacesDiffRequest,
    WorkspacesReadCheckpointRequest,
    WorkspacesReadFileRequest,
    WorkspacesSaveLargePasteRequest,
)
from copilot.session import PermissionHandler

from .testharness import E2ETestContext

pytestmark = pytest.mark.asyncio(loop_scope="module")


def _run_git(repo: Path, *args: str) -> None:
    subprocess.run(
        ["git", *args],
        cwd=repo,
        check=True,
        stdout=subprocess.DEVNULL,
        stderr=subprocess.DEVNULL,
    )


def _create_repo_with_unstaged_changes(work_dir: str) -> Path:
    repo = Path(work_dir) / f"workspace-diff-{uuid.uuid4().hex}"
    repo.mkdir(parents=True)
    _run_git(repo, "init")
    _run_git(repo, "config", "user.email", "copilot-sdk-e2e@example.com")
    _run_git(repo, "config", "user.name", "Copilot SDK E2E")

    (repo / "tracked.txt").write_text("before\n", encoding="utf-8", newline="\n")
    (repo / "removed.txt").write_text("remove me\n", encoding="utf-8", newline="\n")
    _run_git(repo, "add", "tracked.txt", "removed.txt")
    _run_git(repo, "commit", "-m", "initial")

    (repo / "tracked.txt").write_text("after\n", encoding="utf-8", newline="\n")
    (repo / "removed.txt").unlink()
    return repo


class TestRpcWorkspaceCheckpoints:
    async def test_should_list_no_checkpoints_for_fresh_session(self, ctx: E2ETestContext):
        session = await ctx.client.create_session(
            on_permission_request=PermissionHandler.approve_all,
        )
        try:
            result = await session.rpc.workspaces.list_checkpoints()
            assert result.checkpoints == []
        finally:
            await session.disconnect()

    async def test_should_return_null_or_empty_content_for_unknown_checkpoint(
        self, ctx: E2ETestContext
    ):
        session = await ctx.client.create_session(
            on_permission_request=PermissionHandler.approve_all,
        )
        try:
            result = await session.rpc.workspaces.read_checkpoint(
                WorkspacesReadCheckpointRequest(number=2_147_483_647)
            )
            assert not result.content
        finally:
            await session.disconnect()

    async def test_should_return_typed_workspace_diff_result_for_real_changes(
        self, ctx: E2ETestContext
    ):
        repo = _create_repo_with_unstaged_changes(ctx.work_dir)
        session = await ctx.client.create_session(
            on_permission_request=PermissionHandler.approve_all,
            working_directory=str(repo),
        )
        try:
            result = await session.rpc.workspaces.diff(
                WorkspacesDiffRequest(mode=WorkspaceDiffMode.UNSTAGED)
            )

            assert result.requested_mode == WorkspaceDiffMode.UNSTAGED
            assert result.mode in (WorkspaceDiffMode.UNSTAGED, WorkspaceDiffMode.BRANCH)
            by_path = {change.path.replace("\\", "/"): change for change in result.changes}

            tracked = by_path.get("tracked.txt")
            assert tracked is not None
            assert tracked.change_type == WorkspaceDiffFileChangeType.MODIFIED
            assert "after" in tracked.diff

            removed = by_path.get("removed.txt")
            assert removed is not None
            assert removed.change_type == WorkspaceDiffFileChangeType.DELETED
            assert "remove me" in removed.diff
        finally:
            await session.disconnect()

    async def test_should_save_large_paste_and_expose_readable_content(
        self, ctx: E2ETestContext
    ):
        session = await ctx.client.create_session(
            on_permission_request=PermissionHandler.approve_all,
        )
        try:
            content = "Large paste payload 🚀\n" * 512
            result = await session.rpc.workspaces.save_large_paste(
                WorkspacesSaveLargePasteRequest(content=content)
            )
            saved = result.saved

            assert saved is not None
            assert saved.filename
            assert saved.file_path
            assert saved.size_bytes == len(content.encode("utf-8"))

            try:
                read = await session.rpc.workspaces.read_file(
                    WorkspacesReadFileRequest(path=saved.filename)
                )
            except Exception:
                assert Path(saved.file_path).exists()
                assert Path(saved.file_path).read_text(encoding="utf-8") == content
            else:
                assert read.content == content
        finally:
            await session.disconnect()
