# Copyright (c) Microsoft Corporation. All rights reserved.

from unittest.mock import AsyncMock

import pytest

from copilot import CopilotClient, RuntimeConnection
from copilot._jsonrpc import JsonRpcClient


@pytest.mark.asyncio
@pytest.mark.parametrize("roots", [None, [], ["relative-root", "other-root"]])
async def test_customization_roots_preserved_on_create_and_resume(monkeypatch, roots):
    client = CopilotClient(connection=RuntimeConnection.for_uri("localhost:1234"))
    rpc = AsyncMock(spec=JsonRpcClient)
    requests = {}

    async def request(method, params, **kwargs):
        requests[method] = params
        if method in ("session.create", "session.resume"):
            response = {"sessionId": params["sessionId"]}
            if callback := kwargs.get("on_response_inline"):
                callback(response)
            return response
        return {}

    rpc.request.side_effect = request
    monkeypatch.setattr(client, "_client", rpc)
    skills = None if roots is None else [f"{root}-skills" for root in roots]
    agents = None if roots is None else [f"{root}-agents" for root in roots]
    instructions = None if roots is None else [f"{root}-instructions" for root in roots]
    hooks = None if roots is None else [f"{root}-hooks" for root in roots]
    try:
        session = await client.create_session(
            working_directory="request-cwd",
            enable_config_discovery=False,
            enable_file_hooks=False,
            skill_directories=skills,
            agent_directories=agents,
            instruction_directories=instructions,
            hook_directories=hooks,
        )
        await client.resume_session(
            session.session_id,
            working_directory="resume-cwd",
            enable_config_discovery=False,
            enable_file_hooks=False,
            skill_directories=skills,
            agent_directories=agents,
            instruction_directories=instructions,
            hook_directories=hooks,
        )
        for method in ("session.create", "session.resume"):
            payload = requests[method]
            for key, expected in (
                ("skillDirectories", skills),
                ("agentDirectories", agents),
                ("instructionDirectories", instructions),
                ("hookDirectories", hooks),
            ):
                if expected is None:
                    assert key not in payload
                else:
                    assert payload[key] == expected
            assert payload["enableConfigDiscovery"] is False
            assert payload["enableFileHooks"] is False
            assert "additionalDirectories" not in payload
            assert "customAgents" not in payload
            assert "hooks" not in payload
    finally:
        await client.force_stop()
