// Copyright (c) Microsoft Corporation. All rights reserved.

// ureq 3 has no per-read timeout; allow slow archives while bounding stalled transfers.
pub(super) const BODY_TIMEOUT: std::time::Duration = std::time::Duration::from_secs(15 * 60);

pub(super) fn is_transient(error: &ureq::Error) -> bool {
    match error {
        ureq::Error::StatusCode(code) => (500..600).contains(code),
        ureq::Error::BadUri(_)
        | ureq::Error::Http(_)
        | ureq::Error::InvalidProxyUrl
        | ureq::Error::TooManyRedirects
        | ureq::Error::RedirectFailed
        | ureq::Error::RequireHttpsOnly(_)
        | ureq::Error::TlsRequired
        | ureq::Error::Tls(_)
        | ureq::Error::NativeTls(_)
        | ureq::Error::Pem(_)
        | ureq::Error::Der(_)
        | ureq::Error::BodyExceedsLimit(_)
        | ureq::Error::LargeResponseHeader(..) => false,
        _ => true,
    }
}

pub(super) fn message(error: &ureq::Error) -> String {
    match error {
        ureq::Error::StatusCode(code) => {
            let reason = ureq::http::StatusCode::from_u16(*code)
                .ok()
                .and_then(|status| status.canonical_reason());
            match reason {
                Some(reason) => format!("HTTP {code} {reason}"),
                None => format!("HTTP {code}"),
            }
        }
        _ => format!("download error: {error}"),
    }
}
