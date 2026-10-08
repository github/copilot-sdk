use super::*;

#[path = "lifetime_tests.rs"]
mod lifetime_tests;

impl Peer {
    async fn materialize(&mut self, host_id: &str, handoff: &str, config: Value) {
        self.send(json!({
            "jsonrpc": "2.0", "id": 900, "method": "host.materializeSession",
            "params": {"hostId": host_id, "handoffId": handoff, "config": config}
        }))
        .await;
    }

    async fn release(&mut self, host_id: &str, handoff: &str) {
        self.send(json!({
            "jsonrpc": "2.0", "method": "host.sessionReleased",
            "params": {"hostId": host_id, "handoffId": handoff}
        }))
        .await;
    }

    async fn created(&mut self) -> Value {
        let request = self.request().await;
        assert_eq!(request["method"], "session.create");
        self.respond(
            &request,
            json!({"sessionId": request["params"]["sessionId"]}),
        )
        .await;
        request
    }
}

fn factory_options() -> (
    AhpHostOptions,
    mpsc::UnboundedReceiver<Arc<Session>>,
    mpsc::UnboundedReceiver<Arc<Session>>,
) {
    let (created_tx, created) = mpsc::unbounded_channel();
    let (released_tx, released) = mpsc::unbounded_channel();
    let options = local_options()
        .with_create_session(move |request: AhpSessionRequest, client: Client| {
            let created_tx = created_tx.clone();
            async move {
                assert!(!request.cancellation_token.is_cancelled());
                assert!(request.config.permission_handler.is_none());
                let session = Arc::new(client.create_session(request.config).await?);
                created_tx.send(session.clone()).unwrap();
                Ok(session)
            }
        })
        .with_on_session_released(move |session| {
            released_tx.send(session).unwrap();
        });
    (options, created, released)
}

#[tokio::test]
async fn owner_loss_cancels_pending_factory_but_releases_its_late_original() {
    let (client, mut peer) = fixture();
    let (ready_tx, mut ready) = mpsc::unbounded_channel();
    let (released_tx, mut released) = mpsc::unbounded_channel();
    let finish = CancellationToken::new();
    let gate = finish.clone();
    let options = local_options()
        .with_create_session(move |request: AhpSessionRequest, client: Client| {
            let ready_tx = ready_tx.clone();
            let gate = gate.clone();
            async move {
                let original = Arc::new(client.create_session(request.config).await?);
                ready_tx
                    .send((original.clone(), request.cancellation_token))
                    .unwrap();
                gate.cancelled().await;
                Ok(original)
            }
        })
        .with_on_session_released(move |session| {
            released_tx.send(session).unwrap();
        });
    let host = start_factory(&client, &mut peer, options).await;
    peer.materialize(
        &host.host_id,
        "pending-owner-loss",
        json!({"sessionId":"pending"}),
    )
    .await;
    peer.created().await;
    let (original, cancellation) = receive(&mut ready).await;
    client.force_stop();
    timeout(TIMEOUT, cancellation.cancelled()).await.unwrap();
    assert!(released.try_recv().is_err());
    finish.cancel();
    assert!(Arc::ptr_eq(&original, &receive(&mut released).await));
    assert!(timeout(TIMEOUT, released.recv()).await.unwrap().is_none());
}

async fn start_factory(client: &Client, peer: &mut Peer, options: AhpHostOptions) -> AhpHost {
    let pending = start(client, options);
    let request = peer.request().await;
    assert_eq!(request["params"]["sessionFactory"], true);
    assert!(request["params"].get("createSession").is_none());
    peer.started(&request, None).await;
    pending.await.unwrap().unwrap()
}

async fn receive<T>(receiver: &mut mpsc::UnboundedReceiver<T>) -> T {
    timeout(TIMEOUT, receiver.recv()).await.unwrap().unwrap()
}

#[tokio::test]
async fn resume_can_return_retained_original_after_creation_handoff_released() {
    let (client, mut peer) = fixture();
    let (options, mut created, mut released) = factory_options();
    let host = start_factory(&client, &mut peer, options).await;
    peer.materialize(&host.host_id, "create", json!({"sessionId": "retained"}))
        .await;
    peer.created().await;
    assert_eq!(
        peer.request().await["result"],
        json!({"sessionId": "retained"})
    );
    let original = receive(&mut created).await;
    peer.release(&host.host_id, "create").await;
    assert!(Arc::ptr_eq(&original, &receive(&mut released).await));
    let retained = original.clone();
    let (released_tx, mut resumed_released) = mpsc::unbounded_channel();
    let pending = start(
        &client,
        local_options()
            .with_resume_session(move |_: AhpSessionResumeRequest, _: Client| {
                let retained = retained.clone();
                async move { Ok(retained) }
            })
            .with_on_session_released(move |session| {
                released_tx.send(session).unwrap();
            }),
    );
    let request = peer.request().await;
    peer.started(&request, None).await;
    let host = pending.await.unwrap().unwrap();
    peer.send(json!({
        "jsonrpc": "2.0", "id": 901, "method": "host.materializeSession",
        "params": {"hostId": host.host_id, "handoffId": "resume", "resume": true,
                   "config": {"sessionId": "retained", "continuePendingWork": false}}
    }))
    .await;
    let response = peer.request().await;
    assert_eq!(response["id"], 901);
    assert_eq!(response["result"], json!({"sessionId": "retained"}));
    peer.release(&host.host_id, "resume").await;
    assert!(Arc::ptr_eq(
        &original,
        &receive(&mut resumed_released).await
    ));
    client.force_stop();
}

#[tokio::test]
async fn resume_rejects_superseded_retained_original() {
    let (client, mut peer) = fixture();
    let (options, mut created, mut released) = factory_options();
    let host = start_factory(&client, &mut peer, options).await;
    peer.materialize(&host.host_id, "create", json!({"sessionId": "retained"}))
        .await;
    peer.created().await;
    assert!(peer.request().await.get("result").is_some());
    let original = receive(&mut created).await;
    peer.release(&host.host_id, "create").await;
    assert!(Arc::ptr_eq(&original, &receive(&mut released).await));

    let resuming = {
        let client = client.clone();
        tokio::spawn(async move {
            client
                .resume_session(ResumeSessionConfig::new("retained".into()))
                .await
        })
    };
    let request = peer.request().await;
    assert_eq!(request["method"], "session.resume");
    peer.respond(&request, json!({"sessionId": "retained"}))
        .await;
    let reload = peer.request().await;
    assert_eq!(reload["method"], "session.skills.reload");
    peer.respond(&reload, json!({})).await;
    let current = timeout(TIMEOUT, resuming).await.unwrap().unwrap().unwrap();
    let original_retired = original.cancellation_token();
    timeout(TIMEOUT, original_retired.cancelled())
        .await
        .unwrap();
    let retained = original.clone();
    let (released_tx, mut resumed_released) = mpsc::unbounded_channel();
    let pending = start(
        &client,
        local_options()
            .with_resume_session(move |_: AhpSessionResumeRequest, _: Client| {
                let retained = retained.clone();
                async move { Ok(retained) }
            })
            .with_on_session_released(move |session| {
                released_tx.send(session).unwrap();
            }),
    );
    let request = peer.request().await;
    peer.started(&request, None).await;
    let host = pending.await.unwrap().unwrap();
    peer.send(json!({
        "jsonrpc": "2.0", "id": 901, "method": "host.materializeSession",
        "params": {"hostId": host.host_id, "handoffId": "resume", "resume": true,
                   "config": {"sessionId": "retained"}}
    }))
    .await;
    let response = peer.request().await;
    assert_eq!(response["id"], 901);
    assert!(response.get("error").is_some());
    assert!(
        response["error"]["message"]
            .as_str()
            .unwrap()
            .contains("AHP callback must return the requested session from this client"),
        "{response}"
    );
    assert!(Arc::ptr_eq(
        &original,
        &receive(&mut resumed_released).await
    ));
    let expected = serde_json::from_value(json!({"sessionId": "retained"})).unwrap();
    assert!(
        current
            .validate_ahp_handoff(&client, &expected, true)
            .is_ok()
    );
    client.force_stop();
}

#[tokio::test]
async fn resume_factory_registered_before_start_retains_original_and_preserves_settings() {
    let (client, mut peer) = fixture();
    let (created_tx, mut created) = mpsc::unbounded_channel();
    let (released_tx, mut released) = mpsc::unbounded_channel();
    let pending = start(
        &client,
        local_options()
            .with_resume_session(move |request: AhpSessionResumeRequest, client: Client| {
                let created_tx = created_tx.clone();
                async move {
                    let session = Arc::new(client.resume_session(request.config).await?);
                    created_tx.send(session.clone()).unwrap();
                    Ok(session)
                }
            })
            .with_on_session_released(move |session| {
                released_tx.send(session).unwrap();
            }),
    );
    let start = peer.request().await;
    let host_id = start["params"]["hostId"].as_str().unwrap();
    assert_eq!(start["params"]["resumeFactory"], true);
    assert!(start["params"].get("sessionFactory").is_none());
    peer.send(json!({
        "jsonrpc": "2.0", "id": 900, "method": "host.materializeSession",
        "params": {
            "hostId": host_id, "handoffId": "resume", "resume": true,
            "config": {
                "sessionId": "durable", "workingDirectory": "/workspace",
                "continuePendingWork": false, "suppressResumeEvent": true
            }
        }
    }))
    .await;
    let resume = peer.request().await;
    assert_eq!(resume["method"], "session.resume");
    assert_eq!(resume["params"]["sessionId"], "durable");
    assert_eq!(resume["params"]["workingDirectory"], "/workspace");
    assert_eq!(resume["params"]["continuePendingWork"], false);
    assert_eq!(resume["params"]["disableResume"], true);
    peer.respond(&resume, json!({"sessionId": "durable"})).await;
    let reload = peer.request().await;
    assert_eq!(reload["method"], "session.skills.reload");
    peer.respond(&reload, json!({})).await;
    let response = peer.request().await;
    assert_eq!(
        response["result"],
        json!({"sessionId": "durable"}),
        "{response}"
    );
    let original = receive(&mut created).await;
    peer.started(&start, None).await;
    pending.await.unwrap().unwrap();
    peer.release(host_id, "resume").await;
    assert!(Arc::ptr_eq(&original, &receive(&mut released).await));
    peer.release(host_id, "resume").await;
    client.force_stop();
    assert!(timeout(TIMEOUT, released.recv()).await.unwrap().is_none());
}

#[tokio::test]
async fn resume_never_falls_back_to_create_factory() {
    let (client, mut peer) = fixture();
    let options = local_options().with_create_session(|_: AhpSessionRequest, _: Client| async {
        panic!("must not create on resume")
    });
    let host = start_factory(&client, &mut peer, options).await;
    peer.send(json!({
        "jsonrpc": "2.0", "id": 900, "method": "host.materializeSession",
        "params": {"hostId": host.host_id, "handoffId": "resume", "resume": true,
                   "config": {"sessionId": "durable"}}
    }))
    .await;
    assert!(
        peer.request().await["error"]["message"]
            .as_str()
            .unwrap()
            .contains("unavailable")
    );
    assert!(client.host_sessions().unwrap().0.lock().handoffs.is_empty());
    client.force_stop();
}

#[tokio::test]
async fn cancelled_resume_releases_late_original_once() {
    let (client, mut peer) = fixture();
    let (ready_tx, mut ready) = mpsc::unbounded_channel();
    let (released_tx, mut released) = mpsc::unbounded_channel();
    let finish = CancellationToken::new();
    let gate = finish.clone();
    let options = local_options()
        .with_resume_session(move |request: AhpSessionResumeRequest, client: Client| {
            let ready_tx = ready_tx.clone();
            let gate = gate.clone();
            async move {
                let original = Arc::new(client.resume_session(request.config).await?);
                ready_tx
                    .send((original.clone(), request.cancellation_token))
                    .unwrap();
                gate.cancelled().await;
                Ok(original)
            }
        })
        .with_on_session_released(move |session| {
            released_tx.send(session).unwrap();
        });
    let pending = start(&client, options);
    let request = peer.request().await;
    peer.started(&request, None).await;
    let host = pending.await.unwrap().unwrap();
    peer.send(json!({
        "jsonrpc": "2.0", "id": 900, "method": "host.materializeSession",
        "params": {"hostId": host.host_id, "handoffId": "late-resume", "resume": true,
                   "config": {"sessionId": "durable"}}
    }))
    .await;
    let request = peer.request().await;
    assert_eq!(request["method"], "session.resume");
    peer.respond(&request, json!({"sessionId": "durable"}))
        .await;
    let reload = peer.request().await;
    assert_eq!(reload["method"], "session.skills.reload");
    peer.respond(&reload, json!({})).await;
    let (original, cancellation) = receive(&mut ready).await;
    peer.release(&host.host_id, "late-resume").await;
    assert!(peer.request().await.get("error").is_some());
    assert!(cancellation.is_cancelled());
    assert!(released.try_recv().is_err());
    finish.cancel();
    assert!(Arc::ptr_eq(&original, &receive(&mut released).await));
    client.force_stop();
    assert!(timeout(TIMEOUT, released.recv()).await.unwrap().is_none());
}

#[tokio::test]
async fn factory_registered_before_start_response_retains_exact_original_once() {
    let (client, mut peer) = fixture();
    let (options, mut created, mut released) = factory_options();
    let pending = start(&client, options);
    let request = peer.request().await;
    let host_id = request["params"]["hostId"].as_str().unwrap();
    assert_eq!(request["params"]["sessionFactory"], true);
    peer.materialize(
        host_id,
        "handoff",
        json!({
            "sessionId": "requested", "workingDirectory": "/workspace", "streaming": true,
            "enableMcpApps": true, "enableConfigDiscovery": false,
            "mcpOAuthTokenStorage": "in-memory"
        }),
    )
    .await;
    let create = peer.created().await;
    assert_eq!(create["params"]["workingDirectory"], "/workspace");
    assert_eq!(create["params"]["requestMcpApps"], true);
    assert_eq!(create["params"]["requestPermission"], false);
    assert_eq!(create["params"]["mcpOAuthTokenStorage"], "in-memory");
    assert!(create["params"].get("mcpOauthTokenStorage").is_none());
    assert_eq!(
        peer.request().await["result"],
        json!({"sessionId": "requested"})
    );
    let original = receive(&mut created).await;
    peer.started(&request, None).await;
    pending.await.unwrap().unwrap();
    peer.release("other-host", "handoff").await;
    peer.release(host_id, "handoff").await;
    let returned = receive(&mut released).await;
    assert!(Arc::ptr_eq(&original, &returned));
    peer.release(host_id, "handoff").await;
    peer.exited(host_id).await;
    // Round-trip barrier also proves that release did not send any session RPC.
    let session = original.clone();
    let events = tokio::spawn(async move { session.get_events().await });
    let request = peer.request().await;
    assert_eq!(request["method"], "session.getMessages");
    peer.respond(&request, json!({"events": []})).await;
    events.await.unwrap().unwrap();
    assert!(released.try_recv().is_err());
    assert!(client.host_sessions().unwrap().0.lock().handoffs.is_empty());
}

#[tokio::test]
async fn cancellation_responds_promptly_and_releases_late_original_once() {
    let (client, mut peer) = fixture();
    let (ready_tx, mut ready) = mpsc::unbounded_channel();
    let (released_tx, mut released) = mpsc::unbounded_channel();
    let finish = CancellationToken::new();
    let gate = finish.clone();
    let options = local_options()
        .with_create_session(move |request: AhpSessionRequest, client: Client| {
            let ready_tx = ready_tx.clone();
            let gate = gate.clone();
            async move {
                let original = Arc::new(client.create_session(request.config).await?);
                ready_tx
                    .send((original.clone(), request.cancellation_token.clone()))
                    .unwrap();
                gate.cancelled().await;
                Ok(original)
            }
        })
        .with_on_session_released(move |session| {
            released_tx.send(session).unwrap();
        });
    let host = start_factory(&client, &mut peer, options).await;
    peer.materialize(&host.host_id, "late", json!({"sessionId": "late-session"}))
        .await;
    peer.created().await;
    let (original, cancelled) = receive(&mut ready).await;
    peer.release(&host.host_id, "late").await;
    let response = peer.request().await;
    assert_eq!(response["id"], 900);
    assert!(response.get("error").is_some());
    assert!(cancelled.is_cancelled());
    assert!(released.try_recv().is_err());
    finish.cancel();
    assert!(Arc::ptr_eq(&original, &receive(&mut released).await));
    peer.exited(&host.host_id).await;
    client.force_stop();
    assert!(timeout(TIMEOUT, released.recv()).await.unwrap().is_none());
}

#[tokio::test]
async fn callback_failure_and_panic_fail_handoff_without_release() {
    for panic in [false, true] {
        let (client, mut peer) = fixture();
        let options = local_options().with_create_session(
            move |_: AhpSessionRequest, _: Client| async move {
                assert!(!panic, "application panic");
                Err(handoff_error("application failure"))
            },
        );
        let host = start_factory(&client, &mut peer, options).await;
        peer.materialize(&host.host_id, "failed", json!({"sessionId": "failed"}))
            .await;
        assert!(peer.request().await.get("error").is_some());
        assert!(client.host_sessions().unwrap().0.lock().handoffs.is_empty());
        client.force_stop();
    }
}

#[tokio::test]
async fn altered_post_create_setting_is_rejected_and_original_released() {
    let (client, mut peer) = fixture();
    let (released_tx, mut released) = mpsc::unbounded_channel();
    let options = local_options()
        .with_create_session(
            |mut request: AhpSessionRequest, client: Client| async move {
                request.config.skip_custom_instructions = None;
                Ok(Arc::new(client.create_session(request.config).await?))
            },
        )
        .with_on_session_released(move |session| {
            released_tx.send(session).unwrap();
        });
    let host = start_factory(&client, &mut peer, options).await;
    peer.materialize(
        &host.host_id,
        "altered",
        json!({
            "sessionId": "altered", "skipCustomInstructions": true
        }),
    )
    .await;
    peer.created().await;
    let error = peer.request().await;
    assert!(
        error["error"]["message"]
            .as_str()
            .unwrap()
            .contains("preserve")
    );
    assert_eq!(receive(&mut released).await.id().as_str(), "altered");
    client.force_stop();
}

#[tokio::test]
async fn host_exit_transport_loss_and_force_stop_release_original_once() {
    for end in ["host-exit", "transport-loss", "force-stop"] {
        let (client, mut peer) = fixture();
        let (options, mut created, mut released) = factory_options();
        let host = start_factory(&client, &mut peer, options).await;
        peer.materialize(&host.host_id, "ending", json!({"sessionId": "ending"}))
            .await;
        peer.created().await;
        assert!(peer.request().await.get("result").is_some());
        let original = receive(&mut created).await;
        match end {
            "host-exit" => peer.exited(&host.host_id).await,
            "transport-loss" => {
                peer.write.shutdown().await.unwrap();
            }
            _ => client.force_stop(),
        }
        assert!(Arc::ptr_eq(&original, &receive(&mut released).await));
        client.force_stop();
        assert!(timeout(TIMEOUT, released.recv()).await.unwrap().is_none());
        assert!(
            client
                .host_sessions()
                .unwrap()
                .0
                .lock()
                .factories
                .is_empty()
        );
    }
}

#[tokio::test]
async fn failed_and_cancelled_start_remove_factory() {
    for cancel in [false, true] {
        let (client, mut peer) = fixture();
        let (options, _, mut released) = factory_options();
        let pending = start(&client, options);
        let request = peer.request().await;
        if cancel {
            pending.abort();
            assert!(pending.await.unwrap_err().is_cancelled());
            peer.started(&request, None).await;
            let disposal = peer.request().await;
            assert_eq!(disposal["method"], "host.dispose");
            peer.respond(&disposal, json!({})).await;
        } else {
            peer.send(json!({"jsonrpc":"2.0", "id": request["id"],
                "error": {"code": -32000, "message": "start failed"}}))
                .await;
            assert!(pending.await.unwrap().is_err());
        }
        assert!(
            client
                .host_sessions()
                .unwrap()
                .0
                .lock()
                .factories
                .is_empty()
        );
        peer.materialize(
            request["params"]["hostId"].as_str().unwrap(),
            "after-failure",
            json!({"sessionId":"after-failure"}),
        )
        .await;
        assert!(peer.request().await.get("error").is_some());
        assert!(timeout(TIMEOUT, released.recv()).await.unwrap().is_none());
    }
}

#[test]
fn host_settings_round_trip_without_callbacks_or_losing_constraints() {
    let value = json!({
        "sessionId": "id", "workingDirectory": "/work", "additionalDirectories": ["/extra"],
        "configDir": "/config", "enableExperimentalMode": true,
        "infiniteSessions": {"enabled": false, "backgroundCompactionThreshold": 0.7},
        "streaming": true, "gitHubToken": "test", "mcpOAuthTokenStorage": "in-memory",
        "enableConfigDiscovery": false, "featureFlags": {"flag": true},
        "enableManagedSettings": false, "enableSessionStore": true,
        "availableTools": ["read_file"], "excludedTools": ["shell"], "allowedModels": ["model"],
        "systemMessage": {"mode":"append","content":"app"}, "skipCustomInstructions": true,
        "customAgentsLocalOnly": true, "coauthorEnabled": false, "manageScheduleEnabled": false,
        "memory": {"enabled":false}, "pluginDirectories": ["/plugins"],
        "skillDirectories": ["/skills"], "instructionDirectories": ["/instructions"],
        "enableMcpApps": true, "model":"model", "reasoningEffort":"high", "contextTier":"long_context"
    });
    let config = config_from_host(&serde_json::from_value(value.clone()).unwrap()).unwrap();
    assert!(config.permission_handler.is_none());
    assert_eq!(config.config_directory, Some(PathBuf::from("/config")));
    assert_eq!(config.enable_experimental_mode, Some(true));
    assert_eq!(
        config.infinite_sessions.as_ref().unwrap().enabled,
        Some(false)
    );
    assert_eq!(config_for_host(&config).unwrap(), value);
    let mut resume_value = value.clone();
    resume_value["allowTranscriptRecovery"] = json!(false);
    let resume =
        resume_config_from_host(&serde_json::from_value(resume_value.clone()).unwrap()).unwrap();
    assert!(resume.permission_handler.is_none());
    assert_eq!(resume.config_directory, Some(PathBuf::from("/config")));
    assert_eq!(resume.enable_experimental_mode, Some(true));
    assert_eq!(
        resume.infinite_sessions.as_ref().unwrap().enabled,
        Some(false)
    );
    assert_eq!(resume_config_for_host(&resume).unwrap(), resume_value);
    assert_eq!(resume.allow_transcript_recovery, Some(false));
    let explicit_true = crate::ResumeSessionConfig::new(crate::SessionId::from("id"))
        .with_allow_transcript_recovery(true);
    assert_eq!(
        resume_config_for_host(&explicit_true).unwrap()["allowTranscriptRecovery"],
        true
    );
    assert!(config_from_host(&serde_json::from_value(json!({"unknown":true})).unwrap()).is_err());
    assert!(contains_settings(
        Some(&json!({"a":{"b":true,"c":1}})),
        &json!({"a":{"b":true}})
    ));
    assert!(!contains_settings(
        Some(&json!({"a":["b","c"]})),
        &json!({"a":["b"]})
    ));

    let mut resume = crate::ResumeSessionConfig::new(crate::SessionId::from("id"));
    resume.mcp_oauth_token_storage = Some("in-memory".into());
    let (wire, _) = resume.into_wire().unwrap();
    let wire = serde_json::to_value(wire).unwrap();
    assert_eq!(wire["mcpOAuthTokenStorage"], "in-memory");
    assert!(wire.get("mcpOauthTokenStorage").is_none());
}

#[tokio::test]
async fn release_before_request_poll_prevents_factory_invocation() {
    let (client, mut peer) = fixture();
    let (options, mut created, _) = factory_options();
    let host = start_factory(&client, &mut peer, options).await;
    let params: HostSessionCreateCallback = serde_json::from_value(json!({
        "hostId": host.host_id, "handoffId": "ended", "config": {"sessionId":"ended"}
    }))
    .unwrap();
    let (factory, handoff) = client.prepare_ahp_session(&params).unwrap();
    assert!(client.prepare_ahp_session(&params).is_err());
    client
        .host_sessions()
        .unwrap()
        .release(&host.host_id, "ended");
    assert!(
        client
            .materialize_ahp_session(params, factory, handoff)
            .await
            .is_err()
    );
    assert!(created.try_recv().is_err());
    client.force_stop();
}

#[tokio::test]
async fn completed_handoffs_and_unknown_releases_leave_no_history() {
    let (client, mut peer) = fixture();
    let (options, mut created, mut released) = factory_options();
    let host = start_factory(&client, &mut peer, options).await;
    for n in 0..128 {
        let id = format!("session-{n}");
        peer.materialize(&host.host_id, &id, json!({"sessionId": id}))
            .await;
        peer.created().await;
        assert!(peer.request().await.get("result").is_some());
        let original = receive(&mut created).await;
        peer.release(&host.host_id, &id).await;
        assert!(Arc::ptr_eq(&original, &receive(&mut released).await));
        // Duplicate and unknown releases must not allocate tombstones.
        let sessions = client.host_sessions().unwrap();
        sessions.release(&host.host_id, &id);
        sessions.release(&host.host_id, &format!("unknown-{n}"));
        assert!(sessions.0.lock().handoffs.is_empty());
    }
    client.force_stop();
}

#[tokio::test]
async fn callback_returning_another_clients_session_is_released_not_destroyed() {
    let (other, mut other_peer) = fixture();
    let create = tokio::spawn(async move {
        other
            .create_session(SessionConfig::default().with_session_id("foreign"))
            .await
            .unwrap()
    });
    other_peer.created().await;
    let original = Arc::new(create.await.unwrap());
    let (client, mut peer) = fixture();
    let (released_tx, mut released) = mpsc::unbounded_channel();
    let session = original.clone();
    let host = start_factory(
        &client,
        &mut peer,
        local_options()
            .with_create_session(move |_: AhpSessionRequest, _: Client| {
                let session = session.clone();
                async move { Ok(session) }
            })
            .with_on_session_released(move |session| {
                released_tx.send(session).unwrap();
            }),
    )
    .await;
    peer.materialize(&host.host_id, "foreign", json!({"sessionId":"foreign"}))
        .await;
    assert!(peer.request().await.get("error").is_some());
    assert!(Arc::ptr_eq(&original, &receive(&mut released).await));
    let session = original.clone();
    let events = tokio::spawn(async move { session.get_events().await });
    let request = other_peer.request().await;
    assert_eq!(request["method"], "session.getMessages");
    other_peer.respond(&request, json!({"events":[]})).await;
    events.await.unwrap().unwrap();
    client.force_stop();
}

#[tokio::test]
async fn release_callback_panic_does_not_prevent_other_handoff_cleanup() {
    let (client, mut peer) = fixture();
    let calls = Arc::new(AtomicUsize::new(0));
    let counter = calls.clone();
    let (options, mut created, _) = factory_options();
    let host = start_factory(
        &client,
        &mut peer,
        options.with_on_session_released(move |_| {
            counter.fetch_add(1, Ordering::SeqCst);
            panic!("release panic");
        }),
    )
    .await;
    let mut originals = Vec::new();
    for id in ["one", "two"] {
        peer.materialize(&host.host_id, id, json!({"sessionId":id}))
            .await;
        peer.created().await;
        assert!(peer.request().await.get("result").is_some());
        originals.push(receive(&mut created).await);
    }
    peer.exited(&host.host_id).await;
    timeout(TIMEOUT, async {
        while calls.load(Ordering::SeqCst) != 2 {
            tokio::task::yield_now().await;
        }
    })
    .await
    .unwrap();
    client.force_stop();
    assert!(client.host_sessions().unwrap().0.lock().handoffs.is_empty());
}
