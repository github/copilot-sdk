/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

#![cfg(test)]

use std::sync::atomic::{AtomicUsize, Ordering};

use super::*;

const OWNER_REJECTION: &str = "a live auth owner requires SDK-owned in-process startup";
const ADMISSION_REJECTION: &str = "controlled auth owner declined native admission";
const CHILD_CASE: &str = "COPILOT_SDK_AUTH_OWNER_TRANSPORT_TEST";

#[test]
fn default_transport_reaches_native_auth_owner() {
    if let Ok(case) = std::env::var(CHILD_CASE) {
        tokio::runtime::Builder::new_current_thread()
            .enable_all()
            .build()
            .unwrap()
            .block_on(check_default_transport(&case));
        return;
    }

    for case in ["process-inprocess", "stdio", "unset"] {
        let mut command = std::process::Command::new(std::env::current_exe().unwrap());
        command
            .args([
                "--exact",
                "auth_owner_transport_tests::default_transport_reaches_native_auth_owner",
                "--nocapture",
            ])
            .env(CHILD_CASE, case)
            .env_remove(DEFAULT_CONNECTION_ENV_VAR);
        match case {
            "stdio" => {
                command.env(DEFAULT_CONNECTION_ENV_VAR, "stdio");
            }
            "process-inprocess" => {
                command.env(DEFAULT_CONNECTION_ENV_VAR, "inprocess");
            }
            _ => {}
        }
        let output = command.output().expect("run isolated SDK test");
        assert!(
            output.status.success(),
            "{case}: {}\nstdout:\n{}\nstderr:\n{}",
            output.status,
            String::from_utf8_lossy(&output.stdout),
            String::from_utf8_lossy(&output.stderr),
        );
    }
}

async fn check_default_transport(case: &str) {
    let home = tempfile::tempdir().unwrap();
    let admissions = Arc::new(AtomicUsize::new(0));
    let recorded = Arc::clone(&admissions);
    let mut options = ClientOptions::new();
    options.base_directory = Some(home.path().to_owned());
    options.in_process_auth_owner = Some(Arc::new(move |server, connection| {
        assert_ne!(server, 0, "native startup must supply a server");
        assert_ne!(
            connection, 0,
            "native startup must supply an auth reservation"
        );
        recorded.fetch_add(1, Ordering::SeqCst);
        Err(Error::with_message(
            ErrorKind::InvalidConfig,
            ADMISSION_REJECTION,
        ))
    }));
    let native_admission = case == "process-inprocess";
    if native_admission
        && let Some(library) = std::env::var_os("COPILOT_AUTH_OWNER_RUNTIME_LIBRARY")
    {
        let library = PathBuf::from(library);
        assert!(library.is_absolute(), "the runtime image must be explicit");
        assert!(library.is_file(), "runtime image: {}", library.display());
        options.in_process_runtime_library_path = Some(library);
    }

    let error = Client::start(options).await.unwrap_err();
    assert!(
        matches!(error.kind(), ErrorKind::InvalidConfig),
        "{error:?}"
    );
    let expected = if native_admission {
        ADMISSION_REJECTION
    } else {
        OWNER_REJECTION
    };
    assert_eq!(error.to_string(), expected, "{case}");
    assert_eq!(
        admissions.load(Ordering::SeqCst),
        usize::from(native_admission)
    );
}

#[tokio::test]
async fn auth_owner_rejects_non_owned_transports_after_normalization() {
    for transport in [
        Transport::Default,
        Transport::Stdio,
        Transport::Tcp {
            port: 0,
            connection_token: None,
        },
        Transport::External {
            host: "127.0.0.1".into(),
            port: 0,
            connection_token: None,
        },
    ] {
        let mut options = ClientOptions::new().with_transport(transport);
        options
            .env
            .push((DEFAULT_CONNECTION_ENV_VAR.into(), "stdio".into()));
        options.in_process_auth_owner = Some(Arc::new(|_, _| panic!("unexpected admission")));
        let error = Client::start(options).await.unwrap_err();
        assert!(
            matches!(error.kind(), ErrorKind::InvalidConfig),
            "{error:?}"
        );
        assert_eq!(error.to_string(), OWNER_REJECTION);
    }

    for borrowed_session in [false, true] {
        for transport in [Transport::Default, Transport::InProcess] {
            let mut options = ClientOptions::new().with_transport(transport);
            options
                .env
                .push((DEFAULT_CONNECTION_ENV_VAR.into(), "inprocess".into()));
            options.in_process_auth_owner = Some(Arc::new(|_, _| panic!("unexpected admission")));
            if borrowed_session {
                options.in_process_session_binding = Some(InProcessSessionBinding {
                    parent_connection_id: 1,
                    native_session_id: "borrowed".into(),
                    registration_token: "registration".into(),
                });
            } else {
                options.in_process_server_id = Some(1);
            }
            let error = Client::start(options).await.unwrap_err();
            assert!(
                matches!(error.kind(), ErrorKind::InvalidConfig),
                "{error:?}"
            );
            assert_eq!(error.to_string(), OWNER_REJECTION);
        }
    }
}
