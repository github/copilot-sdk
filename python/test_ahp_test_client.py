# Copyright (c) Microsoft Corporation. All rights reserved.

"""Initial-authentication recovery keeps the original read owner."""

import asyncio
import json

import pytest

from e2e.testharness.ahp import AhpTestClient


class _Peer:
    def __init__(self):
        self.stdout = asyncio.StreamReader()
        self.stdin = self
        self.returncode = None
        self.commands = []
        self.second_write = asyncio.Event()
        self.first_read = asyncio.Event()
        self.reads = 0
        self.pending_cancelled = False
        self.block_second_drain = False
        self.drain_released = asyncio.get_running_loop().create_future()
        self.stdout.feed_data(b'{"ready": true}\n')
        self.readline = self.stdout.readline
        self.stdout.readline = self._readline

    async def _readline(self):
        self.reads += 1
        if self.reads == 2:
            self.first_read.set()
        try:
            return await self.readline()
        except asyncio.CancelledError:
            self.pending_cancelled = True
            raise

    def write(self, data):
        self.commands.append(json.loads(data))
        if len(self.commands) == 2:
            self.second_write.set()

    async def drain(self):
        if len(self.commands) == 2 and self.block_second_drain:
            try:
                await self.drain_released
            except asyncio.CancelledError:
                self.pending_cancelled = True
                raise

    def reply(self, response):
        self.stdout.feed_data((json.dumps(response) + "\n").encode())

    def finish_second_exchange(self):
        self.reply({"result": {"admitted": True}})
        if not self.drain_released.done():
            self.drain_released.set_result(None)

    def close(self):
        self.returncode = 0
        self.stdout.feed_eof()

    async def wait(self):
        assert self.returncode == 0
        return 0


def _run(monkeypatch, scenario):
    async def run():
        peer = _Peer()

        async def start(*args, **kwargs):
            return peer

        with monkeypatch.context() as scoped:
            scoped.setattr(asyncio, "create_subprocess_exec", start)
            async with AhpTestClient() as client:
                await scenario(peer, client, scoped)
        assert peer.returncode == 0

    asyncio.run(run())


@pytest.mark.parametrize("phase", ["read", "drain"])
def test_recovery_does_not_replenish_original_read_budget(monkeypatch, phase):
    async def scenario(peer, client, scoped):
        loop = asyncio.get_running_loop()
        now = 0.0
        with scoped.context() as clock:
            clock.setattr(loop, "time", lambda: now)
            peer.block_second_drain = phase == "drain"
            request = asyncio.create_task(
                client.request(
                    {"op": "authenticate", "token": "original"},
                    retry_expired_error=lambda error: error == "expired fixture",
                )
            )
            await peer.first_read.wait()
            now = 59.0
            peer.reply({"error": "expired fixture"})
            await peer.second_write.wait()
            loop.call_at(61.0, peer.finish_second_exchange)
            now = 61.0
            with pytest.raises(TimeoutError):
                await request
            assert peer.pending_cancelled
            assert peer.commands == [{"op": "authenticate", "token": "original"}] * 2

    _run(monkeypatch, scenario)


def test_unmatched_failure_is_not_retried(monkeypatch):
    async def scenario(peer, client, scoped):
        observed = []

        def retry(error):
            observed.append(error)
            return error == "expired fixture"

        request = asyncio.create_task(
            client.request({"op": "authenticate"}, retry_expired_error=retry)
        )
        await peer.first_read.wait()
        peer.reply({"error": "transport failure"})
        with pytest.raises(RuntimeError, match="^transport failure$"):
            await request
        assert observed == ["transport failure"]
        assert peer.commands == [{"op": "authenticate"}]

    _run(monkeypatch, scenario)


def test_caller_cancellation_is_terminal_and_joins_pending_read(monkeypatch):
    async def scenario(peer, client, scoped):
        request = asyncio.create_task(
            client.request({"op": "authenticate"}, retry_expired_error=lambda _: True)
        )
        await peer.first_read.wait()
        request.cancel()
        with pytest.raises(asyncio.CancelledError):
            await request
        assert peer.pending_cancelled
        assert peer.commands == [{"op": "authenticate"}]

    _run(monkeypatch, scenario)
