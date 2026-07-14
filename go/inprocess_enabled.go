//go:build copilot_inprocess && (darwin || linux || windows)

package copilot

import "github.com/github/copilot-sdk/go/internal/ffihost"

const inProcessAvailable = true

func createInProcessHost(runtimePath string) (inProcessHost, error) {
	return ffihost.Create(runtimePath, nil)
}
