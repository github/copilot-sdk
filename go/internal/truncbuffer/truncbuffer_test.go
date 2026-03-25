package truncbuffer

import (
	"io"
	"sync"
	"testing"
)

var _ io.Writer = (*TruncBuffer)(nil)

func TestTruncBuffer_SmallWrites(t *testing.T) {
	tb := NewTruncBuffer(10)
	tb.Write([]byte("hello"))
	if got := string(tb.Bytes()); got != "hello" {
		t.Fatalf("got %q, want %q", got, "hello")
	}
}

func TestTruncBuffer_ExactMax(t *testing.T) {
	tb := NewTruncBuffer(5)
	tb.Write([]byte("abcde"))
	if got := string(tb.Bytes()); got != "abcde" {
		t.Fatalf("got %q, want %q", got, "abcde")
	}
}

func TestTruncBuffer_OverflowSingleWrite(t *testing.T) {
	tb := NewTruncBuffer(5)
	tb.Write([]byte("abcdefgh"))
	if got := string(tb.Bytes()); got != "defgh" {
		t.Fatalf("got %q, want %q", got, "defgh")
	}
}

func TestTruncBuffer_OverflowMultipleWrites(t *testing.T) {
	tb := NewTruncBuffer(6)
	tb.Write([]byte("abc"))
	tb.Write([]byte("defgh"))
	if got := string(tb.Bytes()); got != "cdefgh" {
		t.Fatalf("got %q, want %q", got, "cdefgh")
	}
}

func TestTruncBuffer_ManySmallWrites(t *testing.T) {
	tb := NewTruncBuffer(4)
	for _, b := range []byte("abcdefg") {
		tb.Write([]byte{b})
	}
	if got := string(tb.Bytes()); got != "defg" {
		t.Fatalf("got %q, want %q", got, "defg")
	}
}

func TestTruncBuffer_ConcurrentWrites(t *testing.T) {
	tb := NewTruncBuffer(64)
	var wg sync.WaitGroup
	for i := 0; i < 100; i++ {
		wg.Add(1)
		go func() {
			defer wg.Done()
			tb.Write([]byte("abcdefgh"))
		}()
	}
	wg.Wait()
	if got := len(tb.Bytes()); got > 64 {
		t.Fatalf("buffer exceeded max: got %d bytes", got)
	}
}
