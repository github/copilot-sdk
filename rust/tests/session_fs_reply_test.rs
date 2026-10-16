//! A sessionFs reply on its way out: the SDK encodes the provider's result once,
//! into a single frame buffer, and drops the result before the frame is
//! written. The wire stays as it was: every case asserts the exact frame bytes,
//! a `Content-Length` that states the body's length and the body the published
//! path wrote, `{"jsonrpc":"2.0","id":N,"result":R}` with `R` the result as a
//! `serde_json::Value` writes it (keys sorted). And a large `readFile` reply is
//! allocated once, then at most shrunk: a counting allocator records every
//! allocation and reallocation at least as large as the reply's content, from
//! the request's write until the reply's last byte is read.
//!
//! The allocator is process-global and `cargo test` runs a binary's tests on
//! parallel threads, so the counting case is `#[serial]` and every other case
//! `#[parallel]`: nothing else runs while it counts.

use std::alloc::{GlobalAlloc, Layout, System};
use std::collections::HashMap;
use std::sync::atomic::{AtomicUsize, Ordering};
use std::sync::{Arc, Mutex};
use std::time::Duration;

use async_trait::async_trait;
use github_copilot_sdk::Client;
use github_copilot_sdk::session::Session;
use github_copilot_sdk::session_fs::{
    DirEntry, DirEntryKind, FileInfo, FsError, FsErrorKind, SessionFsProvider,
    SessionFsSqliteProvider, SessionFsSqliteQueryResult, SessionFsSqliteQueryType,
};
use github_copilot_sdk::types::SessionConfig;
use serde_json::{Value, json};
use serial_test::{parallel, serial};
use tokio::io::{AsyncReadExt, AsyncWriteExt, DuplexStream, duplex};
use tokio::time::timeout;

/// A backstop against a hang, never a measurement.
const DEADLINE: Duration = Duration::from_secs(30);
/// The content of the counting case's `readFile` reply. Odd, so no buffer that
/// grows by doubling lands on it.
const CONTENT_BYTES: usize = 8 * 1024 * 1024 + 1;
/// The room the SDK reserves in front of a frame's body for its header
/// (`jsonrpc::FRAME_PREFIX`, private to the crate).
const HEADER_ROOM: usize = 40;

/// The system allocator, recording the size of every allocation (and
/// reallocation) of at least [`WATCH_FROM`] bytes while it is non-zero.
struct LargeAllocationRecorder;

// What each recorded event was: a new allocation, or a reallocation that grew
// or shrank a buffer.
const ALLOC: usize = 0;
const GROW: usize = 1;
const SHRINK: usize = 2;

static WATCH_FROM: AtomicUsize = AtomicUsize::new(0);
static LARGE_COUNT: AtomicUsize = AtomicUsize::new(0);
static LARGE: [AtomicUsize; 16] = [const { AtomicUsize::new(0) }; 16];
static LARGE_KIND: [AtomicUsize; 16] = [const { AtomicUsize::new(0) }; 16];

fn note(size: usize, kind: usize) {
    let from = WATCH_FROM.load(Ordering::SeqCst);
    if from != 0 && size >= from {
        let slot = LARGE_COUNT.fetch_add(1, Ordering::SeqCst);
        if let (Some(entry), Some(entry_kind)) = (LARGE.get(slot), LARGE_KIND.get(slot)) {
            entry.store(size, Ordering::SeqCst);
            entry_kind.store(kind, Ordering::SeqCst);
        }
    }
}

fn kind_name(kind: usize) -> &'static str {
    match kind {
        ALLOC => "alloc",
        GROW => "grow",
        _ => "shrink",
    }
}

// SAFETY: every method forwards to `System` with the caller's own arguments, so
// `System`'s contract is the caller's; the counters are plain atomics and never
// touch the memory.
unsafe impl GlobalAlloc for LargeAllocationRecorder {
    unsafe fn alloc(&self, layout: Layout) -> *mut u8 {
        note(layout.size(), ALLOC);
        // SAFETY: forwarded unchanged (see the impl's SAFETY note).
        unsafe { System.alloc(layout) }
    }

    unsafe fn alloc_zeroed(&self, layout: Layout) -> *mut u8 {
        note(layout.size(), ALLOC);
        // SAFETY: forwarded unchanged (see the impl's SAFETY note).
        unsafe { System.alloc_zeroed(layout) }
    }

    unsafe fn dealloc(&self, ptr: *mut u8, layout: Layout) {
        // SAFETY: forwarded unchanged (see the impl's SAFETY note).
        unsafe { System.dealloc(ptr, layout) }
    }

    unsafe fn realloc(&self, ptr: *mut u8, layout: Layout, new_size: usize) -> *mut u8 {
        let shrinks = new_size < layout.size();
        note(new_size, if shrinks { SHRINK } else { GROW });
        // SAFETY: forwarded unchanged (see the impl's SAFETY note).
        unsafe { System.realloc(ptr, layout, new_size) }
    }
}

#[global_allocator]
static ALLOCATOR: LargeAllocationRecorder = LargeAllocationRecorder;

/// A provider whose files are handed out once each, so a read allocates
/// nothing; every other answer is fixed.
struct FixedProvider {
    files: Mutex<HashMap<String, String>>,
}

impl FixedProvider {
    fn with_file(path: &str, content: String) -> Arc<Self> {
        Arc::new(Self {
            files: Mutex::new(HashMap::from([(path.to_string(), content)])),
        })
    }
}

#[async_trait]
impl SessionFsProvider for FixedProvider {
    async fn read_file(&self, path: &str) -> Result<String, FsError> {
        self.files
            .lock()
            .expect("the files lock")
            .remove(path)
            .ok_or_else(|| FsError::from(FsErrorKind::NotFound(path.to_string())))
    }

    async fn write_file(
        &self,
        _path: &str,
        _content: &str,
        _mode: Option<i64>,
    ) -> Result<(), FsError> {
        Ok(())
    }

    async fn stat(&self, _path: &str) -> Result<FileInfo, FsError> {
        Ok(FileInfo::new(
            true,
            false,
            42,
            "2025-01-01T00:00:00Z",
            "2025-01-02T00:00:00Z",
        ))
    }

    async fn readdir_with_types(&self, _path: &str) -> Result<Vec<DirEntry>, FsError> {
        Ok(vec![
            DirEntry::new("README.md", DirEntryKind::File),
            DirEntry::new("src", DirEntryKind::Directory),
        ])
    }

    fn sqlite(&self) -> Option<&dyn SessionFsSqliteProvider> {
        Some(self)
    }
}

#[async_trait]
impl SessionFsSqliteProvider for FixedProvider {
    async fn sqlite_query(
        &self,
        _query_type: SessionFsSqliteQueryType,
        _query: &str,
        _params: Option<&HashMap<String, Value>>,
    ) -> Result<Option<SessionFsSqliteQueryResult>, FsError> {
        // Keys inserted out of order: the reply must still write them sorted.
        let row = HashMap::from([
            ("zeta".to_string(), json!(1)),
            ("alpha".to_string(), json!("a")),
            ("mid".to_string(), json!(null)),
            ("beta".to_string(), json!([true, 2.5])),
        ]);
        Ok(Some(SessionFsSqliteQueryResult {
            columns: vec![
                "zeta".to_string(),
                "alpha".to_string(),
                "mid".to_string(),
                "beta".to_string(),
            ],
            rows: vec![row],
            rows_affected: 0,
            last_insert_rowid: None,
        }))
    }

    async fn sqlite_exists(&self) -> Result<bool, FsError> {
        Ok(true)
    }
}

/// The CLI's side of one session: the frames the SDK writes, and a writer for
/// the test's requests.
struct Server {
    read: DuplexStream,
    write: DuplexStream,
    session_id: String,
}

impl Server {
    async fn write_frame(&mut self, message: &Value) {
        let body = serde_json::to_vec(message).expect("a JSON message");
        let header = format!("Content-Length: {}\r\n\r\n", body.len());
        self.write
            .write_all(header.as_bytes())
            .await
            .expect("the header to the SDK");
        self.write
            .write_all(&body)
            .await
            .expect("the body to the SDK");
        self.write.flush().await.expect("a flush");
    }

    /// Reads one frame's header and returns the body length it states.
    async fn read_header(&mut self) -> usize {
        let mut header = String::new();
        while !header.ends_with("\r\n\r\n") {
            let byte = self.read.read_u8().await.expect("a header byte");
            header.push(char::from(byte));
        }
        header
            .trim_end()
            .strip_prefix("Content-Length: ")
            .expect("a Content-Length header")
            .parse()
            .expect("a decimal length")
    }

    async fn read_frame(&mut self) -> Vec<u8> {
        let length = self.read_header().await;
        let mut body = vec![0; length];
        self.read
            .read_exact(&mut body)
            .await
            .expect("the frame body");
        body
    }

    async fn request(&mut self, id: u64, method: &str, mut params: Value) {
        params["sessionId"] = json!(self.session_id);
        self.write_frame(&json!({"jsonrpc": "2.0", "id": id, "method": method, "params": params}))
            .await;
    }
}

/// A session with `provider` installed, the test answering `session.create`.
async fn start(provider: Arc<dyn SessionFsProvider>) -> (Session, Server) {
    let (client_write, server_read) = duplex(64 * 1024);
    let (server_write, client_read) = duplex(64 * 1024);
    let client = Client::from_streams(client_read, client_write, std::env::temp_dir())
        .expect("a client over the streams");
    let mut server = Server {
        read: server_read,
        write: server_write,
        session_id: String::new(),
    };
    let create = tokio::spawn(async move {
        client
            .create_session(SessionConfig::default().with_session_fs_provider(provider))
            .await
            .expect("the session starts")
    });
    let request: Value = serde_json::from_slice(&server.read_frame().await).expect("a request");
    assert_eq!(request["method"], "session.create");
    server.session_id = request["params"]["sessionId"]
        .as_str()
        .expect("session.create carries the session id")
        .to_string();
    let reply = json!({
        "jsonrpc": "2.0",
        "id": request["id"],
        "result": {"sessionId": server.session_id, "workspacePath": "/tmp/workspace"}
    });
    server.write_frame(&reply).await;
    let session = timeout(DEADLINE, create)
        .await
        .expect("session.create is answered in time")
        .expect("the create task");
    (session, server)
}

/// The body the published path wrote for a reply to `id` whose result is
/// `result`: the response's fields in order, the result as a `Value` writes it.
fn published_body(id: u64, result: &Value) -> String {
    let result = serde_json::to_string(result).expect("a JSON result");
    format!(r#"{{"jsonrpc":"2.0","id":{id},"result":{result}}}"#)
}

/// Sends one request and returns the reply's result, after asserting that the
/// reply's bytes are exactly the published path's.
async fn reply_to(server: &mut Server, id: u64, method: &str, params: Value) -> Value {
    server.request(id, method, params).await;
    let body = timeout(DEADLINE, server.read_frame())
        .await
        .expect("the reply arrives in time");
    let reply: Value = serde_json::from_slice(&body).expect("a JSON reply");
    let result = reply["result"].clone();
    assert_eq!(
        String::from_utf8(body).expect("a UTF-8 body"),
        published_body(id, &result),
        "the reply to {method}"
    );
    result
}

/// The reply to a `sessionFs.readFile` of `path`, checked as [`reply_to`] does.
async fn read_file(server: &mut Server, id: u64, path: &str) -> Value {
    reply_to(server, id, "sessionFs.readFile", json!({ "path": path })).await
}

#[tokio::test(flavor = "multi_thread")]
#[parallel]
async fn a_read_file_reply_is_written_as_published_for_every_escape_class() {
    let content = "quote \" backslash \\ newline \n return \r tab \t backspace \u{8} \
                   formfeed \u{c} controls \u{1}\u{1f} del \u{7f} utf-8 \u{e9}\u{4e2d}\u{1f600}"
        .to_string();
    let provider = FixedProvider::with_file("/file.txt", content.clone());
    let (_session, mut server) = start(provider).await;
    let result = read_file(&mut server, 7, "/file.txt").await;
    assert_eq!(result, json!({"content": content}));
}

#[tokio::test(flavor = "multi_thread")]
#[parallel]
async fn a_missing_file_reply_is_written_as_published() {
    let (_session, mut server) = start(FixedProvider::with_file("/file.txt", String::new())).await;
    let result = read_file(&mut server, 8, "/missing.txt").await;
    assert_eq!(result["error"]["code"], "ENOENT");
}

#[tokio::test(flavor = "multi_thread")]
#[parallel]
async fn write_file_stat_and_readdir_replies_are_written_as_published() {
    let (_session, mut server) = start(FixedProvider::with_file("/file.txt", String::new())).await;
    let written = reply_to(
        &mut server,
        9,
        "sessionFs.writeFile",
        json!({"path": "/out.txt", "content": "x"}),
    )
    .await;
    assert_eq!(written, Value::Null);
    let path = json!({"path": "/file.txt"});
    let stat = reply_to(&mut server, 10, "sessionFs.stat", path).await;
    assert_eq!(stat["size"], 42);
    let listing = reply_to(
        &mut server,
        11,
        "sessionFs.readdirWithTypes",
        json!({"path": "/"}),
    )
    .await;
    assert_eq!(listing["entries"][1]["name"], "src");
}

/// A sqlite row is a `HashMap`: its keys must still go out sorted, as the
/// published path's `Value` wrote them.
#[tokio::test(flavor = "multi_thread")]
#[parallel]
async fn a_sqlite_reply_writes_its_row_keys_sorted() {
    let (_session, mut server) = start(FixedProvider::with_file("/file.txt", String::new())).await;
    let result = reply_to(
        &mut server,
        12,
        "sessionFs.sqliteQuery",
        json!({"queryType": "query", "query": "select 1"}),
    )
    .await;
    assert_eq!(
        result["rows"][0],
        json!({"alpha": "a", "beta": [true, 2.5], "mid": null, "zeta": 1})
    );
}

#[tokio::test(flavor = "multi_thread")]
#[parallel]
async fn a_six_mebibyte_reply_is_one_frame_written_as_published() {
    let content = "0123456789abcdef".repeat(6 * 1024 * 1024 / 16);
    let (_session, mut server) = start(FixedProvider::with_file("/big.txt", content.clone())).await;
    let result = read_file(&mut server, 13, "/big.txt").await;
    let length = result["content"].as_str().map(str::len);
    assert_eq!(length, Some(content.len()));
}

/// The mechanism: between the request's write and the reply's last byte, the
/// only allocation at least as large as the content is the reply's frame
/// buffer. It holds the frame behind the room reserved for the header and grows
/// by doubling, so it is at least the frame's length and less than twice the
/// frame plus that room. The published path allocated the content again (a
/// `Value` copy), a body grown by doubling, and a framed copy of the body.
#[tokio::test(flavor = "multi_thread")]
#[serial]
async fn a_read_file_reply_is_allocated_once_in_its_frame_buffer() {
    // A quote every 64 bytes, so escaping changes the length.
    let content: String = (0..CONTENT_BYTES)
        .map(|i| if i % 64 == 63 { '"' } else { 'a' })
        .collect();
    let (_session, mut server) = start(FixedProvider::with_file("/events.jsonl", content)).await;
    // Room for the escaped reply, allocated before the count starts.
    let mut body = vec![0u8; 2 * CONTENT_BYTES + 4096];

    LARGE_COUNT.store(0, Ordering::SeqCst);
    WATCH_FROM.store(CONTENT_BYTES, Ordering::SeqCst);
    server
        .request(20, "sessionFs.readFile", json!({"path": "/events.jsonl"}))
        .await;
    let length = timeout(DEADLINE, server.read_header())
        .await
        .expect("the reply arrives in time");
    timeout(DEADLINE, server.read.read_exact(&mut body[..length]))
        .await
        .expect("the reply's body arrives in time")
        .expect("the reply's body");
    WATCH_FROM.store(0, Ordering::SeqCst);

    let count = LARGE_COUNT.load(Ordering::SeqCst);
    let events: Vec<(usize, usize)> = LARGE
        .iter()
        .zip(&LARGE_KIND)
        .take(count)
        .map(|(size, kind)| (size.load(Ordering::SeqCst), kind.load(Ordering::SeqCst)))
        .collect();
    let frame = format!("Content-Length: {length}\r\n\r\n").len() + length;
    let most = 2 * (frame + HEADER_ROOM);
    // The frame's buffer, and at most one shrink of that same buffer:
    // `encode_frame` gives back large spare capacity before the frame is queued.
    let once = match events.as_slice() {
        [(size, _)] => (frame..most).contains(size),
        [(size, _), (shrunk, SHRINK)] => {
            (frame..most).contains(size) && (frame..*size).contains(shrunk)
        }
        _ => false,
    };
    let named: Vec<(usize, &str)> = events
        .iter()
        .map(|&(size, kind)| (size, kind_name(kind)))
        .collect();
    assert!(
        once,
        "allocations of at least the content's {CONTENT_BYTES} bytes while the reply was built \
         and written: {named:?}; want only the frame's buffer, of {frame} to {most} bytes, and at \
         most one shrink of it to no less than the frame"
    );
    let reply: Value = serde_json::from_slice(&body[..length]).expect("a JSON reply");
    assert_eq!(reply["id"], 20);
    assert_eq!(
        reply["result"]["content"].as_str().map(str::len),
        Some(CONTENT_BYTES)
    );
}
