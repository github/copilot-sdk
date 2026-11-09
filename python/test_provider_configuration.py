# Copyright (c) Microsoft Corporation. All rights reserved.

import pytest

from copilot import rpc


@pytest.mark.parametrize("kind", ["openai", "azure", "anthropic", "winml", None])
def test_saved_provider_conversion_preserves_settings_without_credentials(kind):
    settings = {
        "name": "discovered",
        "baseUrl": "http://127.0.0.1:1234",
        "wireApi": "responses",
        "transport": "http",
        "azure": {"apiVersion": "2025-01-01"},
        "localModel": {"device": "gpu", "future": [1, True]},
        "modelProvider": "azure_openai",
    }
    if kind is not None:
        settings["type"] = kind
    provider = rpc.NamedProviderConfig.from_dict(
        {
            **settings,
            "apiKey": "fixture-key",
            "bearerToken": "fixture-token",
            "headers": {"Authorization": "fixture-header"},
            "hasBearerTokenProvider": True,
        }
    )
    original = provider.to_dict()

    saved = rpc.to_saved_provider_configuration(provider)

    assert saved.to_dict() == settings
    assert provider.to_dict() == original


def test_saved_provider_conversion_preserves_absent_optional_settings():
    settings = {"name": "minimal", "baseUrl": "http://127.0.0.1:1234"}
    provider = rpc.NamedProviderConfig.from_dict(settings)

    assert rpc.to_saved_provider_configuration(provider).to_dict() == settings
