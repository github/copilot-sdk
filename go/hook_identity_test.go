// Copyright (c) Microsoft Corporation. All rights reserved.

package copilot

import (
	"encoding/json"
	"reflect"
	"testing"
)

func TestHookIdentityTypedDispatch(t *testing.T) {
	session, cleanup := newTestSession()
	defer cleanup()
	session.SessionID = "callback-owner"
	var captured any
	capture := func(input any, invocation HookInvocation) {
		captured = input
		if invocation.SessionID != "callback-owner" {
			t.Fatalf("callback routing changed: %+v", invocation)
		}
	}
	session.registerHooks(&SessionHooks{
		OnPreToolUse: func(i PreToolUseHookInput, c HookInvocation) (*PreToolUseHookOutput, error) {
			capture(i, c)
			return nil, nil
		},
		OnPreMCPToolCall: func(i PreMCPToolCallHookInput, c HookInvocation) (*PreMCPToolCallHookOutput, error) {
			capture(i, c)
			return nil, nil
		},
		OnPostToolUse: func(i PostToolUseHookInput, c HookInvocation) (*PostToolUseHookOutput, error) {
			capture(i, c)
			return nil, nil
		},
		OnPostToolUseFailure: func(i PostToolUseFailureHookInput, c HookInvocation) (*PostToolUseFailureHookOutput, error) {
			capture(i, c)
			return nil, nil
		},
		OnUserPromptSubmitted: func(i UserPromptSubmittedHookInput, c HookInvocation) (*UserPromptSubmittedHookOutput, error) {
			capture(i, c)
			return nil, nil
		},
		OnUserPromptTransformed: func(i UserPromptTransformedHookInput, c HookInvocation) (*UserPromptTransformedHookOutput, error) {
			capture(i, c)
			return nil, nil
		},
		OnSessionStart: func(i SessionStartHookInput, c HookInvocation) (*SessionStartHookOutput, error) {
			capture(i, c)
			return nil, nil
		},
		OnSessionEnd: func(i SessionEndHookInput, c HookInvocation) (*SessionEndHookOutput, error) {
			capture(i, c)
			return nil, nil
		},
		OnErrorOccurred: func(i ErrorOccurredHookInput, c HookInvocation) (*ErrorOccurredHookOutput, error) {
			capture(i, c)
			return nil, nil
		},
		OnAgentStop: func(i AgentStopHookInput, c HookInvocation) (*AgentStopHookOutput, error) {
			capture(i, c)
			return nil, nil
		},
		OnSubagentStart: func(i SubagentStartHookInput, c HookInvocation) (*SubagentStartHookOutput, error) {
			capture(i, c)
			return nil, nil
		},
		OnSubagentStop: func(i SubagentStopHookInput, c HookInvocation) (*SubagentStopHookOutput, error) {
			capture(i, c)
			return nil, nil
		},
	})
	for _, hook := range []string{"preToolUse", "preMcpToolCall", "postToolUse", "postToolUseFailure", "userPromptSubmitted", "userPromptTransformed", "sessionStart", "sessionEnd", "errorOccurred", "agentStop", "subagentStart", "subagentStop"} {
		for _, enriched := range []bool{true, false} {
			t.Run(hook+map[bool]string{true: "/child", false: "/old-root"}[enriched], func(t *testing.T) {
				input := map[string]any{"sessionId": "reported-session", "timestamp": 1, "cwd": "work"}
				if enriched {
					input["agentId"], input["agentType"], input["parentAgentId"] = "child-actor", "explore", "parent-actor"
				}
				raw, err := json.Marshal(input)
				if err != nil {
					t.Fatal(err)
				}
				captured = nil
				if _, err := session.handleHooksInvoke(hook, raw); err != nil {
					t.Fatal(err)
				}
				value := reflect.ValueOf(captured)
				for field, expected := range map[string]string{"AgentID": "child-actor", "AgentType": "explore", "ParentAgentID": "parent-actor"} {
					if !enriched {
						expected = ""
					}
					actual := value.FieldByName(field)
					if !actual.IsValid() || actual.Kind() != reflect.String || actual.String() != expected {
						t.Errorf("%s: actual=%v expected=%q input=%+v", field, actual, expected, captured)
					}
				}
				if value.FieldByName("SessionID").String() != "reported-session" {
					t.Errorf("report ID changed: %+v", captured)
				}
			})
		}
	}
}
