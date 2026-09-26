# Copyright (c) Microsoft Corporation. All rights reserved.
# Licensed under the MIT License.

import inspect
from unittest.mock import AsyncMock, Mock

import pytest

from copilot import CopilotClient, RuntimeConnection


@pytest.mark.parametrize("refresh", [True, False, None], ids=["true", "false", "omitted"])
async def test_refresh_custom_instructions_is_create_only(refresh):
    client = CopilotClient(connection=RuntimeConnection.for_uri("localhost:1234"))
    request = AsyncMock(return_value={"sessionId": "refresh-instructions"})
    client._client = Mock(request=request)

    options = {} if refresh is None else {"refresh_custom_instructions": refresh}
    await client.create_session(session_id="refresh-instructions", **options)

    request.assert_awaited_once()
    method, payload = request.call_args.args
    assert method == "session.create"
    if refresh is None:
        assert "refreshCustomInstructions" not in payload
    else:
        assert payload.get("refreshCustomInstructions") is refresh

    request.reset_mock()
    await client.resume_session("refresh-instructions")

    request.assert_awaited_once()
    method, payload = request.call_args.args
    assert method == "session.resume"
    assert "refreshCustomInstructions" not in payload
    assert (
        "refresh_custom_instructions"
        not in inspect.signature(CopilotClient.resume_session).parameters
    )
