// SPDX-License-Identifier: MIT

//go:build !darwin

package ffihost

// rearmForeignSignalHandlers is a no-op off Darwin. The Go runtime only enforces
// the SA_ONSTACK requirement that libuv's SIGCHLD handler violates on macOS;
// Linux and Windows are unaffected, so no signal re-arming is needed.
func rearmForeignSignalHandlers() {}
