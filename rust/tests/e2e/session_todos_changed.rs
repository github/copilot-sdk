use github_copilot_sdk::session_events::SessionEventType;

use super::support::{wait_for_event, with_e2e_context};

const PROMPT: &str = concat!(
    "Use the sql tool exactly once to execute all three of the following statements ",
    "together, in this exact order, in a single sql tool call (a single query string ",
    "containing all three statements):\n",
    "1. INSERT INTO todos (id, title, status) VALUES ('alpha', 'First todo', 'pending');\n",
    "2. INSERT INTO todos (id, title, status) VALUES ('beta', 'Second todo', 'done');\n",
    "3. INSERT INTO todo_deps (todo_id, depends_on) VALUES ('beta', 'alpha');\n",
    "Then stop. Do not insert any other rows or create any other tables."
);

#[tokio::test]
async fn fires_session_todos_changed_and_exposes_rows_and_dependencies() {
    with_e2e_context(
        "session_todos_changed",
        "fires_session_todos_changed_and_exposes_rows_and_dependencies",
        |ctx| {
            Box::pin(async move {
                ctx.set_default_copilot_user();
                let client = ctx.start_client().await;
                let session = client
                    .create_session(ctx.approve_all_session_config())
                    .await
                    .expect("create session");

                let todos_changed = wait_for_event(session.subscribe(), "todos changed", |event| {
                    event.parsed_type() == SessionEventType::SessionTodosChanged
                });

                session.send_and_wait(PROMPT).await.expect("send");
                todos_changed.await;

                let result = session
                    .rpc()
                    .plan()
                    .read_sql_todos_with_dependencies()
                    .await
                    .expect("read SQL todos with dependencies");

                let mut ids: Vec<String> =
                    result.rows.into_iter().filter_map(|row| row.id).collect();
                ids.sort();
                assert_eq!(ids, ["alpha", "beta"]);
                assert!(
                    result
                        .dependencies
                        .iter()
                        .any(|dependency| dependency.todo_id == "beta"
                            && dependency.depends_on == "alpha")
                );

                session.disconnect().await.expect("disconnect session");
                client.stop().await.expect("stop client");
            })
        },
    )
    .await;
}
