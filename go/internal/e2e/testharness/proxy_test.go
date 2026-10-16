// Copyright (c) Microsoft Corporation. All rights reserved.

package testharness

import (
	"encoding/json"
	"testing"
)

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
