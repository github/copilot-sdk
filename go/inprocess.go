package copilot

import "io"

type inProcessHost interface {
	Start() error
	Writer() io.WriteCloser
	Reader() io.ReadCloser
	ConfigureUiThread(enabled bool) error
	RunUiAction(actionID uint32) error
	Dispose()
}

type inProcessHostConfig struct {
	Environment map[string]string
	Args        []string
}
