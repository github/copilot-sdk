// Copyright (c) Microsoft Corporation. All rights reserved.

package rpc

// ToSavedProviderConfiguration converts a discovery plan's provider to
// credential-free saved settings. Use the result as Provider and the plan's
// unchanged model as Model in the Providers.Config.AddModel request.
func ToSavedProviderConfiguration(provider NamedProviderConfig) SavedProviderConfigurationProviderInput {
	var providerType *SavedProviderConfigurationType
	if provider.Type != nil {
		value := SavedProviderConfigurationType(*provider.Type)
		providerType = &value
	}
	return SavedProviderConfigurationProviderInput{
		Name:          provider.Name,
		Type:          providerType,
		WireAPI:       provider.WireAPI,
		Transport:     provider.Transport,
		BaseURL:       provider.BaseURL,
		LocalModel:    provider.LocalModel,
		Azure:         provider.Azure,
		ModelProvider: provider.ModelProvider,
	}
}
