// SPDX-License-Identifier: MIT

//go:build copilot_inprocess && (darwin || linux)

package ffihost

import "github.com/ebitengine/purego"

// openLibrary loads the shared library at path and returns an opaque handle.
// RTLD_NOW surfaces any load problem here (eager binding) rather than at first
// call, matching the .NET/Python hosts; RTLD_LOCAL keeps the runtime's symbols
// private to this handle.
func openLibrary(path string) (uintptr, error) {
	return purego.Dlopen(path, purego.RTLD_NOW|purego.RTLD_LOCAL)
}
