package rpc

import (
	"encoding/json"
	"testing"
)

// Requests that declare x-legacy-parameters keep their Go struct: added inputs are optional
// pointer fields, so existing keyed literals compile and encode exactly as before.
func TestLegacyParameterRequestsKeepKeyedLiterals(t *testing.T) {
	contract := CatalogClientContract{ProtocolVersion: 3, RequiredCapabilities: []string{"mcp-install-planning"}}
	scope := MCPPlanScopeUser
	limit := int32(4)

	existing := []any{
		MCPPlanInstallRequest{
			Contract: contract,
			Source:   &MCPPlanInstallSourceCandidate{CandidateHandle: "candidate", SearchID: "search"},
			Scope:    &scope,
		},
		CatalogSearchRequest{Contract: contract, Query: "catalogue query", Limit: &limit},
	}
	for _, request := range existing {
		wire := encodeRequest(t, request)
		if _, ok := wire["policySessionId"]; ok {
			t.Fatalf("existing literal %T sent policySessionId: %v", request, wire)
		}
	}

	session := "session"
	added := encodeRequest(t, MCPPlanInstallRequest{
		Contract:        contract,
		Source:          &MCPPlanInstallSourceCandidate{CandidateHandle: "candidate", SearchID: "search"},
		Scope:           &scope,
		PolicySessionID: &session,
	})
	if added["policySessionId"] != session || added["scope"] != "user" {
		t.Fatalf("added input not encoded alongside the legacy fields: %v", added)
	}
}

func encodeRequest(t *testing.T, request any) map[string]any {
	t.Helper()
	data, err := json.Marshal(request)
	if err != nil {
		t.Fatalf("marshal %T: %v", request, err)
	}
	var wire map[string]any
	if err := json.Unmarshal(data, &wire); err != nil {
		t.Fatalf("unmarshal %T: %v", request, err)
	}
	return wire
}
