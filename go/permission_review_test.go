package copilot

import (
	"bufio"
	"bytes"
	"encoding/json"
	"fmt"
	"net"
	"os/exec"
	"reflect"
	"testing"
	"time"

	"github.com/github/copilot-sdk/go/internal/jsonrpc2"
	"github.com/github/copilot-sdk/go/rpc"
)

func TestPermissionEventSubscriberBeforeSessionReturns(t *testing.T) {
	for _, resume := range []bool{false, true} {
		t.Run(fmt.Sprintf("resume=%t", resume), func(t *testing.T) {
			t.Parallel()
			original := json.RawMessage(`{"kind":"hook","toolName":"review","toolArgs":{"id":9007199254740993},"futureDetails":{"nullable":null}}`)
			observed := make(chan json.RawMessage, 1)
			legacy := make(chan any, 1)
			peers := make(chan *installationConfirmationPeer, 1)
			client, server, _ := newPermissionReviewClient(t, func(server *jsonrpc2.Client) {
				method := "session.create"
				if resume {
					method = "session.resume"
				}
				server.SetRequestHandler(method, func(json.RawMessage) (json.RawMessage, *jsonrpc2.Error) {
					peer := <-peers
					peer.notify("session.event", map[string]any{
						"sessionId": "review-session",
						"event": map[string]any{
							"type": "permission.requested",
							"data": map[string]any{"requestId": "permission-1", "permissionRequest": original},
						},
					})
					select {
					case got := <-observed:
						assertOriginalPermissionRequest(t, got, original)
					case <-time.After(5 * time.Second):
						return nil, &jsonrpc2.Error{Code: -32603, Message: "lossless event handler was not registered before the RPC"}
					}
					return json.RawMessage(`{"sessionId":"review-session"}`), nil
				})
			})
			peers <- server
			handler := func(event SessionEvent) {
				if data, ok := event.Data.(*PermissionRequestedDataWithEnvelope); ok {
					payload, err := json.Marshal(data.Envelope)
					if err != nil {
						t.Error(err)
					}
					observed <- payload
				}
			}
			legacyHandler := func(event SessionEvent) {
				if data, ok := event.Data.(*PermissionRequestedData); ok {
					legacy <- data.PermissionRequest.(*PermissionRequestHook).ToolArgs.(map[string]any)["id"]
				}
			}
			var session *Session
			var err error
			if resume {
				session, err = client.ResumeSession(t.Context(), "review-session", &ResumeSessionConfig{
					OnEvent: legacyHandler, OnEventWithPermissionEnvelope: handler,
				})
			} else {
				session, err = client.CreateSession(t.Context(), &SessionConfig{
					SessionID: "review-session", OnEvent: legacyHandler, OnEventWithPermissionEnvelope: handler,
				})
			}
			if err != nil {
				t.Fatal(err)
			}
			defer session.Disconnect()
			if got := <-legacy; got != float64(9007199254740992) {
				t.Fatalf("legacy event argument = %v (%T), want original float64 decoding", got, got)
			}
		})
	}
}

func TestPermissionEventSubscriberPreservesRequest(t *testing.T) {
	t.Parallel()
	client, server, forwarded := newPermissionReviewClient(t)
	callbackDone := make(chan struct{})
	session, err := client.CreateSessionWithPermissionEnvelope(t.Context(), &SessionConfig{SessionID: "review-session"},
		func(envelope *PermissionRequestEnvelope, _ PermissionHandlerInvocation) (rpc.PermissionDecision, error) {
			request := envelope.Request.(*PermissionRequestHook)
			request.ToolName = "callback-edit"
			request.ToolArgs.(map[string]any)["id"] = json.Number("42")
			close(callbackDone)
			return &rpc.PermissionDecisionNoResult{}, nil
		})
	if err != nil {
		t.Fatal(err)
	}
	done := make(chan error, 1)
	legacy := make(chan any, 2)
	legacyHandler := func(event SessionEvent) {
		data, ok := event.Data.(*PermissionRequestedData)
		if !ok {
			legacy <- fmt.Errorf("legacy event data changed: %T", event.Data)
			return
		}
		legacy <- data.PermissionRequest.(*PermissionRequestHook).ToolArgs.(map[string]any)["id"]
	}
	session.On(legacyHandler)
	session.OnWithAutoModeRecovery(legacyHandler)
	unsubscribe := session.OnWithPermissionEnvelope(func(event SessionEvent) {
		<-callbackDone
		data, ok := event.Data.(*PermissionRequestedDataWithEnvelope)
		if !ok {
			done <- fmt.Errorf("unexpected event data: %T", event.Data)
			return
		}
		_, err := session.RPC.Permissions.ManagedReviewRequired(t.Context(), &rpc.SessionPermissionsManagedReviewRequiredRequest{
			Request: data.Envelope,
		})
		done <- err
	})
	defer unsubscribe()
	original := json.RawMessage(`{"kind":"hook","toolName":"review","toolArgs":{"id":9007199254740993},"futureDetails":{"nullable":null,"id":9007199254740993}}`)
	server.notify("session.event", map[string]any{
		"sessionId": session.SessionID,
		"event": map[string]any{
			"type": "permission.requested",
			"data": map[string]any{"requestId": "permission-1", "permissionRequest": original},
		},
	})
	if err := <-done; err != nil {
		t.Fatal(err)
	}
	assertOriginalPermissionRequest(t, <-forwarded, original)
	for range 2 {
		if value := <-legacy; value != float64(9007199254740992) {
			t.Errorf("legacy handler value = %v (%T), want original float64 decoding", value, value)
		}
	}
}

func TestPermissionReviewPreservesHandlerRequest(t *testing.T) {
	for _, name := range []string{
		"custom-tool", "extension-env-access", "extension-management",
		"extension-permission-access", "hook", "mcp", "memory", "read",
		"shell", "url", "workflow", "write", "future-kind",
		"read-edited", "read-null", "read-null-edited", "read-remove-optional",
		"read-grant-edited", "read-grant-cleared", "read-grant-remove-optional",
		"hook-edited", "shell-commands-edited",
	} {
		t.Run(name, func(t *testing.T) {
			t.Parallel()
			kind := name
			switch name {
			case "read-edited", "read-null", "read-null-edited", "read-remove-optional",
				"read-grant-edited", "read-grant-cleared", "read-grant-remove-optional":
				kind = "read"
			case "hook-edited":
				kind = "hook"
			case "shell-commands-edited":
				kind = "shell"
			}
			original := json.RawMessage(fmt.Sprintf(`{
				"kind": %q,
				"intention": "Review the original request",
				"path": "/workspace/file.txt",
				"managedApprovalRequired": true,
				"futureDetails": {"policy": "future-policy", "number": 9007199254740993, "values": [null, false]}
			}`, kind))
			if name == "read-null" || name == "read-null-edited" {
				original = bytes.Replace(original, []byte(`"Review the original request"`), []byte(`null`), 1)
			}
			var expected map[string]json.RawMessage
			if err := json.Unmarshal(original, &expected); err != nil {
				t.Fatal(err)
			}
			if name == "hook-edited" || name == "shell-commands-edited" {
				field, value := "toolArgs", `{"items":[{"id":9007199254740993,"label":"original"}]}`
				if name == "shell-commands-edited" {
					field, value = "commands", `[{"identifier":null,"readOnly":true,"futureDetails":{"id":9007199254740993,"nullable":null}},{"identifier":"second","readOnly":null,"futureDetails":true}]`
				}
				expected[field] = json.RawMessage(value)
				var err error
				original, err = json.Marshal(expected)
				if err != nil {
					t.Fatal(err)
				}
				if name == "hook-edited" {
					expected[field] = bytes.Replace(expected[field], []byte(`"original"`), []byte(`"edited"`), 1)
				} else {
					expected[field] = bytes.Replace(expected[field], []byte(`"readOnly":true`), []byte(`"readOnly":false`), 1)
				}
			}
			if name == "read-grant-edited" || name == "read-grant-cleared" || name == "read-grant-remove-optional" {
				expected["sandboxPathGrant"] = json.RawMessage(`{"access":"read","path":"/workspace/original","deniedPath":null,"futureGrant":{"number":9007199254740993}}`)
				if name == "read-grant-remove-optional" {
					expected["sandboxPathGrant"] = bytes.Replace(expected["sandboxPathGrant"], []byte(`null`), []byte(`"/workspace/denied"`), 1)
				}
				var err error
				original, err = json.Marshal(expected)
				if err != nil {
					t.Fatal(err)
				}
				switch name {
				case "read-grant-edited":
					expected["sandboxPathGrant"] = bytes.Replace(expected["sandboxPathGrant"], []byte(`"/workspace/original"`), []byte(`"/workspace/changed"`), 1)
				case "read-grant-cleared":
					delete(expected, "sandboxPathGrant")
				case "read-grant-remove-optional":
					expected["sandboxPathGrant"] = json.RawMessage(`{"access":"read","path":"/workspace/original","futureGrant":{"number":9007199254740993}}`)
				}
			}
			if name == "read-edited" || name == "read-null-edited" {
				expected["path"] = json.RawMessage(`"/workspace/changed.txt"`)
				expected["toolCallId"] = json.RawMessage(`"changed-tool-call"`)
			}
			if name == "read-remove-optional" {
				delete(expected, "managedApprovalRequired")
			}
			want, err := json.Marshal(expected)
			if err != nil {
				t.Fatal(err)
			}
			client, server, forwarded := newPermissionReviewClient(t)
			handlerDone := make(chan error, 1)
			marshaled := make(chan json.RawMessage, 1)
			sessionReady := make(chan *Session, 1)
			session, err := client.CreateSessionWithPermissionEnvelope(t.Context(), &SessionConfig{SessionID: "review-session"},
				func(envelope *PermissionRequestEnvelope, invocation PermissionHandlerInvocation) (rpc.PermissionDecision, error) {
					request := envelope.Request
					session := <-sessionReady
					if request.Kind() != rpc.PermissionRequestKind(kind) || !request.RequiresManagedApproval() {
						err := fmt.Errorf("unexpected permission request: %#v", request)
						handlerDone <- err
						return nil, err
					}
					switch name {
					case "hook-edited":
						item := request.(*PermissionRequestHook).ToolArgs.(map[string]any)["items"].([]any)[0].(map[string]any)
						if item["id"] != json.Number("9007199254740993") {
							err := fmt.Errorf("hook argument = %v (%T), want json.Number(9007199254740993)", item["id"], item["id"])
							handlerDone <- err
							return nil, err
						}
						item["label"] = "edited"
					case "shell-commands-edited":
						data, err := json.Marshal(envelope)
						if err != nil {
							handlerDone <- err
							return nil, err
						}
						data = bytes.Replace(data, []byte(`"readOnly":true`), []byte(`"readOnly":false`), 1)
						if err := json.Unmarshal(data, envelope); err != nil {
							handlerDone <- err
							return nil, err
						}
					}
					if kind == "read" {
						read, ok := request.(*PermissionRequestRead)
						if !ok || read.Path != "/workspace/file.txt" {
							err := fmt.Errorf("read request lost its typed fields: %#v", request)
							handlerDone <- err
							return nil, err
						}
						if name == "read-edited" || name == "read-null-edited" {
							read.Path = "/workspace/changed.txt"
							toolCallID := "changed-tool-call"
							read.ToolCallID = &toolCallID
						}
						if name == "read-remove-optional" {
							read.ManagedApprovalRequired = nil
						}
						switch name {
						case "read-grant-edited":
							read.SandboxPathGrant.Path = "/workspace/changed"
						case "read-grant-cleared":
							read.SandboxPathGrant = nil
						case "read-grant-remove-optional":
							read.SandboxPathGrant.DeniedPath = nil
						}
					}
					data, err := json.Marshal(envelope)
					if err != nil {
						handlerDone <- err
						return nil, err
					}
					marshaled <- data
					review, err := session.RPC.Permissions.ManagedReviewRequired(t.Context(), &rpc.SessionPermissionsManagedReviewRequiredRequest{
						Request: envelope,
					})
					if err == nil && (review == nil || !bool(*review)) {
						err = fmt.Errorf("managed review result = %v, want true", review)
					}
					handlerDone <- err
					return &rpc.PermissionDecisionNoResult{}, err
				})
			if err != nil {
				t.Fatal(err)
			}
			sessionReady <- session
			server.notify("session.event", map[string]any{
				"sessionId": session.SessionID,
				"event": map[string]any{
					"type": "permission.requested",
					"data": map[string]any{"requestId": "permission-1", "permissionRequest": original},
				},
			})
			if err := <-handlerDone; err != nil {
				t.Fatal(err)
			}
			assertOriginalPermissionRequest(t, <-marshaled, want)
			assertOriginalPermissionRequest(t, <-forwarded, want)
		})
	}
}

func TestPermissionReviewPreservesCopiedAndEditedEntry(t *testing.T) {
	t.Parallel()
	input := `{"kind":"shell","commands":[{"identifier":"first","readOnly":true,"futurePolicy":"first-policy"},{"identifier":"second","readOnly":true,"futurePolicy":{"id":9007199254740993,"nullable":null}}]}`
	var envelope PermissionRequestEnvelope
	if err := json.Unmarshal([]byte(input), &envelope); err != nil {
		t.Fatal(err)
	}
	original, err := json.Marshal(envelope)
	if err != nil {
		t.Fatal(err)
	}
	var fields map[string]json.RawMessage
	if err := json.Unmarshal(original, &fields); err != nil {
		t.Fatal(err)
	}
	var commands []json.RawMessage
	if err := json.Unmarshal(fields["commands"], &commands); err != nil {
		t.Fatal(err)
	}
	var source map[string]json.RawMessage
	if err := json.Unmarshal(commands[1], &source); err != nil {
		t.Fatal(err)
	}
	source["readOnly"] = json.RawMessage(`false`)
	commands[0], err = json.Marshal(source)
	if err != nil {
		t.Fatal(err)
	}
	fields["commands"], err = json.Marshal(commands)
	if err != nil {
		t.Fatal(err)
	}
	replacement, err := json.Marshal(fields)
	if err != nil {
		t.Fatal(err)
	}
	if err := json.Unmarshal(replacement, &envelope); err != nil {
		t.Fatal(err)
	}
	client, _, forwarded := newPermissionReviewClient(t)
	session, err := client.CreateSession(t.Context(), &SessionConfig{SessionID: "review-session"})
	if err != nil {
		t.Fatal(err)
	}
	_, err = session.RPC.Permissions.ManagedReviewRequired(t.Context(), &rpc.SessionPermissionsManagedReviewRequiredRequest{
		Request: &envelope,
	})
	if err != nil {
		t.Fatal(err)
	}
	expected := json.RawMessage(`{"kind":"shell","commands":[{"identifier":"second","readOnly":false,"futurePolicy":{"id":9007199254740993,"nullable":null}},{"identifier":"second","readOnly":true,"futurePolicy":{"id":9007199254740993,"nullable":null}}]}`)
	assertOriginalPermissionRequest(t, <-forwarded, expected)
}

func TestPermissionReviewPreservesExplicitRequest(t *testing.T) {
	for _, tc := range []struct {
		name    string
		request any
		want    string
	}{
		{
			name:    "typed",
			request: &PermissionRequestRead{Intention: "Read a file", Path: "/workspace/file.txt"},
			want:    `{"kind":"read","intention":"Read a file","path":"/workspace/file.txt"}`,
		},
		{
			name:    "typed value",
			request: PermissionRequestRead{Intention: "Read a file", Path: "/workspace/file.txt"},
			want:    `{"kind":"read","intention":"Read a file","path":"/workspace/file.txt"}`,
		},
		{
			name:    "raw JSON",
			request: json.RawMessage(`{"kind":"read","futureDetails":{"policy":"future"}}`),
			want:    `{"kind":"read","futureDetails":{"policy":"future"}}`,
		},
		{
			name:    "raw permission",
			request: &rpc.RawPermissionRequest{Raw: json.RawMessage(`{"kind":"future-kind","futureDetails":true}`)},
			want:    `{"kind":"future-kind","futureDetails":true}`,
		},
		{
			name:    "legacy raw discriminator",
			request: rpc.RawPermissionRequest{Discriminator: rpc.PermissionRequestKindRead},
			want:    `{"kind":"read"}`,
		},
	} {
		t.Run(tc.name, func(t *testing.T) {
			t.Parallel()
			client, _, forwarded := newPermissionReviewClient(t)
			session, err := client.CreateSession(t.Context(), &SessionConfig{SessionID: "review-session"})
			if err != nil {
				t.Fatal(err)
			}
			result, err := session.RPC.Permissions.ManagedReviewRequired(t.Context(), &rpc.SessionPermissionsManagedReviewRequiredRequest{Request: tc.request})
			if err != nil {
				t.Fatal(err)
			}
			if result == nil || !bool(*result) {
				t.Fatalf("managed review result = %v, want true", result)
			}
			assertOriginalPermissionRequest(t, <-forwarded, json.RawMessage(tc.want))
		})
	}
}

func TestPermissionRequestExternalCompatibility(t *testing.T) {
	command := exec.CommandContext(t.Context(), "go", "run", "./testdata/permission_compat")
	output, err := command.CombinedOutput()
	if err != nil {
		t.Fatalf("external compatibility client failed: %v\n%s", err, output)
	}
	if !bytes.Contains(output, []byte("External positional literals, equality, and map keys work.")) {
		t.Fatalf("external client did not exercise compatibility:\n%s", output)
	}
}

func TestPermissionEnvelopeNestedRemovalAndReplacement(t *testing.T) {
	var envelope PermissionRequestEnvelope
	original := `{"kind":"custom-tool","toolName":"review","toolDescription":"Review a request","args":{"keep":9007199254740993,"remove":"old","items":[1,2],"object":{"before":true}},"futureDetails":true}`
	if err := json.Unmarshal([]byte(original), &envelope); err != nil {
		t.Fatal(err)
	}
	args := envelope.Request.(*PermissionRequestCustomTool).Args.(map[string]any)
	delete(args, "remove")
	args["items"] = []any{json.Number("3")}
	args["object"] = "replacement"
	data, err := json.Marshal(envelope)
	if err != nil {
		t.Fatal(err)
	}
	want := json.RawMessage(`{"kind":"custom-tool","toolName":"review","toolDescription":"Review a request","args":{"keep":9007199254740993,"items":[3],"object":"replacement"},"futureDetails":true}`)
	assertOriginalPermissionRequest(t, data, want)
}

func TestPermissionEnvelopeAndLegacyCallbacks(t *testing.T) {
	for _, resume := range []bool{false, true} {
		for _, mode := range []string{"legacy", "prompt", "envelope", "nil-envelope"} {
			t.Run(fmt.Sprintf("%s/resume=%t", mode, resume), func(t *testing.T) {
				t.Parallel()
				client, server, _ := newPermissionReviewClient(t)
				observed := make(chan any, 1)
				legacy := func(request PermissionRequest, invocation PermissionInvocation) (rpc.PermissionDecision, error) {
					observed <- request
					return &rpc.PermissionDecisionNoResult{}, nil
				}
				prompt := func(request PermissionRequest, invocation PermissionHandlerInvocation) (rpc.PermissionDecision, error) {
					return legacy(request, invocation.PermissionInvocation)
				}
				envelope := func(request *PermissionRequestEnvelope, invocation PermissionHandlerInvocation) (rpc.PermissionDecision, error) {
					observed <- request
					return &rpc.PermissionDecisionNoResult{}, nil
				}
				create := &SessionConfig{SessionID: "review-session"}
				restore := &ResumeSessionConfig{}
				switch mode {
				case "legacy", "nil-envelope":
					create.OnPermissionRequest, restore.OnPermissionRequest = legacy, legacy
				case "prompt":
					create.OnPermissionRequestWithPrompt, restore.OnPermissionRequestWithPrompt = prompt, prompt
				}
				var session *Session
				var err error
				if mode == "envelope" || mode == "nil-envelope" {
					var handler PermissionHandlerWithEnvelopeFunc
					if mode == "envelope" {
						handler = envelope
					}
					if resume {
						session, err = client.ResumeSessionWithPermissionEnvelope(t.Context(), "review-session", restore, handler)
					} else {
						session, err = client.CreateSessionWithPermissionEnvelope(t.Context(), create, handler)
					}
				} else if resume {
					session, err = client.ResumeSession(t.Context(), "review-session", restore)
				} else {
					session, err = client.CreateSession(t.Context(), create)
				}
				if err != nil {
					t.Fatal(err)
				}
				original := json.RawMessage(`{"kind":"read","intention":"Read a file","path":"/workspace/file.txt","futureDetails":9007199254740993}`)
				server.notify("session.event", map[string]any{
					"sessionId": session.SessionID,
					"event": map[string]any{
						"type": "permission.requested",
						"data": map[string]any{"requestId": "permission-1", "permissionRequest": original},
					},
				})
				request := <-observed
				var typed PermissionRequest
				want := original
				if mode == "envelope" {
					typed = request.(*PermissionRequestEnvelope).Request
				} else {
					typed = request.(PermissionRequest)
					want = json.RawMessage(`{"kind":"read","intention":"Read a file","path":"/workspace/file.txt"}`)
				}
				if read, ok := typed.(*PermissionRequestRead); !ok || read.Path != "/workspace/file.txt" {
					t.Fatalf("callback lost the concrete read request: %#v", typed)
				}
				data, err := json.Marshal(request)
				if err != nil {
					t.Fatal(err)
				}
				assertOriginalPermissionRequest(t, data, want)

				events := make(chan PermissionRequest, 1)
				session.On(func(event SessionEvent) {
					if permission, ok := event.Data.(*PermissionRequestedData); ok && permission.RequestID == "permission-2" {
						events <- permission.PermissionRequest
					}
				})
				original = json.RawMessage(`{"kind":"custom-tool","toolName":"review","toolDescription":"Review a request","args":{"count":42,"id":9007199254740993}}`)
				server.notify("session.event", map[string]any{
					"sessionId": session.SessionID,
					"event": map[string]any{
						"type": "permission.requested",
						"data": map[string]any{"requestId": "permission-2", "permissionRequest": original},
					},
				})
				request = <-observed
				eventRequest := <-events
				client.ForceStop()
				var wantCount any = float64(42)
				if mode == "envelope" {
					typed = request.(*PermissionRequestEnvelope).Request
					wantCount = json.Number("42")
				} else {
					typed = request.(PermissionRequest)
				}
				args := typed.(*PermissionRequestCustomTool).Args.(map[string]any)
				if args["count"] != wantCount {
					t.Errorf("callback Args.count = %v (%T), want %v (%T)", args["count"], args["count"], wantCount, wantCount)
				}
				if mode == "envelope" && args["id"] != json.Number("9007199254740993") {
					t.Errorf("envelope Args.id = %v (%T), want json.Number(9007199254740993)", args["id"], args["id"])
				}
				eventArgs := eventRequest.(*PermissionRequestCustomTool).Args.(map[string]any)
				if eventArgs["count"] != float64(42) {
					t.Errorf("event Args.count = %v (%T), want float64(42)", eventArgs["count"], eventArgs["count"])
				}
			})
		}
	}
}

func TestPermissionEnvelopeHandlerSelection(t *testing.T) {
	for _, resume := range []bool{false, true} {
		t.Run(fmt.Sprintf("resume=%t", resume), func(t *testing.T) {
			t.Parallel()
			client, requests, cleanup := newInMemoryClient(t)
			defer cleanup()
			var observed *PermissionRequestEnvelope
			var observedInvocation PermissionHandlerInvocation
			handler := func(request *PermissionRequestEnvelope, invocation PermissionHandlerInvocation) (rpc.PermissionDecision, error) {
				observed = request
				observedInvocation = invocation
				return &rpc.PermissionDecisionNoResult{}, nil
			}
			legacy := func(PermissionRequest, PermissionInvocation) (rpc.PermissionDecision, error) {
				t.Error("legacy handler replaced the explicit envelope handler")
				return &rpc.PermissionDecisionNoResult{}, nil
			}
			prompt := func(PermissionRequest, PermissionHandlerInvocation) (rpc.PermissionDecision, error) {
				t.Error("prompt handler replaced the explicit envelope handler")
				return &rpc.PermissionDecisionNoResult{}, nil
			}
			var session *Session
			var err error
			method := "session.create"
			if resume {
				method = "session.resume"
				config := &ResumeSessionConfig{
					OnPermissionRequest: legacy, OnPermissionRequestWithPrompt: prompt,
				}
				session, err = client.ResumeSessionWithPermissionEnvelope(t.Context(), "review-session", config, handler)
				if reflect.ValueOf(config.OnPermissionRequest).Pointer() != reflect.ValueOf(legacy).Pointer() ||
					reflect.ValueOf(config.OnPermissionRequestWithPrompt).Pointer() != reflect.ValueOf(prompt).Pointer() {
					t.Fatal("resume changed caller-owned handlers")
				}
			} else {
				config := &SessionConfig{
					OnPermissionRequest: legacy, OnPermissionRequestWithPrompt: prompt,
				}
				session, err = client.CreateSessionWithPermissionEnvelope(t.Context(), config, handler)
				if reflect.ValueOf(config.OnPermissionRequest).Pointer() != reflect.ValueOf(legacy).Pointer() ||
					reflect.ValueOf(config.OnPermissionRequestWithPrompt).Pointer() != reflect.ValueOf(prompt).Pointer() {
					t.Fatal("create changed caller-owned handlers")
				}
			}
			if err != nil {
				t.Fatal(err)
			}
			request, ok := findRequest(requests.snapshot(), method)
			if !ok || request.Params["requestPermission"] != true {
				t.Fatalf("envelope handler did not register permission interest: %+v", requests.snapshot())
			}
			envelope := &PermissionRequestEnvelope{Request: &PermissionRequestRead{Path: "/workspace/file.txt"}}
			promptRequest := &rpc.PermissionPromptRequestRead{}
			assessment := &rpc.AssistedApprovalAssessmentCompleted{}
			invocation := PermissionInvocation{SessionID: "context-session", ManagedSettingsEnabled: true}
			_, err = session.getPermissionHandler()(envelope.Request, invocation, promptRequest, assessment, envelope)
			if err != nil || observed != envelope {
				t.Fatalf("envelope handler not selected: observed=%p want=%p err=%v", observed, envelope, err)
			}
			if observedInvocation.PermissionInvocation != invocation || observedInvocation.PromptRequest != promptRequest ||
				observedInvocation.AssistedApprovalAssessment != assessment {
				t.Fatalf("envelope handler lost invocation context: %#v", observedInvocation)
			}
		})
	}
}

func TestPermissionEnvelopeAndMCPAuthRuntimeRefresh(t *testing.T) {
	for _, resume := range []bool{false, true} {
		for _, envelopeEnabled := range []bool{false, true} {
			for _, refreshEnabled := range []bool{false, true} {
				t.Run(fmt.Sprintf("resume=%t/envelope=%t/refresh=%t", resume, envelopeEnabled, refreshEnabled), func(t *testing.T) {
					t.Parallel()
					authResults := make(chan json.RawMessage, 1)
					client, peer, forwarded := newPermissionReviewClient(t, func(server *jsonrpc2.Client) {
						server.SetRequestHandler("session.eventLog.registerInterest", func(json.RawMessage) (json.RawMessage, *jsonrpc2.Error) {
							return json.RawMessage(`{}`), nil
						})
						server.SetRequestHandler("session.mcp.oauth.handlePendingRequest", func(params json.RawMessage) (json.RawMessage, *jsonrpc2.Error) {
							authResults <- params
							return json.RawMessage(`{}`), nil
						})
					})
					permissionCalls := make(chan string, 1)
					authCalls := make(chan string, 1)
					sessionReady := make(chan *Session, 1)
					forwardPermission := func(request any, sessionID string) (rpc.PermissionDecision, error) {
						session := <-sessionReady
						_, err := session.RPC.Permissions.ManagedReviewRequired(t.Context(), &rpc.SessionPermissionsManagedReviewRequiredRequest{Request: request})
						permissionCalls <- sessionID
						return &rpc.PermissionDecisionNoResult{}, err
					}
					legacyPermission := func(PermissionRequest, PermissionInvocation) (rpc.PermissionDecision, error) {
						t.Error("legacy handler replaced the prompt or envelope handler")
						return &rpc.PermissionDecisionNoResult{}, nil
					}
					promptPermission := func(request PermissionRequest, invocation PermissionHandlerInvocation) (rpc.PermissionDecision, error) {
						if _, ok := request.(*PermissionRequestRead); !ok {
							t.Errorf("legacy callback received a wrapper: %T", request)
						}
						return forwardPermission(request, invocation.SessionID)
					}
					var permission PermissionHandlerWithEnvelopeFunc
					if envelopeEnabled {
						permission = func(request *PermissionRequestEnvelope, invocation PermissionHandlerInvocation) (rpc.PermissionDecision, error) {
							return forwardPermission(request, invocation.SessionID)
						}
					}
					var refresh MCPAuthRuntimeRefreshHandler
					if refreshEnabled {
						refresh = func(request MCPAuthRequest, invocation MCPAuthInvocation, supported bool) (*MCPAuthResult, error) {
							if !supported || request.Reason != MCPOauthRequestReasonRefresh {
								t.Errorf("runtime refresh capability lost: supported=%t request=%+v", supported, request)
							}
							authCalls <- "refresh:" + invocation.SessionID
							return MCPAuthResultRuntimeRefresh(), nil
						}
					}
					legacyAuth := func(_ MCPAuthRequest, invocation MCPAuthInvocation) (*MCPAuthResult, error) {
						authCalls <- "legacy:" + invocation.SessionID
						return MCPAuthResultCancelled(), nil
					}
					var session *Session
					var err error
					if resume {
						config := &ResumeSessionConfig{OnMCPAuthRequest: legacyAuth, OnPermissionRequest: legacyPermission, OnPermissionRequestWithPrompt: promptPermission}
						before := *config
						session, err = client.ResumeSessionWithPermissionEnvelopeAndMCPAuthRuntimeRefresh(t.Context(), "review-session", config, permission, refresh)
						assertPermissionConfigUnchanged(t, before, *config)
					} else {
						config := &SessionConfig{SessionID: "review-session", OnMCPAuthRequest: legacyAuth, OnPermissionRequest: legacyPermission, OnPermissionRequestWithPrompt: promptPermission}
						before := *config
						session, err = client.CreateSessionWithPermissionEnvelopeAndMCPAuthRuntimeRefresh(t.Context(), config, permission, refresh)
						assertPermissionConfigUnchanged(t, before, *config)
					}
					if err != nil {
						t.Fatal(err)
					}
					sessionReady <- session
					original := json.RawMessage(`{"kind":"read","intention":"Read a file","path":"/workspace/file.txt","futureDetails":9007199254740993}`)
					peer.notify("session.event", map[string]any{
						"sessionId": session.SessionID,
						"event": map[string]any{"type": "permission.requested", "data": map[string]any{
							"requestId": "permission-1", "permissionRequest": original,
						}},
					})
					want := original
					if !envelopeEnabled {
						want = json.RawMessage(`{"kind":"read","intention":"Read a file","path":"/workspace/file.txt"}`)
					}
					assertOriginalPermissionRequest(t, <-forwarded, want)
					if got := <-permissionCalls; got != session.SessionID {
						t.Fatalf("permission callback session=%s want=%s", got, session.SessionID)
					}
					peer.notify("session.event", map[string]any{
						"sessionId": session.SessionID,
						"event": map[string]any{"type": "mcp.oauth_required", "data": map[string]any{
							"requestId": "auth-1", "serverName": "oauth-server", "serverUrl": "https://example.com/mcp",
							"reason": "refresh", "runtimeRefreshSupported": true,
						}},
					})
					wantKind, wantHandler := "cancelled", "legacy:"
					if refreshEnabled {
						wantKind, wantHandler = "runtimeRefresh", "refresh:"
					}
					assertOriginalPermissionRequest(t, <-authResults, json.RawMessage(fmt.Sprintf(`{"sessionId":"review-session","requestId":"auth-1","result":{"kind":%q}}`, wantKind)))
					select {
					case got := <-authCalls:
						if got != wantHandler+session.SessionID {
							t.Fatalf("MCP callback=%s want=%s", got, wantHandler+session.SessionID)
						}
					default:
						t.Error("the runtime-refresh callback did not run on the permission-envelope session")
					}
				})
			}
		}
	}
}

func TestPermissionEnvelopeAndMCPAuthRuntimeRefreshComposition(t *testing.T) {
	for _, mode := range []string{"create", "cold resume", "warm resume"} {
		for _, callbacks := range []string{"both", "envelope only", "refresh only", "legacy"} {
			t.Run(mode+"/"+callbacks, func(t *testing.T) {
				t.Parallel()
				client, requests, cleanup := newInMemoryClient(t)
				defer cleanup()

				var observedPermission any
				legacyPermission := func(request PermissionRequest, _ PermissionInvocation) (rpc.PermissionDecision, error) {
					observedPermission = request
					return &rpc.PermissionDecisionNoResult{}, nil
				}
				var envelopeHandler PermissionHandlerWithEnvelopeFunc
				if callbacks == "both" || callbacks == "envelope only" {
					envelopeHandler = func(request *PermissionRequestEnvelope, _ PermissionHandlerInvocation) (rpc.PermissionDecision, error) {
						observedPermission = request
						return &rpc.PermissionDecisionNoResult{}, nil
					}
				}
				legacyAuthCalls, refreshCalls := 0, 0
				legacyAuth := func(MCPAuthRequest, MCPAuthInvocation) (*MCPAuthResult, error) {
					legacyAuthCalls++
					return MCPAuthResultCancelled(), nil
				}
				var refreshHandler MCPAuthRuntimeRefreshHandler
				if callbacks == "both" || callbacks == "refresh only" {
					refreshHandler = func(request MCPAuthRequest, invocation MCPAuthInvocation, supported bool) (*MCPAuthResult, error) {
						refreshCalls++
						if request.RequestID != "refresh-request" || invocation.SessionID != "review-session" || !supported {
							t.Errorf("incorrect refresh invocation: %+v, %+v, supported=%t", request, invocation, supported)
						}
						return MCPAuthResultRuntimeRefresh(), nil
					}
				}

				var session *Session
				var err error
				method := "session.create"
				if mode == "create" {
					session, err = client.CreateSessionWithPermissionEnvelopeAndMCPAuthRuntimeRefresh(t.Context(), &SessionConfig{
						SessionID: "review-session", OnPermissionRequest: legacyPermission, OnMCPAuthRequest: legacyAuth,
					}, envelopeHandler, refreshHandler)
				} else {
					method = "session.resume"
					if mode == "warm resume" {
						previous, err := client.CreateSession(t.Context(), &SessionConfig{
							SessionID: "review-session", OnPermissionRequest: legacyPermission, OnMCPAuthRequest: legacyAuth,
						})
						if err != nil {
							t.Fatal(err)
						}
						defer previous.Disconnect()
						requests.clear()
					}
					session, err = client.ResumeSessionWithPermissionEnvelopeAndMCPAuthRuntimeRefresh(t.Context(), "review-session", &ResumeSessionConfig{
						OnPermissionRequest: legacyPermission, OnMCPAuthRequest: legacyAuth,
					}, envelopeHandler, refreshHandler)
				}
				if err != nil {
					t.Fatal(err)
				}
				defer session.Disconnect()
				initialization, ok := findRequest(requests.snapshot(), method)
				if !ok || initialization.Params["requestPermission"] != true {
					t.Fatalf("missing permission registration: %+v", requests.snapshot())
				}
				interest, ok := findRequest(requests.snapshot(), "session.eventLog.registerInterest")
				if !ok {
					t.Fatalf("missing MCP auth registration: %+v", requests.snapshot())
				}
				assertMCPAuthInterest(t, interest)

				original := json.RawMessage(`{"kind":"read","intention":"Review a file","path":"/workspace/file.txt","futureDetails":9007199254740993}`)
				var envelope PermissionRequestEnvelope
				if err := json.Unmarshal(original, &envelope); err != nil {
					t.Fatal(err)
				}
				_, err = session.getPermissionHandler()(envelope.Request, PermissionInvocation{SessionID: session.SessionID}, nil, nil, &envelope)
				if err != nil {
					t.Fatal(err)
				}
				data, err := json.Marshal(observedPermission)
				if err != nil {
					t.Fatal(err)
				}
				want := original
				if envelopeHandler == nil {
					want = json.RawMessage(`{"kind":"read","intention":"Review a file","path":"/workspace/file.txt"}`)
				}
				assertOriginalPermissionRequest(t, data, want)

				session.handleBroadcastEvent(SessionEvent{Data: &MCPOauthRequiredData{
					RequestID: "refresh-request", Reason: rpc.MCPOauthRequestReasonRefresh, RuntimeRefreshSupported: Bool(true),
				}}, nil)
				wantKind, wantRefreshCalls, wantLegacyCalls := "cancelled", 0, 1
				if refreshHandler != nil {
					wantKind, wantRefreshCalls, wantLegacyCalls = "runtimeRefresh", 1, 0
				}
				result, ok := findRequest(requests.snapshot(), "session.mcp.oauth.handlePendingRequest")
				if !ok || refreshCalls != wantRefreshCalls || legacyAuthCalls != wantLegacyCalls ||
					result.Params["result"].(map[string]any)["kind"] != wantKind {
					t.Fatalf("MCP auth dispatch: refresh=%d legacy=%d want kind=%s; requests=%+v",
						refreshCalls, legacyAuthCalls, wantKind, requests.snapshot())
				}
			})
		}
	}
}

func assertPermissionConfigUnchanged(t *testing.T, before, after any) {
	t.Helper()
	a, b := reflect.ValueOf(before), reflect.ValueOf(after)
	for i := 0; i < a.NumField(); i++ {
		x, y := a.Field(i), b.Field(i)
		equal := false
		if x.Kind() == reflect.Func {
			equal = x.Pointer() == y.Pointer()
		} else {
			equal = reflect.DeepEqual(x.Interface(), y.Interface())
		}
		if !equal {
			t.Errorf("caller-owned config field changed: %s", a.Type().Field(i).Name)
		}
	}
}

func newPermissionReviewClient(t *testing.T, setup ...func(*jsonrpc2.Client)) (*Client, *installationConfirmationPeer, <-chan json.RawMessage) {
	t.Helper()
	listener, err := net.Listen("tcp", "127.0.0.1:0")
	if err != nil {
		t.Fatal(err)
	}
	t.Cleanup(func() { listener.Close() })
	ready := make(chan struct {
		server *jsonrpc2.Client
		peer   *installationConfirmationPeer
	}, 1)
	forwarded := make(chan json.RawMessage, 1)
	go func() {
		conn, err := listener.Accept()
		if err != nil {
			t.Errorf("accept SDK connection: %v", err)
			return
		}
		server := jsonrpc2.NewClient(conn, conn)
		server.SetRequestHandler("connect", func(json.RawMessage) (json.RawMessage, *jsonrpc2.Error) {
			return json.RawMessage(`{"ok":true,"protocolVersion":3,"version":"test"}`), nil
		})
		server.SetRequestHandler("plugins.builtin.set", func(json.RawMessage) (json.RawMessage, *jsonrpc2.Error) {
			return json.RawMessage(`{}`), nil
		})
		server.SetRequestHandler("session.create", func(json.RawMessage) (json.RawMessage, *jsonrpc2.Error) {
			return json.RawMessage(`{"sessionId":"review-session"}`), nil
		})
		server.SetRequestHandler("session.resume", func(json.RawMessage) (json.RawMessage, *jsonrpc2.Error) {
			return json.RawMessage(`{"sessionId":"review-session"}`), nil
		})
		server.SetRequestHandler("session.permissions.managedReviewRequired", func(params json.RawMessage) (json.RawMessage, *jsonrpc2.Error) {
			var request struct {
				SessionID string          `json:"sessionId"`
				Request   json.RawMessage `json:"request"`
			}
			if err := json.Unmarshal(params, &request); err != nil {
				return nil, &jsonrpc2.Error{Code: -32602, Message: err.Error()}
			}
			if request.SessionID != "review-session" {
				return nil, &jsonrpc2.Error{Code: -32602, Message: "incorrect sessionId"}
			}
			forwarded <- request.Request
			return json.RawMessage(`true`), nil
		})
		for _, configure := range setup {
			configure(server)
		}
		server.Start()
		ready <- struct {
			server *jsonrpc2.Client
			peer   *installationConfirmationPeer
		}{server, &installationConfirmationPeer{t: t, conn: conn, reader: bufio.NewReader(conn)}}
	}()
	client := NewClient(&ClientOptions{Connection: URIConnection{URL: listener.Addr().String()}})
	var server *jsonrpc2.Client
	t.Cleanup(func() {
		client.ForceStop()
		if server != nil {
			server.Stop()
		}
	})
	if err := client.Start(t.Context()); err != nil {
		t.Fatal(err)
	}
	transport := <-ready
	server = transport.server
	return client, transport.peer, forwarded
}

func assertOriginalPermissionRequest(t *testing.T, got, want json.RawMessage) {
	t.Helper()
	var gotValue, wantValue any
	gotDecoder := json.NewDecoder(bytes.NewReader(got))
	gotDecoder.UseNumber()
	if err := gotDecoder.Decode(&gotValue); err != nil {
		t.Fatal(err)
	}
	wantDecoder := json.NewDecoder(bytes.NewReader(want))
	wantDecoder.UseNumber()
	if err := wantDecoder.Decode(&wantValue); err != nil {
		t.Fatal(err)
	}
	if !reflect.DeepEqual(gotValue, wantValue) {
		t.Errorf("permission request differs:\ngot:  %s\nwant: %s", got, want)
	}
}
