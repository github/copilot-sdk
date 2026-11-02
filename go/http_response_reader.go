package copilot

import (
	"context"
	"errors"
	"io"
	"runtime"
	"sync"
)

const httpResponseReadAheadSize = 32 * 1024

// httpResponseReader keeps one bounded buffer of response bytes ready while
// the preceding chunk waits for its runtime acknowledgement.
type httpResponseReader struct {
	body io.ReadCloser

	mu       sync.Mutex
	buffered []byte
	err      error

	notify chan struct{}
	space  chan struct{}
	stop   chan struct{}
	done   chan struct{}

	closeOnce sync.Once
}

func newHTTPResponseReader(body io.ReadCloser) *httpResponseReader {
	r := &httpResponseReader{
		body:     body,
		buffered: make([]byte, 0, httpResponseReadAheadSize),
		notify:   make(chan struct{}, 1),
		space:    make(chan struct{}, 1),
		stop:     make(chan struct{}),
		done:     make(chan struct{}),
	}
	go r.readLoop()
	return r
}

func (r *httpResponseReader) readLoop() {
	defer close(r.done)
	scratch := make([]byte, httpResponseReadAheadSize)
	for {
		select {
		case <-r.stop:
			return
		default:
		}

		r.mu.Lock()
		remaining := httpResponseReadAheadSize - len(r.buffered)
		stopped := r.err != nil
		r.mu.Unlock()

		if stopped {
			return
		}
		if remaining == 0 {
			select {
			case <-r.space:
				continue
			case <-r.stop:
				return
			}
		}

		n, err := r.body.Read(scratch[:remaining])
		r.mu.Lock()
		if n > 0 {
			r.buffered = append(r.buffered, scratch[:n]...)
		}
		if err != nil {
			r.err = err
		}
		r.mu.Unlock()

		if n > 0 || err != nil {
			signal(r.notify)
		}
		if err != nil {
			return
		}
		if n == 0 {
			// Broken readers are permitted to return (0, nil). Do not let one
			// starve acknowledgement or cancellation processing.
			runtime.Gosched()
		}
	}
}

func (r *httpResponseReader) nextChunk(ctx context.Context, output *[]byte) (bool, error) {
	for {
		select {
		case <-ctx.Done():
			return false, ctx.Err()
		default:
		}

		r.mu.Lock()
		if len(r.buffered) > 0 {
			replacement := (*output)[:0]
			if cap(replacement) < httpResponseReadAheadSize {
				replacement = make([]byte, 0, httpResponseReadAheadSize)
			}
			*output, r.buffered = r.buffered, replacement
			r.mu.Unlock()
			signal(r.space)
			return true, nil
		}
		err := r.err
		if err != nil {
			r.err = nil
		}
		r.mu.Unlock()

		if err != nil {
			if errors.Is(err, io.EOF) {
				return false, nil
			}
			return false, err
		}

		select {
		case <-r.notify:
		case <-ctx.Done():
			return false, ctx.Err()
		}
	}
}

func (r *httpResponseReader) Close() {
	r.closeOnce.Do(func() {
		close(r.stop)
		_ = r.body.Close()
		<-r.done
	})
}

func signal(ch chan struct{}) {
	select {
	case ch <- struct{}{}:
	default:
	}
}
