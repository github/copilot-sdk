// Copyright (c) Microsoft Corporation. All rights reserved.

#[expect(
    dead_code,
    reason = "message is used only by shipping acquisition callers"
)]
#[path = "../build/download.rs"]
mod download;

#[test]
fn retries_server_and_connection_failures() {
    assert!(download::is_transient(&ureq::Error::StatusCode(503)));
    assert!(download::is_transient(&ureq::Error::Io(
        std::io::Error::other("disconnected")
    )));
    assert!(download::is_transient(&ureq::Error::HostNotFound));
    assert!(download::is_transient(&ureq::Error::BodyStalled));
}

#[test]
fn rejects_client_and_configuration_failures() {
    assert!(!download::is_transient(&ureq::Error::StatusCode(407)));
    assert!(!download::is_transient(&ureq::Error::BadUri(
        "invalid".into()
    )));
    assert!(!download::is_transient(&ureq::Error::InvalidProxyUrl));
    assert!(!download::is_transient(&ureq::Error::TooManyRedirects));
    assert!(!download::is_transient(&ureq::Error::TlsRequired));
}

#[cfg(not(target_os = "macos"))]
#[test]
fn native_tls_configuration_failures_remain_terminal() {
    let error = native_tls::Certificate::from_der(b"invalid certificate")
        .err()
        .expect("invalid DER must be rejected");
    let error: ureq::Error = error.into();
    assert!(matches!(&error, ureq::Error::NativeTls(_)));
    assert!(!download::is_transient(&error));
}

#[test]
fn builds_platform_tls_agent() {
    let agent = download::agent();
    #[cfg(target_os = "macos")]
    assert_eq!(
        agent.config().tls_config().provider(),
        ureq::tls::TlsProvider::Rustls
    );
    #[cfg(not(target_os = "macos"))]
    assert_eq!(
        agent.config().tls_config().provider(),
        ureq::tls::TlsProvider::NativeTls
    );
    assert!(matches!(
        agent.config().tls_config().root_certs(),
        ureq::tls::RootCerts::PlatformVerifier
    ));
}
