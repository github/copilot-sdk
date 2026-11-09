package embeddedcli

import "github.com/github/copilot-sdk/go/internal/embeddedcli"

// Config defines the inputs used to install and locate the embedded Copilot CLI.
//
// Cli and CliHash are required. If Dir is empty, the CLI is installed into the
// per-user cache directory or a protected fallback cache.
// When Version is set, the CLI and runtime library are
// installed into a version-specific child directory so multiple versions can
// coexist. Linux musl alternatives, when provided, are selected automatically.
// License, when provided, is written next to the installed binary.
type Config = embeddedcli.Config

// Setup sets the embedded GitHub Copilot CLI install configuration.
// The CLI will be lazily installed when needed.
func Setup(cfg Config) {
	embeddedcli.Setup(cfg)
}

// Path returns the absolute path to the embedded Copilot CLI, installing it on
// first call if necessary. It returns an empty string when no embedded CLI was
// configured via Setup (e.g. a build compiled without the embedded runtime) or
// installation failed. Set COPILOT_CLI_INSTALL_VERBOSE=1 for installation errors.
// Only a successful result is cached for the life of the process; a failed
// installation is retried on the next call after the environment is corrected.
func Path() string {
	return embeddedcli.Path()
}
