//go:build copilot_inprocess && (darwin || linux || windows)

package ffihost

import (
	"bytes"
	"io"
	"testing"
	"unsafe"
)

func TestReceiveBufferReleasesConsumedStorage(t *testing.T) {
	rb := newReceiveBuffer()
	defer rb.Close()
	chunk := newNativeCallbackBuffer(t, 64*1024+3)
	for i := range chunk {
		chunk[i] = 42
	}
	host := &Host{recv: rb}
	host.onOutbound(uintptr(unsafe.Pointer(unsafe.SliceData(chunk))), uintptr(len(chunk)))
	chunk[0] = 0

	first := make([]byte, len(chunk)-1)
	if n, err := rb.Read(first); n != len(first) || err != nil {
		t.Fatalf("partial read = (%d, %v), want (%d, nil)", n, err, len(first))
	}
	if first[0] != 42 || len(rb.buf) != 1 {
		t.Fatalf("partial read lost callback data: first=%d, remaining=%d", first[0], len(rb.buf))
	}
	if n, err := rb.Read(nil); n != 0 || err != nil || len(rb.buf) != 1 {
		t.Fatalf("empty read = (%d, %v), remaining=%d; want (0, nil), remaining=1", n, err, len(rb.buf))
	}

	last := make([]byte, 1)
	if n, err := rb.Read(last); n != 1 || err != nil || last[0] != 42 {
		t.Fatalf("final read = (%d, %v), data=%v; want (1, nil), [42]", n, err, last)
	}
	if rb.buf != nil {
		t.Fatal("fully consumed callback storage remains referenced while the connection is idle")
	}

	rb.feed([]byte{7, 8})
	if n, err := rb.Read(last); n != 1 || err != nil || last[0] != 7 {
		t.Fatalf("next read = (%d, %v), data=%v; want (1, nil), [7]", n, err, last)
	}
	rb.feed([]byte{9, 10})
	rb.Close()
	rb.feed([]byte{11})
	if n, err := rb.Read(first); n != 3 || err != nil || !bytes.Equal(first[:n], []byte{8, 9, 10}) {
		t.Fatalf("closed drain = (%d, %v), data=%v; want (3, nil), [8 9 10]", n, err, first[:n])
	}
	if n, err := rb.Read(last); n != 0 || err != io.EOF {
		t.Fatalf("closed read = (%d, %v), want (0, EOF)", n, err)
	}
}
