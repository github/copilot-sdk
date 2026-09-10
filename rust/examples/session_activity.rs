use std::sync::Arc;

use github_copilot_sdk::handler::ApproveAllHandler;
use github_copilot_sdk::session_activity::{SessionActivityReducer, SessionActivitySupport};
use github_copilot_sdk::{Client, ClientOptions, SessionConfig};

#[tokio::main(flavor = "current_thread")]
async fn main() -> Result<(), Box<dyn std::error::Error>> {
    let client = Client::start(ClientOptions::default()).await?;
    let session = client
        .create_session(
            SessionConfig::default().with_permission_handler(Arc::new(ApproveAllHandler)),
        )
        .await?;

    let mut reducer = SessionActivityReducer::new();
    let connection = reducer.connection_token();

    // Subscribe first so activity changes cannot fall into a query/subscription gap.
    let mut events = session.subscribe();
    let snapshot = session.activity();
    tokio::pin!(snapshot);

    let mut snapshot_pending = true;
    loop {
        tokio::select! {
            result = &mut snapshot, if snapshot_pending => {
                snapshot_pending = false;
                if let SessionActivitySupport::Supported(snapshot) = result? {
                    reducer.apply(connection, snapshot);
                }
            }
            result = events.recv() => {
                let event = result?;
                if let Some(SessionActivitySupport::Supported(snapshot)) =
                    event.session_activity()?
                {
                    reducer.apply(connection, snapshot);
                }
            }
        }

        if let Some(activity) = reducer.current() {
            println!(
                "main={:?} background={} processes={}",
                activity.main_agent.state,
                activity.background_agents.running,
                activity.processes.running
            );
        }
    }
}
