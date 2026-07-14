// SPDX-License-Identifier: MIT

//go:build copilot_inprocess && windows

package ffihost

// rearmForeignSignalHandlers is a no-op on platforms other than darwin and
// linux. Only those Unix platforms deliver the SA_ONSTACK-less SIGCHLD handler
// (installed by libuv) that the Go runtime rejects; Windows is unaffected.
func rearmForeignSignalHandlers(_ uintptr) {}
