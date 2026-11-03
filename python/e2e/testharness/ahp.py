"""Test-side subprocess using the standard TypeScript AHP 0.9 client."""

import asyncio
import json
from collections.abc import Callable
from pathlib import Path
from typing import Any


class AhpTestClient:
    def __init__(self, driver_path: Path | None = None):
        self._process: asyncio.subprocess.Process | None = None
        self._lock = asyncio.Lock()
        self._driver_path = driver_path

    async def __aenter__(self):
        sdk = Path(__file__).parents[3]
        self._process = await asyncio.create_subprocess_exec(
            "node",
            "--import",
            "tsx",
            str(self._driver_path or sdk / "nodejs/test/e2e/harness/ahpTestDriver.ts"),
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
        return await asyncio.wait_for(self._read_response(), 60)

    async def _read_response(self) -> dict[str, Any]:
        assert self._process and self._process.stdout
        line = await self._process.stdout.readline()
        if not line:
            raise RuntimeError("AHP test client closed its output unexpectedly")
        return json.loads(line)

    async def request(
        self,
        command: dict[str, Any],
        *,
        retry_expired_error: Callable[[str], bool] | None = None,
    ) -> dict[str, Any]:
        async with self._lock:
            assert self._process and self._process.stdin
            self._process.stdin.write((json.dumps(command) + "\n").encode())
            await self._process.stdin.drain()
            if retry_expired_error is None:
                response = await self._read()
            else:
                deadline = asyncio.get_running_loop().time() + 60

                async def exchange():
                    while True:
                        response = await self._read_response()
                        if "error" not in response or not retry_expired_error(response["error"]):
                            return response
                        if asyncio.get_running_loop().time() >= deadline:
                            raise TimeoutError(
                                "Initial authentication exhausted its original read budget"
                            )
                        self._process.stdin.write((json.dumps(command) + "\n").encode())
                        await self._process.stdin.drain()

                response = await asyncio.wait_for(exchange(), 60)
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
