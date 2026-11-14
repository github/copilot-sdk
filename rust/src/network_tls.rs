// Copyright (c) Microsoft Corporation. All rights reserved.

//! Client-local TLS 1.3 with macOS certificate policy.

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
