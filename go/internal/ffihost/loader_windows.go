// SPDX-License-Identifier: MIT

//go:build copilot_inprocess && windows

package ffihost

import "syscall"

// openLibrary loads the DLL at path and returns its module handle. purego's
// RegisterLibFunc resolves exports from this handle via GetProcAddress, so the
// standard-library loader is sufficient and keeps CGO disabled.
func openLibrary(path string) (uintptr, error) {
	handle, err := syscall.LoadLibrary(path)
	if err != nil {
		return 0, err
	}
	return uintptr(handle), nil
}
