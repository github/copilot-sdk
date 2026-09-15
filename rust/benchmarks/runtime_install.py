#!/usr/bin/env python3
"""macOS installer-only memory/latency probe using fresh processes and homes."""

import argparse
import ctypes
import hashlib
import json
import os
from pathlib import Path
import select
import signal
import subprocess
import sys
import tempfile
import time


class RusageInfoV0(ctypes.Structure):
    _fields_ = [("uuid", ctypes.c_uint8 * 16)] + [
        (name, ctypes.c_uint64)
        for name in (
            "user_time", "system_time", "pkg_idle_wkups", "interrupt_wkups",
            "pageins", "wired_size", "resident_size", "phys_footprint",
            "proc_start_abstime", "proc_exit_abstime",
        )
    ]


def memory(pid):
    info = RusageInfoV0()
    if LIBPROC.proc_pid_rusage(pid, 0, ctypes.byref(info)) != 0:
        raise OSError(ctypes.get_errno(), "proc_pid_rusage failed")
    return info.resident_size, info.phys_footprint


def read_line(process):
    if not select.select([process.stdout], [], [], 120)[0]:
        raise TimeoutError("installer did not respond within 120 seconds")
    line = process.stdout.readline().strip()
    if not line:
        raise RuntimeError("installer exited without a response")
    return line.split()


def run(binary, home, diagnostic_dir=None):
    env = {"HOME": str(home), "PATH": "/usr/bin:/bin", "TMPDIR": str(home)}
    if diagnostic_dir:
        env.update(MallocStackLogging="1", MallocStackLoggingNoCompact="1")
    with tempfile.TemporaryFile(mode="w+") as stderr:
        process = subprocess.Popen(
            ["/usr/bin/time", "-l", str(binary)],
            stdin=subprocess.PIPE, stdout=subprocess.PIPE, stderr=stderr,
            text=True, env=env, bufsize=1,
        )
        pid = None
        try:
            ready, pid = read_line(process)
            assert ready == "ready"
            pid = int(pid)
            initial_rss, initial_physical = memory(pid)
            process.stdin.write("\n")
            process.stdin.flush()
            while not select.select([process.stdout], [], [], 0.005)[0]:
                # The OS lifetime high-water marks below capture short spikes
                # that sampling could miss. This also checks process liveness.
                memory(pid)
            installed, elapsed = read_line(process)
            assert installed == "installed"
            time.sleep(1)
            retained_rss, retained_physical = memory(pid)
            if diagnostic_dir:
                diagnostic_dir.mkdir(parents=True, exist_ok=True)
                for name, command in (
                    ("vmmap.txt", ["vmmap", "-summary", str(pid)]),
                    ("allocations.txt", [
                        "malloc_history", str(pid), "-allBySize", "-fullStacks"
                    ]),
                    ("history.txt", [
                        "malloc_history", str(pid), "-allEvents", "-noContent"
                    ]),
                ):
                    with (diagnostic_dir / name).open("w") as output:
                        subprocess.run(
                            command, stdout=output, stderr=subprocess.STDOUT,
                            check=True, timeout=120,
                        )
            process.stdin.write("\n")
            process.stdin.flush()
            if process.wait(timeout=120):
                raise RuntimeError("installer process failed")
            stderr.seek(0)
            metrics = {}
            for line in stderr:
                for label, key in (
                    ("maximum resident set size", "peak_rss_bytes"),
                    ("peak memory footprint", "peak_physical_bytes"),
                ):
                    if label in line:
                        metrics[key] = int(line.split()[0])
            if len(metrics) != 2:
                raise RuntimeError("macOS time did not report both memory metrics")
            return dict(
                elapsed_seconds=float(elapsed),
                initial_rss_bytes=initial_rss,
                initial_physical_bytes=initial_physical,
                retained_rss_bytes=retained_rss,
                retained_physical_bytes=retained_physical,
                **metrics,
            )
        finally:
            if process.poll() is None:
                if pid is not None:
                    try:
                        os.kill(pid, signal.SIGKILL)
                    except ProcessLookupError:
                        pass
                process.kill()
                process.wait()
            process.stdin.close()
            process.stdout.close()


def digest(path):
    with path.open("rb") as file:
        return hashlib.file_digest(file, "sha256").hexdigest()


def inventory(home):
    root = home / "Library/Caches/github-copilot-sdk/cli"
    return {
        path.relative_to(root).as_posix(): {
            "bytes": path.stat().st_size,
            "sha256": digest(path),
            "mode": oct(path.stat().st_mode & 0o777),
        }
        for path in sorted(root.rglob("*")) if path.is_file()
    }


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("binary", type=Path)
    parser.add_argument("--runs", type=int, default=5)
    parser.add_argument("--diagnostics", type=Path)
    parser.add_argument("--cohort", choices=("cold", "warm", "corrupt", "truncated"))
    args = parser.parse_args()
    binary = args.binary.resolve(strict=True)
    if args.runs < 1:
        parser.error("--runs must be positive")
    result = {
        "binary_sha256": digest(binary), "instrumented": bool(args.diagnostics),
        "runs": [], "outputs": None,
    }
    cohorts = [args.cohort] if args.cohort else ("cold", "warm", "corrupt", "truncated")
    for cohort in cohorts:
        for iteration in range(args.runs):
            with tempfile.TemporaryDirectory(prefix="sdk-runtime-") as directory:
                home = Path(directory)
                expected = None
                if cohort != "cold":
                    run(binary, home)
                    expected = inventory(home)
                    runtime = next(home.rglob("runtime.node"))
                    if cohort == "corrupt":
                        with runtime.open("r+b") as file:
                            file.seek(-1, os.SEEK_END)
                            byte = file.read(1)
                            file.seek(-1, os.SEEK_END)
                            file.write(bytes([byte[0] ^ 0xFF]))
                    elif cohort == "truncated":
                        with runtime.open("r+b") as file:
                            file.truncate(1024)
                diagnostic_dir = (
                    args.diagnostics / f"{cohort}-{iteration}"
                    if args.diagnostics else None
                )
                measured = run(binary, home, diagnostic_dir)
                outputs = inventory(home)
                if expected is not None:
                    assert outputs == expected, "repair changed installed output"
                if result["outputs"] is not None:
                    assert outputs == result["outputs"], "output identity changed"
                result["outputs"] = outputs
                result["runs"].append(dict(cohort=cohort, iteration=iteration, **measured))
                print(f"{cohort} {iteration}: {measured}", file=sys.stderr)
    print(json.dumps(result, indent=2))


if __name__ == "__main__":
    if sys.platform != "darwin":
        sys.exit("This measurement harness requires macOS.")
    LIBPROC = ctypes.CDLL("/usr/lib/libproc.dylib", use_errno=True)
    LIBPROC.proc_pid_rusage.argtypes = [ctypes.c_int, ctypes.c_int, ctypes.c_void_p]
    LIBPROC.proc_pid_rusage.restype = ctypes.c_int
    main()
