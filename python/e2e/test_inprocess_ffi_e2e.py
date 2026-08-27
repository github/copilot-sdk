"""E2E smoke test for the in-process (FFI) transport.

Starts a client over the in-process FFI transport, performs a ``ping``
round-trip through the native runtime library, and stops cleanly. Resolution of
the transport from ``COPILOT_SDK_DEFAULT_CONNECTION`` is exercised by the full
E2E suite running under the ``inprocess`` CI matrix cell, not here.

Mirrors nodejs/test/e2e/inprocess_ffi.e2e.test.ts.
"""

from __future__ import annotations

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
