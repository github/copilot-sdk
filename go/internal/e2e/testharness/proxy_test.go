// Copyright (c) Microsoft Corporation. All rights reserved.

package testharness

import (
	"encoding/json"
	"io"
	"net/http"
	"net/http/httptest"
	"os"
	"os/exec"
	"strings"
	"testing"
)

func TestProxyShutdownReportsReplayFailure(t *testing.T) {
	for _, status := range []int{http.StatusOK, http.StatusInternalServerError} {
		t.Run(http.StatusText(status), func(t *testing.T) {
			cmd := exec.Command(os.Args[0], "-test.run=^TestProxyShutdownProcess$")
			cmd.Env = append(os.Environ(), "COPILOT_PROXY_SHUTDOWN_PROCESS=1")
			stdin, err := cmd.StdinPipe()
			if err != nil {
				t.Fatal(err)
			}
			if err := cmd.Start(); err != nil {
				t.Fatal(err)
			}
			t.Cleanup(func() {
				stdin.Close()
				if cmd.ProcessState == nil {
					cmd.Process.Kill()
					cmd.Wait()
				}
			})
			const diagnostic = "Replay cache misses (1): No cached response found"
			server := httptest.NewServer(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
				if r.Method != http.MethodPost || r.URL.String() != "/stop?skipWritingCache=true" {
					t.Errorf("Unexpected shutdown request: %s %s", r.Method, r.URL)
				}
				w.WriteHeader(status)
				if status != http.StatusOK {
					io.WriteString(w, diagnostic)
				}
				stdin.Close()
			}))
			t.Cleanup(server.Close)
			proxy := &CapiProxy{cmd: cmd, proxyURL: server.URL}
			err = proxy.StopWithOptions(true)
			if status == http.StatusOK && err != nil {
				t.Fatalf("Clean shutdown failed: %v", err)
			}
			if status != http.StatusOK && (err == nil || !strings.Contains(err.Error(), diagnostic)) {
				t.Fatalf("Shutdown error = %v, want %q", err, diagnostic)
			}
			if cmd.ProcessState == nil || !cmd.ProcessState.Exited() {
				t.Fatal("Proxy process was not reaped")
			}
			if err := proxy.Stop(); err != nil {
				t.Fatalf("Repeated shutdown failed: %v", err)
			}
		})
	}
}

func TestProxyShutdownProcess(t *testing.T) {
	if os.Getenv("COPILOT_PROXY_SHUTDOWN_PROCESS") != "1" {
		return
	}
	io.Copy(io.Discard, os.Stdin)
	os.Exit(0)
}

func TestCompactionProviderUsageInputCounts(t *testing.T) {
	for _, tc := range []struct {
		name          string
		inputProperty string
		want          int64
		known         bool
	}{
		{"recorded", `,"inputTokens":37`, 37, true},
		{"replayed", `,"inputTokens":0`, 0, true},
		{"unavailable", ``, 0, false},
		{"input null", `,"inputTokens":null`, 0, false},
	} {
		t.Run(tc.name, func(t *testing.T) {
			var exchange ParsedHttpExchange
			body := `{"compactionUsage":{"interactionId":"compaction-interaction","summary":"summary","responseCount":2` +
				tc.inputProperty + `},"requestHeaders":{"x-interaction-type":"conversation-compaction"}}`
			if err := json.Unmarshal([]byte(body), &exchange); err != nil {
				t.Fatal(err)
			}
			if exchange.CompactionUsage == nil {
				t.Fatal("Expected compaction provider usage")
			}
			var interactionType string
			if err := json.Unmarshal(exchange.RequestHeaders["x-interaction-type"], &interactionType); err != nil || interactionType != "conversation-compaction" {
				t.Fatalf("Compaction interaction header = %q, %v", interactionType, err)
			}
			usage := exchange.CompactionUsage
			if usage.InteractionID != "compaction-interaction" || usage.Summary != "summary" || usage.ResponseCount != 2 {
				t.Fatalf("Unexpected compaction chain metadata: %+v", usage)
			}
			var got int64
			known := usage.InputTokens != nil
			if known != tc.known {
				t.Fatalf("Provider input usage availability = %v, want %v", known, tc.known)
			}
			if known {
				got = *usage.InputTokens
			}
			if got != tc.want {
				t.Fatalf("Provider input tokens = %d, want %d", got, tc.want)
			}
		})
	}
}
