#![allow(clippy::unwrap_used)]

use std::alloc::System;
use std::hint::black_box;
use std::time::{Duration, Instant};

use github_copilot_sdk::{Client, SessionConfig};
use serde_json::{Value, json};
use stats_alloc::{INSTRUMENTED_SYSTEM, Region, StatsAlloc};
use tokio::io::{AsyncBufReadExt, AsyncReadExt, AsyncWriteExt, BufReader, DuplexStream, duplex};

#[global_allocator]
static ALLOCATOR: &StatsAlloc<System> = &INSTRUMENTED_SYSTEM;

const BATCH_SIZE: usize = 64;
const WARMUP_BATCHES: usize = 4;
const SAMPLES: usize = 15;

async fn write_frame(writer: &mut DuplexStream, body: &[u8]) {
    let header = format!("Content-Length: {}\r\n\r\n", body.len());
    writer.write_all(header.as_bytes()).await.unwrap();
    writer.write_all(body).await.unwrap();
}

async fn read_frame(reader: &mut BufReader<DuplexStream>) -> Value {
    let mut header = String::new();
    reader.read_line(&mut header).await.unwrap();
    let length: usize = header
        .trim()
        .strip_prefix("Content-Length: ")
        .unwrap()
        .parse()
        .unwrap();
    let mut separator = String::new();
    reader.read_line(&mut separator).await.unwrap();
    assert_eq!(separator, "\r\n");
    let mut body = vec![0; length];
    reader.read_exact(&mut body).await.unwrap();
    serde_json::from_slice(&body).unwrap()
}

async fn measure(payload_size: usize, subscribers: usize) {
    let workdir = tempfile::tempdir().unwrap();
    let (client_write, server_read) = duplex(8192);
    let (mut server_write, client_read) = duplex(8192);
    let client =
        Client::from_streams(client_read, client_write, workdir.path().to_path_buf()).unwrap();
    let mut server_read = BufReader::new(server_read);
    let create = client.create_session(SessionConfig::default());
    let respond = async {
        let request = read_frame(&mut server_read).await;
        assert_eq!(request["method"], "session.create");
        write_frame(
            &mut server_write,
            &serde_json::to_vec(&json!({
                "jsonrpc": "2.0",
                "id": request["id"],
                "result": {"sessionId": request["params"]["sessionId"]}
            }))
            .unwrap(),
        )
        .await;
    };
    let (session, ()) = tokio::join!(create, respond);
    let session = session.unwrap();
    let mut subscriptions: Vec<_> = (0..subscribers).map(|_| session.subscribe()).collect();
    let content = "x".repeat(payload_size);
    let frames: Vec<_> = (0..BATCH_SIZE)
        .map(|index| {
            serde_json::to_vec(&json!({
                "jsonrpc": "2.0",
                "method": "session.event",
                "params": {
                    "sessionId": session.id(),
                    "event": {
                        "id": index.to_string(),
                        "timestamp": "2026-01-01T00:00:00Z",
                        "parentId": "previous-event",
                        "agentId": "test-agent",
                        "ephemeral": true,
                        "type": "assistant.message_delta",
                        "data": {
                            "messageId": "message",
                            "deltaContent": content,
                            "extra": {"nested": [1, true, null, {"key": "value"}]}
                        }
                    }
                }
            }))
            .unwrap()
        })
        .collect();
    // A lifecycle barrier exercises the second internal notification consumer
    // and ensures its work is included even when there are no session observers.
    let mut lifecycle = client.subscribe_lifecycle();
    let barrier = serde_json::to_vec(&json!({
        "jsonrpc": "2.0",
        "method": "session.lifecycle",
        "params": {"type": "session.updated", "sessionId": session.id()}
    }))
    .unwrap();
    let session_barriers: Vec<_> = [false, true]
        .map(|elicitation| {
            serde_json::to_vec(&json!({
                "jsonrpc": "2.0",
                "method": "session.event",
                "params": {
                    "sessionId": session.id(),
                    "event": {
                        "id": "barrier",
                        "timestamp": "2026-01-01T00:00:00Z",
                        "type": "capabilities.changed",
                        "data": {"ui": {"elicitation": elicitation}}
                    }
                }
            }))
            .unwrap()
        })
        .into();
    let mut timings = Vec::with_capacity(SAMPLES);
    let mut allocations = Vec::with_capacity(SAMPLES);
    let mut allocated_bytes = Vec::with_capacity(SAMPLES);

    for sample in 0..WARMUP_BATCHES + SAMPLES {
        let region = Region::new(ALLOCATOR);
        let started = Instant::now();
        tokio::time::timeout(Duration::from_secs(30), async {
            let send = async {
                for frame in &frames {
                    write_frame(&mut server_write, frame).await;
                }
                write_frame(&mut server_write, &session_barriers[sample % 2]).await;
                write_frame(&mut server_write, &barrier).await;
            };
            let receive = async {
                for index in 0..BATCH_SIZE {
                    for subscription in &mut subscriptions {
                        let event = subscription.recv().await.unwrap();
                        assert_eq!(event.id.parse::<usize>().unwrap(), index);
                        assert_eq!(event.event_type, "assistant.message_delta");
                        assert_eq!(event.data["deltaContent"].as_str().unwrap(), content);
                        assert_eq!(event.data["extra"]["nested"][3]["key"], "value");
                        assert_eq!(event.agent_id.as_deref(), Some("test-agent"));
                        assert_eq!(event.parent_id.as_deref(), Some("previous-event"));
                        assert_eq!(event.ephemeral, Some(true));
                        black_box(event);
                    }
                }
                for subscription in &mut subscriptions {
                    assert_eq!(subscription.recv().await.unwrap().id, "barrier");
                }
                while session.capabilities().ui.and_then(|ui| ui.elicitation)
                    != Some(sample % 2 != 0)
                {
                    tokio::task::yield_now().await;
                }
                lifecycle.recv().await.unwrap();
            };
            tokio::join!(send, receive);
        })
        .await
        .expect("notification pipeline stalled");
        let elapsed = started.elapsed().as_nanos() as f64 / BATCH_SIZE as f64;
        let stats = region.change();
        if sample >= WARMUP_BATCHES {
            timings.push(elapsed);
            allocations.push(stats.allocations as f64 / BATCH_SIZE as f64);
            allocated_bytes.push(stats.bytes_allocated as f64 / BATCH_SIZE as f64);
        }
    }
    timings.sort_by(f64::total_cmp);
    allocations.sort_by(f64::total_cmp);
    allocated_bytes.sort_by(f64::total_cmp);
    println!(
        "{payload_size},{subscribers},{:.0},{:.0},{:.0},{:.1},{:.0}",
        timings[0],
        timings[SAMPLES / 2],
        timings[SAMPLES - 1],
        allocations[SAMPLES / 2],
        allocated_bytes[SAMPLES / 2],
    );
    session.stop_event_loop().await;
    drop(session);
    drop(client);
    drop(server_write);
    drop(server_read);
    tokio::task::yield_now().await;
}

fn main() {
    println!(
        "payload_bytes,subscribers,min_ns_per_event,median_ns_per_event,max_ns_per_event,allocations_per_event,allocated_bytes_per_event"
    );
    for payload_size in [128, 4096, 262_144] {
        for subscribers in [0, 1, 2] {
            let runtime = tokio::runtime::Builder::new_current_thread()
                .enable_all()
                .build()
                .unwrap();
            runtime.block_on(measure(payload_size, subscribers));
        }
    }
}
