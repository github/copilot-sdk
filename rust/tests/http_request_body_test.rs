//! The LLM-callback request body on its way through the dispatcher: the runtime sends an HTTP
//! request body in `llmInference.httpRequestChunk` frames, and the dispatcher moves the body out
//! of the parsed frame instead of copying it (a deep clone of the params `Value`, a clone of
//! `data`, and a copy of the first frame into an empty buffer). The handler must see the
//! runtime's bytes exactly: an escaped JSON body, an empty body, a body in several chunks, a
//! base64 chunk, a chunk that races ahead of its start frame, and a cancel chunk after part of
//! the body. The wire around the body is pinned too: every case asserts the exact ack of each
//! request it sends and the response frames in order, and one more case the exact error reply
//! to params that do not parse. And once the frame is parsed, a single-chunk body must not be
//! copied again: a counting allocator counts the allocations of exactly the body's length
//! between the chunk frame's write and the handler's entry.
//!
//! Each case drives the real dispatcher: `Client::start` over loopback TCP with a request
//! handler, the test playing the CLI. The allocator is process-global and `cargo test` runs a
//! binary's tests on parallel threads, so the counting case is `#[serial]` and every other case
//! `#[parallel]`: nothing else runs while it counts.

use std::alloc::{GlobalAlloc, Layout, System};
use std::collections::HashMap;
use std::sync::atomic::{AtomicUsize, Ordering};
use std::time::Duration;

use async_trait::async_trait;
use bytes::Bytes;
use futures_util::stream;
use github_copilot_sdk::{
    Client, ClientOptions, CopilotHttpRequest, CopilotHttpResponse, CopilotRequestContext,
    CopilotRequestError, CopilotRequestHandler, SDK_PROTOCOL_VERSION, Transport,
};
use http::HeaderMap;
use serde_json::{Value, json};
use serial_test::{parallel, serial};
use tokio::io::{AsyncBufReadExt, AsyncReadExt, AsyncWriteExt, BufReader};
use tokio::net::TcpListener;
use tokio::net::tcp::{OwnedReadHalf, OwnedWriteHalf};
use tokio::sync::mpsc;
use tokio::time::timeout;

/// A backstop against a hang, never a measurement.
const DEADLINE: Duration = Duration::from_secs(30);
/// The single-chunk body the allocation count watches. Odd, so no buffer that grows by doubling
/// (serde_json's unescape scratch) lands on it, and larger than every buffer between the socket
/// and the dispatcher.
const WATCHED_BODY_BYTES: usize = 2 * 1024 * 1024 + 1;

/// The system allocator, counting the allocations (and reallocations) of exactly [`WATCHED`]
/// bytes while it is non-zero.
struct ExactSizeCounting;

static WATCHED: AtomicUsize = AtomicUsize::new(0);
static HITS: AtomicUsize = AtomicUsize::new(0);

fn note(size: usize) {
    let watched = WATCHED.load(Ordering::SeqCst);
    if watched != 0 && size == watched {
        HITS.fetch_add(1, Ordering::SeqCst);
    }
}

// SAFETY: every method forwards to `System` with the caller's own arguments, so `System`'s
// contract is the caller's; the counters are plain atomics and never touch the memory.
unsafe impl GlobalAlloc for ExactSizeCounting {
    unsafe fn alloc(&self, layout: Layout) -> *mut u8 {
        note(layout.size());
        // SAFETY: forwarded unchanged (see the impl's SAFETY note).
        unsafe { System.alloc(layout) }
    }

    unsafe fn alloc_zeroed(&self, layout: Layout) -> *mut u8 {
        note(layout.size());
        // SAFETY: forwarded unchanged (see the impl's SAFETY note).
        unsafe { System.alloc_zeroed(layout) }
    }

    unsafe fn dealloc(&self, ptr: *mut u8, layout: Layout) {
        // SAFETY: forwarded unchanged (see the impl's SAFETY note).
        unsafe { System.dealloc(ptr, layout) }
    }

    unsafe fn realloc(&self, ptr: *mut u8, layout: Layout, new_size: usize) -> *mut u8 {
        note(new_size);
        // SAFETY: forwarded unchanged (see the impl's SAFETY note).
        unsafe { System.realloc(ptr, layout, new_size) }
    }
}

#[global_allocator]
static ALLOCATOR: ExactSizeCounting = ExactSizeCounting;

/// What the handler saw: the body, and the watched-size allocations counted when it was entered.
struct Captured {
    body: Vec<u8>,
    hits_at_entry: usize,
}

/// What the SDK wrote while one exchange ran: its replies to the test's requests, by id, and the
/// response requests it sent (method and params), in order.
struct Wire {
    replies: HashMap<u64, Value>,
    response_frames: Vec<(String, Value)>,
}

/// The wire of a well-formed exchange: the empty-result ack of every request the test sent, and
/// nothing else, then the response head (200, no headers) and its terminal empty chunk.
fn assert_wire(wire: &Wire, request_id: &str, request_ids: &[u64]) {
    for id in request_ids {
        assert_eq!(
            wire.replies[id],
            json!({"jsonrpc": "2.0", "id": id, "result": {}}),
            "the ack of request {id}"
        );
    }
    assert_eq!(
        wire.replies.len(),
        request_ids.len(),
        "replies: {:?}",
        wire.replies
    );
    assert_eq!(
        wire.response_frames,
        vec![
            (
                "llmInference.httpResponseStart".to_owned(),
                json!({"requestId": request_id, "status": 200, "headers": {}}),
            ),
            (
                "llmInference.httpResponseChunk".to_owned(),
                json!({"requestId": request_id, "data": "", "end": true}),
            ),
        ]
    );
}

/// Hands every request's body to the test and answers an empty 200.
struct CaptureHandler(mpsc::UnboundedSender<Captured>);

#[async_trait]
impl CopilotRequestHandler for CaptureHandler {
    async fn send_request(
        &self,
        request: CopilotHttpRequest,
        _ctx: &CopilotRequestContext,
    ) -> Result<CopilotHttpResponse, CopilotRequestError> {
        let hits_at_entry = HITS.load(Ordering::SeqCst);
        let _ = self.0.send(Captured {
            body: request.body,
            hits_at_entry,
        });
        Ok(CopilotHttpResponse::new(
            200,
            None,
            HeaderMap::new(),
            Box::pin(stream::empty::<Result<Bytes, CopilotRequestError>>()),
        ))
    }
}

/// One frame in the SDK's JSON-RPC shape: a `Content-Length` header, a blank line, the body.
fn frame_bytes(message: &Value) -> Vec<u8> {
    let body = serde_json::to_vec(message).expect("a JSON-RPC message serialises");
    let mut frame = format!("Content-Length: {}\r\n\r\n", body.len()).into_bytes();
    frame.extend_from_slice(&body);
    frame
}

/// The next frame the SDK wrote, decoded.
async fn read_frame(read: &mut BufReader<OwnedReadHalf>) -> Value {
    let mut length = None;
    loop {
        let mut line = String::new();
        let read_bytes = read.read_line(&mut line).await.expect("a header line");
        assert_ne!(read_bytes, 0, "the SDK closed the socket");
        if line == "\r\n" {
            break;
        }
        if let Some(value) = line.strip_prefix("Content-Length:") {
            length = Some(value.trim().parse::<usize>().expect("a decimal length"));
        }
    }
    let mut body = vec![0; length.expect("a Content-Length header")];
    read.read_exact(&mut body).await.expect("the frame body");
    serde_json::from_slice(&body).expect("a JSON frame body")
}

/// The CLI's side of one SDK client: the public `Client` over real loopback TCP, with the
/// request handler registered through `llmInference.setProvider`.
struct Peer {
    client: Client,
    read: BufReader<OwnedReadHalf>,
    write: OwnedWriteHalf,
    captured: mpsc::UnboundedReceiver<Captured>,
    _work: tempfile::TempDir,
}

impl Peer {
    async fn start() -> Self {
        let listener = TcpListener::bind("127.0.0.1:0")
            .await
            .expect("a loopback listener");
        let port = listener.local_addr().expect("a bound address").port();
        let work = tempfile::tempdir().expect("a temp dir");
        let (captured_tx, captured) = mpsc::unbounded_channel();
        let options = ClientOptions::new()
            .with_program(std::env::current_exe().expect("the test binary's path"))
            .with_cwd(work.path())
            .with_transport(Transport::External {
                host: "127.0.0.1".into(),
                port,
                connection_token: None,
            })
            .with_request_handler(CaptureHandler(captured_tx));
        let (client, (read, write)) = timeout(DEADLINE, async {
            tokio::join!(Client::start(options), async {
                let (socket, _) = listener.accept().await.expect("the SDK connects");
                socket.set_nodelay(true).expect("TCP_NODELAY");
                let (read, mut write) = socket.into_split();
                let mut read = BufReader::new(read);
                for method in ["connect", "llmInference.setProvider"] {
                    let message = read_frame(&mut read).await;
                    assert_eq!(message["method"], method);
                    let result = if method == "connect" {
                        json!({
                            "ok": true,
                            "version": "test",
                            "protocolVersion": SDK_PROTOCOL_VERSION
                        })
                    } else {
                        json!({"success": true})
                    };
                    let reply = json!({"jsonrpc": "2.0", "id": message["id"], "result": result});
                    write
                        .write_all(&frame_bytes(&reply))
                        .await
                        .expect("the handshake reply");
                }
                (read, write)
            })
        })
        .await
        .expect("the client handshake finishes");
        Self {
            client: client.expect("the client starts"),
            read,
            write,
            captured,
            _work: work,
        }
    }

    /// Bounded: an SDK that stops reading with the socket open would otherwise park a large frame's
    /// write on TCP backpressure, past every receive deadline.
    async fn send(&mut self, message: &Value) {
        timeout(DEADLINE, self.write.write_all(&frame_bytes(message)))
            .await
            .expect("the SDK takes the frame in time")
            .expect("a frame to the SDK");
    }

    async fn start_request(&mut self, request_id: &str, id: u64) {
        self.send(&json!({
            "jsonrpc": "2.0",
            "id": id,
            "method": "llmInference.httpRequestStart",
            "params": {
                "requestId": request_id,
                "method": "POST",
                "url": "http://unused.test/chat",
                "headers": {}
            }
        }))
        .await;
    }

    async fn chunk(&mut self, id: u64, params: Value) {
        self.send(&json!({
            "jsonrpc": "2.0",
            "id": id,
            "method": "llmInference.httpRequestChunk",
            "params": params
        }))
        .await;
    }

    /// What the handler received for the next request.
    async fn captured(&mut self) -> Captured {
        timeout(DEADLINE, self.captured.recv())
            .await
            .expect("the handler is entered")
            .expect("the handler's channel is open")
    }

    /// Serves the CLI's side of the response (acking the SDK's head and body requests) until its
    /// terminal body chunk has arrived and every one of `request_ids` has its reply, so the
    /// exchange finishes before the client stops; returns everything the SDK wrote meanwhile.
    async fn finish_response(&mut self, request_ids: &[u64]) -> Wire {
        let mut wire = Wire {
            replies: HashMap::new(),
            response_frames: Vec::new(),
        };
        timeout(DEADLINE, async {
            let mut terminal = false;
            while !terminal || request_ids.iter().any(|id| !wire.replies.contains_key(id)) {
                let message = read_frame(&mut self.read).await;
                let Some(method) = message.get("method").and_then(Value::as_str) else {
                    let id = message["id"]
                        .as_u64()
                        .expect("a reply carries its request's id");
                    wire.replies.insert(id, message);
                    continue;
                };
                terminal =
                    method == "llmInference.httpResponseChunk" && message["params"]["end"] == true;
                wire.response_frames
                    .push((method.to_owned(), message["params"].clone()));
                let ack = json!({
                    "jsonrpc": "2.0",
                    "id": message["id"],
                    "result": {"accepted": true}
                });
                self.send(&ack).await;
            }
        })
        .await
        .expect("the response finishes and every request is answered");
        wire
    }

    /// The SDK's replies to `request_ids`, for frames that start no exchange: any request from the
    /// SDK meanwhile fails the test.
    async fn replies(&mut self, request_ids: &[u64]) -> HashMap<u64, Value> {
        let mut replies = HashMap::new();
        timeout(DEADLINE, async {
            while request_ids.iter().any(|id| !replies.contains_key(id)) {
                let message = read_frame(&mut self.read).await;
                assert!(
                    message.get("method").is_none(),
                    "an unexpected request: {message}"
                );
                let id = message["id"]
                    .as_u64()
                    .expect("a reply carries its request's id");
                replies.insert(id, message);
            }
        })
        .await
        .expect("every request is answered");
        replies
    }

    async fn stop(self) {
        self.client.stop().await.expect("the client stops");
    }
}

/// A chat-request-shaped JSON body of exactly `len` bytes, dense with quotes and backslashes so
/// its frame carries it escaped, as the runtime's does.
fn escaped_body(len: usize) -> String {
    let unit = r#"{"role":"user","content":"a \"quoted\" path C:\\dir\\file, tab\there"},"#;
    let mut out = String::with_capacity(len);
    out.push_str(r#"{"messages":["#);
    while out.len() + unit.len() <= len {
        out.push_str(unit);
    }
    while out.len() < len {
        out.push('x');
    }
    out
}

/// Every JSON escape class (quote, backslash, the short escapes, the other control bytes) and
/// multi-byte UTF-8, after a body large enough to span many socket reads.
fn every_escape_class_body() -> String {
    let mut out = escaped_body(1024 * 1024);
    out.push_str(
        "controls: \u{0}\u{1}\u{8}\u{b}\u{c}\u{1f} cr\r lf\n unicode: \u{e9}\u{4e2d}\u{1f600}",
    );
    out
}

#[tokio::test(flavor = "multi_thread")]
#[parallel]
async fn an_escaped_json_body_reaches_the_handler_byte_for_byte() {
    let mut peer = Peer::start().await;
    let body = every_escape_class_body();

    peer.start_request("escaped", 10).await;
    peer.chunk(
        11,
        json!({"requestId": "escaped", "data": body, "end": true}),
    )
    .await;

    let captured = peer.captured().await;
    assert_eq!(captured.body, body.as_bytes());
    let wire = peer.finish_response(&[10, 11]).await;
    assert_wire(&wire, "escaped", &[10, 11]);
    peer.stop().await;
}

#[tokio::test(flavor = "multi_thread")]
#[parallel]
async fn an_empty_body_reaches_the_handler_empty() {
    let mut peer = Peer::start().await;

    peer.start_request("empty", 20).await;
    peer.chunk(21, json!({"requestId": "empty", "data": "", "end": true}))
        .await;

    let captured = peer.captured().await;
    assert_eq!(captured.body, Vec::<u8>::new());
    let wire = peer.finish_response(&[20, 21]).await;
    assert_wire(&wire, "empty", &[20, 21]);
    peer.stop().await;
}

#[tokio::test(flavor = "multi_thread")]
#[parallel]
async fn a_body_in_several_chunks_reaches_the_handler_in_order() {
    let mut peer = Peer::start().await;
    let parts = [
        escaped_body(64 * 1024),
        escaped_body(1000),
        escaped_body(70_001),
    ];

    peer.start_request("several", 30).await;
    peer.chunk(31, json!({"requestId": "several", "data": parts[0]}))
        .await;
    // An empty, non-terminal chunk carries no bytes and must not disturb the order.
    peer.chunk(32, json!({"requestId": "several", "data": ""}))
        .await;
    peer.chunk(33, json!({"requestId": "several", "data": parts[1]}))
        .await;
    peer.chunk(
        34,
        json!({"requestId": "several", "data": parts[2], "end": true}),
    )
    .await;

    let captured = peer.captured().await;
    assert_eq!(captured.body, parts.concat().as_bytes());
    let wire = peer.finish_response(&[30, 31, 32, 33, 34]).await;
    assert_wire(&wire, "several", &[30, 31, 32, 33, 34]);
    peer.stop().await;
}

#[tokio::test(flavor = "multi_thread")]
#[parallel]
async fn a_base64_chunk_reaches_the_handler_decoded_before_a_text_chunk() {
    let mut peer = Peer::start().await;

    peer.start_request("binary", 40).await;
    // "AAEC/w==" is the standard base64 of the bytes 0x00 0x01 0x02 0xff.
    peer.chunk(
        41,
        json!({"requestId": "binary", "data": "AAEC/w==", "binary": true}),
    )
    .await;
    peer.chunk(
        42,
        json!({"requestId": "binary", "data": "tail", "end": true}),
    )
    .await;

    let captured = peer.captured().await;
    assert_eq!(captured.body, b"\x00\x01\x02\xfftail".to_vec());
    let wire = peer.finish_response(&[40, 41, 42]).await;
    assert_wire(&wire, "binary", &[40, 41, 42]);
    peer.stop().await;
}

#[tokio::test(flavor = "multi_thread")]
#[parallel]
async fn a_chunk_ahead_of_its_start_frame_reaches_the_handler() {
    let mut peer = Peer::start().await;
    let body = escaped_body(300_001);

    peer.chunk(51, json!({"requestId": "early", "data": body, "end": true}))
        .await;
    peer.start_request("early", 50).await;

    let captured = peer.captured().await;
    assert_eq!(captured.body, body.as_bytes());
    let wire = peer.finish_response(&[50, 51]).await;
    assert_wire(&wire, "early", &[50, 51]);
    peer.stop().await;
}

/// A cancel chunk after part of the body, on the `apply_chunk` path that moves `data`: the handler
/// sees the bytes that arrived before it, each request is acked, and the response is its head and
/// then a terminal chunk carrying the cancellation.
#[tokio::test(flavor = "multi_thread")]
#[parallel]
async fn a_cancel_chunk_ends_the_body_and_the_response_with_the_cancellation() {
    let mut peer = Peer::start().await;
    let part = escaped_body(10_001);

    peer.start_request("cancelled", 80).await;
    peer.chunk(81, json!({"requestId": "cancelled", "data": part}))
        .await;
    peer.chunk(
        82,
        json!({"requestId": "cancelled", "data": "", "cancel": true}),
    )
    .await;

    let captured = peer.captured().await;
    assert_eq!(captured.body, part.as_bytes());
    let wire = peer.finish_response(&[80, 81, 82]).await;
    for id in [80, 81, 82] {
        assert_eq!(
            wire.replies[&id],
            json!({"jsonrpc": "2.0", "id": id, "result": {}}),
            "the ack of request {id}"
        );
    }
    assert_eq!(wire.replies.len(), 3, "replies: {:?}", wire.replies);
    assert_eq!(
        wire.response_frames,
        vec![
            (
                "llmInference.httpResponseStart".to_owned(),
                json!({"requestId": "cancelled", "status": 200, "headers": {}}),
            ),
            (
                "llmInference.httpResponseChunk".to_owned(),
                json!({
                    "requestId": "cancelled",
                    "data": "",
                    "end": true,
                    "error": {"code": "cancelled", "message": "Request cancelled by runtime"}
                }),
            ),
        ]
    );
    peer.stop().await;
}

/// A start or chunk frame whose params do not parse gets the dispatcher's error reply: the params
/// move into the parse instead of being cloned into it, and a failed parse still answers.
#[tokio::test(flavor = "multi_thread")]
#[parallel]
async fn unparsable_start_and_chunk_params_get_the_error_reply() {
    let mut peer = Peer::start().await;

    // Neither frame names the requestId that both params types require.
    peer.send(&json!({
        "jsonrpc": "2.0",
        "id": 70,
        "method": "llmInference.httpRequestStart",
        "params": {"method": "POST", "url": "http://unused.test/chat", "headers": {}}
    }))
    .await;
    peer.chunk(71, json!({"data": "x", "end": true})).await;

    let replies = peer.replies(&[70, 71]).await;
    assert_eq!(
        replies[&70],
        json!({
            "jsonrpc": "2.0",
            "id": 70,
            "error": {"code": -32603, "message": "invalid llmInference.httpRequestStart params"}
        })
    );
    assert_eq!(
        replies[&71],
        json!({
            "jsonrpc": "2.0",
            "id": 71,
            "error": {"code": -32603, "message": "invalid llmInference.httpRequestChunk params"}
        })
    );
    peer.stop().await;
}

/// The mechanism: between the chunk frame's write and the handler's entry, the body is
/// allocated at its exact length once, by the frame parse (`serde_json`'s `Value` string), and
/// the dispatcher moves that buffer to the handler. Copying it instead allocated it three more
/// times (the params `Value` clone, the `data` clone, the drain into an empty `Vec`); reverting
/// any one of the three moves adds one.
#[tokio::test(flavor = "multi_thread")]
#[serial]
async fn a_single_chunk_body_reaches_the_handler_without_a_copy() {
    let mut peer = Peer::start().await;
    let body = escaped_body(WATCHED_BODY_BYTES);
    assert_eq!(body.len(), WATCHED_BODY_BYTES);
    peer.start_request("single", 60).await;
    // Encoded before the count starts: building the frame allocates the body's length too.
    let chunk = frame_bytes(&json!({
        "jsonrpc": "2.0",
        "id": 61,
        "method": "llmInference.httpRequestChunk",
        "params": {"requestId": "single", "data": body, "end": true}
    }));

    HITS.store(0, Ordering::SeqCst);
    WATCHED.store(WATCHED_BODY_BYTES, Ordering::SeqCst);
    timeout(DEADLINE, peer.write.write_all(&chunk))
        .await
        .expect("the SDK takes the chunk frame in time")
        .expect("the chunk frame to the SDK");
    let captured = peer.captured().await;
    WATCHED.store(0, Ordering::SeqCst);

    assert_eq!(captured.body, body.as_bytes());
    assert_eq!(
        captured.hits_at_entry, 1,
        "allocations of exactly the body's {WATCHED_BODY_BYTES} bytes between the frame's write \
         and the handler's entry: the frame parse's one, and no relay copy"
    );
    let wire = peer.finish_response(&[60, 61]).await;
    assert_wire(&wire, "single", &[60, 61]);
    peer.stop().await;
}
