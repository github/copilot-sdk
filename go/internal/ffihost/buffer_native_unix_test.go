//go:build copilot_inprocess && (darwin || linux)

package ffihost

import (
	"syscall"
	"testing"
)

func newNativeCallbackBuffer(t *testing.T, size int) []byte {
	t.Helper()
	memory, err := syscall.Mmap(-1, 0, size, syscall.PROT_READ|syscall.PROT_WRITE, syscall.MAP_ANON|syscall.MAP_PRIVATE)
	if err != nil {
		t.Fatal(err)
	}
	t.Cleanup(func() {
		if err := syscall.Munmap(memory); err != nil {
			t.Errorf("unmap callback memory: %v", err)
		}
	})
	return memory
}
