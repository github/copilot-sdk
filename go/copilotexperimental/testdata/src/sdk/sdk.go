// Package sdk is a miniature stand-in for the generated Copilot SDK surface.
package sdk

// StableGreeting is a stable API.
func StableGreeting(name string) string {
	return "Hello, " + name
}

// StartCanvas starts an experimental canvas session.
//
// Experimental: StartCanvas is an experimental API and may change or be removed.
func StartCanvas() string { // want StartCanvas:"experimental"
	return "canvas"
}

// CanvasOptions configures a canvas.
//
// Experimental: CanvasOptions is part of an experimental API and may change or be removed.
type CanvasOptions struct { // want CanvasOptions:"experimental"
	Title string
}

// Client is a stable client.
type Client struct {
	// Name is a stable field.
	Name string

	// Experimental: EnableMCPApps is part of an experimental wire-protocol surface and may change or be removed.
	EnableMCPApps bool // want EnableMCPApps:"experimental"
}

// Connect is a stable method.
func (c *Client) Connect() {}

// EnableExperimentalMode enables an experimental mode.
//
// Experimental: EnableExperimentalMode is an experimental API and may change or be removed.
func (c *Client) EnableExperimentalMode() {} // want EnableExperimentalMode:"experimental"
