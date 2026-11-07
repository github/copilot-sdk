#![cfg(test)]

use std::fs;
use std::os::unix::fs::{PermissionsExt, symlink};

use super::private_temporary_cache_dir;

#[test]
fn private_cache_creates_and_reuses_in_a_trusted_base() {
    let fixture = tempfile::tempdir().expect("temporary fixture");
    fs::set_permissions(fixture.path(), fs::Permissions::from_mode(0o700))
        .expect("private fixture");
    let base = fixture.path().join("scratch").join("tmp");
    let root = private_temporary_cache_dir(&base).expect("create temporary parent and cache");
    assert_eq!(
        root.parent().expect("cache parent"),
        fs::canonicalize(&base).expect("created temporary parent")
    );
    let metadata = fs::symlink_metadata(&root).expect("cache metadata");
    assert!(metadata.is_dir());
    assert_eq!(metadata.permissions().mode() & 0o777, 0o700);
    fs::write(root.join("retained"), b"warm cache").expect("warm marker");
    // This base remains protected by the fixture's owner-only ancestor.
    fs::set_permissions(&base, fs::Permissions::from_mode(0o777)).expect("trusted base mode");
    assert_eq!(
        private_temporary_cache_dir(&base).expect("warm cache"),
        root
    );
    assert_eq!(
        fs::read(root.join("retained")).expect("retained marker"),
        b"warm cache"
    );
}

#[test]
fn unsafe_existing_roots_are_never_repaired_or_followed() {
    for kind in ["readable", "symlink", "file"] {
        let base = tempfile::tempdir().expect("temporary parent");
        let root = private_temporary_cache_dir(base.path()).expect("initial private cache");
        fs::remove_dir(&root).expect("remove empty fixture cache");
        match kind {
            "readable" => {
                fs::create_dir(&root).expect("readable directory");
                fs::set_permissions(&root, fs::Permissions::from_mode(0o755))
                    .expect("readable mode");
            }
            "symlink" => symlink(base.path(), &root).expect("symlink cache"),
            "file" => fs::write(&root, b"not a cache").expect("file cache"),
            _ => unreachable!(),
        }
        let before = fs::symlink_metadata(&root)
            .expect("fixture metadata")
            .permissions()
            .mode();
        assert!(
            private_temporary_cache_dir(base.path()).is_err(),
            "{kind} root was accepted"
        );
        assert_eq!(
            fs::symlink_metadata(&root)
                .expect("unchanged metadata")
                .permissions()
                .mode(),
            before
        );
    }
}
