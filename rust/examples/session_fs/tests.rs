#![cfg(test)]
#![allow(clippy::unwrap_used)]

use super::*;

#[tokio::test]
async fn mkdir_preserves_files_and_rejects_file_ancestors_without_creating_parents() {
    let provider = InMemoryProvider::new();
    provider
        .write_file("/workspace/report", "original", None)
        .await
        .unwrap();

    for recursive in [false, true] {
        assert!(
            provider
                .mkdir("/workspace/report", recursive, None)
                .await
                .is_err()
        );
        assert!(
            provider
                .mkdir("/workspace/report/new/child", recursive, None)
                .await
                .is_err()
        );
        assert_eq!(
            provider.read_file("/workspace/report").await.unwrap(),
            "original"
        );
        assert!(!provider.exists("/workspace/report/new").await.unwrap());
    }

    provider
        .mkdir("/workspace/notes", true, None)
        .await
        .unwrap();
    provider
        .write_file("/workspace/notes/report", "keep", None)
        .await
        .unwrap();
    provider
        .mkdir("/workspace/notes", true, None)
        .await
        .unwrap();
    assert_eq!(
        provider.read_file("/workspace/notes/report").await.unwrap(),
        "keep"
    );
}

#[tokio::test]
async fn rename_rejects_nonempty_directories_and_type_collisions_without_data_loss() {
    let provider = InMemoryProvider::new();
    provider
        .write_file("/workspace/src/report", "source", None)
        .await
        .unwrap();
    provider
        .write_file("/workspace/dst/report", "destination", None)
        .await
        .unwrap();
    provider
        .write_file("/workspace/file", "file", None)
        .await
        .unwrap();

    for (src, dest) in [
        ("/workspace/src", "/workspace/dst"),
        ("/workspace/src", "/workspace/file"),
        ("/workspace/file", "/workspace/dst"),
        ("/workspace/src", "/workspace/file/new/child"),
    ] {
        assert!(provider.rename(src, dest).await.is_err(), "{src} -> {dest}");
        assert_eq!(
            provider.read_file("/workspace/src/report").await.unwrap(),
            "source"
        );
        assert_eq!(
            provider.read_file("/workspace/dst/report").await.unwrap(),
            "destination"
        );
        assert_eq!(provider.read_file("/workspace/file").await.unwrap(), "file");
        assert!(!provider.exists("/workspace/file/new").await.unwrap());
    }
}

#[tokio::test]
async fn rename_rejects_descendants_and_keeps_identical_paths_unchanged() {
    let provider = InMemoryProvider::new();
    provider
        .write_file("/workspace/reports/a.txt", "report", None)
        .await
        .unwrap();

    assert!(
        provider
            .rename("/workspace/reports", "/workspace/reports/archive")
            .await
            .is_err()
    );
    provider
        .rename("/workspace/reports", "/workspace/reports")
        .await
        .unwrap();

    assert_eq!(
        provider
            .read_file("/workspace/reports/a.txt")
            .await
            .unwrap(),
        "report"
    );
    assert!(!provider.exists("/workspace/reports/archive").await.unwrap());
}

#[tokio::test]
async fn rename_replaces_files_and_moves_trees_to_empty_directories() {
    let provider = InMemoryProvider::new();
    provider
        .write_file("/workspace/temp", "new", None)
        .await
        .unwrap();
    provider
        .write_file("/workspace/file", "old", None)
        .await
        .unwrap();
    provider
        .rename("/workspace/temp", "/workspace/file")
        .await
        .unwrap();
    assert_eq!(provider.read_file("/workspace/file").await.unwrap(), "new");
    assert!(!provider.exists("/workspace/temp").await.unwrap());

    provider
        .write_file("/workspace/src/nested/report", "report", None)
        .await
        .unwrap();
    provider.mkdir("/workspace/dst", true, None).await.unwrap();
    provider
        .rename("/workspace/src", "/workspace/dst")
        .await
        .unwrap();
    assert_eq!(
        provider
            .read_file("/workspace/dst/nested/report")
            .await
            .unwrap(),
        "report"
    );
    assert!(!provider.exists("/workspace/src").await.unwrap());
}

#[tokio::test]
async fn writes_reject_directories_and_file_ancestors() {
    let provider = InMemoryProvider::new();
    provider
        .write_file("/workspace/report", "keep", None)
        .await
        .unwrap();

    for path in ["/workspace", "/workspace/report/new/child"] {
        assert!(provider.write_file(path, "replace", None).await.is_err());
        assert!(provider.append_file(path, "append", None).await.is_err());
    }
    assert_eq!(
        provider.read_file("/workspace/report").await.unwrap(),
        "keep"
    );
    assert!(!provider.exists("/workspace/report/new").await.unwrap());
    assert!(provider.stat("/workspace").await.unwrap().is_directory);
}
