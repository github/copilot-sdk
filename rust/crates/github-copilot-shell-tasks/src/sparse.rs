//! Shared, napi-free helpers for granting an *unpackaged* executable a Windows
//! **package identity** through a *sparse* (external-location) package.
//!
//! The taskbar-presence API (`Windows.UI.Shell.Tasks`) only works for a process
//! that has package identity, because it derives its per-package `tasks.json`
//! path from `GetCurrentPackageFamilyName()`. Both the Copilot CLI and the
//! desktop app grant that identity by registering a *sparse* package: a manifest
//! registered against the external directory that already contains the exe (via
//! `PackageManager::AddPackageByUriAsync` + `AddPackageOptions::ExternalLocationUri`),
//! with no files copied into a package root.
//!
//! This module factors out the registration routine — including the hard-won
//! process-lifetime MTA keepalive — as **plain, synchronous Rust** with no napi
//! or Tauri coupling, so each product can wrap it in its own async surface (the
//! CLI in a napi `AsyncTask`, the app in a `spawn_blocking` / Tauri command).
//!
//! Everything that actually touches WinRT is `#[cfg(windows)]`. The neutral
//! result types ([`RegisterOutcome`], [`SparseError`]) are cross-platform so
//! callers can name them on any target.
//!
//! # What stays product-specific
//!
//! The `AppxManifest.xml` itself is *not* shared — each product ships its own
//! (different identity, logos, applications, activation schemes). Nor is the
//! *trigger*: the CLI self-provisions in-process at launch, while the app
//! historically registers at install time from its installer. This module only
//! owns the register/deregister/identity-detect logic that is identical
//! regardless of who ships the manifest or decides when to run it.

use std::fmt;

/// Outcome of a successful sparse-package registration attempt.
#[derive(Debug, Clone, Copy, PartialEq, Eq)]
pub enum RegisterOutcome {
    /// The manifest was registered, or an identical registration was already in
    /// place. The caller may treat the exe as identity-carrying (after any
    /// relaunch its activation model requires).
    Registered,
    /// A package with this identity is already registered and currently running
    /// in another session (`ERROR_PACKAGES_IN_USE`, `0x80073D02`). Registration
    /// was intentionally a best-effort no-op — forcing it would terminate the
    /// other session and lose its work. This does **not** prove the live
    /// registration matches what was requested, so callers should leave any
    /// provisioning marker unadvanced and retry on a later launch rather than
    /// relaunch through a possibly-stale alias.
    InUse,
}

/// A sparse-package operation failure, carrying the flattened WinRT/deployment
/// error text for logging. Registration is best-effort in both products: a
/// failure never blocks startup, it is logged and the feature degrades.
#[derive(Debug, Clone)]
pub struct SparseError(pub String);

impl fmt::Display for SparseError {
    fn fmt(&self, f: &mut fmt::Formatter<'_>) -> fmt::Result {
        f.write_str(&self.0)
    }
}

impl std::error::Error for SparseError {}

#[cfg(windows)]
mod imp {
    use std::sync::Once;
    use std::thread;

    use windows::ApplicationModel::Package;
    use windows::Foundation::Uri;
    use windows::Management::Deployment::{AddPackageOptions, PackageManager};
    use windows::Win32::System::Com::{
        COINIT_MULTITHREADED, CoIncrementMTAUsage, CoInitializeEx, CoUninitialize,
    };
    use windows_core::HSTRING;

    use super::{RegisterOutcome, SparseError};

    /// `ERROR_PACKAGES_IN_USE` — a package with this identity is registered and
    /// currently running, so deployment could not apply this manifest/external
    /// location without force-killing that session.
    const ERROR_PACKAGES_IN_USE: i32 = 0x80073D02u32 as i32;

    /// Ensures an implicit multithreaded apartment (MTA) is kept alive for the
    /// entire process lifetime.
    ///
    /// `windows-rs` caches WinRT activation factories (e.g. the one behind
    /// `Package::Current()`) in process-global statics the first time they are
    /// used. Those cached pointers are only valid while the MTA that created
    /// them stays alive. Because `run_on_mta_thread` spawns a *fresh* thread
    /// per call that `CoInitializeEx(MTA)`s and `CoUninitialize`s, the MTA can
    /// be torn down between calls once the last participating thread exits —
    /// leaving the cached factory pointers dangling. A subsequent call then
    /// dereferences freed memory inside `Package::Current()` and faults with
    /// `0xC0000005` (access violation).
    ///
    /// `CoIncrementMTAUsage` registers an implicit MTA that persists
    /// independently of any specific thread, so the apartment (and the cached
    /// factories) survive for the process lifetime. The returned cookie is
    /// intentionally leaked: the MTA must never be torn down while cached
    /// factories may still be used, so we never call `CoDecrementMTAUsage`.
    pub fn ensure_process_mta() {
        static MTA_KEEPALIVE: Once = Once::new();
        MTA_KEEPALIVE.call_once(|| {
            // SAFETY: `CoIncrementMTAUsage` is callable from any thread. The
            // increment persists for the process regardless of the returned
            // cookie; we deliberately discard the cookie so nothing ever calls
            // `CoDecrementMTAUsage`, keeping the implicit MTA (and the cached
            // WinRT factories) alive for the process lifetime.
            unsafe {
                let _ = CoIncrementMTAUsage();
            }
        });
    }

    /// Runs `f` on a dedicated MTA thread and returns its result. WinRT
    /// deployment APIs require a COM-initialized thread; spawning a fresh one
    /// per call keeps the apartment state isolated and torn down cleanly.
    fn run_on_mta_thread<T, F>(f: F) -> Result<T, SparseError>
    where
        T: Send + 'static,
        F: FnOnce() -> Result<T, SparseError> + Send + 'static,
    {
        // Keep the MTA alive for the whole process so windows-rs's globally
        // cached activation factories (e.g. behind `Package::Current()`) never
        // dangle when this per-call thread later tears its own apartment down.
        // See `ensure_process_mta` for the full rationale.
        ensure_process_mta();

        thread::Builder::new()
            .name("copilot-sparse-com".to_owned())
            .spawn(move || {
                // SAFETY: CoInitializeEx is safe from any thread; a NULL
                // reserved pointer with the MTA flag is the documented
                // contract. Paired with CoUninitialize before the thread exits.
                unsafe {
                    let hr = CoInitializeEx(None, COINIT_MULTITHREADED);
                    let result = f();
                    // Only uninitialize when this call actually initialized the
                    // apartment (S_OK/S_FALSE are both success; RPC_E_CHANGED_MODE
                    // means someone else set the apartment first and we must not
                    // balance it).
                    if hr.is_ok() {
                        CoUninitialize();
                    }
                    result
                }
            })
            .map_err(|e| SparseError(format!("failed to spawn COM thread: {e}")))?
            .join()
            .map_err(|_| SparseError("COM thread panicked".to_owned()))?
    }

    /// Maps a WinRT `Result` into this module's `Result`, flattening the
    /// HRESULT into text.
    fn winrt<T>(value: windows_core::Result<T>) -> Result<T, SparseError> {
        value.map_err(|e| SparseError(format!("WinRT error: {e}")))
    }

    /// Returns the current process's Package Family Name if it runs with package
    /// identity, or `None` when it does not (the common unpackaged case). Never
    /// errors for the no-identity case — that is a normal, expected result.
    pub fn current_package_family_name() -> Option<String> {
        // Run on the MTA thread for parity with the other operations and so the
        // cached `Package` factory is created under the kept-alive apartment.
        run_on_mta_thread(|| {
            // `Package::Current()` fails with a well-known error when the
            // process has no identity; treat any failure as "no identity"
            // rather than a hard error so callers can branch on `None`.
            let name = match Package::Current() {
                Ok(package) => match package.Id().and_then(|id| id.FamilyName()) {
                    Ok(name) => Some(name.to_string()),
                    Err(_) => None,
                },
                Err(_) => None,
            };
            Ok(name)
        })
        .unwrap_or(None)
    }

    /// Registers the sparse package described by `manifest_uri` (a `file:///`
    /// URI to `AppxManifest.xml` or a signed sparse `.msix`) against
    /// `external_location` (the install directory containing the exe).
    ///
    /// Returns [`RegisterOutcome::Registered`] on a successful (or already
    /// applied) deployment, or [`RegisterOutcome::InUse`] when the identity is
    /// held by another running session (see that variant's docs). This blocks
    /// the calling thread until deployment completes; wrap it in your product's
    /// async surface.
    pub fn register_sparse_package(
        manifest_uri: &str,
        external_location: &str,
    ) -> Result<RegisterOutcome, SparseError> {
        let manifest_uri = manifest_uri.to_owned();
        let external_location = external_location.to_owned();
        run_on_mta_thread(move || {
            let manager = winrt(PackageManager::new())?;
            let package_uri = winrt(Uri::CreateUri(&HSTRING::from(&manifest_uri)))?;
            let external_uri = winrt(Uri::CreateUri(&HSTRING::from(&external_location)))?;

            let options = winrt(AddPackageOptions::new())?;
            winrt(options.SetExternalLocationUri(&external_uri))?;
            // Intentionally leave ForceAppShutdown at its default `false`.
            // Forcing a shutdown would terminate every other running process of
            // this package — i.e. other identity-launched sessions — and lose
            // their in-progress work. A re-registration that collides with a
            // running instance is handled below as a best-effort no-op instead.

            let operation = winrt(manager.AddPackageByUriAsync(&package_uri, &options))?;
            // Block this dedicated MTA thread until deployment completes.
            let result = winrt(operation.join())?;

            // A non-S_OK extended error code indicates a deployment failure.
            let error_code = winrt(result.ExtendedErrorCode())?;
            if error_code.is_err() {
                // ERROR_PACKAGES_IN_USE: report as incomplete rather than
                // success so the caller retries later instead of relaunching
                // through a possibly-stale alias. See RegisterOutcome::InUse.
                if error_code.0 == ERROR_PACKAGES_IN_USE {
                    return Ok(RegisterOutcome::InUse);
                }
                let text = result.ErrorText().map(|t| t.to_string()).unwrap_or_default();
                return Err(SparseError(format!(
                    "sparse package registration failed ({error_code:?}): {text}"
                )));
            }
            Ok(RegisterOutcome::Registered)
        })
    }

    /// Deregisters the sparse package identified by its full package name.
    /// Returns `true` on success. Blocks until deployment completes.
    pub fn deregister_sparse_package(package_full_name: &str) -> Result<bool, SparseError> {
        let package_full_name = package_full_name.to_owned();
        run_on_mta_thread(move || {
            let manager = winrt(PackageManager::new())?;
            let operation = winrt(manager.RemovePackageAsync(&HSTRING::from(&package_full_name)))?;
            let result = winrt(operation.join())?;
            let error_code = winrt(result.ExtendedErrorCode())?;
            if error_code.is_err() {
                let text = result.ErrorText().map(|t| t.to_string()).unwrap_or_default();
                return Err(SparseError(format!(
                    "sparse package deregistration failed ({error_code:?}): {text}"
                )));
            }
            Ok(true)
        })
    }
}

#[cfg(windows)]
pub use imp::{
    current_package_family_name, deregister_sparse_package, ensure_process_mta,
    register_sparse_package,
};

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn sparse_error_displays_its_message() {
        let err = SparseError("deployment 0x80073CF9".to_owned());
        assert_eq!(err.to_string(), "deployment 0x80073CF9");
        // Exercised via the `std::error::Error` object-safe path too.
        let dyn_err: &dyn std::error::Error = &err;
        assert_eq!(dyn_err.to_string(), "deployment 0x80073CF9");
    }

    #[test]
    fn register_outcome_variants_are_distinct() {
        assert_ne!(RegisterOutcome::Registered, RegisterOutcome::InUse);
        assert_eq!(RegisterOutcome::Registered, RegisterOutcome::Registered);
    }
}
