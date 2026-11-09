/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

import type {
    NamedProviderConfig,
    SavedProviderConfigurationProviderInput,
} from "./generated/rpc.js";

/** Converts a discovered provider to credential-free saved configuration. */
export function toSavedProviderConfiguration(
    provider: NamedProviderConfig
): SavedProviderConfigurationProviderInput {
    return {
        name: provider.name,
        type: provider.type,
        wireApi: provider.wireApi,
        transport: provider.transport,
        baseUrl: provider.baseUrl,
        localModel: provider.localModel,
        azure: provider.azure,
        modelProvider: provider.modelProvider,
    };
}
