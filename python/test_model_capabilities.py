"""Model capability overrides must use the runtime schema's wire names."""

from dataclasses import fields
from unittest.mock import AsyncMock, Mock

import pytest

from copilot import (
    CopilotClient,
    ModelCapabilitiesOverride,
    ModelLimitsOverride,
    ModelSupportsOverride,
    ModelVisionLimitsOverride,
    RuntimeConnection,
)
from copilot.generated.rpc import ModelCapabilitiesOverride as RpcModelCapabilitiesOverride
from copilot.session import CopilotSession, PermissionHandler, _capabilities_to_dict


def test_capability_overrides_match_generated_contract():
    capabilities = ModelCapabilitiesOverride(
        supports=ModelSupportsOverride(vision=False, reasoning_effort=False),
        limits=ModelLimitsOverride(
            max_prompt_tokens=1_000_000,
            max_output_tokens=4096,
            max_context_window_tokens=1_048_576,
            vision=ModelVisionLimitsOverride(
                supported_media_types=["image/png"],
                max_prompt_images=2,
                max_prompt_image_size=1024,
            ),
        ),
    )
    expected = {
        "supports": {"vision": False, "reasoningEffort": False},
        "limits": {
            "max_prompt_tokens": 1_000_000,
            "max_output_tokens": 4096,
            "max_context_window_tokens": 1_048_576,
            "vision": {
                "supported_media_types": ["image/png"],
                "max_prompt_images": 2,
                "max_prompt_image_size": 1024,
            },
        },
    }

    wire = _capabilities_to_dict(capabilities)
    assert wire == expected
    assert RpcModelCapabilitiesOverride.from_dict(wire).to_dict() == expected


def test_capability_overrides_preserve_omission_zero_and_empty_values():
    assert _capabilities_to_dict(ModelCapabilitiesOverride()) == {}
    assert (
        _capabilities_to_dict(
            ModelCapabilitiesOverride(
                supports=ModelSupportsOverride(),
                limits=ModelLimitsOverride(vision=ModelVisionLimitsOverride()),
            )
        )
        == {}
    )
    assert _capabilities_to_dict(
        ModelCapabilitiesOverride(
            limits=ModelLimitsOverride(
                max_prompt_tokens=0,
                max_output_tokens=0,
                max_context_window_tokens=0,
                vision=ModelVisionLimitsOverride(
                    supported_media_types=[], max_prompt_images=0, max_prompt_image_size=0
                ),
            )
        )
    ) == {
        "limits": {
            "max_prompt_tokens": 0,
            "max_output_tokens": 0,
            "max_context_window_tokens": 0,
            "vision": {
                "supported_media_types": [],
                "max_prompt_images": 0,
                "max_prompt_image_size": 0,
            },
        }
    }


def test_model_limits_override_keeps_positional_constructor():
    vision = ModelVisionLimitsOverride(["image/png"], 2, 1024)
    limits = ModelLimitsOverride(1_000_000, 4096, 1_048_576, vision)

    assert [field.name for field in fields(limits)] == [
        "max_prompt_tokens",
        "max_output_tokens",
        "max_context_window_tokens",
        "vision",
    ]
    assert limits.max_prompt_tokens == 1_000_000
    assert limits.max_output_tokens == 4096
    assert limits.max_context_window_tokens == 1_048_576
    assert limits.vision is vision


@pytest.mark.parametrize("operation", ["create", "resume", "set_model"])
@pytest.mark.asyncio
async def test_session_operations_forward_capability_limits(operation):
    async def request(method, params, **kwargs):
        response = (
            {"sessionId": params["sessionId"]}
            if method in ("session.create", "session.resume")
            else {}
        )
        if callback := kwargs.get("on_response_inline"):
            callback(response)
        return response

    rpc = Mock(request=AsyncMock(side_effect=request))
    client = CopilotClient(connection=RuntimeConnection.for_uri("localhost:1234"))
    client._client = rpc
    client._state = "connected"
    capabilities = ModelCapabilitiesOverride(
        limits=ModelLimitsOverride(max_context_window_tokens=1_048_576)
    )

    if operation == "set_model":
        session = CopilotSession("capability-session", rpc)
        await session.set_model("custom-model", model_capabilities=capabilities)
        method = "session.model.switchTo"
    else:
        config = {
            "model_capabilities": capabilities,
            "on_permission_request": PermissionHandler.approve_all,
        }
        if operation == "create":
            await client.create_session(session_id="capability-session", **config)
        else:
            await client.resume_session("capability-session", **config)
        method = f"session.{operation}"

    payload = next(call.args[1] for call in rpc.request.await_args_list if call.args[0] == method)
    assert payload["modelCapabilities"] == {"limits": {"max_context_window_tokens": 1_048_576}}


@pytest.mark.parametrize("operation", ["create", "resume"])
@pytest.mark.parametrize("context_limit", [None, 0, 1_048_576])
@pytest.mark.asyncio
async def test_session_operations_forward_provider_context_limit(operation, context_limit):
    async def request(method, params, **kwargs):
        response = {"sessionId": params["sessionId"]}
        if callback := kwargs.get("on_response_inline"):
            callback(response)
        return response

    rpc = Mock(request=AsyncMock(side_effect=request))
    client = CopilotClient(connection=RuntimeConnection.for_uri("localhost:1234"))
    client._client = rpc
    client._state = "connected"
    provider = {"base_url": "https://provider.invalid/v1", "max_prompt_tokens": 900_000}
    if context_limit is not None:
        provider["max_context_window_tokens"] = context_limit
    config = {"provider": provider, "on_permission_request": PermissionHandler.approve_all}

    if operation == "create":
        await client.create_session(session_id="provider-session", **config)
    else:
        await client.resume_session("provider-session", **config)

    payload = next(
        call.args[1]
        for call in rpc.request.await_args_list
        if call.args[0] == f"session.{operation}"
    )
    expected = {"type": None, "baseUrl": "https://provider.invalid/v1", "maxPromptTokens": 900_000}
    if context_limit is not None:
        expected["maxContextWindowTokens"] = context_limit
    assert payload["provider"] == expected


def test_named_model_capabilities_use_the_same_wire_contract():
    client = CopilotClient(connection=RuntimeConnection.for_uri("localhost:1234"))
    capabilities = ModelCapabilitiesOverride(
        limits=ModelLimitsOverride(max_context_window_tokens=1_048_576)
    )
    wire = client._convert_model_to_wire_format(
        {"id": "custom", "provider": "example", "capabilities": capabilities}
    )

    assert wire == {
        "id": "custom",
        "provider": "example",
        "capabilities": {"limits": {"max_context_window_tokens": 1_048_576}},
    }
