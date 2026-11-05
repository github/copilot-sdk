package testharness

import (
	"context"

	copilot "github.com/github/copilot-sdk/go"
)

// Client supplies fixture-owned session directories without changing process cwd.
// The embedded Client remains available to tests of raw SDK defaults.
type Client struct {
	*copilot.Client
	workDir string
}

func (c *Client) sessionConfig(config *copilot.SessionConfig) *copilot.SessionConfig {
	if config == nil || config.WorkingDirectory != "" || config.Cloud != nil || c.workDir == "" {
		return config
	}
	scoped := *config
	scoped.WorkingDirectory = c.workDir
	return &scoped
}

func (c *Client) CreateSession(ctx context.Context, config *copilot.SessionConfig) (*copilot.Session, error) {
	return c.Client.CreateSession(ctx, c.sessionConfig(config))
}
