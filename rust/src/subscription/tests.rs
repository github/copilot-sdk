// Copyright (c) Microsoft Corporation. All rights reserved.

#![cfg(test)]

use std::sync::Barrier;
use std::time::Duration;

use futures_util::FutureExt;
use tokio::sync::broadcast;
use tokio::time::timeout;

use super::*;

#[track_caller]
fn recv_ready(events: &mut EventSubscription) -> Result<SessionEvent, RecvError> {
    events
        .recv()
        .now_or_never()
        .expect("expected receive to complete on its first poll")
}

#[track_caller]
fn next_ready(events: &mut EventSubscription) -> Option<Result<SessionEvent, Lagged>> {
    events
        .next()
        .now_or_never()
        .expect("expected stream item or closure on its first poll")
}

fn make_event(id: &str) -> SessionEvent {
    SessionEvent {
        id: id.into(),
        timestamp: "2025-01-01T00:00:00Z".into(),
        parent_id: None,
        ephemeral: None,
        agent_id: None,
        debug_cli_received_at_ms: None,
        debug_ws_forwarded_at_ms: None,
        event_type: "noop".into(),
        data: serde_json::json!({}),
    }
}

#[tokio::test]
async fn recv_yields_then_closes_on_drop_sender() {
    let (tx, rx) = broadcast::channel(8);
    let mut sub = EventSubscription::new(rx);
    tx.send(make_event("a")).unwrap();
    tx.send(make_event("b")).unwrap();
    drop(tx);

    assert_eq!(recv_ready(&mut sub).unwrap().id, "a");
    assert_eq!(recv_ready(&mut sub).unwrap().id, "b");
    assert!(matches!(
        recv_ready(&mut sub).unwrap_err().kind(),
        RecvErrorKind::Closed
    ));
}

#[tokio::test]
async fn recv_surfaces_lag() {
    let (tx, rx) = broadcast::channel(2);
    let mut sub = EventSubscription::new(rx);
    for id in ["a", "b", "c", "d"] {
        tx.send(make_event(id)).unwrap();
    }
    let err = recv_ready(&mut sub).expect_err("expected a Lagged error");
    let RecvErrorKind::Lagged(l) = err.kind() else {
        panic!("expected Lagged, got {:?}", err.kind());
    };
    assert_eq!(l.skipped(), 2);
    assert_eq!(recv_ready(&mut sub).unwrap().id, "c");
    assert_eq!(recv_ready(&mut sub).unwrap().id, "d");
}

#[tokio::test]
async fn stream_impl_matches_recv_semantics() {
    let (tx, rx) = broadcast::channel(8);
    let mut sub = EventSubscription::new(rx);
    tx.send(make_event("a")).unwrap();
    drop(tx);

    let next = next_ready(&mut sub);
    assert_eq!(next.unwrap().unwrap().id, "a");
    assert!(next_ready(&mut sub).is_none());
}

#[tokio::test]
async fn resume_bootstrap_is_lossless_before_and_during_catchup() {
    let (tx, _) = broadcast::channel(1);
    let bootstrap = ResumeBootstrap::new(&tx);
    for index in 0..600 {
        bootstrap.publish(&tx, make_event(&format!("event-{index}")));
    }
    let mut events = bootstrap.subscribe(&tx);
    for index in 0..300 {
        assert_eq!(
            recv_ready(&mut events).unwrap().id,
            format!("event-{index}")
        );
    }
    for index in 600..1200 {
        bootstrap.publish(&tx, make_event(&format!("event-{index}")));
    }
    for index in 300..1200 {
        assert_eq!(
            recv_ready(&mut events).unwrap().id,
            format!("event-{index}")
        );
    }
    assert!(events.recv().now_or_never().is_none());
    bootstrap.publish(&tx, make_event("live"));
    assert_eq!(recv_ready(&mut events).unwrap().id, "live");
    assert!(events.recv().now_or_never().is_none());
}

#[tokio::test]
async fn first_subscription_claims_before_polling_while_later_observers_stay_live() {
    let (tx, _) = broadcast::channel(8);
    let bootstrap = ResumeBootstrap::new(&tx);
    bootstrap.publish(&tx, make_event("bootstrap"));

    let mut first = bootstrap.subscribe(&tx);
    let mut second = bootstrap.subscribe(&tx);
    assert!(second.recv().now_or_never().is_none());
    bootstrap.publish(&tx, make_event("during-catchup"));
    assert_eq!(recv_ready(&mut second).unwrap().id, "during-catchup");
    assert_eq!(recv_ready(&mut first).unwrap().id, "bootstrap");
    assert_eq!(recv_ready(&mut first).unwrap().id, "during-catchup");
    assert!(first.recv().now_or_never().is_none());
    bootstrap.publish(&tx, make_event("live"));
    for sub in [&mut first, &mut second] {
        assert_eq!(recv_ready(sub).unwrap().id, "live");
        assert!(sub.recv().now_or_never().is_none());
    }
}

#[tokio::test]
async fn concurrent_subscribers_claim_bootstrap_exactly_once() {
    let (tx, _) = broadcast::channel(8);
    let bootstrap = ResumeBootstrap::new(&tx);
    bootstrap.publish(&tx, make_event("bootstrap"));
    let barrier = Barrier::new(2);
    let mut subscriptions = std::thread::scope(|scope| {
        let subscribe = || {
            barrier.wait();
            bootstrap.subscribe(&tx)
        };
        let first = scope.spawn(subscribe);
        let second = scope.spawn(subscribe);
        [first.join().unwrap(), second.join().unwrap()]
    });

    let mut owners = 0;
    for sub in &mut subscriptions {
        if let Some(event) = sub.recv().now_or_never() {
            assert_eq!(event.unwrap().id, "bootstrap");
            owners += 1;
        }
    }
    assert_eq!(owners, 1);
    for id in ["during-catchup", "live"] {
        bootstrap.publish(&tx, make_event(id));
        for sub in &mut subscriptions {
            assert_eq!(recv_ready(sub).unwrap().id, id);
            assert!(sub.recv().now_or_never().is_none());
        }
    }
}

#[tokio::test]
async fn claimed_bootstrap_drains_after_shutdown_without_retaining_sender() {
    let (tx, _) = broadcast::channel(1);
    let bootstrap = ResumeBootstrap::new(&tx);
    bootstrap.publish(&tx, make_event("first"));
    let mut events = bootstrap.subscribe(&tx);
    bootstrap.publish(&tx, make_event("second"));
    drop(bootstrap.cleanup_guard());
    assert_eq!(tx.strong_count(), 1);
    drop(tx);

    assert_eq!(next_ready(&mut events).unwrap().unwrap().id, "first");
    assert_eq!(next_ready(&mut events).unwrap().unwrap().id, "second");
    assert!(next_ready(&mut events).is_none());
    assert!(matches!(
        recv_ready(&mut events).unwrap_err().kind(),
        RecvErrorKind::Closed
    ));
}

#[tokio::test]
async fn bootstrap_handoff_is_cancel_safe_and_preserves_live_lag() {
    let (tx, _) = broadcast::channel(1);
    let bootstrap = ResumeBootstrap::new(&tx);
    bootstrap.publish(&tx, make_event("bootstrap"));
    let mut events = bootstrap.subscribe(&tx);
    // A buffered event must return Ready on the first poll, never be removed
    // and held across a suspension that cancellation could discard.
    assert_eq!(recv_ready(&mut events).unwrap().id, "bootstrap");
    // Cancel the pending receive after it installs bounded live delivery.
    assert!(events.recv().now_or_never().is_none());
    bootstrap.publish(&tx, make_event("overwritten"));
    bootstrap.publish(&tx, make_event("live"));
    let error = recv_ready(&mut events).unwrap_err();
    let RecvErrorKind::Lagged(lag) = error.kind() else {
        panic!("expected live lag, got {error:?}");
    };
    assert_eq!(lag.skipped(), 1);
    assert_eq!(recv_ready(&mut events).unwrap().id, "live");
    assert!(events.next().now_or_never().is_none());
    bootstrap.publish(&tx, make_event("stream-overwritten"));
    bootstrap.publish(&tx, make_event("stream-live"));
    assert_eq!(next_ready(&mut events).unwrap().unwrap_err().skipped(), 1);
    assert_eq!(next_ready(&mut events).unwrap().unwrap().id, "stream-live");
}

#[tokio::test]
async fn publication_racing_empty_queue_handoff_has_no_gap_or_duplicate() {
    for _ in 0..32 {
        let (tx, _) = broadcast::channel(1024);
        let bootstrap = ResumeBootstrap::new(&tx);
        bootstrap.publish(&tx, make_event("prefix"));
        let mut events = bootstrap.subscribe(&tx);
        assert_eq!(recv_ready(&mut events).unwrap().id, "prefix");

        let barrier = Arc::new(Barrier::new(2));
        let producer = std::thread::spawn({
            let barrier = barrier.clone();
            let bootstrap = bootstrap.clone();
            move || {
                barrier.wait();
                for index in 0..600 {
                    bootstrap.publish(&tx, make_event(&format!("event-{index}")));
                }
            }
        });
        barrier.wait();
        for index in 0..600 {
            let event = timeout(Duration::from_secs(5), events.recv())
                .await
                .unwrap()
                .unwrap();
            assert_eq!(event.id, format!("event-{index}"));
        }
        producer.join().unwrap();
        assert!(
            timeout(Duration::from_secs(5), events.next())
                .await
                .unwrap()
                .is_none()
        );
    }
}

#[tokio::test]
async fn shutdown_discards_unclaimed_bootstrap() {
    let (tx, _) = broadcast::channel(1);
    let bootstrap = ResumeBootstrap::new(&tx);
    bootstrap.publish(&tx, make_event("unclaimed"));
    bootstrap.release_unclaimed();
    assert!(matches!(
        *bootstrap.state.lock(),
        ResumeBootstrapState::Disabled
    ));
    let mut events = bootstrap.subscribe(&tx);
    assert!(events.recv().now_or_never().is_none());
    bootstrap.publish(&tx, make_event("live"));
    assert_eq!(recv_ready(&mut events).unwrap().id, "live");
}

#[tokio::test]
async fn dropping_unpolled_bootstrap_owner_discards_backlog_without_transfer() {
    let (tx, _) = broadcast::channel(8);
    let bootstrap = ResumeBootstrap::new(&tx);
    bootstrap.publish(&tx, make_event("discarded"));
    let first = bootstrap.subscribe(&tx);
    let mut second = bootstrap.subscribe(&tx);
    drop(first);
    assert!(matches!(
        *bootstrap.state.lock(),
        ResumeBootstrapState::Disabled
    ));
    let mut third = bootstrap.subscribe(&tx);
    for sub in [&mut second, &mut third] {
        assert!(sub.recv().now_or_never().is_none());
    }
    bootstrap.publish(&tx, make_event("live"));
    for sub in [&mut second, &mut third] {
        assert_eq!(recv_ready(sub).unwrap().id, "live");
    }
}

#[tokio::test]
async fn ordinary_subscription_does_not_replay_events_without_a_receiver() {
    let (tx, _) = broadcast::channel(8);
    assert!(tx.send(make_event("before-subscribe")).is_err());
    let mut sub = EventSubscription::new(tx.subscribe());
    tx.send(make_event("live")).unwrap();
    assert_eq!(recv_ready(&mut sub).unwrap().id, "live");
}

#[tokio::test]
async fn dropping_partially_drained_owner_discards_remaining_backlog() {
    let (tx, _) = broadcast::channel(1);
    let bootstrap = ResumeBootstrap::new(&tx);
    for id in ["consumed", "discarded"] {
        bootstrap.publish(&tx, make_event(id));
    }
    let mut owner = bootstrap.subscribe(&tx);
    assert_eq!(recv_ready(&mut owner).unwrap().id, "consumed");
    bootstrap.publish(&tx, make_event("also-discarded"));
    drop(owner);
    assert!(matches!(
        *bootstrap.state.lock(),
        ResumeBootstrapState::Disabled
    ));
    let mut observer = bootstrap.subscribe(&tx);
    assert!(observer.recv().now_or_never().is_none());
    bootstrap.publish(&tx, make_event("live"));
    assert_eq!(recv_ready(&mut observer).unwrap().id, "live");
    assert!(matches!(
        *bootstrap.state.lock(),
        ResumeBootstrapState::Disabled
    ));
}

#[tokio::test]
async fn cleanup_releases_backlog_when_task_is_aborted_before_first_poll() {
    let (tx, _) = broadcast::channel(1);
    let bootstrap = ResumeBootstrap::new(&tx);
    bootstrap.publish(&tx, make_event("unclaimed"));
    let cleanup = bootstrap.cleanup_guard();
    let task = tokio::spawn(async move {
        let _cleanup = cleanup;
        std::future::pending::<()>().await;
    });
    // This current-thread runtime cannot poll the spawned task before we yield.
    task.abort();
    let error = timeout(Duration::from_secs(5), task)
        .await
        .unwrap()
        .unwrap_err();
    assert!(error.is_cancelled(), "expected cancellation, got {error:?}");
    assert!(matches!(
        *bootstrap.state.lock(),
        ResumeBootstrapState::Disabled
    ));
    assert_eq!(Arc::strong_count(&bootstrap), 1);
}

#[tokio::test]
async fn cleanup_releases_backlog_when_task_panics() {
    let (tx, _) = broadcast::channel(1);
    let bootstrap = ResumeBootstrap::new(&tx);
    bootstrap.publish(&tx, make_event("unclaimed"));
    let cleanup = bootstrap.cleanup_guard();
    let task = tokio::spawn(async move {
        let _cleanup = cleanup;
        panic!("simulated event-loop unwind");
    });
    let error = timeout(Duration::from_secs(5), task)
        .await
        .unwrap()
        .unwrap_err();
    assert!(error.is_panic(), "expected panic, got {error:?}");
    assert!(matches!(
        *bootstrap.state.lock(),
        ResumeBootstrapState::Disabled
    ));
    assert_eq!(Arc::strong_count(&bootstrap), 1);
}
