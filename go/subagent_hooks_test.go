/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package copilot

import (
	"encoding/json"
	"reflect"
	"testing"
	"time"
)

func TestSession_SubagentStartHook(t *testing.T) {
	session, cleanup := newTestSession()
	defer cleanup()
	session.SessionID = "parent-session"

	var captured SubagentStartHookInput
	session.registerHooks(&SessionHooks{
		OnSubagentStart: func(input SubagentStartHookInput, invocation HookInvocation) (*SubagentStartHookOutput, error) {
			captured = input
			if invocation.SessionID != "parent-session" {
				t.Errorf("invocation session ID = %q, want parent-session", invocation.SessionID)
			}
			return &SubagentStartHookOutput{AdditionalContext: "read only the requested file"}, nil
		},
	})

	raw := json.RawMessage(`{"sessionId":"parent-session","timestamp":1700000000123,"cwd":"C:\\work","transcriptPath":"C:\\transcript.jsonl","agentName":"explore","agentDisplayName":"Explore Agent","agentDescription":"Find files"}`)
	output, err := session.handleHooksInvoke("subagentStart", raw)
	if err != nil {
		t.Fatalf("subagentStart dispatch failed: %v", err)
	}
	if captured.SessionID != "parent-session" || !captured.Timestamp.Equal(time.UnixMilli(1700000000123)) ||
		captured.WorkingDirectory != `C:\work` || captured.TranscriptPath != `C:\transcript.jsonl` ||
		captured.AgentName != "explore" || captured.AgentDisplayName != "Explore Agent" ||
		captured.AgentDescription != "Find files" {
		t.Errorf("subagentStart input = %+v", captured)
	}
	assertHookJSON(t, captured, raw)
	assertHookJSON(t, output, json.RawMessage(`{"additionalContext":"read only the requested file"}`))

	session.registerHooks(&SessionHooks{})
	output, err = session.handleHooksInvoke("subagentStart", raw)
	if err != nil || output != nil {
		t.Errorf("unregistered subagentStart = (%v, %v), want (nil, nil)", output, err)
	}
}

func TestSession_SubagentStopHook(t *testing.T) {
	session, cleanup := newTestSession()
	defer cleanup()
	session.SessionID = "parent-session"

	raw := json.RawMessage(`{"sessionId":"parent-session","timestamp":1700000000456,"cwd":"C:\\work","transcriptPath":"C:\\transcript.jsonl","agentId":"read-file","agentType":"explore","agentName":"explore","agentDisplayName":"Explore Agent","agentDescription":"Find files","stopReason":"end_turn","response":"Hello from subagent test!"}`)
	tests := []struct {
		name   string
		output *SubagentStopHookOutput
		want   json.RawMessage
	}{
		{
			name:   "block with a reason",
			output: &SubagentStopHookOutput{Decision: "block", Reason: "Read the file again"},
			want:   json.RawMessage(`{"decision":"block","reason":"Read the file again"}`),
		},
		{
			name:   "replace the response",
			output: &SubagentStopHookOutput{ModifiedResponse: String("Changed result")},
			want:   json.RawMessage(`{"modifiedResponse":"Changed result"}`),
		},
		{
			name:   "replace with an empty response",
			output: &SubagentStopHookOutput{ModifiedResponse: String("")},
			want:   json.RawMessage(`{"modifiedResponse":""}`),
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			var captured SubagentStopHookInput
			session.registerHooks(&SessionHooks{
				OnSubagentStop: func(input SubagentStopHookInput, invocation HookInvocation) (*SubagentStopHookOutput, error) {
					captured = input
					if invocation.SessionID != "parent-session" {
						t.Errorf("invocation session ID = %q, want parent-session", invocation.SessionID)
					}
					return tt.output, nil
				},
			})

			output, err := session.handleHooksInvoke("subagentStop", raw)
			if err != nil {
				t.Fatalf("subagentStop dispatch failed: %v", err)
			}
			if captured.SessionID != "parent-session" || !captured.Timestamp.Equal(time.UnixMilli(1700000000456)) ||
				captured.WorkingDirectory != `C:\work` || captured.TranscriptPath != `C:\transcript.jsonl` ||
				captured.AgentID != "read-file" || captured.AgentType != "explore" ||
				captured.AgentName != "explore" || captured.AgentDisplayName != "Explore Agent" ||
				captured.AgentDescription != "Find files" || captured.StopReason != "end_turn" ||
				captured.Response != "Hello from subagent test!" {
				t.Errorf("subagentStop input = %+v", captured)
			}
			assertHookJSON(t, captured, raw)
			assertHookJSON(t, output, tt.want)
		})
	}

	session.registerHooks(&SessionHooks{})
	output, err := session.handleHooksInvoke("subagentStop", raw)
	if err != nil || output != nil {
		t.Errorf("unregistered subagentStop = (%v, %v), want (nil, nil)", output, err)
	}
}

func assertHookJSON(t *testing.T, value any, expected json.RawMessage) {
	t.Helper()
	gotJSON, err := json.Marshal(value)
	if err != nil {
		t.Fatalf("marshal %T: %v", value, err)
	}
	var got, want any
	if err := json.Unmarshal(gotJSON, &got); err != nil {
		t.Fatalf("decode marshaled %T: %v", value, err)
	}
	if err := json.Unmarshal(expected, &want); err != nil {
		t.Fatalf("decode expected JSON: %v", err)
	}
	if !reflect.DeepEqual(got, want) {
		t.Errorf("JSON for %T = %s, want %s", value, gotJSON, expected)
	}
}
