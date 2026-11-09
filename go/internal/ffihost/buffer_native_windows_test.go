//go:build copilot_inprocess && windows

package ffihost

import (
	"syscall"
	"testing"
	"unsafe"
)

func newNativeCallbackBuffer(t *testing.T, size int) []byte {
	t.Helper()
	mapping, err := syscall.CreateFileMapping(syscall.InvalidHandle, nil, syscall.PAGE_READWRITE, 0, uint32(size), nil)
	if err != nil {
		t.Fatal(err)
	}
	t.Cleanup(func() {
		if err := syscall.CloseHandle(mapping); err != nil {
			t.Errorf("close callback memory mapping: %v", err)
		}
	})
	address, err := syscall.MapViewOfFile(mapping, syscall.FILE_MAP_READ|syscall.FILE_MAP_WRITE, 0, 0, uintptr(size))
	if err != nil {
		t.Fatal(err)
	}
	t.Cleanup(func() {
		if err := syscall.UnmapViewOfFile(address); err != nil {
			t.Errorf("unmap callback memory: %v", err)
		}
	})
	return unsafe.Slice((*byte)(unsafe.Pointer(address)), size)
}
