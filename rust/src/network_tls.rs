// Copyright (c) Microsoft Corporation. All rights reserved.

//! Client-local TLS 1.3 with macOS certificate policy.
//!
//! native-tls 0.2 uses Apple's Secure Transport API, which only supports TLS
//! through 1.2 (https://developer.apple.com/documentation/security/secure-transport).
//! Selecting it would lose TLS-1.3-only upstreams. Network.framework can
//! provide native TLS 1.3, but the SDK's reqwest/tokio-tungstenite connectors
//! do not wrap it. Removing Rustls requires a native forwarding transport
//! preserving TLS 1.3, HTTP ALPN, platform trust, and hostname verification.
//!
//! Platform certificate verification does not make the record/key-exchange
//! crypto OS-backed: this config still uses AWS-LC. The standalone SDK has no
//! dependency on the in-tree copilot-platform-crypto provider; adopting a
//! distributable provider would be separate packaging and compatibility work.
//! That provider retains Rustls and lacks PQ/QUIC, with custom Windows crypto
//! exceptions; those are not proof that a macOS-only migration is impossible.

use rustls_platform_verifier::BuilderVerifierExt;
use tokio_rustls::rustls;

pub(crate) fn client_config() -> Result<rustls::ClientConfig, rustls::Error> {
    rustls::ClientConfig::builder_with_provider(std::sync::Arc::new(
        rustls::crypto::aws_lc_rs::default_provider(),
    ))
    .with_safe_default_protocol_versions()?
    .with_platform_verifier()
    .map(|builder| builder.with_no_client_auth())
}
