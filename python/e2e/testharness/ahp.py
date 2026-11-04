"""Test-side subprocess using the standard TypeScript AHP 0.9 client."""

import asyncio
import json
from pathlib import Path
from typing import Any


class AhpTestClient:
    def __init__(self):
        self._process: asyncio.subprocess.Process | None = None
        self._lock = asyncio.Lock()

    async def __aenter__(self):
        sdk = Path(__file__).parents[3]
        self._process = await asyncio.create_subprocess_exec(
            "node",
            "--import",
            "tsx",
            str(sdk / "nodejs/test/e2e/harness/ahpTestDriver.ts"),
            cwd=sdk / "nodejs",
            stdin=asyncio.subprocess.PIPE,
            stdout=asyncio.subprocess.PIPE,
            limit=4 * 1024 * 1024,
        )
        try:
            assert await self._read() == {"ready": True}
        except BaseException:
            if self._process.returncode is None:
                self._process.kill()
            await self._process.wait()
            raise
        return self

    async def _read(self) -> dict[str, Any]:
        assert self._process and self._process.stdout
        line = await asyncio.wait_for(self._process.stdout.readline(), 60)
        if not line:
            raise RuntimeError("AHP test client closed its output unexpectedly")
        return json.loads(line)

    async def request(self, command: dict[str, Any]) -> dict[str, Any]:
        async with self._lock:
            assert self._process and self._process.stdin
            self._process.stdin.write((json.dumps(command) + "\n").encode())
            await self._process.stdin.drain()
            response = await self._read()
            if "error" in response:
                raise RuntimeError(response["error"])
            return response["result"]

    async def __aexit__(self, *_):
        assert self._process and self._process.stdin
        self._process.stdin.close()
        try:
            code = await asyncio.wait_for(self._process.wait(), 10)
        except TimeoutError:
            self._process.kill()
            await self._process.wait()
            raise
        if code != 0:
            raise RuntimeError(f"AHP test client exited with status {code}")
