# Copyright (c) Microsoft Corporation. All rights reserved.

"""Image generation request serialization without starting a runtime."""

import json
from unittest.mock import AsyncMock, Mock

import pytest

from copilot import CopilotClient, ImageGenerationConfig, PermissionHandler, RuntimeConnection


@pytest.mark.asyncio
@pytest.mark.parametrize("option", [None, {}, {"enabled": True}, {"enabled": False}])
async def test_image_generation_create_and_resume_wire(option: ImageGenerationConfig | None):
    client = CopilotClient(connection=RuntimeConnection.for_stdio(path="unused-cli"))
    client._state = "connected"
    transport = Mock()
    captured = {}

    async def request(method, params, **kwargs):
        captured[method] = json.loads(json.dumps(params))
        response = {"sessionId": params["sessionId"]}
        if callback := kwargs.get("on_response_inline"):
            callback(response)
        return response

    transport.request = AsyncMock(side_effect=request)
    transport.stop = AsyncMock()
    client._client = transport
    try:
        session = await client.create_session(
            on_permission_request=PermissionHandler.approve_all,
            image_generation=option,
        )
        await client.resume_session(
            session.session_id,
            on_permission_request=PermissionHandler.approve_all,
            image_generation=option,
        )
        for method in ("session.create", "session.resume"):
            if option is None:
                assert "imageGeneration" not in captured[method], captured[method]
            else:
                assert captured[method]["imageGeneration"] == option
    finally:
        await client.force_stop()
