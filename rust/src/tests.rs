/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

#![cfg(test)]

use super::*;

#[test]
fn is_transport_failure_matches_request_cancelled() {
    let err = Error::from(ErrorKind::Protocol(ProtocolErrorKind::RequestCancelled));
    assert!(err.is_transport_failure());
}

#[test]
fn is_transport_failure_matches_io_error() {
    let err = Error::from(std::io::Error::new(std::io::ErrorKind::BrokenPipe, "gone"));
    assert!(err.is_transport_failure());
}

#[test]
fn is_transport_failure_rejects_rpc_error() {
    let err = Error::with_message(ErrorKind::Rpc { code: -1 }, "bad");
    assert!(!err.is_transport_failure());
}

#[test]
fn is_transport_failure_rejects_session_error() {
    let err = Error::from(ErrorKind::Session(SessionErrorKind::NotFound("s1".into())));
    assert!(!err.is_transport_failure());
}

#[test]
fn client_options_builder_composes() {
    let opts = ClientOptions::new()
        .with_program(CliProgram::Path(PathBuf::from("/usr/local/bin/copilot")))
        .with_prefix_args(["node"])
        .with_cwd(PathBuf::from("/tmp"))
        .with_env([("KEY", "value")])
        .with_env_remove(["UNWANTED"])
        .with_extra_args(["--quiet"])
        .with_github_token("ghp_test")
        .with_use_logged_in_user(false)
        .with_log_level(LogLevel::Debug)
        .with_session_idle_timeout_seconds(120)
        .with_enable_remote_sessions(true);
    assert!(matches!(opts.program, CliProgram::Path(_)));
    assert_eq!(opts.prefix_args, vec![std::ffi::OsString::from("node")]);
    assert_eq!(opts.working_directory, PathBuf::from("/tmp"));
    assert_eq!(
        opts.env,
        vec![(
            std::ffi::OsString::from("KEY"),
            std::ffi::OsString::from("value")
        )]
    );
    assert_eq!(opts.env_remove, vec![std::ffi::OsString::from("UNWANTED")]);
    assert_eq!(opts.extra_args, vec!["--quiet".to_string()]);
    assert_eq!(opts.github_token.as_deref(), Some("ghp_test"));
    assert_eq!(opts.use_logged_in_user, Some(false));
    assert!(matches!(opts.log_level, Some(LogLevel::Debug)));
    assert_eq!(opts.session_idle_timeout_seconds, Some(120));
    assert!(opts.enable_remote_sessions);
}

#[test]
fn default_transport_values_resolve_without_process_state() {
    assert!(matches!(
        resolve_default_transport_value(None).unwrap(),
        Transport::Stdio
    ));
    assert!(matches!(
        resolve_default_transport_value(Some("stdio")).unwrap(),
        Transport::Stdio
    ));
    assert!(matches!(
        resolve_default_transport_value(Some("INPROCESS")).unwrap(),
        Transport::InProcess
    ));
    assert!(resolve_default_transport_value(Some("tcp")).is_err());
}

#[test]
fn inprocess_rejects_process_scoped_options() {
    let invalid = [
        ClientOptions::new().with_cwd("."),
        ClientOptions::new().with_env([("KEY", "value")]),
        ClientOptions::new().with_env_remove(["KEY"]),
        ClientOptions::new().with_telemetry(TelemetryConfig::default()),
        ClientOptions::new().with_prefix_args(["index.js"]),
        ClientOptions::new().with_program(CliProgram::Path("copilot".into())),
        ClientOptions::new().with_extra_args(["--verbose"]),
    ];

    for options in invalid {
        assert!(validate_inprocess_options(&options).is_err());
    }
}

#[test]
fn inprocess_allows_typed_runtime_options() {
    let options = ClientOptions::new()
        .with_base_directory("state")
        .with_log_level(LogLevel::Debug)
        .with_session_idle_timeout_seconds(10)
        .with_github_token("token")
        .with_use_logged_in_user(false)
        .with_enable_remote_sessions(true);

    assert!(validate_inprocess_options(&options).is_ok());
}

#[cfg(not(feature = "in-process"))]
#[tokio::test]
async fn inprocess_requires_cargo_feature() {
    let error = Client::start(ClientOptions::new().with_transport(Transport::InProcess))
        .await
        .unwrap_err();

    assert!(error.to_string().contains("in-process"));
}

#[test]
fn is_transport_failure_rejects_other_protocol_errors() {
    let err = Error::from(ErrorKind::Protocol(ProtocolErrorKind::CliStartupTimeout));
    assert!(!err.is_transport_failure());
}

#[test]
fn build_command_lets_env_remove_strip_injected_token() {
    let opts = ClientOptions {
        github_token: Some("secret".to_string()),
        env_remove: vec![std::ffi::OsString::from("COPILOT_SDK_AUTH_TOKEN")],
        ..Default::default()
    };
    let cmd = Client::build_command(Path::new("/bin/echo"), &opts, Path::new("/tmp"));
    // get_envs() iter yields the latest action per key — None means removed.
    let action = cmd
        .as_std()
        .get_envs()
        .find(|(k, _)| *k == std::ffi::OsStr::new("COPILOT_SDK_AUTH_TOKEN"))
        .map(|(_, v)| v);
    assert_eq!(
        action,
        Some(None),
        "env_remove should win over github_token"
    );
}

#[test]
fn build_command_lets_env_override_injected_token() {
    let opts = ClientOptions {
        github_token: Some("from-options".to_string()),
        env: vec![(
            std::ffi::OsString::from("COPILOT_SDK_AUTH_TOKEN"),
            std::ffi::OsString::from("from-env"),
        )],
        ..Default::default()
    };
    let cmd = Client::build_command(Path::new("/bin/echo"), &opts, Path::new("/tmp"));
    let value = cmd
        .as_std()
        .get_envs()
        .find(|(k, _)| *k == std::ffi::OsStr::new("COPILOT_SDK_AUTH_TOKEN"))
        .and_then(|(_, v)| v);
    assert_eq!(value, Some(std::ffi::OsStr::new("from-env")));
}

#[test]
fn build_command_injects_github_token_by_default() {
    let opts = ClientOptions {
        github_token: Some("just-the-token".to_string()),
        ..Default::default()
    };
    let cmd = Client::build_command(Path::new("/bin/echo"), &opts, Path::new("/tmp"));
    let value = cmd
        .as_std()
        .get_envs()
        .find(|(k, _)| *k == std::ffi::OsStr::new("COPILOT_SDK_AUTH_TOKEN"))
        .and_then(|(_, v)| v);
    assert_eq!(value, Some(std::ffi::OsStr::new("just-the-token")));
}

fn env_value<'a>(cmd: &'a tokio::process::Command, key: &str) -> Option<&'a std::ffi::OsStr> {
    cmd.as_std()
        .get_envs()
        .find(|(k, _)| *k == std::ffi::OsStr::new(key))
        .and_then(|(_, v)| v)
}

#[test]
fn telemetry_config_builder_composes() {
    let cfg = TelemetryConfig::new()
        .with_otlp_endpoint("http://collector:4318")
        .with_otlp_protocol(OtlpHttpProtocol::HttpProtobuf)
        .with_file_path(PathBuf::from("/var/log/copilot.jsonl"))
        .with_exporter_type(OtelExporterType::OtlpHttp)
        .with_source_name("my-app")
        .with_capture_content(true);

    assert_eq!(cfg.otlp_endpoint.as_deref(), Some("http://collector:4318"));
    assert_eq!(cfg.otlp_protocol, Some(OtlpHttpProtocol::HttpProtobuf));
    assert_eq!(
        cfg.file_path.as_deref(),
        Some(Path::new("/var/log/copilot.jsonl")),
    );
    assert_eq!(cfg.exporter_type, Some(OtelExporterType::OtlpHttp));
    assert_eq!(cfg.source_name.as_deref(), Some("my-app"));
    assert_eq!(cfg.capture_content, Some(true));
    assert!(!cfg.is_empty());
    assert!(TelemetryConfig::new().is_empty());
}

#[test]
fn otlp_http_protocol_serde_matches_env_value() {
    for (protocol, wire) in [
        (OtlpHttpProtocol::HttpJson, "http/json"),
        (OtlpHttpProtocol::HttpProtobuf, "http/protobuf"),
    ] {
        assert_eq!(protocol.as_str(), wire);

        let serialized = serde_json::to_string(&protocol).unwrap();
        assert_eq!(serialized, format!("\"{wire}\""));

        let deserialized: OtlpHttpProtocol = serde_json::from_str(&serialized).unwrap();
        assert_eq!(deserialized, protocol);
    }
}

#[test]
fn build_command_sets_otel_env_when_telemetry_enabled() {
    let opts = ClientOptions {
        telemetry: Some(TelemetryConfig {
            otlp_endpoint: Some("http://collector:4318".to_string()),
            otlp_protocol: Some(OtlpHttpProtocol::HttpProtobuf),
            file_path: Some(PathBuf::from("/var/log/copilot.jsonl")),
            exporter_type: Some(OtelExporterType::OtlpHttp),
            source_name: Some("my-app".to_string()),
            capture_content: Some(true),
        }),
        ..Default::default()
    };
    let cmd = Client::build_command(Path::new("/bin/echo"), &opts, Path::new("/tmp"));
    assert_eq!(
        env_value(&cmd, "COPILOT_OTEL_ENABLED"),
        Some(std::ffi::OsStr::new("true")),
    );
    assert_eq!(
        env_value(&cmd, "OTEL_EXPORTER_OTLP_ENDPOINT"),
        Some(std::ffi::OsStr::new("http://collector:4318")),
    );
    assert_eq!(
        env_value(&cmd, "OTEL_EXPORTER_OTLP_PROTOCOL"),
        Some(std::ffi::OsStr::new("http/protobuf")),
    );
    assert_eq!(
        env_value(&cmd, "COPILOT_OTEL_FILE_EXPORTER_PATH"),
        Some(std::ffi::OsStr::new("/var/log/copilot.jsonl")),
    );
    assert_eq!(
        env_value(&cmd, "COPILOT_OTEL_EXPORTER_TYPE"),
        Some(std::ffi::OsStr::new("otlp-http")),
    );
    assert_eq!(
        env_value(&cmd, "COPILOT_OTEL_SOURCE_NAME"),
        Some(std::ffi::OsStr::new("my-app")),
    );
    assert_eq!(
        env_value(&cmd, "OTEL_INSTRUMENTATION_GENAI_CAPTURE_MESSAGE_CONTENT"),
        Some(std::ffi::OsStr::new("true")),
    );
}

#[test]
fn build_command_omits_otel_env_when_telemetry_none() {
    let opts = ClientOptions::default();
    let cmd = Client::build_command(Path::new("/bin/echo"), &opts, Path::new("/tmp"));
    for key in [
        "COPILOT_OTEL_ENABLED",
        "OTEL_EXPORTER_OTLP_ENDPOINT",
        "OTEL_EXPORTER_OTLP_PROTOCOL",
        "COPILOT_OTEL_FILE_EXPORTER_PATH",
        "COPILOT_OTEL_EXPORTER_TYPE",
        "COPILOT_OTEL_SOURCE_NAME",
        "OTEL_INSTRUMENTATION_GENAI_CAPTURE_MESSAGE_CONTENT",
    ] {
        assert!(
            env_value(&cmd, key).is_none(),
            "expected {key} to be unset when telemetry is None",
        );
    }
}

#[test]
fn build_command_omits_unset_telemetry_fields() {
    let opts = ClientOptions {
        telemetry: Some(TelemetryConfig {
            otlp_endpoint: Some("http://collector:4318".to_string()),
            ..Default::default()
        }),
        ..Default::default()
    };
    let cmd = Client::build_command(Path::new("/bin/echo"), &opts, Path::new("/tmp"));
    // The one set field plus the implicit enabled flag should propagate.
    assert_eq!(
        env_value(&cmd, "COPILOT_OTEL_ENABLED"),
        Some(std::ffi::OsStr::new("true")),
    );
    assert_eq!(
        env_value(&cmd, "OTEL_EXPORTER_OTLP_ENDPOINT"),
        Some(std::ffi::OsStr::new("http://collector:4318")),
    );
    // None of the other fields should leak as env vars.
    for key in [
        "OTEL_EXPORTER_OTLP_PROTOCOL",
        "COPILOT_OTEL_FILE_EXPORTER_PATH",
        "COPILOT_OTEL_EXPORTER_TYPE",
        "COPILOT_OTEL_SOURCE_NAME",
        "OTEL_INSTRUMENTATION_GENAI_CAPTURE_MESSAGE_CONTENT",
    ] {
        assert!(env_value(&cmd, key).is_none(), "{key} should be unset");
    }
}

#[test]
fn build_command_lets_user_env_override_telemetry() {
    let opts = ClientOptions {
        telemetry: Some(TelemetryConfig {
            otlp_endpoint: Some("http://from-config:4318".to_string()),
            ..Default::default()
        }),
        env: vec![(
            std::ffi::OsString::from("OTEL_EXPORTER_OTLP_ENDPOINT"),
            std::ffi::OsString::from("http://from-user-env:4318"),
        )],
        ..Default::default()
    };
    let cmd = Client::build_command(Path::new("/bin/echo"), &opts, Path::new("/tmp"));
    assert_eq!(
        env_value(&cmd, "OTEL_EXPORTER_OTLP_ENDPOINT"),
        Some(std::ffi::OsStr::new("http://from-user-env:4318")),
        "user-supplied options.env should override telemetry config",
    );
}

#[test]
fn build_command_sets_copilot_home_env_when_configured() {
    let opts = ClientOptions::new().with_base_directory(PathBuf::from("/custom/copilot"));
    let cmd = Client::build_command(Path::new("/bin/echo"), &opts, Path::new("/tmp"));
    assert_eq!(
        env_value(&cmd, "COPILOT_HOME"),
        Some(std::ffi::OsStr::new("/custom/copilot")),
    );

    let opts = ClientOptions::default();
    let cmd = Client::build_command(Path::new("/bin/echo"), &opts, Path::new("/tmp"));
    assert!(env_value(&cmd, "COPILOT_HOME").is_none());
}

#[test]
fn build_command_sets_connection_token_env_when_configured() {
    let opts = ClientOptions::new().with_transport(Transport::Tcp {
        port: 0,
        connection_token: Some("secret-token".to_string()),
    });
    let cmd = Client::build_command(Path::new("/bin/echo"), &opts, Path::new("/tmp"));
    assert_eq!(
        env_value(&cmd, "COPILOT_CONNECTION_TOKEN"),
        Some(std::ffi::OsStr::new("secret-token")),
    );

    let opts = ClientOptions::default();
    let cmd = Client::build_command(Path::new("/bin/echo"), &opts, Path::new("/tmp"));
    assert!(env_value(&cmd, "COPILOT_CONNECTION_TOKEN").is_none());
}

#[tokio::test]
async fn start_rejects_empty_connection_token() {
    let opts = ClientOptions::new()
        .with_transport(Transport::Tcp {
            port: 0,
            connection_token: Some(String::new()),
        })
        .with_program(CliProgram::Path(PathBuf::from("/bin/echo")));
    let err = Client::start(opts).await.unwrap_err();
    assert!(
        matches!(err.kind(), ErrorKind::InvalidConfig),
        "got {err:?}"
    );
}

#[tokio::test]
async fn start_rejects_empty_external_connection_token() {
    let opts = ClientOptions::new()
        .with_transport(Transport::External {
            host: "127.0.0.1".to_string(),
            port: 1,
            connection_token: Some(String::new()),
        })
        .with_program(CliProgram::Path(PathBuf::from("/bin/echo")));
    let err = Client::start(opts).await.unwrap_err();
    assert!(
        matches!(err.kind(), ErrorKind::InvalidConfig),
        "got {err:?}"
    );
}

#[test]
fn telemetry_config_capture_content_serializes_as_lowercase_bool() {
    let opts_true = ClientOptions {
        telemetry: Some(TelemetryConfig {
            capture_content: Some(true),
            ..Default::default()
        }),
        ..Default::default()
    };
    let opts_false = ClientOptions {
        telemetry: Some(TelemetryConfig {
            capture_content: Some(false),
            ..Default::default()
        }),
        ..Default::default()
    };
    let cmd_true = Client::build_command(Path::new("/bin/echo"), &opts_true, Path::new("/tmp"));
    let cmd_false = Client::build_command(Path::new("/bin/echo"), &opts_false, Path::new("/tmp"));
    assert_eq!(
        env_value(
            &cmd_true,
            "OTEL_INSTRUMENTATION_GENAI_CAPTURE_MESSAGE_CONTENT"
        ),
        Some(std::ffi::OsStr::new("true")),
    );
    assert_eq!(
        env_value(
            &cmd_false,
            "OTEL_INSTRUMENTATION_GENAI_CAPTURE_MESSAGE_CONTENT"
        ),
        Some(std::ffi::OsStr::new("false")),
    );
}

#[test]
fn session_idle_timeout_args_are_omitted_by_default() {
    let opts = ClientOptions::default();
    assert!(Client::session_idle_timeout_args(&opts).is_empty());
}

#[test]
fn session_idle_timeout_args_omitted_for_zero() {
    let opts = ClientOptions {
        session_idle_timeout_seconds: Some(0),
        ..Default::default()
    };
    assert!(Client::session_idle_timeout_args(&opts).is_empty());
}

#[test]
fn session_idle_timeout_args_emit_flag_for_positive_value() {
    let opts = ClientOptions {
        session_idle_timeout_seconds: Some(300),
        ..Default::default()
    };
    assert_eq!(
        Client::session_idle_timeout_args(&opts),
        vec!["--session-idle-timeout".to_string(), "300".to_string()]
    );
}

#[test]
fn remote_args_omitted_by_default() {
    let opts = ClientOptions::default();
    assert!(Client::remote_args(&opts).is_empty());
}

#[test]
fn remote_args_emit_flag_when_enabled() {
    let opts = ClientOptions {
        enable_remote_sessions: true,
        ..Default::default()
    };
    assert_eq!(Client::remote_args(&opts), vec!["--remote".to_string()]);
}

#[test]
fn log_level_args_omitted_when_unset() {
    let opts = ClientOptions::default();
    assert!(opts.log_level.is_none());
    assert!(
        Client::log_level_args(&opts).is_empty(),
        "with no caller-supplied log_level the SDK must not pass --log-level"
    );
}

#[test]
fn log_level_args_emit_flag_when_set() {
    let opts = ClientOptions::default().with_log_level(LogLevel::Debug);
    assert_eq!(Client::log_level_args(&opts), vec!["--log-level", "debug"]);
}

#[test]
fn cli_mode_opts_into_process_logging_without_changing_empty_mode_environment() {
    for (mode, expected) in [
        (ClientMode::Empty, None),
        (ClientMode::CopilotCli, Some("1")),
    ] {
        let mut options = ClientOptions::default().with_mode(mode);
        options.env.push((
            std::ffi::OsString::from("COPILOT_RUNTIME_PROCESS_FILE_LOGGING"),
            std::ffi::OsString::from("opposite"),
        ));
        let command = Client::build_command(Path::new("copilot-runtime"), &options, Path::new("."));
        let actual = command
            .as_std()
            .get_envs()
            .find(|(key, _)| *key == std::ffi::OsStr::new("COPILOT_RUNTIME_PROCESS_FILE_LOGGING"))
            .and_then(|(_, value)| value);
        assert_eq!(
            actual,
            Some(std::ffi::OsStr::new(expected.unwrap_or("opposite"))),
            "mode: {mode:?}"
        );
    }
}

#[test]
fn log_level_str_round_trips() {
    for level in [
        LogLevel::None,
        LogLevel::Error,
        LogLevel::Warning,
        LogLevel::Info,
        LogLevel::Debug,
        LogLevel::All,
    ] {
        let s = level.as_str();
        let json = serde_json::to_string(&level).unwrap();
        assert_eq!(json, format!("\"{s}\""));
        let parsed: LogLevel = serde_json::from_str(&json).unwrap();
        assert_eq!(parsed, level);
    }
}

#[test]
fn client_options_debug_redacts_handler() {
    struct StubHandler;
    #[async_trait]
    impl ListModelsHandler for StubHandler {
        async fn list_models(&self) -> Result<Vec<Model>> {
            Ok(vec![])
        }
    }
    let opts = ClientOptions {
        on_list_models: Some(Arc::new(StubHandler)),
        github_token: Some("secret-token".into()),
        ..Default::default()
    };
    let debug = format!("{opts:?}");
    assert!(debug.contains("on_list_models: Some(\"<set>\")"));
    assert!(debug.contains("github_token: Some(\"<redacted>\")"));
    assert!(!debug.contains("secret-token"));
}

#[tokio::test]
async fn list_models_uses_on_list_models_handler_when_set() {
    use std::sync::atomic::{AtomicUsize, Ordering};

    struct CountingHandler {
        calls: Arc<AtomicUsize>,
        models: Vec<Model>,
    }
    #[async_trait]
    impl ListModelsHandler for CountingHandler {
        async fn list_models(&self) -> Result<Vec<Model>> {
            self.calls.fetch_add(1, Ordering::SeqCst);
            Ok(self.models.clone())
        }
    }

    let calls = Arc::new(AtomicUsize::new(0));
    let model = Model {
        id: "byok-gpt-4".into(),
        name: "BYOK GPT-4".into(),
        ..Default::default()
    };
    let handler: Arc<dyn ListModelsHandler> = Arc::new(CountingHandler {
        calls: Arc::clone(&calls),
        models: vec![model.clone()],
    });

    let client = client_with_list_models_handler(handler);

    let result = client.list_models().await.unwrap();
    assert_eq!(result.len(), 1);
    assert_eq!(result[0].id, "byok-gpt-4");
    assert_eq!(calls.load(Ordering::SeqCst), 1);
}

#[tokio::test]
async fn list_models_serializes_concurrent_cache_misses() {
    use std::sync::atomic::{AtomicUsize, Ordering};

    struct SlowCountingHandler {
        calls: Arc<AtomicUsize>,
        models: Vec<Model>,
    }
    #[async_trait]
    impl ListModelsHandler for SlowCountingHandler {
        async fn list_models(&self) -> Result<Vec<Model>> {
            self.calls.fetch_add(1, Ordering::SeqCst);
            tokio::time::sleep(std::time::Duration::from_millis(25)).await;
            Ok(self.models.clone())
        }
    }

    let calls = Arc::new(AtomicUsize::new(0));
    let model = Model {
        id: "single-flight-model".into(),
        name: "Single Flight Model".into(),
        ..Default::default()
    };
    let handler: Arc<dyn ListModelsHandler> = Arc::new(SlowCountingHandler {
        calls: Arc::clone(&calls),
        models: vec![model],
    });
    let client = client_with_list_models_handler(handler);

    let (first, second) = tokio::join!(client.list_models(), client.list_models());
    assert_eq!(first.unwrap()[0].id, "single-flight-model");
    assert_eq!(second.unwrap()[0].id, "single-flight-model");
    assert_eq!(calls.load(Ordering::SeqCst), 1);
}

#[tokio::test]
async fn cancelled_resume_session_unregisters_pending_session() {
    let (client_write, _server_read) = tokio::io::duplex(8192);
    let (_server_write, client_read) = tokio::io::duplex(8192);
    let client = Client::from_streams(client_read, client_write, std::env::temp_dir()).unwrap();
    assert!(client.startup_timings().is_none());
    let session_id = SessionId::new("resume-cancel-test");
    let handle = tokio::spawn({
        let client = client.clone();
        async move {
            client
                .resume_session(ResumeSessionConfig::new(session_id))
                .await
        }
    });

    wait_for_pending_session_registration(&client).await;
    handle.abort();
    let _ = handle.await;

    assert!(client.inner.router.session_ids().is_empty());
    client.force_stop();
}

#[cfg(any(unix, windows))]
#[tokio::test]
async fn dropping_last_client_kills_spawned_cli() {
    let temp = tempfile::tempdir().unwrap();
    let ready = temp.path().join("ready");
    let survived = temp.path().join("survived");
    let child = test_child_command(temp.path(), &ready, &survived)
        .spawn()
        .unwrap();
    let (client_write, _server_read) = tokio::io::duplex(64);
    let (_server_write, client_read) = tokio::io::duplex(64);
    let client = Client::from_transport(
        client_read,
        client_write,
        Some(child),
        None,
        temp.path().to_path_buf(),
        None,
        None,
        false,
        false,
        false,
        None,
        None,
        None,
        ClientMode::default(),
        None,
        false,
    )
    .unwrap();

    wait_for_test_child(&ready).await;
    drop(client);

    assert_test_child_killed(&survived).await;
}

#[cfg(any(unix, windows))]
#[tokio::test]
async fn spawned_child_is_killed_when_dropped() {
    let temp = tempfile::tempdir().unwrap();
    let ready = temp.path().join("ready");
    let survived = temp.path().join("survived");
    let child = test_child_command(temp.path(), &ready, &survived)
        .spawn()
        .unwrap();

    wait_for_test_child(&ready).await;
    drop(child);

    assert_test_child_killed(&survived).await;
}

#[cfg(any(unix, windows))]
fn test_child_command(temp: &Path, ready: &Path, survived: &Path) -> Command {
    let mut command = Client::build_command(Path::new("node"), &ClientOptions::default(), temp);
    #[cfg(windows)]
    {
        const CREATE_NO_WINDOW: u32 = 0x0800_0000;
        command.creation_flags(CREATE_NO_WINDOW);
    }
    command
        .args([
            "-e",
            r#"
                const fs = require("node:fs");
                fs.writeFileSync(process.env.READY, "ready");
                setTimeout(() => fs.writeFileSync(process.env.SURVIVED, "survived"), 1000);
                "#,
        ])
        .env("READY", ready)
        .env("SURVIVED", survived)
        .stderr(Stdio::inherit());
    command
}

#[cfg(any(unix, windows))]
async fn wait_for_test_child(ready: &Path) {
    let deadline = tokio::time::Instant::now() + Duration::from_secs(30);
    while !ready.exists() {
        assert!(
            tokio::time::Instant::now() < deadline,
            "child did not report readiness"
        );
        tokio::time::sleep(Duration::from_millis(10)).await;
    }
}

#[cfg(any(unix, windows))]
async fn assert_test_child_killed(survived: &Path) {
    tokio::time::sleep(Duration::from_millis(1500)).await;

    assert!(
        !survived.exists(),
        "child survived after its owner was dropped"
    );
}

fn client_with_list_models_handler(handler: Arc<dyn ListModelsHandler>) -> Client {
    Client {
        ahp_host_sessions: None,
        inner: Arc::new(ClientInner {
            ahp_host_callbacks: Arc::new(ahp_host::ExitCallbacks::default()),
            ahp_host_sessions: std::sync::Weak::new(),
            child: parking_lot::Mutex::new(None),
            owns_stdio: false,
            force_stop_requested: tokio_util::sync::CancellationToken::new(),
            process_tree: parking_lot::Mutex::new(None),
            #[cfg(feature = "in-process")]
            ffi_host: parking_lot::Mutex::new(None),
            rpc: {
                let (req_tx, _req_rx) = mpsc::unbounded_channel();
                let (notif_tx, _notif_rx) = broadcast::channel(16);
                let (read_pipe, _write_pipe) = tokio::io::duplex(64);
                let (_unused_read, write_pipe) = tokio::io::duplex(64);
                JsonRpcClient::new(write_pipe, read_pipe, notif_tx, req_tx)
            },
            cwd: PathBuf::from("."),
            request_rx: parking_lot::Mutex::new(None),
            notification_tx: broadcast::channel(16).0,
            router: router::SessionRouter::new(),
            github_token_registry: Arc::new(github_token::GitHubTokenRegistry::new()),
            negotiated_protocol_version: OnceLock::new(),
            state: parking_lot::Mutex::new(ConnectionState::Connected),
            lifecycle_tx: broadcast::channel(16).0,
            on_list_models: Some(handler),
            models_cache: parking_lot::Mutex::new(Arc::new(tokio::sync::OnceCell::new())),
            session_fs_configured: false,
            session_fs_sqlite_declared: false,
            session_fs_binary_declared: false,
            llm_inference: OnceLock::new(),
            extension_launch_provider: Arc::new(
                extension_launch_provider::ExtensionLaunchProviderDispatcher::new(None),
            ),
            installation_confirmation: Arc::new(
                installation_confirmation::InstallationConfirmationDispatcher::new(),
            ),
            on_github_telemetry: None,
            on_get_trace_context: None,
            effective_connection_token: None,
            mode: ClientMode::default(),
            client_info: None,
            startup_timings: OnceLock::new(),
        }),
    }
}

async fn wait_for_pending_session_registration(client: &Client) {
    let deadline = tokio::time::Instant::now() + std::time::Duration::from_secs(1);
    while client.inner.router.session_ids().is_empty() {
        assert!(
            tokio::time::Instant::now() < deadline,
            "session was not registered"
        );
        tokio::time::sleep(std::time::Duration::from_millis(10)).await;
    }
}
