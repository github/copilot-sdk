"""E2E smoke test for the in-process (FFI) transport.

Starts a client over the in-process FFI transport, performs a ``ping``
round-trip through the native runtime library, and stops cleanly. Resolution of
the transport from ``COPILOT_SDK_DEFAULT_CONNECTION`` is exercised by the full
E2E suite running under the ``inprocess`` CI matrix cell, not here.

Mirrors nodejs/test/e2e/inprocess_ffi.e2e.test.ts.
"""

from __future__ import annotations

import asyncio

import pytest

from copilot import CopilotClient, RuntimeConnection

from .testharness import E2ETestContext

pytestmark = pytest.mark.asyncio(loop_scope="module")


class TestInProcessFfi:
    async def test_should_start_and_connect_over_in_process_ffi(self, ctx: E2ETestContext):
        # In-process hosting loads runtime.node directly. ``ping`` is a purely local
        # RPC round-trip, so no auth or replay proxy is involved.
        client = CopilotClient(connection=RuntimeConnection.for_inprocess())
        await client.start()

        try:
            pong = await client.ping("ffi message")
            assert pong.message == "pong: ffi message"
            assert pong.timestamp is not None
        finally:
            await client.stop()

    async def test_should_force_stop_over_in_process_ffi_within_bounded_time(
        self, ctx: E2ETestContext
    ):
        # Regression test for github/copilot-sdk#2525: the in-process FFI host's
        # dispose() used to call the native host_shutdown export synchronously
        # with no timeout. A slow or stuck native shutdown (observed on Windows,
        # closing the runtime's SQLite session store) would hang force_stop
        # indefinitely, even though force_stop exists specifically as the
        # recovery path for a hung/slow stop(). Asserting a bounded completion
        # time here catches any regression back to an unbounded wait.
        client = CopilotClient(connection=RuntimeConnection.for_inprocess())
        await client.start()
        await client.ping("hello before force_stop")

        await asyncio.wait_for(client.force_stop(), timeout=20.0)
