# Copyright (c) Microsoft Corporation. All rights reserved.

from .generated.rpc import (
    NamedProviderConfig,
    SavedProviderConfigurationProviderInput,
    SavedProviderConfigurationType,
)


def to_saved_provider_configuration(
    provider: NamedProviderConfig,
) -> SavedProviderConfigurationProviderInput:
    """Convert a discovery plan's provider to credential-free saved settings.

    Use the result as ``provider`` and the plan's unchanged model as ``model`` in
    the ``providers.config.add_model`` request.
    """
    return SavedProviderConfigurationProviderInput(
        name=provider.name,
        type=SavedProviderConfigurationType(provider.type.value) if provider.type else None,
        wire_api=provider.wire_api,
        transport=provider.transport,
        base_url=provider.base_url,
        local_model=provider.local_model,
        azure=provider.azure,
        model_provider=provider.model_provider,
    )
