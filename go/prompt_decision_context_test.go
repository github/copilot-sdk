package copilot

import (
	"encoding/json"
	"fmt"
	"reflect"
	"testing"
	"time"

	"github.com/github/copilot-sdk/go/internal/jsonrpc2"
	"github.com/github/copilot-sdk/go/rpc"
)

// personAnsweredContext is the label a host attaches when a person answered a
// prompt interactively.
func personAnsweredContext() *PermissionDecisionContext {
	interactive := PermissionResponseCapabilityInteractive
	return &PermissionDecisionContext{
		Source:             PermissionDecisionSourceHumanResponse,
		ResponseCapability: &interactive,
		Surface:            PermissionDecisionSurfaceSDK,
		Outcome:            PermissionDecisionOutcomePromptedUser,
	}
}

const personAnsweredContextJSON = `{"source":"human_response","responseCapability":"interactive","surface":"sdk","outcome":"prompted_user"}`

// assertWireJSON compares JSON documents structurally, so key order does not
// matter but any extra, missing or misplaced key does.
func assertWireJSON(t *testing.T, label string, got []byte, want string) {
	t.Helper()
	var gotValue, wantValue any
	if err := json.Unmarshal(got, &gotValue); err != nil {
		t.Fatalf("%s: decode actual JSON %s: %v", label, got, err)
	}
	if err := json.Unmarshal([]byte(want), &wantValue); err != nil {
		t.Fatalf("%s: decode expected JSON %s: %v", label, want, err)
	}
	if !reflect.DeepEqual(gotValue, wantValue) {
		t.Errorf("%s:\n got %s\nwant %s", label, got, want)
	}
}

func TestUserInputReplyCarriesDecisionContextOnlyWhenLabeled(t *testing.T) {
	answer := UserInputResponse{Answer: "Only src/app.ts", WasFreeform: true}
	for _, tc := range []struct {
		name     string
		register func(*Session)
		want     string
	}{
		{
			name: "person answered",
			register: func(s *Session) {
				s.registerAttributedUserInputHandler(func(UserInputRequest, UserInputInvocation) (AttributedUserInputResponse, error) {
					return AttributedUserInputResponse{UserInputResponse: answer, DecisionContext: personAnsweredContext()}, nil
				})
			},
			want: `{"answer":"Only src/app.ts","wasFreeform":true,"decisionContext":` + personAnsweredContextJSON + `}`,
		},
		{
			name: "attributed handler without a label",
			register: func(s *Session) {
				s.registerAttributedUserInputHandler(func(UserInputRequest, UserInputInvocation) (AttributedUserInputResponse, error) {
					return AttributedUserInputResponse{UserInputResponse: answer}, nil
				})
			},
			want: `{"answer":"Only src/app.ts","wasFreeform":true}`,
		},
		{
			name: "plain handler",
			register: func(s *Session) {
				s.registerUserInputHandler(func(UserInputRequest, UserInputInvocation) (UserInputResponse, error) {
					return answer, nil
				})
			},
			want: `{"answer":"Only src/app.ts","wasFreeform":true}`,
		},
	} {
		t.Run(tc.name, func(t *testing.T) {
			session := &Session{SessionID: "s1"}
			client := &Client{sessions: map[string]*Session{"s1": session}}
			tc.register(session)

			handler := jsonrpc2.RequestHandlerFor(client.handleUserInputRequest)
			reply, rpcErr := handler(json.RawMessage(`{"sessionId":"s1","question":"Which files?","requestId":"req-user-1"}`))
			if rpcErr != nil {
				t.Fatalf("unexpected RPC error: %v", rpcErr)
			}
			assertWireJSON(t, "userInput.request reply", reply, tc.want)
		})
	}
}

func TestExitPlanModeReplyCarriesDecisionContextOnlyWhenLabeled(t *testing.T) {
	review := ExitPlanModeResult{Approved: true, SelectedAction: "interactive"}
	for _, tc := range []struct {
		name     string
		register func(*Session)
		want     string
	}{
		{
			name: "person answered",
			register: func(s *Session) {
				s.registerAttributedExitPlanModeHandler(func(ExitPlanModeRequest, ExitPlanModeInvocation) (AttributedExitPlanModeResult, error) {
					return AttributedExitPlanModeResult{ExitPlanModeResult: review, DecisionContext: personAnsweredContext()}, nil
				})
			},
			want: `{"approved":true,"selectedAction":"interactive","decisionContext":` + personAnsweredContextJSON + `}`,
		},
		{
			name: "attributed handler without a label",
			register: func(s *Session) {
				s.registerAttributedExitPlanModeHandler(func(ExitPlanModeRequest, ExitPlanModeInvocation) (AttributedExitPlanModeResult, error) {
					return AttributedExitPlanModeResult{ExitPlanModeResult: review}, nil
				})
			},
			want: `{"approved":true,"selectedAction":"interactive"}`,
		},
		{
			name: "plain handler",
			register: func(s *Session) {
				s.registerExitPlanModeHandler(func(ExitPlanModeRequest, ExitPlanModeInvocation) (ExitPlanModeResult, error) {
					return review, nil
				})
			},
			want: `{"approved":true,"selectedAction":"interactive"}`,
		},
	} {
		t.Run(tc.name, func(t *testing.T) {
			session := &Session{SessionID: "s1"}
			client := &Client{sessions: map[string]*Session{"s1": session}}
			tc.register(session)

			handler := jsonrpc2.RequestHandlerFor(client.handleExitPlanModeRequest)
			reply, rpcErr := handler(json.RawMessage(`{"sessionId":"s1","summary":"Plan","actions":["interactive","autopilot"],"recommendedAction":"autopilot","requestId":"req-plan-1"}`))
			if rpcErr != nil {
				t.Fatalf("unexpected RPC error: %v", rpcErr)
			}
			assertWireJSON(t, "exitPlanMode.request reply", reply, tc.want)
		})
	}
}

func TestElicitationAnswerSendsDecisionContextBesideResult(t *testing.T) {
	accepted := ElicitationResult{
		Action:  ElicitationActionAccept,
		Content: map[string]ElicitationFieldValue{"region": "us-east"},
	}
	const unlabeled = `{"sessionId":"session-1","requestId":"elicitation-1","result":{"action":"accept","content":{"region":"us-east"}}}`
	for _, tc := range []struct {
		name     string
		register func(*Session)
		want     string
	}{
		{
			name: "person answered",
			register: func(s *Session) {
				s.registerAttributedElicitationHandler(func(ElicitationContext) (AttributedElicitationResult, error) {
					return AttributedElicitationResult{ElicitationResult: accepted, DecisionContext: personAnsweredContext()}, nil
				})
			},
			want: `{"sessionId":"session-1","requestId":"elicitation-1","result":{"action":"accept","content":{"region":"us-east"}},"decisionContext":` + personAnsweredContextJSON + `}`,
		},
		{
			name: "attributed handler without a label",
			register: func(s *Session) {
				s.registerAttributedElicitationHandler(func(ElicitationContext) (AttributedElicitationResult, error) {
					return AttributedElicitationResult{ElicitationResult: accepted}, nil
				})
			},
			want: unlabeled,
		},
		{
			name: "plain handler",
			register: func(s *Session) {
				s.registerElicitationHandler(func(ElicitationContext) (ElicitationResult, error) {
					return accepted, nil
				})
			},
			want: unlabeled,
		},
		{
			// The SDK, not the person, cancels a result it cannot send.
			name: "SDK cancels an unsendable labeled answer",
			register: func(s *Session) {
				s.registerAttributedElicitationHandler(func(ElicitationContext) (AttributedElicitationResult, error) {
					return AttributedElicitationResult{
						ElicitationResult: ElicitationResult{
							Action:  ElicitationActionAccept,
							Content: map[string]ElicitationFieldValue{"region": struct{}{}},
						},
						DecisionContext: personAnsweredContext(),
					}, nil
				})
			},
			want: `{"sessionId":"session-1","requestId":"elicitation-1","result":{"action":"cancel"}}`,
		},
	} {
		t.Run(tc.name, func(t *testing.T) {
			method, params := dispatchAndCaptureReply(t, tc.register, SessionEvent{Data: &ElicitationRequestedData{RequestID: "elicitation-1", Message: "Pick a region"}})

			if method != "session.ui.handlePendingElicitation" {
				t.Fatalf("method = %q, want session.ui.handlePendingElicitation", method)
			}
			got, err := json.Marshal(params)
			if err != nil {
				t.Fatalf("encode params: %v", err)
			}
			assertWireJSON(t, "session.ui.handlePendingElicitation params", got, tc.want)
		})
	}
}

// TestPromptHandlersFromSessionConfig drives create and resume with plain
// handlers, attributed handlers, or both. Either kind enables the prompt's
// callbacks; when both are set, the attributed handler answers.
func TestPromptHandlersFromSessionConfig(t *testing.T) {
	plainUserInput := func(UserInputRequest, UserInputInvocation) (UserInputResponse, error) {
		return UserInputResponse{Answer: "plain", WasFreeform: true}, nil
	}
	attributedUserInput := func(UserInputRequest, UserInputInvocation) (AttributedUserInputResponse, error) {
		return AttributedUserInputResponse{
			UserInputResponse: UserInputResponse{Answer: "attributed", WasFreeform: true},
			DecisionContext:   personAnsweredContext(),
		}, nil
	}
	plainPlan := func(ExitPlanModeRequest, ExitPlanModeInvocation) (ExitPlanModeResult, error) {
		return ExitPlanModeResult{Approved: true, Feedback: "plain"}, nil
	}
	attributedPlan := func(ExitPlanModeRequest, ExitPlanModeInvocation) (AttributedExitPlanModeResult, error) {
		return AttributedExitPlanModeResult{
			ExitPlanModeResult: ExitPlanModeResult{Approved: true, Feedback: "attributed"},
			DecisionContext:    personAnsweredContext(),
		}, nil
	}
	plainElicitation := func(ElicitationContext) (ElicitationResult, error) {
		return ElicitationResult{Action: ElicitationActionAccept, Content: map[string]ElicitationFieldValue{"by": "plain"}}, nil
	}
	attributedElicitation := func(ElicitationContext) (AttributedElicitationResult, error) {
		return AttributedElicitationResult{
			ElicitationResult: ElicitationResult{Action: ElicitationActionAccept, Content: map[string]ElicitationFieldValue{"by": "attributed"}},
			DecisionContext:   personAnsweredContext(),
		}, nil
	}

	for _, resume := range []bool{false, true} {
		for _, tc := range []struct {
			name              string
			plain, attributed bool
		}{
			{name: "plain", plain: true},
			{name: "attributed", attributed: true},
			{name: "both", plain: true, attributed: true},
		} {
			t.Run(fmt.Sprintf("%s/resume=%t", tc.name, resume), func(t *testing.T) {
				rpcClient, server, _ := newRuntimeShutdownRpcPair(t)
				t.Cleanup(server.Stop)
				client := &Client{
					client:   rpcClient,
					RPC:      rpc.NewServerRPC(rpcClient),
					sessions: make(map[string]*Session),
				}
				client.setupNotificationHandler()

				openParams := make(chan json.RawMessage, 1)
				open := func(params json.RawMessage) (json.RawMessage, *jsonrpc2.Error) {
					openParams <- append(json.RawMessage(nil), params...)
					return []byte(`{"sessionId":"` + sessionIDFromParams(t, params) + `","workspacePath":"/workspace"}`), nil
				}
				server.SetRequestHandler("session.create", open)
				server.SetRequestHandler("session.resume", open)
				elicitationAnswers := make(chan json.RawMessage, 1)
				server.SetRequestHandler("session.ui.handlePendingElicitation", func(params json.RawMessage) (json.RawMessage, *jsonrpc2.Error) {
					elicitationAnswers <- append(json.RawMessage(nil), params...)
					return []byte(`{"success":true}`), nil
				})

				var session *Session
				var err error
				if resume {
					config := &ResumeSessionConfig{}
					if tc.plain {
						config.OnUserInputRequest, config.OnExitPlanModeRequest, config.OnElicitationRequest = plainUserInput, plainPlan, plainElicitation
					}
					if tc.attributed {
						config.OnAttributedUserInputRequest, config.OnAttributedExitPlanModeRequest, config.OnAttributedElicitationRequest = attributedUserInput, attributedPlan, attributedElicitation
					}
					session, err = client.ResumeSession(t.Context(), "prompt-handlers", config)
				} else {
					config := &SessionConfig{SessionID: "prompt-handlers"}
					if tc.plain {
						config.OnUserInputRequest, config.OnExitPlanModeRequest, config.OnElicitationRequest = plainUserInput, plainPlan, plainElicitation
					}
					if tc.attributed {
						config.OnAttributedUserInputRequest, config.OnAttributedExitPlanModeRequest, config.OnAttributedElicitationRequest = attributedUserInput, attributedPlan, attributedElicitation
					}
					session, err = client.CreateSession(t.Context(), config)
				}
				if err != nil {
					t.Fatalf("open session: %v", err)
				}
				t.Cleanup(session.stopEventProcessing)

				var flags struct {
					RequestUserInput    *bool `json:"requestUserInput"`
					RequestExitPlanMode *bool `json:"requestExitPlanMode"`
					RequestElicitation  *bool `json:"requestElicitation"`
				}
				params := <-openParams
				if err := json.Unmarshal(params, &flags); err != nil {
					t.Fatalf("decode open params: %v", err)
				}
				for name, flag := range map[string]*bool{
					"requestUserInput":    flags.RequestUserInput,
					"requestExitPlanMode": flags.RequestExitPlanMode,
					"requestElicitation":  flags.RequestElicitation,
				} {
					if flag == nil || !*flag {
						t.Errorf("%s = %v, want true in %s", name, flag, params)
					}
				}

				wantUserInput := `{"answer":"plain","wasFreeform":true}`
				wantPlan := `{"approved":true,"feedback":"plain"}`
				wantElicitation := `{"sessionId":"prompt-handlers","requestId":"elicitation-1","result":{"action":"accept","content":{"by":"plain"}}}`
				if tc.attributed {
					wantUserInput = `{"answer":"attributed","wasFreeform":true,"decisionContext":` + personAnsweredContextJSON + `}`
					wantPlan = `{"approved":true,"feedback":"attributed","decisionContext":` + personAnsweredContextJSON + `}`
					wantElicitation = `{"sessionId":"prompt-handlers","requestId":"elicitation-1","result":{"action":"accept","content":{"by":"attributed"}},"decisionContext":` + personAnsweredContextJSON + `}`
				}

				reply, err := server.Request(t.Context(), "userInput.request", map[string]any{
					"sessionId": session.SessionID, "question": "Which files?", "requestId": "req-user-1",
				})
				if err != nil {
					t.Fatalf("userInput.request: %v", err)
				}
				assertWireJSON(t, "userInput.request reply", reply, wantUserInput)

				reply, err = server.Request(t.Context(), "exitPlanMode.request", map[string]any{
					"sessionId": session.SessionID, "summary": "Plan", "actions": []string{"interactive"}, "recommendedAction": "interactive",
				})
				if err != nil {
					t.Fatalf("exitPlanMode.request: %v", err)
				}
				assertWireJSON(t, "exitPlanMode.request reply", reply, wantPlan)

				event := json.RawMessage(`{"sessionId":"prompt-handlers","event":{"id":"event-1","timestamp":"2026-11-11T00:00:00Z","type":"elicitation.requested","data":{"requestId":"elicitation-1","message":"Pick a region"}}}`)
				if _, rpcErr := jsonrpc2.NotificationHandlerFor(client.handleSessionEvent)(event); rpcErr != nil {
					t.Fatalf("session.event: %v", rpcErr)
				}
				select {
				case answer := <-elicitationAnswers:
					assertWireJSON(t, "session.ui.handlePendingElicitation params", answer, wantElicitation)
				case <-time.After(5 * time.Second):
					t.Fatal("timed out waiting for the elicitation answer")
				}
			})
		}
	}
}
