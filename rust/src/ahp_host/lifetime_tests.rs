use super::*;

#[test]
fn runtime_shutdown_cannot_lose_retained_session_release() {
    for drop_order in ["after-shutdown", "queued-cleanup", "pending-cleanup"] {
        let runtime = tokio::runtime::Builder::new_current_thread()
            .enable_all()
            .build()
            .unwrap();
        let (client, peer, original, mut released) = runtime.block_on(async {
            let (client, mut peer) = fixture();
            let (options, mut created, released) = factory_options();
            let host = start_factory(&client, &mut peer, options).await;
            peer.materialize(&host.host_id, "owned", json!({"sessionId":"owned"}))
                .await;
            peer.created().await;
            assert!(peer.request().await.get("result").is_some());
            let original = receive(&mut created).await;
            drop(host);
            let client = if drop_order == "after-shutdown" {
                Some(client)
            } else {
                drop(client);
                if drop_order == "pending-cleanup" {
                    assert_eq!(peer.request().await["method"], "host.dispose");
                }
                None
            };
            (client, peer, original, released)
        });
        assert!(released.try_recv().is_err());
        drop(runtime);
        drop(client);
        assert!(Arc::ptr_eq(&original, &released.try_recv().unwrap()));
        assert!(matches!(
            released.try_recv(),
            Err(mpsc::error::TryRecvError::Disconnected)
        ));
        drop(peer);
    }
}

#[tokio::test]
async fn dropping_all_external_owners_reclaims_successful_handoff() {
    let (client, mut peer) = fixture();
    let owner = Arc::downgrade(&client.inner);
    let retention = client.inner.ahp_host_sessions.clone();
    let releases = Arc::new(AtomicUsize::new(0));
    let count = releases.clone();
    let (options, mut created, _) = factory_options();
    let host = start_factory(
        &client,
        &mut peer,
        options.with_on_session_released(move |_| {
            count.fetch_add(1, Ordering::SeqCst);
        }),
    )
    .await;
    peer.materialize(&host.host_id, "owned", json!({"sessionId":"owned"}))
        .await;
    peer.created().await;
    assert!(peer.request().await.get("result").is_some());
    let original = receive(&mut created).await;
    let session = Arc::downgrade(&original);
    drop(original);
    drop(created);
    drop(host);
    drop(client);
    assert!(
        retention.upgrade().is_none(),
        "retention root must not be owned by its sessions"
    );
    let cleanup = peer.request().await;
    assert_eq!(cleanup["method"], "host.dispose");
    peer.respond(&cleanup, json!({})).await;
    timeout(TIMEOUT, async {
        while owner.upgrade().is_some() || session.upgrade().is_some() {
            tokio::task::yield_now().await;
        }
    })
    .await
    .unwrap();
    assert_eq!(releases.load(Ordering::SeqCst), 1);
    assert!(
        timeout(TIMEOUT, peer.read.read_u8())
            .await
            .unwrap()
            .is_err()
    );
}

#[tokio::test]
async fn app_retained_session_outlives_owner_dropped_on_plain_thread() {
    let (client, mut peer) = fixture();
    let owner = Arc::downgrade(&client.inner);
    let (options, mut created, mut released) = factory_options();
    let host = start_factory(&client, &mut peer, options).await;
    peer.materialize(&host.host_id, "app-owned", json!({"sessionId":"app-owned"}))
        .await;
    peer.created().await;
    assert!(peer.request().await.get("result").is_some());
    let original = receive(&mut created).await;
    drop(created);
    drop(host);
    std::thread::spawn(move || drop(client)).join().unwrap();
    assert!(
        released.try_recv().is_err(),
        "release must wait for host disposal"
    );
    let cleanup = peer.request().await;
    assert_eq!(cleanup["method"], "host.dispose");
    peer.respond(&cleanup, json!({})).await;
    assert!(Arc::ptr_eq(&original, &receive(&mut released).await));
    assert!(
        owner.upgrade().is_some(),
        "app session must keep its connection alive"
    );
    let session = original.clone();
    let events = tokio::spawn(async move { session.get_events().await });
    let request = peer.request().await;
    assert_eq!(request["method"], "session.getMessages");
    peer.respond(&request, json!({"events":[]})).await;
    events.await.unwrap().unwrap();
    assert!(timeout(TIMEOUT, released.recv()).await.unwrap().is_none());
    drop(original);
    timeout(TIMEOUT, async {
        while owner.upgrade().is_some() {
            tokio::task::yield_now().await;
        }
    })
    .await
    .unwrap();
}

#[tokio::test(flavor = "multi_thread", worker_threads = 2)]
async fn blocked_release_callback_cannot_lose_another_release_behind_notification_flood() {
    let (client, mut peer) = fixture();
    let (created_tx, mut created) = mpsc::unbounded_channel();
    let (released_tx, mut released) = mpsc::unbounded_channel();
    let (blocked_tx, mut blocked) = mpsc::unbounded_channel();
    let (unblock, gate) = std::sync::mpsc::channel::<()>();
    let gate = std::sync::Mutex::new(gate);
    let options = local_options()
        .with_create_session(move |request: AhpSessionRequest, client: Client| {
            let created_tx = created_tx.clone();
            async move {
                let session = Arc::new(client.create_session(request.config).await?);
                created_tx
                    .send((session.clone(), request.cancellation_token))
                    .unwrap();
                Ok(session)
            }
        })
        .with_on_session_released(move |session| {
            if session.id().as_str() == "blocked" {
                blocked_tx.send(()).unwrap();
                gate.lock().unwrap().recv_timeout(TIMEOUT * 2).unwrap();
            }
            released_tx.send(session).unwrap();
        });
    let host = start_factory(&client, &mut peer, options).await;
    for id in ["blocked", "following"] {
        peer.materialize(&host.host_id, id, json!({"sessionId":id}))
            .await;
        peer.created().await;
        assert!(peer.request().await.get("result").is_some());
    }
    let (first, first_cancelled) = receive(&mut created).await;
    let (second, second_cancelled) = receive(&mut created).await;
    peer.release(&host.host_id, "blocked").await;
    receive(&mut blocked).await;
    assert!(first_cancelled.is_cancelled());
    for n in 0..2048 {
        peer.send(json!({"jsonrpc":"2.0", "method":"unrelated", "params":{"n":n}}))
            .await;
    }
    peer.release(&host.host_id, "following").await;
    timeout(TIMEOUT, second_cancelled.cancelled())
        .await
        .unwrap();
    assert!(Arc::ptr_eq(&second, &receive(&mut released).await));
    unblock.send(()).unwrap();
    assert!(Arc::ptr_eq(&first, &receive(&mut released).await));
    peer.release(&host.host_id, "blocked").await;
    peer.release(&host.host_id, "following").await;
    client.force_stop();
    assert!(timeout(TIMEOUT, released.recv()).await.unwrap().is_none());
}
