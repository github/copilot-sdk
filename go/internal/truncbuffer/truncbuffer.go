package truncbuffer

import "sync"

// TruncBuffer is a ring buffer that retains only the last max bytes,
// discarding older data. This is useful for capturing stderr output in a
// memory-bounded way when the full output may be arbitrarily large.
// All methods are safe for concurrent use.
type TruncBuffer struct {
	mu   sync.RWMutex
	buf  []byte
	head int
	size int
	full bool
}

// NewTruncBuffer creates a TruncBuffer that keeps at most n bytes.
func NewTruncBuffer(n int) *TruncBuffer {
	return &TruncBuffer{
		buf:  make([]byte, n),
		size: n,
	}
}

// Write appends p to the buffer, keeping only the last size bytes.
// The return value n is the length of p;
func (t *TruncBuffer) Write(p []byte) (int, error) {
	t.mu.Lock()
	defer t.mu.Unlock()

	// If input is larger than the buffer, only keep the tail.
	if len(p) >= t.size {
		copy(t.buf, p[len(p)-t.size:])
		t.head = 0
		t.full = true
		return len(p), nil
	}

	for _, b := range p {
		t.buf[t.head] = b
		t.head++
		if t.head == t.size {
			t.head = 0
			t.full = true
		}
	}

	return len(p), nil
}

// Bytes returns a copy of the current buffer contents in order.
func (t *TruncBuffer) Bytes() []byte {
	t.mu.RLock()
	defer t.mu.RUnlock()

	if !t.full {
		return append([]byte(nil), t.buf[:t.head]...)
	}

	out := make([]byte, t.size)
	n := copy(out, t.buf[t.head:])
	copy(out[n:], t.buf[:t.head])
	return out
}

// String returns the buffer contents as a string.
func (t *TruncBuffer) String() string {
	return string(t.Bytes())
}
