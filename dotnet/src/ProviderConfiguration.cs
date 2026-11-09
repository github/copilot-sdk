/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

using System.Diagnostics.CodeAnalysis;
using GitHub.Copilot.Rpc;
using RpcNamedProviderConfig = GitHub.Copilot.Rpc.NamedProviderConfig;

namespace GitHub.Copilot;

/// <summary>Conversions from discovered providers to saved provider settings.</summary>
[Experimental(Diagnostics.Experimental)]
public static class ProviderConfiguration
{
    /// <summary>
    /// Converts a discovery plan's provider to credential-free saved settings.
    /// Pass the result and the plan's unchanged model to <see cref="ServerProvidersConfigApi.AddModelAsync"/>.
    /// </summary>
    /// <param name="provider">The provider from a discovery plan.</param>
    /// <returns>Saved settings without inline credentials, headers or token callbacks.</returns>
    public static SavedProviderConfigurationProviderInput ToSavedProviderConfiguration(this RpcNamedProviderConfig provider)
    {
        ArgumentNullException.ThrowIfNull(provider);
        return new()
        {
            Name = provider.Name,
            Type = provider.Type is { } type ? new SavedProviderConfigurationType(type.Value) : null,
            WireApi = provider.WireApi,
            Transport = provider.Transport,
            BaseUrl = provider.BaseUrl,
            LocalModel = provider.LocalModel,
            Azure = provider.Azure,
            ModelProvider = provider.ModelProvider,
        };
    }
}
