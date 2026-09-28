# Copyright (c) Microsoft Corporation. All rights reserved.
# Licensed under the MIT License.

"""Public lifecycle regressions for host cleanup after stdio EOF."""

import asyncio
import shutil
import subprocess
import sys
import time
from pathlib import Path

import pytest

from copilot import CopilotClient, RuntimeConnection

_RUNTIME = Path(__file__).parent.parent / "test" / "harness" / "stdio-shutdown-runtime.cjs"

_EXIT_PROBE = """
const fs = require("node:fs");
const { spawnSync } = require("node:child_process");
const pid = Number(fs.readFileSync(process.argv[1], "utf8"));
const deadline = Date.now() + Number(process.argv[2]);
function check() {
    try {
        process.kill(pid, 0);
    } catch (error) {
        if (error.code === "ESRCH") return;
        throw error;
    }
    // Force-stop does not reap children; a zombie has already exited.
    if (process.platform === "linux") {
        try {
            const stat = fs.readFileSync(`/proc/${pid}/stat`, "utf8");
            // The parenthesized command name can itself contain spaces and parentheses.
            if (stat.charAt(stat.lastIndexOf(")") + 2) === "Z") return;
        } catch (error) {
            if (error.code === "ENOENT") return;
            throw error;
        }
    } else if (process.platform !== "win32") {
        const status = spawnSync("ps", ["-o", "stat=", "-p", String(pid)], { encoding: "utf8" });
        if (status.status === 0 && status.stdout.trim().startsWith("Z")) return;
    }
    if (Date.now() >= deadline) throw new Error("Child still running");
    setTimeout(check, 25);
}
check();
"""


async def _assert_child_exited(directory: Path, wait_millis: int = 0) -> None:
    node = shutil.which("node")
    assert node is not None
    result = await asyncio.to_thread(
        subprocess.run,
        [node, "-e", _EXIT_PROBE, str(directory / "pid"), str(wait_millis)],
        capture_output=True,
        text=True,
        timeout=10,
        check=False,
    )
    assert result.returncode == 0, result.stdout + result.stderr


async def test_exit_probe_distinguishes_running_and_exited_child(tmp_path, monkeypatch):
    node = shutil.which("node")
    assert node is not None
    if sys.platform == "linux":
        # Linux must not depend on procps options absent from Alpine's BusyBox ps.
        (tmp_path / "node").symlink_to(node)
        monkeypatch.setenv("PATH", str(tmp_path))
    child = subprocess.Popen([node, "-e", "setInterval(() => {}, 1000)"])
    try:
        (tmp_path / "pid").write_text(str(child.pid))
        with pytest.raises(AssertionError, match="Child still running"):
            await _assert_child_exited(tmp_path)
        child.kill()
        # Keep the Popen alive without wait/poll so POSIX retains an exited zombie.
        await _assert_child_exited(tmp_path, wait_millis=5000)
    finally:
        child.kill()
        child.wait(timeout=10)


def _client(directory: Path, mode: str) -> CopilotClient:
    node = shutil.which("node")
    assert node is not None, "Node.js is required for the shared SDK shutdown fixture"
    return CopilotClient(
        connection=RuntimeConnection.for_stdio(
            path=node,
            args=[
                str(_RUNTIME),
                str(directory / "cleanup.jsonl"),
                mode,
                str(directory / "pid"),
            ],
        )
    )


@pytest.mark.parametrize("mode", ["stop", "dispose"])
@pytest.mark.timeout(60)
async def test_graceful_shutdown_waits_for_host_cleanup(tmp_path, mode):
    client = _client(tmp_path, mode)
    try:
        if mode == "dispose":
            async with client:
                assert not (tmp_path / "cleanup.jsonl").exists()
        else:
            await client.start()
            assert not (tmp_path / "cleanup.jsonl").exists()
            await client.stop()
        assert (tmp_path / "cleanup.jsonl").read_text() == '{"type":"span"}\n'
        await _assert_child_exited(tmp_path)
        with pytest.raises(RuntimeError, match="Client is not connected"):
            _ = client.rpc
    finally:
        await client.force_stop()


@pytest.mark.timeout(60)
async def test_force_stop_does_not_run_graceful_host_cleanup(tmp_path):
    client = _client(tmp_path, "force")
    try:
        await client.start()
        started = time.monotonic()
        await asyncio.wait_for(client.force_stop(), timeout=30)
        assert time.monotonic() - started < 10
        await _assert_child_exited(tmp_path, wait_millis=5000)
        assert not (tmp_path / "cleanup.jsonl").exists()
        with pytest.raises(RuntimeError, match="Client is not connected"):
            _ = client.rpc
    finally:
        await client.force_stop()


@pytest.mark.timeout(60)
async def test_stop_terminates_uncooperative_child_after_grace_period(tmp_path):
    client = _client(tmp_path, "fallback")
    try:
        await client.start()
        started = time.monotonic()
        await asyncio.wait_for(client.stop(), timeout=45)
        assert time.monotonic() - started >= 10
        assert (tmp_path / "cleanup.jsonl").read_text() == '{"type":"span"}\n'
        await _assert_child_exited(tmp_path)
        with pytest.raises(RuntimeError, match="Client is not connected"):
            _ = client.rpc
    finally:
        await client.force_stop()


@pytest.mark.timeout(60)
async def test_force_stop_cleans_up_after_startup_failure(tmp_path):
    client = _client(tmp_path, "start-failure")
    try:
        with pytest.raises(RuntimeError, match="[Pp]rotocol"):
            await client.start()
        await asyncio.wait_for(client.force_stop(), timeout=30)
        await _assert_child_exited(tmp_path, wait_millis=5000)
        assert not (tmp_path / "cleanup.jsonl").exists()
        with pytest.raises(RuntimeError, match="Client is not connected"):
            _ = client.rpc
    finally:
        await client.force_stop()
