package copilotexperimental_test

import (
	"testing"

	"golang.org/x/tools/go/analysis/analysistest"

	"github.com/github/copilot-sdk/go/copilotexperimental"
)

func TestAnalyzer(t *testing.T) {
	analysistest.Run(t, analysistest.TestData(), copilotexperimental.Analyzer, "sdk", "consumer")
}
