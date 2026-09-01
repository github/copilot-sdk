//go:build copilot_inprocess && (darwin || linux || windows)

package copilot

import "github.com/github/copilot-sdk/go/internal/ffihost"

const inProcessAvailable = true

func createInProcessHost(runtimePath, cliEntrypoint string, config inProcessHostConfig) (inProcessHost, error) {
	return ffihost.Create(runtimePath, cliEntrypoint, config.Environment, config.Args)
}
