// Copyright (c) Microsoft Corporation. All rights reserved.

package rpc

import (
	"bytes"
	"context"
	"encoding/json"
	"net"
	"slices"
	"testing"
	"time"

	"github.com/github/copilot-sdk/go/internal/jsonrpc2"
)

func TestFeatureFlagConfigPatchValuePresenceOnWire(t *testing.T) {
	for _, test := range []struct {
		name        string
		params      *SessionFeatureFlagsUpdateRequest
		patch       string
		assignments string
	}{
		{
			name:   "values only",
			params: &SessionFeatureFlagsUpdateRequest{ConfigPatch: &FeatureFlagConfigPatch{Values: map[string]any{"debug": true}}},
			patch:  `{"undefinedKeys":[],"values":{"debug":true}}`,
		},
		{
			name:   "empty removal list",
			params: &SessionFeatureFlagsUpdateRequest{ConfigPatch: &FeatureFlagConfigPatch{UndefinedKeys: []string{}, Values: map[string]any{}}},
			patch:  `{"undefinedKeys":[],"values":{}}`,
		},
		{
			name:   "populated removal list",
			params: &SessionFeatureFlagsUpdateRequest{ConfigPatch: &FeatureFlagConfigPatch{UndefinedKeys: []string{"obsolete"}, Values: map[string]any{"debug": false}}},
			patch:  `{"undefinedKeys":["obsolete"],"values":{"debug":false}}`,
		},
		{
			name:   "null values",
			params: &SessionFeatureFlagsUpdateRequest{ConfigPatch: &FeatureFlagConfigPatch{}},
			patch:  `{"undefinedKeys":[],"values":null}`,
		},
		{name: "omitted patch", params: &SessionFeatureFlagsUpdateRequest{}},
		{
			name:        "clear assignments",
			params:      &SessionFeatureFlagsUpdateRequest{Assignments: &FeatureFlagAssignmentsUpdate{}},
			assignments: `{"response":null}`,
		},
	} {
		t.Run(test.name, func(t *testing.T) {
			clientConn, serverConn := net.Pipe()
			defer clientConn.Close()
			defer serverConn.Close()
			client := jsonrpc2.NewClient(clientConn, clientConn)
			server := jsonrpc2.NewClient(serverConn, serverConn)
			requests := make(chan json.RawMessage, 1)
			server.SetRequestHandler("session.featureFlags.update", func(params json.RawMessage) (json.RawMessage, *jsonrpc2.Error) {
				requests <- params
				return json.RawMessage(`{}`), nil
			})
			client.Start()
			server.Start()
			defer client.Stop()
			defer server.Stop()
			ctx, cancel := context.WithTimeout(context.Background(), 5*time.Second)
			defer cancel()
			var originalKeys []string
			var originalValues []byte
			if patch := test.params.ConfigPatch; patch != nil {
				originalKeys = slices.Clone(patch.UndefinedKeys)
				var err error
				originalValues, err = json.Marshal(patch.Values)
				if err != nil {
					t.Fatal(err)
				}
			}
			if _, err := NewSessionRPC(client, "feature-session").FeatureFlags.Update(ctx, test.params); err != nil {
				t.Fatal(err)
			}
			select {
			case raw := <-requests:
				var fields map[string]json.RawMessage
				if err := json.Unmarshal(raw, &fields); err != nil {
					t.Fatal(err)
				}
				if string(fields["sessionId"]) != `"feature-session"` {
					t.Fatalf("wrong session: %s", raw)
				}
				for key, expected := range map[string]string{"configPatch": test.patch, "assignments": test.assignments} {
					got, present := fields[key]
					if present != (expected != "") || string(got) != expected {
						t.Errorf("%s = %s (present %v), want %s; wire params: %s", key, got, present, expected, raw)
					}
				}
			case <-ctx.Done():
				t.Fatal(ctx.Err())
			}
			if patch := test.params.ConfigPatch; patch != nil {
				if (patch.UndefinedKeys == nil) != (originalKeys == nil) || len(patch.UndefinedKeys) != len(originalKeys) {
					t.Errorf("caller removal list changed: got %#v, want %#v", patch.UndefinedKeys, originalKeys)
				}
				for i, key := range originalKeys {
					if i < len(patch.UndefinedKeys) && patch.UndefinedKeys[i] != key {
						t.Errorf("caller removal key %d changed: got %q, want %q", i, patch.UndefinedKeys[i], key)
					}
				}
				values, err := json.Marshal(patch.Values)
				if err != nil {
					t.Fatal(err)
				}
				if !bytes.Equal(values, originalValues) {
					t.Errorf("caller values changed: got %s, want %s", values, originalValues)
				}
			}
		})
	}
}

func TestSubagentSettingsValuePresenceOnWire(t *testing.T) {
	for _, test := range []struct {
		name     string
		settings SubagentSettings
	}{
		{name: "clear"},
		{name: "empty override", settings: &SubagentSettingsConfig{}},
	} {
		t.Run(test.name, func(t *testing.T) {
			clientConn, serverConn := net.Pipe()
			defer clientConn.Close()
			defer serverConn.Close()
			client := jsonrpc2.NewClient(clientConn, clientConn)
			server := jsonrpc2.NewClient(serverConn, serverConn)
			requests := make(chan json.RawMessage, 1)
			server.SetRequestHandler("session.tools.updateSubagentSettings", func(params json.RawMessage) (json.RawMessage, *jsonrpc2.Error) {
				requests <- params
				return json.RawMessage(`{}`), nil
			})
			client.Start()
			server.Start()
			defer client.Stop()
			defer server.Stop()
			ctx, cancel := context.WithTimeout(context.Background(), 5*time.Second)
			defer cancel()
			_, err := NewSessionRPC(client, "settings-session").Tools.UpdateSubagentSettings(ctx, &UpdateSubagentSettingsRequest{
				Subagents: test.settings,
			})
			if err != nil {
				t.Fatal(err)
			}
			select {
			case raw := <-requests:
				var fields map[string]json.RawMessage
				if err := json.Unmarshal(raw, &fields); err != nil {
					t.Fatal(err)
				}
				if string(fields["sessionId"]) != `"settings-session"` {
					t.Fatalf("wrong session: %s", raw)
				}
				value, present := fields["subagents"]
				expected, err := json.Marshal(test.settings)
				if err != nil {
					t.Fatal(err)
				}
				if !present || string(value) != string(expected) {
					t.Fatalf("subagents = %s (present %v), want %s; wire params: %s", value, present, expected, raw)
				}
			case <-ctx.Done():
				t.Fatal(ctx.Err())
			}
		})
	}
}

func TestSettingsValuePresenceOnWire(t *testing.T) {
	for _, method := range []string{"user.settings.applyTopKey", "repo.settings.writeKey", "user.settings.edit"} {
		for _, test := range []struct {
			name    string
			value   any
			present bool
			delete  bool
		}{
			{name: "omitted"},
			{name: "explicit null", value: json.RawMessage("null"), present: true},
			{name: "value", value: "chosen", present: true},
			{name: "delete", delete: true},
		} {
			t.Run(method+"/"+test.name, func(t *testing.T) {
				clientConn, serverConn := net.Pipe()
				defer clientConn.Close()
				defer serverConn.Close()
				client := jsonrpc2.NewClient(clientConn, clientConn)
				server := jsonrpc2.NewClient(serverConn, serverConn)
				requests := make(chan json.RawMessage, 1)
				server.SetRequestHandler(method, func(params json.RawMessage) (json.RawMessage, *jsonrpc2.Error) {
					requests <- params
					return json.RawMessage(`{}`), nil
				})
				client.Start()
				server.Start()
				defer client.Stop()
				defer server.Stop()
				ctx, cancel := context.WithTimeout(context.Background(), 5*time.Second)
				defer cancel()
				api := NewInternalServerRPC(client)
				var err error
				switch method {
				case "user.settings.applyTopKey":
					var value *any
					if test.present {
						value = &test.value
					}
					_, err = api.User.Settings().ApplyTopKey(ctx, &UserSettingsApplyTopKeyRequest{
						CanonicalPath: []string{"model"},
						TopKey:        "model",
						Value:         value,
						Delete:        &test.delete,
					})
				case "repo.settings.writeKey":
					_, err = api.Repo.Settings().WriteKey(ctx, &RepoSettingsWriteKeyRequest{
						RepoRoot: t.TempDir(),
						Scope:    "repo",
						Key:      "model",
						Value:    test.value,
						Delete:   &test.delete,
					})
				case "user.settings.edit":
					operation := SettingsEditOperationSet
					if test.delete {
						operation = SettingsEditOperationRemove
					}
					_, err = api.User.Settings().Edit(ctx, &UserSettingsEditRequest{
						CanonicalPath: []string{"model"},
						Operation:     operation,
						Value:         test.value,
					})
				}
				if err != nil {
					t.Fatal(err)
				}
				select {
				case raw := <-requests:
					var fields map[string]json.RawMessage
					if err := json.Unmarshal(raw, &fields); err != nil {
						t.Fatal(err)
					}
					value, present := fields["value"]
					wantPresent := test.present || (method == "user.settings.edit" && !test.delete)
					if present != wantPresent {
						t.Fatalf("value presence = %v, want %v; wire params: %s", present, wantPresent, raw)
					}
					if present {
						expected, err := json.Marshal(test.value)
						if err != nil {
							t.Fatal(err)
						}
						if string(value) != string(expected) {
							t.Fatalf("value = %s, want %s; wire params: %s", value, expected, raw)
						}
					}
				case <-ctx.Done():
					t.Fatal(ctx.Err())
				}
			})
		}
	}
}
