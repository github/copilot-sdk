//go:build windows

package copilot

import (
	"fmt"
	"os/exec"
	"syscall"
)

// configureProcAttr hides the console window on Windows.
func configureProcAttr(cmd *exec.Cmd) {
	cmd.SysProcAttr = &syscall.SysProcAttr{
		HideWindow: true,
	}
}

// killProcessTreeByPid terminates the entire process tree via taskkill /T /F.
func killProcessTreeByPid(pid int) error {
	return exec.Command("taskkill", "/T", "/F", "/PID", fmt.Sprintf("%d", pid)).Run()
}
