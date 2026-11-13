// Copyright (c) Microsoft Corporation. All rights reserved.

#[cfg(windows)]
pub(super) fn sha256(data: &[u8]) -> [u8; 32] {
    // Build scripts compile for HOST, not the downloaded runtime's TARGET.
    windows::hash_chunks(std::iter::once(data))
        .unwrap_or_else(|error| panic!("Cannot verify Copilot archive SHA256: {error}"))
}

#[cfg(target_os = "linux")]
pub(super) fn sha256(data: &[u8]) -> [u8; 32] {
    let digest = openssl::hash::hash(openssl::hash::MessageDigest::sha256(), data)
        .unwrap_or_else(|error| panic!("Cannot verify Copilot archive SHA256: {error}"));
    digest
        .as_ref()
        .try_into()
        .expect("OpenSSL SHA256 produces a 32-byte digest")
}

#[cfg(target_os = "macos")]
pub(super) fn sha256(data: &[u8]) -> [u8; 32] {
    use std::ffi::{c_int, c_void};

    // CommonCrypto's public CC_SHA256_CTX ABI (CommonCrypto/CommonDigest.h).
    // libSystem supplies the implementation; no bundled crypto or Swift bridge
    // is needed by this standalone SDK build script.
    #[repr(C)]
    struct Sha256Context {
        count: [u32; 2],
        hash: [u32; 8],
        wbuf: [u32; 16],
    }

    #[link(name = "System")]
    unsafe extern "C" {
        fn CC_SHA256_Init(context: *mut Sha256Context) -> c_int;
        fn CC_SHA256_Update(context: *mut Sha256Context, data: *const c_void, len: u32) -> c_int;
        fn CC_SHA256_Final(digest: *mut u8, context: *mut Sha256Context) -> c_int;
    }

    let mut context = Sha256Context {
        count: [0; 2],
        hash: [0; 8],
        wbuf: [0; 16],
    };
    let mut digest = [0; 32];
    // SAFETY: context has the public C layout, is exclusively owned, and lives
    // through finalization. Each input slice is valid for its supplied length;
    // the output has CC_SHA256_DIGEST_LENGTH bytes. None of these calls retain
    // pointers. CommonDigest.h specifies a success return of 1 for all three.
    unsafe {
        assert_eq!(
            CC_SHA256_Init(&mut context),
            1,
            "Cannot verify Copilot archive SHA256: CC_SHA256_Init failed"
        );
        // CC_LONG is 32-bit even on 64-bit hosts. Do not truncate larger inputs.
        for chunk in data.chunks(u32::MAX as usize) {
            assert_eq!(
                CC_SHA256_Update(&mut context, chunk.as_ptr().cast(), chunk.len() as u32),
                1,
                "Cannot verify Copilot archive SHA256: CC_SHA256_Update failed"
            );
        }
        assert_eq!(
            CC_SHA256_Final(digest.as_mut_ptr(), &mut context),
            1,
            "Cannot verify Copilot archive SHA256: CC_SHA256_Final failed"
        );
    }
    digest
}

#[cfg(not(any(windows, target_os = "macos", target_os = "linux")))]
pub(super) fn sha256(data: &[u8]) -> [u8; 32] {
    use sha2::Digest;

    sha2::Sha256::digest(data).into()
}

#[cfg(windows)]
mod windows {
    use std::io;
    use std::ptr::{null, null_mut};

    use windows_sys::Win32::Security::Cryptography::{
        BCRYPT_ALG_HANDLE, BCRYPT_HASH_HANDLE, BCRYPT_SHA256_ALGORITHM,
        BCryptCloseAlgorithmProvider, BCryptCreateHash, BCryptDestroyHash, BCryptFinishHash,
        BCryptHashData, BCryptOpenAlgorithmProvider, MS_PRIMITIVE_PROVIDER,
    };

    struct Hash {
        algorithm: BCRYPT_ALG_HANDLE,
        hash: BCRYPT_HASH_HANDLE,
    }

    impl Drop for Hash {
        fn drop(&mut self) {
            unsafe {
                if !self.hash.is_null() {
                    BCryptDestroyHash(self.hash);
                }
                if !self.algorithm.is_null() {
                    BCryptCloseAlgorithmProvider(self.algorithm, 0);
                }
            }
        }
    }

    pub(super) fn check_status(operation: &str, status: i32) -> io::Result<()> {
        if status >= 0 {
            Ok(())
        } else {
            Err(io::Error::other(format!(
                "{operation} failed: NTSTATUS {status:#010x}"
            )))
        }
    }

    pub(super) fn hash_chunks<'a>(
        chunks: impl IntoIterator<Item = &'a [u8]>,
    ) -> io::Result<[u8; 32]> {
        let mut owner = Hash {
            algorithm: null_mut(),
            hash: null_mut(),
        };
        let mut digest = [0; 32];
        unsafe {
            check_status(
                "BCryptOpenAlgorithmProvider",
                BCryptOpenAlgorithmProvider(
                    &mut owner.algorithm,
                    BCRYPT_SHA256_ALGORITHM,
                    MS_PRIMITIVE_PROVIDER,
                    0,
                ),
            )?;
            // Windows 7+ owns the hash-object allocation when its buffer is null.
            check_status(
                "BCryptCreateHash",
                BCryptCreateHash(
                    owner.algorithm,
                    &mut owner.hash,
                    null_mut(),
                    0,
                    null(),
                    0,
                    0,
                ),
            )?;
            for chunk in chunks {
                for block in chunk.chunks(u32::MAX as usize) {
                    check_status(
                        "BCryptHashData",
                        BCryptHashData(owner.hash, block.as_ptr(), block.len() as u32, 0),
                    )?;
                }
            }
            check_status(
                "BCryptFinishHash",
                BCryptFinishHash(owner.hash, digest.as_mut_ptr(), digest.len() as u32, 0),
            )?;
        }
        Ok(digest)
    }
}

#[cfg(test)]
#[path = "archive_hash/tests.rs"]
mod tests;
