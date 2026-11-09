/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot;

import java.util.Objects;

import com.github.copilot.generated.rpc.NamedProviderConfig;
import com.github.copilot.generated.rpc.SavedProviderConfigurationProviderInput;
import com.github.copilot.generated.rpc.SavedProviderConfigurationType;

/**
 * Conversions from discovered providers to saved provider settings.
 */
@CopilotExperimental
public final class ProviderConfiguration {
    private ProviderConfiguration() {
    }

    /**
     * Converts a discovery plan's provider to credential-free saved settings. Use
     * the result as {@code provider} and the plan's unchanged model as
     * {@code model} in the {@code providers().config().addModel()} request.
     *
     * @param provider
     *            the provider from a discovery plan
     * @return saved settings without inline credentials, headers or token callbacks
     */
    public static SavedProviderConfigurationProviderInput toSavedProviderConfiguration(NamedProviderConfig provider) {
        Objects.requireNonNull(provider, "provider");
        var saved = SavedProviderConfigurationProviderInput.builder().name(provider.name()).type(
                provider.type() == null ? null : SavedProviderConfigurationType.fromValue(provider.type().getValue()))
                .wireApi(provider.wireApi()).transport(provider.transport()).baseUrl(provider.baseUrl())
                .azure(provider.azure()).modelProvider(provider.modelProvider());
        if (provider.localModel() != null) {
            saved.localModel(provider.localModel());
        }
        return saved.build();
    }
}
