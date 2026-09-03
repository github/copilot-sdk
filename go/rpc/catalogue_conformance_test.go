package rpc

import (
	"encoding/json"
	"testing"
)

const (
	opaqueMCPHandle   = "opaque:mcp/01-do-not-parse"
	opaqueSkillHandle = "opaque:skill/02-do-not-parse"
)

func TestCatalogSearchResultPreservesCandidateSemantics(t *testing.T) {
	result, err := unmarshalCatalogSearchResult([]byte(`{
		"kind":"succeeded",
		"searchId":"search-01",
		"candidates":[
			{
				"kind":"mcp-server",
				"handle":"opaque:mcp/01-do-not-parse",
				"handleExpiresAt":"2026-09-02T12:00:00Z",
				"mediaType":"application/mcp-server-card+json",
				"installability":"installable",
				"displayName":"Example MCP",
				"rawCard":{"secret":"must-not-survive"},
				"source":{"kind":"url","url":"https://catalog.example/mcp.json"},
				"provenance":{
					"authority":"catalog.example",
					"observedAt":"2026-09-02T11:00:00Z",
					"mediaType":"application/mcp-server-card+json"
				}
			},
			{
				"kind":"ai-skill",
				"handle":"opaque:skill/02-do-not-parse",
				"handleExpiresAt":"2026-09-02T12:00:00Z",
				"mediaType":"application/ai-skill",
				"installability":"not-installable-kind",
				"displayName":"Example skill",
				"rawCard":{"secret":"must-not-survive"},
				"source":{"kind":"embedded"},
				"provenance":{
					"authority":"catalog.example",
					"observedAt":"2026-09-02T11:00:00Z",
					"mediaType":"application/ai-skill"
				}
			}
		],
		"truncated":false,
		"negotiated":{
			"runtimeProtocolVersion":1,
			"grantedCapabilities":["mcp-server-card","ai-skill-discovery"]
		}
	}`))
	if err != nil {
		t.Fatalf("unmarshal catalogue success: %v", err)
	}

	success, ok := result.(*CatalogSearchSucceeded)
	if !ok {
		t.Fatalf("catalogue result = %T, want *CatalogSearchSucceeded", result)
	}
	mcp, ok := success.Candidates[0].(*CatalogMCPServerCandidate)
	if !ok {
		t.Fatalf("first candidate = %T, want *CatalogMCPServerCandidate", success.Candidates[0])
	}
	skill, ok := success.Candidates[1].(*CatalogAiSkillCandidate)
	if !ok {
		t.Fatalf("second candidate = %T, want *CatalogAiSkillCandidate", success.Candidates[1])
	}
	if mcp.Handle != opaqueMCPHandle || skill.Handle != opaqueSkillHandle {
		t.Fatalf("opaque handles changed: %q, %q", mcp.Handle, skill.Handle)
	}
	if _, ok := mcp.Source.(*CatalogCandidateSourceURL); !ok {
		t.Fatalf("MCP source = %T, want *CatalogCandidateSourceURL", mcp.Source)
	}
	if _, ok := skill.Source.(*CatalogCandidateSourceEmbedded); !ok {
		t.Fatalf("skill source = %T, want *CatalogCandidateSourceEmbedded", skill.Source)
	}

	encoded, err := json.Marshal(success)
	if err != nil {
		t.Fatalf("marshal catalogue success: %v", err)
	}
	var wire map[string]any
	if err := json.Unmarshal(encoded, &wire); err != nil {
		t.Fatalf("decode catalogue wire result: %v", err)
	}
	for _, candidate := range wire["candidates"].([]any) {
		fields := candidate.(map[string]any)
		for _, forbidden := range []string{"card", "cardData", "rawCard"} {
			if _, exists := fields[forbidden]; exists {
				t.Fatalf("candidate leaked %q: %s", forbidden, encoded)
			}
		}
	}
}

func TestCatalogSearchResultPreservesRefusalsAndFailures(t *testing.T) {
	tests := []struct {
		name    string
		payload string
		assert  func(*testing.T, CatalogSearchResult)
	}{
		{
			name:    "authentication required",
			payload: `{"kind":"authentication-required","reason":"no-credential","message":"Sign in is required."}`,
			assert: func(t *testing.T, result CatalogSearchResult) {
				if _, ok := result.(*CatalogAuthenticationRequiredError); !ok {
					t.Fatalf("result = %T, want *CatalogAuthenticationRequiredError", result)
				}
			},
		},
		{
			name:    "network failure",
			payload: `{"kind":"network-failure","reason":"timeout","retryAfterSeconds":30,"message":"The catalogue timed out."}`,
			assert: func(t *testing.T, result CatalogSearchResult) {
				failure, ok := result.(*CatalogNetworkFailureError)
				if !ok {
					t.Fatalf("result = %T, want *CatalogNetworkFailureError", result)
				}
				if failure.RetryAfterSeconds == nil || *failure.RetryAfterSeconds != 30 {
					t.Fatalf("retryAfterSeconds = %v, want 30", failure.RetryAfterSeconds)
				}
			},
		},
	}

	for _, test := range tests {
		t.Run(test.name, func(t *testing.T) {
			result, err := unmarshalCatalogSearchResult([]byte(test.payload))
			if err != nil {
				t.Fatalf("unmarshal catalogue result: %v", err)
			}
			test.assert(t, result)
		})
	}
}

func TestCatalogSearchResultRejectsUnknownCandidateKinds(t *testing.T) {
	_, err := unmarshalCatalogSearchResult([]byte(`{
		"kind":"succeeded",
		"searchId":"search-unknown",
		"candidates":[{
			"kind":"future-kind",
			"handle":"opaque:future/03-do-not-parse",
			"rawCard":{"secret":"must-not-survive"}
		}],
		"truncated":false,
		"negotiated":{"runtimeProtocolVersion":1,"grantedCapabilities":[]}
	}`))
	if err == nil {
		t.Fatal("unknown catalogue candidate kind with rawCard must be rejected")
	}
}
