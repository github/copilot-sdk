package embeddedcli

import "github.com/github/copilot-sdk/go/internal/embeddedcli"

// Config defines the inputs used to install and locate the embedded Copilot CLI.
//
// Cli and CliHash are required. If Dir is empty, the CLI is installed into the
// system cache directory. Version is used to suffix the installed binary name to
// allow multiple versions to coexist. License, when provided, is written next
// to the installed binary.
type Config = embeddedcli.Config

// Setup sets the embedded GitHub Copilot CLI install configuration.
// The CLI will be lazily installed when needed.
func Setup(cfg Config) {
	embeddedcli.Setup(cfg)
}
