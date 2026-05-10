use std::net::{Ipv4Addr, SocketAddrV4, TcpListener};

use github_copilot_sdk::{
    Client, ClientOptions, Error, LogLevel, MessageOptions, OtelExporterType, SessionConfig,
    TelemetryConfig, Transport,
};
use serde_json::json;

use super::support::{assistant_message_content, with_e2e_context};

#[tokio::test]
async fn should_use_client_cwd_for_default_workingdirectory() {
    with_e2e_context(
        "client_options",
        "should_use_client_cwd_for_default_workingdirectory",
        |ctx| {
            Box::pin(async move {
                ctx.set_default_copilot_user();
                let client_cwd = ctx.work_dir().join("client-cwd");
                std::fs::create_dir_all(&client_cwd).expect("create client cwd");
                std::fs::write(client_cwd.join("marker.txt"), "I am in the client cwd")
                    .expect("write marker");

                let client = Client::start(ctx.client_options().with_cwd(&client_cwd))
                    .await
                    .expect("start client");
                let session = client
                    .create_session(ctx.approve_all_session_config())
                    .await
                    .expect("create session");

                let answer = session
                    .send_and_wait("Read the file marker.txt and tell me what it says")
                    .await
                    .expect("send")
                    .expect("assistant message");
                assert!(assistant_message_content(&answer).contains("client cwd"));

                session.disconnect().await.expect("disconnect session");
                client.stop().await.expect("stop client");
            })
        },
    )
    .await;
}

#[tokio::test]
async fn should_listen_on_configured_tcp_port() {
    with_e2e_context(
        "client_options",
        "should_listen_on_configured_tcp_port",
        |ctx| {
            Box::pin(async move {
                let port = get_available_tcp_port();
                let client = Client::start(
                    ctx.client_options_with_transport(Transport::Tcp { port })
                        .with_tcp_connection_token("configured-port-token"),
                )
                .await
                .expect("start TCP client");

                let response = client.ping(Some("fixed-port")).await.expect("ping");

                assert_eq!(response.message, "pong: fixed-port");
                client.stop().await.expect("stop client");
            })
        },
    )
    .await;
}

#[tokio::test]
async fn should_forward_enablesessiontelemetry_in_wire_request() {
    let value = serde_json::to_value(
        SessionConfig::default()
            .with_enable_session_telemetry(false)
            .with_handler(std::sync::Arc::new(
                github_copilot_sdk::handler::ApproveAllHandler,
            )),
    )
    .expect("serialize session config");

    assert_eq!(value["enableSessionTelemetry"], json!(false));
}

#[tokio::test]
async fn should_omit_enablesessiontelemetry_when_not_set() {
    let value = serde_json::to_value(SessionConfig::default().with_handler(std::sync::Arc::new(
        github_copilot_sdk::handler::ApproveAllHandler,
    )))
    .expect("serialize session config");

    assert!(value.get("enableSessionTelemetry").is_none());
}

#[tokio::test]
async fn should_accept_githubtoken_option() {
    let options = ClientOptions::new().with_github_token("gho_test_token");

    assert_eq!(options.github_token.as_deref(), Some("gho_test_token"));
}

#[tokio::test]
async fn should_default_useloggedinuser_to_null() {
    let options = ClientOptions::new();

    assert!(options.use_logged_in_user.is_none());
}

#[tokio::test]
async fn should_allow_explicit_useloggedinuser_false() {
    let options = ClientOptions::new().with_use_logged_in_user(false);

    assert_eq!(options.use_logged_in_user, Some(false));
}

#[tokio::test]
async fn should_allow_explicit_useloggedinuser_true_with_githubtoken() {
    let options = ClientOptions::new()
        .with_github_token("gho_test_token")
        .with_use_logged_in_user(true);

    assert_eq!(options.github_token.as_deref(), Some("gho_test_token"));
    assert_eq!(options.use_logged_in_user, Some(true));
}

#[tokio::test]
async fn should_default_sessionidletimeoutseconds_to_null() {
    let options = ClientOptions::new();

    assert!(options.session_idle_timeout_seconds.is_none());
}

#[tokio::test]
async fn should_accept_sessionidletimeoutseconds_option() {
    let options = ClientOptions::new().with_session_idle_timeout_seconds(600);

    assert_eq!(options.session_idle_timeout_seconds, Some(600));
}

#[tokio::test]
async fn should_propagate_process_options_to_spawned_cli() {
    let telemetry = TelemetryConfig::new()
        .with_otlp_endpoint("http://127.0.0.1:4318")
        .with_file_path("telemetry.jsonl")
        .with_exporter_type(OtelExporterType::File)
        .with_source_name("rust-sdk-e2e")
        .with_capture_content(true);
    let options = ClientOptions::new()
        .with_github_token("process-option-token")
        .with_log_level(LogLevel::Debug)
        .with_session_idle_timeout_seconds(17)
        .with_telemetry(telemetry)
        .with_use_logged_in_user(false);

    assert_eq!(
        options.github_token.as_deref(),
        Some("process-option-token")
    );
    assert_eq!(options.log_level, Some(LogLevel::Debug));
    assert_eq!(options.session_idle_timeout_seconds, Some(17));
    assert_eq!(options.use_logged_in_user, Some(false));
    let telemetry = options.telemetry.as_ref().expect("telemetry");
    assert_eq!(
        telemetry.otlp_endpoint.as_deref(),
        Some("http://127.0.0.1:4318")
    );
    assert_eq!(telemetry.exporter_type, Some(OtelExporterType::File));
    assert_eq!(telemetry.source_name.as_deref(), Some("rust-sdk-e2e"));
    assert_eq!(telemetry.capture_content, Some(true));
}

#[tokio::test]
async fn should_propagate_activity_tracecontext_to_session_create_and_send() {
    let create = serde_json::to_value(
        SessionConfig::default()
            .with_handler(std::sync::Arc::new(
                github_copilot_sdk::handler::ApproveAllHandler,
            ))
            .with_github_token("token"),
    )
    .expect("serialize create config");
    let send = MessageOptions::new("Trace this message.")
        .with_traceparent("00-4bf92f3577b34da6a3ce929d0e0e4736-00f067aa0ba902b7-01")
        .with_tracestate("vendor=create-send");

    assert!(create.get("traceparent").is_none());
    assert_eq!(
        send.traceparent.as_deref(),
        Some("00-4bf92f3577b34da6a3ce929d0e0e4736-00f067aa0ba902b7-01")
    );
    assert_eq!(send.tracestate.as_deref(), Some("vendor=create-send"));
}

#[tokio::test]
async fn auto_start_false_requires_explicit_start() {
    let options = ClientOptions::new();

    assert!(matches!(
        &options.program,
        github_copilot_sdk::CliProgram::Resolve
    ));
    assert!(options.copilot_home.is_none());
}

#[tokio::test]
async fn force_stop_does_not_rethrow_when_tcp_cli_drops_during_startup() {
    let options = ClientOptions::new().with_transport(Transport::Tcp { port: 0 });

    assert!(matches!(options.transport, Transport::Tcp { port: 0 }));
}

#[tokio::test]
async fn startasync_cleans_up_tcp_cli_process_when_connect_fails() {
    let options = ClientOptions::new().with_transport(Transport::External {
        host: "127.0.0.1".to_string(),
        port: get_available_tcp_port(),
    });

    assert!(matches!(options.transport, Transport::External { .. }));
}

#[tokio::test]
async fn should_propagate_activity_tracecontext_to_session_resume() {
    let message = MessageOptions::new("resume trace")
        .with_traceparent("00-11111111111111111111111111111111-2222222222222222-01")
        .with_tracestate("vendor=resume");

    assert_eq!(
        message.traceparent.as_deref(),
        Some("00-11111111111111111111111111111111-2222222222222222-01")
    );
    assert_eq!(message.tracestate.as_deref(), Some("vendor=resume"));
}

#[tokio::test]
async fn should_throw_when_githubtoken_used_with_cliurl() {
    let options = ClientOptions::new()
        .with_transport(Transport::External {
            host: "localhost".to_string(),
            port: 12345,
        })
        .with_github_token("token");

    let err = Client::start(options).await.unwrap_err();
    assert!(
        matches!(err, Error::InvalidConfig(_)),
        "expected InvalidConfig, got {err:?}"
    );
    let Error::InvalidConfig(msg) = err else {
        unreachable!()
    };
    assert!(
        msg.contains("github_token"),
        "error message should mention github_token, got: {msg}"
    );
}

#[tokio::test]
async fn should_throw_when_useloggedinuser_used_with_cliurl() {
    let options = ClientOptions::new()
        .with_transport(Transport::External {
            host: "localhost".to_string(),
            port: 12345,
        })
        .with_use_logged_in_user(true);

    let err = Client::start(options).await.unwrap_err();
    assert!(
        matches!(err, Error::InvalidConfig(_)),
        "expected InvalidConfig, got {err:?}"
    );
    let Error::InvalidConfig(msg) = err else {
        unreachable!()
    };
    assert!(
        msg.contains("use_logged_in_user"),
        "error message should mention use_logged_in_user, got: {msg}"
    );
}

fn get_available_tcp_port() -> u16 {
    let listener =
        TcpListener::bind(SocketAddrV4::new(Ipv4Addr::LOCALHOST, 0)).expect("bind ephemeral port");
    listener.local_addr().expect("local addr").port()
}
