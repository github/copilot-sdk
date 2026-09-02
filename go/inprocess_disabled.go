//go:build !copilot_inprocess || (!darwin && !linux && !windows)

package copilot

import "errors"

const inProcessAvailable = false

var errInProcessUnavailable = errors.New("in-process transport unavailable")

func createInProcessHost(string, string, inProcessHostConfig) (inProcessHost, error) {
	return nil, errInProcessUnavailable
}
