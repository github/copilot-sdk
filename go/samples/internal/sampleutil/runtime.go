package sampleutil

import (
	"fmt"
	"os"
	"os/exec"
	"path/filepath"
	"strings"
)

// CLIPath resolves the pinned source-checkout runtime used by SDK samples.
func CLIPath() (string, error) {
	if cliPath := os.Getenv("COPILOT_CLI_PATH"); cliPath != "" {
		return cliPath, nil
	}

	current, err := os.Getwd()
	if err != nil {
		return "", err
	}
	for {
		nodeDir := filepath.Join(current, "nodejs")
		if _, err := os.Stat(filepath.Join(nodeDir, "package.json")); err == nil {
			command := exec.Command(
				"node",
				"node_modules/tsx/dist/cli.mjs",
				"scripts/prepare-runtime.ts",
				"--print-path",
			)
			command.Dir = nodeDir
			output, err := command.CombinedOutput()
			if err != nil {
				return "", fmt.Errorf("prepare pinned Copilot CLI: %w: %s", err, output)
			}
			cliPath := strings.TrimSpace(string(output))
			if info, err := os.Stat(cliPath); err != nil || info.IsDir() {
				return "", fmt.Errorf("prepared Copilot CLI path is not a file: %q", cliPath)
			}
			return cliPath, nil
		}

		parent := filepath.Dir(current)
		if parent == current {
			return "", fmt.Errorf("could not find nodejs/package.json; set COPILOT_CLI_PATH")
		}
		current = parent
	}
}
