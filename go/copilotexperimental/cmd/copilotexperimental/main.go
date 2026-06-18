// Command copilotexperimental runs the copilotexperimental analyzer.
package main

import (
	"golang.org/x/tools/go/analysis/singlechecker"

	"github.com/github/copilot-sdk/go/copilotexperimental"
)

func main() {
	singlechecker.Main(copilotexperimental.Analyzer)
}
