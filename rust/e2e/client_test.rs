//! Client E2E tests.

use copilot_sdk::{ClientOptions, ConnectionState, CopilotClient};

mod testharness;
use testharness::cli_path;

/// Skip test if CLI is not available.
macro_rules! require_cli {
    () => {
        if cli_path().is_none() {
            eprintln!("Skipping test: CLI not found. Run 'npm install' in the nodejs directory first.");
            return;
        }
    };
}

#[tokio::test]
async fn test_start_and_connect_using_stdio() {
    require_cli!();

    let cli = cli_path().unwrap();
    let client = CopilotClient::new(Some(ClientOptions {
        cli_path: Some(cli),
        use_stdio: Some(true),
        ..Default::default()
    }));

    // Start the client
    let result = client.start().await;
    assert!(result.is_ok(), "Failed to start client: {:?}", result.err());

    // Verify state
    assert_eq!(client.get_state().await, ConnectionState::Connected);

    // Ping the server
    let pong = client.ping(Some("test message")).await;
    assert!(pong.is_ok(), "Failed to ping: {:?}", pong.err());

    let pong = pong.unwrap();
    assert_eq!(pong.message, "pong: test message");
    assert!(pong.timestamp >= 0);

    // Stop the client
    let errors = client.stop().await;
    assert!(errors.is_empty(), "Expected no errors on stop, got: {:?}", errors);

    // Verify disconnected state
    assert_eq!(client.get_state().await, ConnectionState::Disconnected);
}

#[tokio::test]
async fn test_start_and_connect_using_tcp() {
    require_cli!();

    let cli = cli_path().unwrap();
    let client = CopilotClient::new(Some(ClientOptions {
        cli_path: Some(cli),
        use_stdio: Some(false),
        ..Default::default()
    }));

    // Start the client
    let result = client.start().await;
    assert!(result.is_ok(), "Failed to start client: {:?}", result.err());

    // Verify state
    assert_eq!(client.get_state().await, ConnectionState::Connected);

    // Ping the server
    let pong = client.ping(Some("test message")).await;
    assert!(pong.is_ok(), "Failed to ping: {:?}", pong.err());

    let pong = pong.unwrap();
    assert_eq!(pong.message, "pong: test message");
    assert!(pong.timestamp >= 0);

    // Stop the client
    let errors = client.stop().await;
    assert!(errors.is_empty(), "Expected no errors on stop, got: {:?}", errors);

    // Verify disconnected state
    assert_eq!(client.get_state().await, ConnectionState::Disconnected);
}

#[tokio::test]
async fn test_force_stop_without_cleanup() {
    require_cli!();

    let cli = cli_path().unwrap();
    let client = CopilotClient::new(Some(ClientOptions {
        cli_path: Some(cli),
        ..Default::default()
    }));

    // Create a session
    let session = client.create_session(None).await;
    assert!(session.is_ok(), "Failed to create session: {:?}", session.err());

    // Force stop
    client.force_stop().await;

    // Verify disconnected state
    assert_eq!(client.get_state().await, ConnectionState::Disconnected);
}

#[tokio::test]
async fn test_auto_start_on_create_session() {
    require_cli!();

    let cli = cli_path().unwrap();
    let client = CopilotClient::new(Some(ClientOptions {
        cli_path: Some(cli),
        auto_start: Some(true),
        ..Default::default()
    }));

    // Don't call start() - it should auto-start
    assert_eq!(client.get_state().await, ConnectionState::Disconnected);

    // Create a session - this should auto-start
    let session = client.create_session(None).await;
    assert!(session.is_ok(), "Failed to create session: {:?}", session.err());

    // Should now be connected
    assert_eq!(client.get_state().await, ConnectionState::Connected);

    client.force_stop().await;
}
