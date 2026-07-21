"""Helpers for terminating SDK-spawned CLI process trees."""

from __future__ import annotations

import os
import signal
import subprocess
import sys
import time


def popen_process_group_kwargs() -> dict[str, bool]:
    """Return Popen kwargs that isolate spawned CLI servers in their own process group."""
    if sys.platform == "win32":
        return {}
    return {"start_new_session": True}


def _terminate_windows_process_tree(pid: int, *, force: bool) -> None:
    args = ["taskkill", "/T", "/PID", str(pid)]
    if force:
        args.insert(1, "/F")
    kwargs: dict = {
        "capture_output": True,
        "check": False,
    }
    if hasattr(subprocess, "CREATE_NO_WINDOW"):
        kwargs["creationflags"] = subprocess.CREATE_NO_WINDOW
    subprocess.run(args, **kwargs)


def terminate_owned_cli_process(
    process: subprocess.Popen | None,
    *,
    graceful: bool = True,
    timeout: float = 5.0,
) -> bool:
    """Terminate an SDK-owned CLI process and its descendants.

    Returns True when the process is no longer running.
    """
    if process is None:
        return True

    if process.poll() is not None:
        return True

    pid = process.pid
    if sys.platform == "win32":
        _terminate_windows_process_tree(pid, force=not graceful)
    else:
        sig = signal.SIGTERM if graceful else signal.SIGKILL
        try:
            os.killpg(os.getpgid(pid), sig)
        except ProcessLookupError:
            return True
        except OSError:
            try:
                if graceful:
                    process.terminate()
                else:
                    process.kill()
            except OSError:
                return process.poll() is not None

    deadline = time.monotonic() + timeout
    while time.monotonic() < deadline:
        if process.poll() is not None:
            return True
        time.sleep(0.05)

    if graceful:
        return terminate_owned_cli_process(process, graceful=False, timeout=timeout)
    return process.poll() is not None
