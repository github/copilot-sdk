// Copyright (c) Microsoft Corporation. All rights reserved.
#![cfg(test)]

use super::*;

#[test]
fn shared_host_versions_preserve_registry_policy_and_runtime_owner_validation() {
    for runtime in [false, true] {
        for version in [0x3000_0000, 0x3050_0090] {
            assert_eq!(
                host_provider_filenames(version, runtime),
                (!runtime).then_some((b"libcrypto.so.3".as_slice(), b"libssl.so.3".as_slice()))
            );
        }
        for version in [0, 0x1000_20bf, 0x1010_000f, 0x1010_2000, 0x4000_0000] {
            assert!(
                std::panic::catch_unwind(|| host_provider_filenames(version, runtime)).is_err(),
                "accepted version {version:#x}, runtime={runtime}"
            );
        }
    }
    for version in [0x1010_100f, 0x1010_107f, 0x1010_117f] {
        assert_eq!(host_provider_filenames(version, true), None);
        assert!(std::panic::catch_unwind(|| host_provider_filenames(version, false)).is_err());
    }
}

#[test]
fn shared_provider_requires_the_versioned_library_outside_the_executable() {
    assert!(shared_provider(
        b"/usr/lib/libcrypto.so.3",
        b"libcrypto.so.3",
        2,
        1
    ));
    assert!(shared_provider(
        b"/lib64/libcrypto.so.3",
        b"libcrypto.so.3",
        2,
        1
    ));
    assert!(shared_provider(b"/lib64/libssl.so.3", b"libssl.so.3", 2, 1));
    assert!(!shared_provider(
        b"/lib64/libcrypto.so.3",
        b"libssl.so.3",
        2,
        1
    ));
    for (name, provider, executable) in [
        (b"/usr/lib/libcrypto.so.3".as_slice(), 1, 1),
        (b"/usr/lib/libcrypto.so.3".as_slice(), 0, 1),
        (b"/usr/lib/libcrypto.so.3".as_slice(), 2, 0),
        (b"/usr/lib/libcrypto.so".as_slice(), 2, 1),
        (b"/usr/lib/libcrypto.so.1.1".as_slice(), 2, 1),
        (b"/usr/lib/libcrypto.a".as_slice(), 2, 1),
        (b"/out/build-script-build".as_slice(), 2, 1),
        (b"".as_slice(), 2, 1),
    ] {
        assert!(
            !shared_provider(name, b"libcrypto.so.3", provider, executable),
            "accepted {name:?}, provider {provider}, executable {executable}"
        );
    }
}

#[cfg(target_os = "linux")]
#[test]
fn linked_host_provider_is_a_shared_os_openssl_3_library() {
    require_shared_host_provider();
}
