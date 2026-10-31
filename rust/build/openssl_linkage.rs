// Copyright (c) Microsoft Corporation. All rights reserved.

pub(super) fn require_target_provider(shared_host_verified: bool) {
    if std::env::var("CARGO_CFG_TARGET_OS").as_deref() != Ok("linux")
        || std::env::var_os("CARGO_FEATURE_NETWORK_FORWARDING").is_none()
    {
        return;
    }
    let target = std::env::var("TARGET").expect("Cargo provides TARGET");
    // A linked provider on another target needs explicit shared-linkage
    // overrides. Runtime bindings attest their loader contract through Cargo
    // metadata instead. Check target vendoring independently in either case.
    let runtime_bindings = runtime_bindings();
    let shared_host_verified = runtime_bindings
        || (shared_host_verified && std::env::var("HOST").is_ok_and(|host| host == target));
    require_dynamic_openssl("Linux SDK target", &target, shared_host_verified);
    let requirement = if runtime_bindings {
        "runtime-loaded OpenSSL API profile 1.1.1w"
    } else {
        "OS OpenSSL 3"
    };
    let version = std::env::var("DEP_OPENSSL_VERSION_NUMBER").unwrap_or_else(|error| {
        panic!("Linux SDK target requires {requirement} version metadata: {error}")
    });
    let version = i64::from_str_radix(&version, 16).unwrap_or_else(|error| {
        panic!("Linux SDK target requires {requirement} version metadata: {error}")
    });
    if runtime_bindings {
        assert_eq!(
            version, 0x1010_117f,
            "Linux SDK target requires {requirement}; found version {version:#x}"
        );
    } else {
        require_openssl_3("Linux SDK target", version);
    }
}

fn runtime_bindings() -> bool {
    std::env::var("DEP_OPENSSL_RUNTIME_BINDINGS").as_deref() == Ok("1")
}

pub(super) fn require_openssl_3(boundary: &str, version: i64) {
    assert!(
        (0x3000_0000..0x4000_0000).contains(&version),
        "{boundary} requires OS OpenSSL 3; found version {version:#x}"
    );
}

pub(super) fn require_dynamic_openssl(boundary: &str, triple: &str, shared_host_verified: bool) {
    let prefix = triple.to_ascii_uppercase().replace('-', "_");
    let requirement = if runtime_bindings() {
        "non-vendored runtime OpenSSL bindings"
    } else {
        "OS OpenSSL 3"
    };
    for (name, expected) in [("OPENSSL_STATIC", "0"), ("OPENSSL_NO_VENDOR", "1")] {
        let prefixed = format!("{prefix}_{name}");
        println!("cargo:rerun-if-env-changed={name}");
        println!("cargo:rerun-if-env-changed={prefixed}");
        let value = std::env::var_os(&prefixed).or_else(|| std::env::var_os(name));
        assert!(
            value.as_deref() == Some(std::ffi::OsStr::new(expected))
                || (value.is_none() && shared_host_verified),
            "{boundary} requires {requirement} without static linkage; set {name}={expected} (or {prefixed})"
        );
    }
    println!("cargo:rerun-if-env-changed=DEP_OPENSSL_VENDORED");
    assert!(
        std::env::var_os("DEP_OPENSSL_VENDORED").is_none(),
        "{boundary} requires {requirement}, not vendored OpenSSL"
    );
}

#[cfg(all(target_os = "linux", any(feature = "runtime", test)))]
pub(super) fn require_shared_host_provider() {
    use std::ffi::{CStr, c_char, c_int, c_void};
    use std::mem::MaybeUninit;

    #[repr(C)]
    struct DlInfo {
        filename: *const c_char,
        base: *mut c_void,
        symbol_name: *const c_char,
        symbol_address: *mut c_void,
    }

    #[link(name = "dl")]
    unsafe extern "C" {
        fn dladdr(address: *const c_void, info: *mut DlInfo) -> c_int;
    }
    // Calling through the forwarding API initializes the runtime owner, which
    // validates the complete registry and coherent ownership before returning.
    // Its resident provider may belong to Node, not versioned OS DSOs.
    let Some((crypto_filename, ssl_filename)) =
        host_provider_filenames(openssl::version::number(), runtime_bindings())
    else {
        return;
    };
    // Registry bindings must still identify shared OS libraries, never a new
    // static/vendored implementation.
    let mut executable = MaybeUninit::<DlInfo>::uninit();
    assert!(
        unsafe {
            dladdr(
                require_shared_host_provider as *const () as *const c_void,
                executable.as_mut_ptr(),
            )
        } != 0,
        "Linux SDK build host cannot identify its executable mapping"
    );
    let executable = unsafe { executable.assume_init() };
    for (symbol, filename) in [
        (
            openssl::hash::MessageDigest::sha256().as_ptr() as *const (),
            crypto_filename,
        ),
        (
            openssl::ssl::SslMethod::tls().as_ptr() as *const (),
            ssl_filename,
        ),
    ] {
        let mut provider = MaybeUninit::<DlInfo>::uninit();
        assert!(
            unsafe { dladdr(symbol as *const c_void, provider.as_mut_ptr()) } != 0,
            "Linux SDK build host cannot verify the OS OpenSSL provider"
        );
        let provider = unsafe { provider.assume_init() };
        assert!(
            !provider.filename.is_null()
                && shared_provider(
                    unsafe { CStr::from_ptr(provider.filename) }.to_bytes(),
                    filename,
                    provider.base as usize,
                    executable.base as usize,
                ),
            "Linux SDK build host requires OS OpenSSL from the selected versioned shared libssl and libcrypto, not a static or vendored provider"
        );
    }
}

#[cfg(any(all(target_os = "linux", feature = "runtime"), test))]
fn host_provider_filenames(
    version: i64,
    runtime_bindings: bool,
) -> Option<(&'static [u8], &'static [u8])> {
    if runtime_bindings && (0x1010_1000..0x1010_2000).contains(&version) {
        return None;
    }
    require_openssl_3("Linux SDK build host", version);
    if runtime_bindings {
        None
    } else {
        Some((b"libcrypto.so.3", b"libssl.so.3"))
    }
}

#[cfg(any(all(target_os = "linux", feature = "runtime"), test))]
fn shared_provider(
    filename: &[u8],
    expected_filename: &[u8],
    provider_base: usize,
    executable_base: usize,
) -> bool {
    provider_base != 0
        && executable_base != 0
        && provider_base != executable_base
        && filename.rsplit(|byte| *byte == b'/').next() == Some(expected_filename)
}

#[cfg(test)]
#[path = "openssl_linkage/tests.rs"]
mod tests;
