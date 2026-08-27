use std::path::Path;
use std::time::Duration;

use github_copilot_sdk::rpc::{
    HistoryListRewindPointsResult, HistoryPreviewRewindRequest, HistoryRewindMode,
    HistoryRewindOutcome, HistoryRewindRequest,
};

use super::support::assistant_message_content;

const FILE_NAME: &str = "rewind-sdk.txt";
const FILE_CONTENT: &str = "SDK rewind content";

#[tokio::test]
async fn should_restore_tracked_file_and_conversation() {
    super::support::with_shared_e2e_context(
        &E2E,
        "rewind",
        "should_restore_tracked_file_and_conversation",
        |ctx| {
            Box::pin(async move {
                ctx.set_default_copilot_user();
                let file_path = ctx.work_dir().join(FILE_NAME);
                let client = ctx.start_client().await;
                let session = client
                    .create_session(
                        ctx.approve_all_session_config()
                            .with_model("claude-sonnet-4.5")
                            .with_enable_file_change_tracking(true),
                    )
                    .await
                    .expect("create session");

                let response = session
                    .send_and_wait(format!(
                        "Use the create tool to create {FILE_NAME} containing exactly \
                         {FILE_CONTENT}. After the tool succeeds, reply with exactly \
                         SDK_REWIND_DONE."
                    ))
                    .await
                    .expect("send rewind setup prompt")
                    .expect("assistant message");
                assert_eq!(assistant_message_content(&response), "SDK_REWIND_DONE");
                assert_eq!(
                    std::fs::read_to_string(&file_path).expect("read tracked file"),
                    FILE_CONTENT
                );

                let rewind_points = wait_for_rewind_points(&session).await;
                assert!(rewind_points.file_change_tracking_enabled);
                assert_eq!(rewind_points.points.len(), 1);
                let rewind_point = &rewind_points.points[0];
                assert!(rewind_point.can_restore_files);
                assert_eq!(rewind_point.file_count, 1);

                let preview = session
                    .rpc()
                    .history()
                    .preview_rewind(HistoryPreviewRewindRequest {
                        event_id: rewind_point.event_id.clone(),
                    })
                    .await
                    .expect("preview rewind");
                assert!(preview.available);
                assert_eq!(preview.files.len(), 1);
                assert_same_path(&file_path, Path::new(&preview.files[0].path));

                let rewind = session
                    .rpc()
                    .history()
                    .rewind(HistoryRewindRequest {
                        event_id: rewind_point.event_id.clone(),
                        mode: HistoryRewindMode::ConversationAndFiles,
                    })
                    .await
                    .expect("rewind conversation and files");
                assert_eq!(rewind.outcome, HistoryRewindOutcome::Success);
                assert!(rewind.events_removed.is_some_and(|count| count > 0));
                assert_eq!(rewind.restored_files.len(), 1);
                assert_same_path(&file_path, Path::new(&rewind.restored_files[0]));
                assert!(!file_path.exists());

                let events = session.get_events().await.expect("get events after rewind");
                assert!(events.iter().all(|event| event.id != rewind_point.event_id));

                session.disconnect().await.expect("disconnect session");
                client.stop().await.expect("stop client");
            })
        },
    )
    .await;
}

async fn wait_for_rewind_points(
    session: &github_copilot_sdk::session::Session,
) -> HistoryListRewindPointsResult {
    let deadline = tokio::time::Instant::now() + Duration::from_secs(30);
    loop {
        let result = session
            .rpc()
            .history()
            .list_rewind_points()
            .await
            .expect("list rewind points");
        if result.unavailable_reason.is_none()
            && result
                .points
                .first()
                .is_some_and(|point| point.can_restore_files && point.file_count == 1)
        {
            return result;
        }
        assert!(
            tokio::time::Instant::now() < deadline,
            "timed out waiting for a restorable rewind point: {result:?}"
        );
        tokio::time::sleep(Duration::from_millis(100)).await;
    }
}

fn assert_same_path(expected: &Path, actual: &Path) {
    let expected = expected.to_string_lossy();
    let actual = actual.to_string_lossy();
    if cfg!(windows) {
        let expected = expected.replace('\\', "/");
        let actual = actual.replace('\\', "/");
        assert!(
            expected.eq_ignore_ascii_case(&actual),
            "expected path {expected:?}, got {actual:?}"
        );
    } else {
        assert_eq!(expected, actual);
    }
}

static E2E: super::support::SharedE2eGroup = super::support::SharedE2eGroup::standard("rewind", 1);
