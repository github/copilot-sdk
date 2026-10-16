// Copyright (c) Microsoft Corporation. All rights reserved.

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

#[test]
fn reports_http_reason_and_distinguishes_other_errors() {
    assert_eq!(
        download::message(&ureq::Error::StatusCode(407)),
        "HTTP 407 Proxy Authentication Required"
    );
    assert_eq!(
        download::message(&ureq::Error::InvalidProxyUrl),
        "download error: invalid proxy url"
    );
}

#[test]
fn body_deadline_exceeds_prior_two_minute_limit() {
    assert!(download::BODY_TIMEOUT > std::time::Duration::from_secs(120));
}
