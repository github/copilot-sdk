//go:build copilot_inprocess && (darwin || linux || windows)

package ffihost

import (
	"io"
	"sync"
)

// receiveBuffer is a thread-safe byte buffer that feeds blocking Read from a
// producer thread. The native outbound callback (invoked on a foreign runtime
// thread) appends frames via feed without ever blocking; the JSON-RPC reader
// goroutine drains them via Read, which blocks until data or EOF.
//
// It implements io.ReadCloser so it can be handed to jsonrpc2.NewClient as the
// server → client stream.
type receiveBuffer struct {
	mu     sync.Mutex
	cond   *sync.Cond
	buf    []byte
	closed bool
}

func newReceiveBuffer() *receiveBuffer {
	rb := &receiveBuffer{}
	rb.cond = sync.NewCond(&rb.mu)
	return rb
}

func (rb *receiveBuffer) feed(data []byte) {
	rb.mu.Lock()
	defer rb.mu.Unlock()
	if rb.closed {
		return
	}
	rb.buf = append(rb.buf, data...)
	rb.cond.Broadcast()
}

// Read blocks until at least one byte is available or the buffer is closed.
// It returns io.EOF only once the buffer is closed and fully drained.
func (rb *receiveBuffer) Read(p []byte) (int, error) {
	if len(p) == 0 {
		return 0, nil
	}
	rb.mu.Lock()
	defer rb.mu.Unlock()
	for len(rb.buf) == 0 && !rb.closed {
		rb.cond.Wait()
	}
	if len(rb.buf) == 0 {
		return 0, io.EOF
	}
	n := copy(p, rb.buf)
	rb.buf = rb.buf[n:]
	return n, nil
}

// Close marks the buffer closed; subsequent Reads drain remaining bytes then
// return io.EOF. Idempotent.
func (rb *receiveBuffer) Close() error {
	rb.mu.Lock()
	defer rb.mu.Unlock()
	rb.closed = true
	rb.cond.Broadcast()
	return nil
}
