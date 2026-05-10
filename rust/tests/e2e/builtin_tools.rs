use super::support::{assistant_message_content, with_e2e_context};

#[tokio::test]
async fn should_capture_exit_code_in_output() {
    with_e2e_context(
        "builtin_tools",
        "should_capture_exit_code_in_output",
        |ctx| {
            Box::pin(async move {
                ctx.set_default_copilot_user();
                let client = ctx.start_client().await;
                let session = client
                    .create_session(ctx.approve_all_session_config())
                    .await
                    .expect("create session");

                let msg = session
                    .send_and_wait("Run 'echo hello && echo world'. Tell me the exact output.")
                    .await
                    .expect("send")
                    .expect("assistant message");
                let content = assistant_message_content(&msg);
                assert!(content.contains("hello"));
                assert!(content.contains("world"));

                session.disconnect().await.expect("disconnect session");
                client.stop().await.expect("stop client");
            })
        },
    )
    .await;
}

#[tokio::test]
async fn should_capture_stderr_output() {
    with_e2e_context("builtin_tools", "should_capture_stderr_output", |ctx| {
        Box::pin(async move {
            if cfg!(windows) {
                return;
            }
            ctx.set_default_copilot_user();
            let client = ctx.start_client().await;
            let session = client
                .create_session(ctx.approve_all_session_config())
                .await
                .expect("create session");

            let msg = session
                .send_and_wait("Run 'echo error_msg >&2; echo ok' and tell me what stderr said. Reply with just the stderr content.")
                .await
                .expect("send")
                .expect("assistant message");
            assert!(assistant_message_content(&msg).contains("error_msg"));

            session.disconnect().await.expect("disconnect session");
            client.stop().await.expect("stop client");
        })
    })
    .await;
}

#[tokio::test]
async fn should_read_file_with_line_range() {
    with_e2e_context("builtin_tools", "should_read_file_with_line_range", |ctx| {
        Box::pin(async move {
            ctx.set_default_copilot_user();
            std::fs::write(ctx.work_dir().join("lines.txt"), "line1\nline2\nline3\nline4\nline5\n")
                .expect("write lines file");
            let client = ctx.start_client().await;
            let session = client
                .create_session(ctx.approve_all_session_config())
                .await
                .expect("create session");

            let msg = session
                .send_and_wait("Read lines 2 through 4 of the file 'lines.txt' in this directory. Tell me what those lines contain.")
                .await
                .expect("send")
                .expect("assistant message");
            let content = assistant_message_content(&msg);
            assert!(content.contains("line2"));
            assert!(content.contains("line4"));

            session.disconnect().await.expect("disconnect session");
            client.stop().await.expect("stop client");
        })
    })
    .await;
}

#[tokio::test]
async fn should_handle_nonexistent_file_gracefully() {
    with_e2e_context(
        "builtin_tools",
        "should_handle_nonexistent_file_gracefully",
        |ctx| {
            Box::pin(async move {
                ctx.set_default_copilot_user();
                let client = ctx.start_client().await;
                let session = client
                    .create_session(ctx.approve_all_session_config())
                    .await
                    .expect("create session");

                let msg = session
                    .send_and_wait("Try to read the file 'does_not_exist.txt'. If it doesn't exist, say 'FILE_NOT_FOUND'.")
                    .await
                    .expect("send")
                    .expect("assistant message");
                let content = assistant_message_content(&msg).to_uppercase();
                assert!(
                    content.contains("NOT FOUND")
                        || content.contains("NOT EXIST")
                        || content.contains("NO SUCH")
                        || content.contains("FILE_NOT_FOUND")
                        || content.contains("DOES NOT EXIST")
                        || content.contains("ERROR"),
                    "expected missing-file response, got: {content}"
                );

                session.disconnect().await.expect("disconnect session");
                client.stop().await.expect("stop client");
            })
        },
    )
    .await;
}

#[tokio::test]
async fn should_edit_a_file_successfully() {
    with_e2e_context("builtin_tools", "should_edit_a_file_successfully", |ctx| {
        Box::pin(async move {
            ctx.set_default_copilot_user();
            std::fs::write(ctx.work_dir().join("edit_me.txt"), "Hello World\nGoodbye World\n")
                .expect("write edit file");
            let client = ctx.start_client().await;
            let session = client
                .create_session(ctx.approve_all_session_config())
                .await
                .expect("create session");

            let msg = session
                .send_and_wait("Edit the file 'edit_me.txt': replace 'Hello World' with 'Hi Universe'. Then read it back and tell me its contents.")
                .await
                .expect("send")
                .expect("assistant message");
            assert!(assistant_message_content(&msg).contains("Hi Universe"));

            session.disconnect().await.expect("disconnect session");
            client.stop().await.expect("stop client");
        })
    })
    .await;
}

#[tokio::test]
async fn should_create_a_new_file() {
    with_e2e_context("builtin_tools", "should_create_a_new_file", |ctx| {
        Box::pin(async move {
            ctx.set_default_copilot_user();
            let client = ctx.start_client().await;
            let session = client
                .create_session(ctx.approve_all_session_config())
                .await
                .expect("create session");

            let msg = session
                .send_and_wait("Create a file called 'new_file.txt' with the content 'Created by test'. Then read it back to confirm.")
                .await
                .expect("send")
                .expect("assistant message");
            assert!(assistant_message_content(&msg).contains("Created by test"));

            session.disconnect().await.expect("disconnect session");
            client.stop().await.expect("stop client");
        })
    })
    .await;
}

#[tokio::test]
async fn should_search_for_patterns_in_files() {
    with_e2e_context(
        "builtin_tools",
        "should_search_for_patterns_in_files",
        |ctx| {
            Box::pin(async move {
                ctx.set_default_copilot_user();
                std::fs::write(ctx.work_dir().join("data.txt"), "apple\nbanana\napricot\ncherry\n")
                    .expect("write data file");
                let client = ctx.start_client().await;
                let session = client
                    .create_session(ctx.approve_all_session_config())
                    .await
                    .expect("create session");

                let msg = session
                    .send_and_wait("Search for lines starting with 'ap' in the file 'data.txt'. Tell me which lines matched.")
                    .await
                    .expect("send")
                    .expect("assistant message");
                let content = assistant_message_content(&msg);
                assert!(content.contains("apple"));
                assert!(content.contains("apricot"));

                session.disconnect().await.expect("disconnect session");
                client.stop().await.expect("stop client");
            })
        },
    )
    .await;
}

#[tokio::test]
async fn should_find_files_by_pattern() {
    with_e2e_context("builtin_tools", "should_find_files_by_pattern", |ctx| {
        Box::pin(async move {
            ctx.set_default_copilot_user();
            let src = ctx.work_dir().join("src");
            std::fs::create_dir(&src).expect("create src directory");
            std::fs::write(src.join("index.ts"), "export const index = 1;")
                .expect("write index.ts");
            std::fs::write(ctx.work_dir().join("README.md"), "# Readme").expect("write readme");
            let client = ctx.start_client().await;
            let session = client
                .create_session(ctx.approve_all_session_config())
                .await
                .expect("create session");

            let msg = session
                .send_and_wait("Find all .ts files in this directory (recursively). List the filenames you found.")
                .await
                .expect("send")
                .expect("assistant message");
            assert!(assistant_message_content(&msg).contains("index.ts"));

            session.disconnect().await.expect("disconnect session");
            client.stop().await.expect("stop client");
        })
    })
    .await;
}
