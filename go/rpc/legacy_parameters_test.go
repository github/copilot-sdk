package rpc

import (
	"context"
	"encoding/json"
	"fmt"
	"net"
	"reflect"
	"testing"
	"time"

	"github.com/github/copilot-sdk/go/internal/jsonrpc2"
)

// Existing catalog requests retain their optional pointer fields and keyed literals.
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

func TestExternalToolDefinitionPreservesPositionalLiteral(t *testing.T) {
	definition := ProtocolExternalToolDefinition{
		nil, "Description", nil, nil, map[string]any{"custom": "kept"}, "lookup",
		nil, nil, nil, nil, nil,
	}
	data, err := json.Marshal(definition)
	if err != nil {
		t.Fatal(err)
	}
	if want := `{"description":"Description","metadata":{"custom":"kept"},"name":"lookup"}`; string(data) != want {
		t.Fatalf("positional definition = %s, want %s", data, want)
	}
}

func TestLoginBeginKeepsPositionalLiterals(t *testing.T) {
	request := AuthLoginBeginRequest{nil, LoginProviderKindGitHubDotCom, nil}
	wire := encodeRequest(t, request)
	if len(wire) != 1 || wire["kind"] != "githubDotCom" {
		t.Fatalf("legacy login request changed: %v", wire)
	}
}

func TestPermissionRequiredKeepsPositionalLiterals(t *testing.T) {
	request := PermissionsSetRequiredRequest{true}
	wire := encodeRequest(t, request)
	if len(wire) != 1 || wire["required"] != true {
		t.Fatalf("legacy permission requirement changed: %v", wire)
	}
}

func TestPermissionRequiredOptionsOnWire(t *testing.T) {
	for _, provider := range []bool{false, true} {
		for _, required := range []bool{false, true} {
			t.Run(fmt.Sprintf("provider=%t/required=%t", provider, required), func(t *testing.T) {
				clientConn, serverConn := net.Pipe()
				defer clientConn.Close()
				defer serverConn.Close()
				client := jsonrpc2.NewClient(clientConn, clientConn)
				server := jsonrpc2.NewClient(serverConn, serverConn)
				requests := make(chan json.RawMessage, 1)
				server.SetRequestHandler("session.permissions.setRequired", func(params json.RawMessage) (json.RawMessage, *jsonrpc2.Error) {
					requests <- params
					return json.RawMessage(`{"success":true}`), nil
				})
				client.Start()
				server.Start()
				defer client.Stop()
				defer server.Stop()
				ctx, cancel := context.WithTimeout(context.Background(), 5*time.Second)
				defer cancel()
				api := NewSessionRPC(client, "permission-session").Permissions
				expected := map[string]any{"sessionId": "permission-session", "required": required}
				var result *PermissionsSetRequiredResult
				var err error
				if provider {
					providerID := "provider"
					options := PermissionsSetRequiredOptions{Required: required, ProviderID: &providerID}
					result, err = api.SetRequiredWithOptions(ctx, &options)
					expected["providerId"] = providerID
				} else {
					result, err = api.SetRequired(ctx, &PermissionsSetRequiredRequest{required})
				}
				if err != nil {
					t.Fatal(err)
				}
				if result == nil || !result.Success {
					t.Fatalf("permission requirement result = %#v", result)
				}
				select {
				case raw := <-requests:
					var actual map[string]any
					if err := json.Unmarshal(raw, &actual); err != nil {
						t.Fatal(err)
					}
					if !reflect.DeepEqual(actual, expected) {
						t.Fatalf("permission requirement = %v, want %v", actual, expected)
					}
				case <-ctx.Done():
					t.Fatal(ctx.Err())
				}
			})
		}
	}
}

func TestPermissionRequirementsKeepPositionalLiteralsAndProviderOptions(t *testing.T) {
	request := PermissionsSetRequiredRequest{true}
	if wire := encodeRequest(t, request); !reflect.DeepEqual(wire, map[string]any{"required": true}) {
		t.Fatalf("legacy permission requirement changed: %v", wire)
	}
	options := PermissionsSetRequiredOptions{Required: false}
	if wire := encodeRequest(t, options); !reflect.DeepEqual(wire, map[string]any{"required": false}) {
		t.Fatalf("provider identity must be omitted by default: %v", wire)
	}
	provider := "provider"
	options.ProviderID = &provider
	expected := map[string]any{"required": false, "providerId": provider}
	if wire := encodeRequest(t, options); !reflect.DeepEqual(wire, expected) {
		t.Fatalf("provider requirement = %v, want %v", wire, expected)
	}
}

func TestPermissionRequirementOptionsOnWire(t *testing.T) {
	clientConn, serverConn := net.Pipe()
	defer clientConn.Close()
	defer serverConn.Close()
	client := jsonrpc2.NewClient(clientConn, clientConn)
	server := jsonrpc2.NewClient(serverConn, serverConn)
	requests := make(chan json.RawMessage, 2)
	server.SetRequestHandler("session.permissions.setRequired", func(params json.RawMessage) (json.RawMessage, *jsonrpc2.Error) {
		requests <- params
		return json.RawMessage(`{"success":true}`), nil
	})
	client.Start()
	server.Start()
	defer client.Stop()
	defer server.Stop()
	ctx, cancel := context.WithTimeout(context.Background(), 5*time.Second)
	defer cancel()
	api := NewSessionRPC(client, "permission-session").Permissions
	if _, err := api.SetRequired(ctx, &PermissionsSetRequiredRequest{true}); err != nil {
		t.Fatal(err)
	}
	provider := "provider"
	if _, err := api.SetRequiredWithOptions(ctx, &PermissionsSetRequiredOptions{
		Required: false, ProviderID: &provider,
	}); err != nil {
		t.Fatal(err)
	}
	for _, expected := range []map[string]any{
		{"sessionId": "permission-session", "required": true},
		{"sessionId": "permission-session", "required": false, "providerId": provider},
	} {
		select {
		case raw := <-requests:
			var actual map[string]any
			if err := json.Unmarshal(raw, &actual); err != nil {
				t.Fatal(err)
			}
			if !reflect.DeepEqual(actual, expected) {
				t.Fatalf("permission requirement = %v, want %v", actual, expected)
			}
		case <-ctx.Done():
			t.Fatal(ctx.Err())
		}
	}
}

func TestSessionInfoKeepsPositionalLiterals(t *testing.T) {
	data := SessionInfoData{"notification", "message", nil, nil}
	wire := encodeRequest(t, data)
	if len(wire) != 2 || wire["infoType"] != "notification" || wire["message"] != "message" {
		t.Fatalf("legacy info payload changed: %v", wire)
	}
	tip, url := "a tip", "https://example.com"
	data = SessionInfoData{"notification", "message", &tip, &url}
	wire = encodeRequest(t, data)
	if wire["tip"] != tip || wire["url"] != url {
		t.Fatalf("legacy info payload field order changed: %v", wire)
	}
}

func TestSessionInfoDecodesWithInternalReasoningMetadata(t *testing.T) {
	var event SessionEvent
	err := json.Unmarshal([]byte(`{
		"type":"session.info",
		"data":{
			"infoType":"reasoning_effort","message":"Reasoning effort changed",
			"tip":"a tip","url":"https://example.com",
			"responsesReasoning":{"model":"gpt-6-astra","initialEffort":"low","effort":"high"}
		}
	}`), &event)
	if err != nil {
		t.Fatal(err)
	}
	data, ok := event.Data.(*SessionInfoData)
	if !ok || data.InfoType != "reasoning_effort" || data.Message != "Reasoning effort changed" ||
		data.Tip == nil || *data.Tip != "a tip" || data.URL == nil || *data.URL != "https://example.com" {
		t.Fatalf("info event with internal metadata changed: %#v", event.Data)
	}
}

func TestLoginBeginOptionsOnWire(t *testing.T) {
	for _, name := range []string{"legacy", "device-code", "nil"} {
		t.Run(name, func(t *testing.T) {
			clientConn, serverConn := net.Pipe()
			defer clientConn.Close()
			defer serverConn.Close()
			client := jsonrpc2.NewClient(clientConn, clientConn)
			server := jsonrpc2.NewClient(serverConn, serverConn)
			requests := make(chan json.RawMessage, 1)
			server.SetRequestHandler("session.accounts.login.begin", func(params json.RawMessage) (json.RawMessage, *jsonrpc2.Error) {
				requests <- params
				return json.RawMessage(`{"flowId":"flow","step":{"kind":"complete","success":true}}`), nil
			})
			client.Start()
			server.Start()
			defer client.Stop()
			defer server.Stop()
			ctx, cancel := context.WithTimeout(context.Background(), 5*time.Second)
			defer cancel()
			api := NewSessionRPC(client, "login-session").Accounts.Login()
			var err error
			expected := map[string]any{"sessionId": "login-session"}
			switch name {
			case "legacy":
				accountID := "cached-account"
				mode := AuthLoginModeDefault
				_, err = api.Begin(ctx, &AuthLoginBeginRequest{&accountID, LoginProviderKindGitHubDotCom, &mode})
				expected["accountId"] = accountID
				expected["kind"] = "githubDotCom"
				expected["mode"] = "default"
			case "device-code":
				method := AuthLoginMethodDeviceCode
				_, err = api.BeginWithOptions(ctx, &AuthLoginBeginOptions{
					Kind: LoginProviderKindGitHubDotCom, Method: &method,
				})
				expected["kind"] = "githubDotCom"
				expected["method"] = "device-code"
			case "nil":
				_, err = api.Begin(ctx, nil)
			}
			if err != nil {
				t.Fatal(err)
			}
			select {
			case raw := <-requests:
				var actual map[string]any
				if err := json.Unmarshal(raw, &actual); err != nil {
					t.Fatal(err)
				}
				if !reflect.DeepEqual(actual, expected) {
					t.Fatalf("login request = %v, want %v", actual, expected)
				}
			case <-ctx.Done():
				t.Fatal(ctx.Err())
			}
		})
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
