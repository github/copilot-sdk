//! JSON-RPC request/response types and typed namespace builders.
//!
//! All types are auto-generated from the Copilot CLI protocol schemas.
//! This module is the stable public access point — the underlying
//! crate-private modules where the types are defined are an
//! implementation detail whose layout may change.
//!
//! Use the [`crate::Client::rpc`] and [`crate::session::Session::rpc`] helper
//! methods to obtain a typed view over the protocol surface.

pub use crate::generated::api_types::*;
pub use crate::generated::rpc::*;

/// Convert a discovery plan's provider to credential-free saved settings.
///
/// Use the result as `provider` and the plan's unchanged model as `model` in the
/// `providers().config().add_model()` request.
/// Unknown provider families return an [`crate::ErrorKind::InvalidConfig`] error.
pub fn to_saved_provider_configuration(
    provider: NamedProviderConfig,
) -> crate::errors::Result<SavedProviderConfigurationProviderInput> {
    let provider_type = match provider.r#type {
        Some(ProviderConfigType::Openai) => Some(SavedProviderConfigurationType::Openai),
        Some(ProviderConfigType::Azure) => Some(SavedProviderConfigurationType::Azure),
        Some(ProviderConfigType::Anthropic) => Some(SavedProviderConfigurationType::Anthropic),
        Some(ProviderConfigType::Winml) => Some(SavedProviderConfigurationType::Winml),
        Some(ProviderConfigType::Unknown) => {
            return Err(crate::Error::with_message(
                crate::ErrorKind::InvalidConfig,
                "Cannot save an unknown discovered provider type",
            ));
        }
        None => None,
    };
    Ok(SavedProviderConfigurationProviderInput {
        name: provider.name,
        r#type: provider_type,
        wire_api: provider.wire_api,
        transport: provider.transport,
        base_url: provider.base_url,
        local_model: provider.local_model,
        azure: provider.azure,
        model_provider: provider.model_provider,
    })
}

impl SendRequest {
    /// Set the message provenance without changing other request options.
    ///
    /// When this is not called, the source field is omitted by default.
    pub fn with_source(mut self, source: crate::MessageSource) -> Self {
        self.source = Some(source.to_string());
        self
    }
}

#[cfg(test)]
mod tests;
