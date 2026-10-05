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
mod tests;
