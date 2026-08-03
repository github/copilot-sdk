//go:build !windows

package copilot

import (
	"os/exec"
	"syscall"
)

// configureProcAttr places the runtime in its own process group so
// killProcessTreeByPid can signal all descendants atomically.
func configureProcAttr(cmd *exec.Cmd) {
	cmd.SysProcAttr = &syscall.SysProcAttr{Setpgid: true}
}

// killProcessTreeByPid signals the process group (negative PID) with SIGKILL.
func killProcessTreeByPid(pid int) {
	_ = syscall.Kill(-pid, syscall.SIGKILL)
}
