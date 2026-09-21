use std::panic::AssertUnwindSafe;

use bytes::{Buf, Bytes};
use futures_util::{FutureExt, StreamExt};

use super::{CopilotHttpResponseBody, CopilotRequestError};

const CHUNK_SIZE: usize = 32 * 1024;

/// One write of read-ahead, not a queue of `Bytes` slices: even a tiny slice can
/// retain an arbitrarily large allocation. Only the current source frame may
/// retain such an allocation, and it is released as soon as its bytes are copied.
pub(super) struct HttpResponseReader {
    body: Option<CopilotHttpResponseBody>,
    pending: Bytes,
    buffered: Vec<u8>,
    error: Option<CopilotRequestError>,
}

impl HttpResponseReader {
    pub(super) fn new(body: CopilotHttpResponseBody) -> Self {
        Self {
            body: Some(body),
            pending: Bytes::new(),
            buffered: Vec::new(),
            error: None,
        }
    }

    pub(super) fn can_read(&self) -> bool {
        self.buffered.len() < CHUNK_SIZE && (!self.pending.is_empty() || self.body.is_some())
    }

    /// Cancel-safe: no suspension occurs between acquiring bytes and saving them.
    pub(super) async fn read_more(&mut self) {
        // Custom streams can yield arbitrarily many ready (even empty) frames.
        tokio::task::consume_budget().await;
        if self.pending.is_empty() {
            let Some(body) = &mut self.body else {
                return;
            };
            match AssertUnwindSafe(body.next()).catch_unwind().await {
                Ok(Some(Ok(bytes))) => self.pending = bytes,
                Ok(Some(Err(error))) => {
                    self.error = Some(error);
                    self.body = None;
                }
                Ok(None) => self.body = None,
                Err(_) => {
                    self.error = Some(CopilotRequestError::message(
                        "HTTP response body stream panicked",
                    ));
                    self.body = None;
                }
            }
        }
        if self.pending.is_empty() {
            // An empty Bytes can still retain its source allocation.
            self.pending = Bytes::new();
            return;
        }
        if self.buffered.capacity() == 0 {
            self.buffered.reserve_exact(CHUNK_SIZE);
        }
        let count = self.pending.len().min(CHUNK_SIZE - self.buffered.len());
        self.buffered.extend_from_slice(&self.pending[..count]);
        self.pending.advance(count);
        if self.pending.is_empty() {
            self.pending = Bytes::new();
        }
    }

    pub(super) async fn next_chunk(
        &mut self,
        output: &mut Vec<u8>,
    ) -> Result<bool, CopilotRequestError> {
        while self.buffered.is_empty() && self.can_read() {
            self.read_more().await;
        }
        // Flush partial bytes before an upstream failure or EOF, without waiting
        // to fill the buffer. Reuse the two bounded allocations on later writes.
        if !self.buffered.is_empty() {
            output.clear();
            std::mem::swap(output, &mut self.buffered);
            return Ok(true);
        }
        if let Some(error) = self.error.take() {
            return Err(error);
        }
        Ok(false)
    }
}

#[cfg(test)]
mod tests {
    use std::sync::Arc;
    use std::sync::atomic::{AtomicUsize, Ordering};
    use std::time::Duration;

    use futures_util::stream;

    use super::*;

    #[tokio::test]
    async fn preserves_empty_large_fragmented_and_partial_bodies() {
        for size in [0, 1, CHUNK_SIZE - 1, CHUNK_SIZE, CHUNK_SIZE * 3 + 7] {
            let expected: Vec<u8> = (0..size).map(|i| (i % 256) as u8).collect();
            for fragment_size in [1, 1024, CHUNK_SIZE, CHUNK_SIZE * 4] {
                let fragments: Vec<_> = expected
                    .chunks(fragment_size)
                    .map(Bytes::copy_from_slice)
                    .collect();
                let body = stream::iter(
                    [Bytes::new()]
                        .into_iter()
                        .chain(fragments)
                        .chain([Bytes::new()])
                        .map(Ok),
                );
                let mut reader = HttpResponseReader::new(Box::pin(body));
                let mut output = Vec::new();
                let mut actual = Vec::new();
                while reader.next_chunk(&mut output).await.unwrap() {
                    assert!(output.len() <= CHUNK_SIZE);
                    actual.extend_from_slice(&output);
                    while reader.can_read() {
                        reader.read_more().await;
                    }
                    assert!(reader.buffered.capacity() <= CHUNK_SIZE);
                    assert!(output.capacity() <= CHUNK_SIZE);
                }
                assert_eq!(actual, expected);
            }
        }
    }

    #[tokio::test]
    async fn flushes_partial_bytes_without_polling_pending_input() {
        let body = stream::once(async { Ok(Bytes::from_static(b"data: first\n\n")) })
            .chain(stream::pending());
        let mut reader = HttpResponseReader::new(Box::pin(body));
        let mut output = Vec::new();
        assert!(
            reader
                .next_chunk(&mut output)
                .now_or_never()
                .unwrap()
                .unwrap()
        );
        assert_eq!(output, b"data: first\n\n");
        assert!(reader.read_more().now_or_never().is_none());
    }

    #[tokio::test]
    async fn bounds_read_ahead_even_for_single_byte_fragments() {
        let polls = Arc::new(AtomicUsize::new(0));
        let counter = polls.clone();
        let body = stream::repeat_with(move || {
            counter.fetch_add(1, Ordering::SeqCst);
            Ok(Bytes::from_static(b"x"))
        });
        let mut reader = HttpResponseReader::new(Box::pin(body));
        let mut output = Vec::new();
        assert!(reader.next_chunk(&mut output).await.unwrap());
        while reader.can_read() {
            reader.read_more().await;
        }
        assert_eq!(polls.load(Ordering::SeqCst), CHUNK_SIZE + 1);
        assert_eq!(reader.buffered.len(), CHUNK_SIZE);
        assert_eq!(reader.buffered.capacity(), CHUNK_SIZE);
        assert_eq!(output.capacity(), CHUNK_SIZE);
        assert!(reader.pending.is_empty());
    }

    struct BackingAllocation {
        data: Vec<u8>,
        visible: usize,
        live: Arc<AtomicUsize>,
    }

    impl AsRef<[u8]> for BackingAllocation {
        fn as_ref(&self) -> &[u8] {
            &self.data[..self.visible]
        }
    }

    impl Drop for BackingAllocation {
        fn drop(&mut self) {
            self.live.fetch_sub(1, Ordering::SeqCst);
        }
    }

    #[tokio::test]
    async fn releases_large_backing_allocations_including_empty_frames() {
        let live = Arc::new(AtomicUsize::new(0));
        let counter = live.clone();
        let body = stream::iter([0, 1, 1, CHUNK_SIZE * 2]).map(move |visible| {
            counter.fetch_add(1, Ordering::SeqCst);
            Ok(Bytes::from_owner(BackingAllocation {
                data: vec![42; CHUNK_SIZE * 64],
                visible,
                live: counter.clone(),
            }))
        });
        let mut reader = HttpResponseReader::new(Box::pin(body));
        reader.read_more().await;
        assert_eq!(live.load(Ordering::SeqCst), 0);
        reader.read_more().await;
        reader.read_more().await;
        assert_eq!(live.load(Ordering::SeqCst), 0);
        reader.read_more().await;
        assert_eq!(live.load(Ordering::SeqCst), 1);
        assert_eq!(reader.pending.len(), CHUNK_SIZE + 2);
        let mut output = Vec::new();
        assert!(reader.next_chunk(&mut output).await.unwrap());
        reader.read_more().await;
        assert_eq!(live.load(Ordering::SeqCst), 1);
        assert!(reader.next_chunk(&mut output).await.unwrap());
        reader.read_more().await;
        assert_eq!(live.load(Ordering::SeqCst), 0);
    }

    #[tokio::test]
    async fn flushes_last_partial_before_upstream_error_or_panic() {
        for panic in [false, true] {
            let body = stream::iter([Ok(Bytes::from_static(b"partial"))]).chain(stream::once(
                async move {
                    assert!(!panic, "failing user stream");
                    Err(CopilotRequestError::message("upstream failed"))
                },
            ));
            let mut reader = HttpResponseReader::new(Box::pin(body));
            reader.read_more().await;
            reader.read_more().await;
            let mut output = Vec::new();
            assert!(reader.next_chunk(&mut output).await.unwrap());
            assert_eq!(output, b"partial");
            let error = reader.next_chunk(&mut output).await.unwrap_err();
            assert_eq!(
                error.to_string(),
                if panic {
                    "HTTP response body stream panicked"
                } else {
                    "upstream failed"
                }
            );
        }
    }

    #[tokio::test]
    async fn dropping_reader_drops_pending_source() {
        let (tx, rx) = tokio::sync::mpsc::channel(1);
        let mut reader =
            HttpResponseReader::new(Box::pin(tokio_stream::wrappers::ReceiverStream::new(rx)));
        assert!(reader.read_more().now_or_never().is_none());
        drop(reader);
        assert!(tx.is_closed());
    }

    #[tokio::test]
    async fn always_ready_empty_frames_yield_to_cancellation() {
        let mut reader =
            HttpResponseReader::new(Box::pin(stream::repeat_with(|| Ok(Bytes::new()))));
        let mut output = Vec::new();
        assert!(
            tokio::time::timeout(Duration::from_millis(10), reader.next_chunk(&mut output))
                .await
                .is_err()
        );
    }
}
