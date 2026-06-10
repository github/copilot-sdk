"""
E2E coverage for session-scoped UI ephemeral query RPC.

Mirrors ``dotnet/test/E2E/RpcUiEphemeralQueryE2ETests.cs`` (snapshot
category ``rpc_ui_ephemeral_query``).
"""

from __future__ import annotations

import pytest

from copilot.rpc import UIEphemeralQueryRequest
from copilot.session import PermissionHandler

from .testharness import E2ETestContext

pytestmark = pytest.mark.asyncio(loop_scope="module")


class TestRpcUiEphemeralQuery:
    async def test_should_answer_ephemeral_query(self, ctx: E2ETestContext):
        async with await ctx.client.create_session(
            on_permission_request=PermissionHandler.approve_all,
        ) as session:
            result = await session.rpc.ui.ephemeral_query(
                UIEphemeralQueryRequest(
                    question="In one word, what is the primary color of a clear daytime sky?"
                )
            )

            assert result is not None
            assert (result.answer or "").strip()
            assert "blue" in result.answer.lower()
