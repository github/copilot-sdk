// Copyright (c) Microsoft Corporation. All rights reserved.

package rpc

import (
	"context"
	"encoding/json"
	"io"
	"reflect"
	"testing"

	"github.com/github/copilot-sdk/go/internal/jsonrpc2"
)

var _ interface {
	RegisterDirectAutoModeSwitchHandler(context.Context) (*UIRegisterDirectAutoModeSwitchHandlerResult, error)
} = (*UIAPI)(nil)

var _ func(context.Context) (*UIRegisterDirectAutoModeSwitchHandlerResult, error) = (*UIAPI)(nil).RegisterDirectAutoModeSwitchHandler

func TestSessionPrefixLegacyRequestConstruction(t *testing.T) {
	request := SessionsFindByPrefixRequest{"abcdef12"}
	wire, err := json.Marshal(request)
	if err != nil {
		t.Fatal(err)
	}
	if string(wire) != `{"prefix":"abcdef12"}` {
		t.Fatalf("legacy request = %s, want only the prefix", wire)
	}
}

func TestDirectAutoModeSwitchLegacyResultConstruction(t *testing.T) {
	result := UIRegisterDirectAutoModeSwitchHandlerResult{"legacy-handle"}
	wire, err := json.Marshal(result)
	if err != nil {
		t.Fatal(err)
	}
	if string(wire) != `{"handle":"legacy-handle"}` {
		t.Fatalf("legacy result = %s, want only the handle", wire)
	}
}

func TestDirectAutoModeSwitchRegistrationWireCompatibility(t *testing.T) {
	enabled, disabled := true, false
	for _, test := range []struct {
		name     string
		legacy   bool
		params   *SessionUIRegisterDirectAutoModeSwitchHandlerRequest
		response string
		wantAck  *bool
	}{
		{
			name:     "legacy",
			legacy:   true,
			response: `{"handle":"registration","autoModeRecovery":true}`,
		},
		{
			name:     "nil request",
			response: `{"handle":"registration"}`,
		},
		{
			name:     "omitted enrollment",
			params:   &SessionUIRegisterDirectAutoModeSwitchHandlerRequest{},
			response: `{"handle":"registration"}`,
		},
		{
			name:     "disabled enrollment",
			params:   &SessionUIRegisterDirectAutoModeSwitchHandlerRequest{AutoModeRecovery: &disabled},
			response: `{"handle":"registration","autoModeRecovery":false}`,
			wantAck:  &disabled,
		},
		{
			name:     "acknowledged enrollment",
			params:   &SessionUIRegisterDirectAutoModeSwitchHandlerRequest{AutoModeRecovery: &enabled},
			response: `{"handle":"registration","autoModeRecovery":true}`,
			wantAck:  &enabled,
		},
		{
			name:     "older runtime without acknowledgment",
			params:   &SessionUIRegisterDirectAutoModeSwitchHandlerRequest{AutoModeRecovery: &enabled},
			response: `{"handle":"registration"}`,
		},
	} {
		t.Run(test.name, func(t *testing.T) {
			clientToServerReader, clientToServerWriter := io.Pipe()
			serverToClientReader, serverToClientWriter := io.Pipe()
			client := jsonrpc2.NewClient(clientToServerWriter, serverToClientReader)
			server := jsonrpc2.NewClient(serverToClientWriter, clientToServerReader)
			requests := make(chan map[string]any, 1)
			server.SetRequestHandler("session.ui.registerDirectAutoModeSwitchHandler", func(params json.RawMessage) (json.RawMessage, *jsonrpc2.Error) {
				var request map[string]any
				if err := json.Unmarshal(params, &request); err != nil {
					return nil, &jsonrpc2.Error{Code: -32602, Message: err.Error()}
				}
				requests <- request
				return json.RawMessage(test.response), nil
			})
			client.Start()
			server.Start()
			t.Cleanup(func() {
				client.Stop()
				server.Stop()
				_ = clientToServerWriter.Close()
				_ = clientToServerReader.Close()
				_ = serverToClientWriter.Close()
				_ = serverToClientReader.Close()
			})

			ui := NewSessionRPC(client, "bound-session").UI
			if test.legacy {
				register := ui.RegisterDirectAutoModeSwitchHandler
				result, err := register(t.Context())
				if err != nil {
					t.Fatal(err)
				}
				if result.Handle != "registration" {
					t.Fatalf("legacy handle = %q, want registration", result.Handle)
				}
			} else {
				result, err := ui.RegisterDirectAutoModeSwitchHandlerWithAutoModeRecovery(t.Context(), test.params)
				if err != nil {
					t.Fatal(err)
				}
				if result.Handle != "registration" || !reflect.DeepEqual(result.AutoModeRecovery, test.wantAck) {
					t.Fatalf("registration = %#v, want handle registration and acknowledgment %v", result, test.wantAck)
				}
			}
			wantRequest := map[string]any{"sessionId": "bound-session"}
			if test.params != nil && test.params.AutoModeRecovery != nil {
				wantRequest["autoModeRecovery"] = *test.params.AutoModeRecovery
			}
			if request := <-requests; !reflect.DeepEqual(request, wantRequest) {
				t.Fatalf("registration request = %#v, want %#v", request, wantRequest)
			}
		})
	}
}
