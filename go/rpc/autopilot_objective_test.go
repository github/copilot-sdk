package rpc

import (
	"encoding/json"
	"io"
	"testing"

	"github.com/github/copilot-sdk/go/internal/jsonrpc2"
)

func TestAutopilotObjectiveGetState(t *testing.T) {
	clientToServerReader, clientToServerWriter := io.Pipe()
	serverToClientReader, serverToClientWriter := io.Pipe()

	client := jsonrpc2.NewClient(clientToServerWriter, serverToClientReader)
	server := jsonrpc2.NewClient(serverToClientWriter, clientToServerReader)
	responses := []json.RawMessage{
		json.RawMessage(`{"state":null}`),
		json.RawMessage(`{"state":{"id":1,"objective":"Ship the release","status":"active","turnCount":2,"creditCountNanoAiu":"0"}}`),
		json.RawMessage(`{"state":{"id":2,"objective":"Wait for approval","status":"paused","turnCount":3,"pauseReason":"Approval required","creditCountNanoAiu":"9007199254740993","creditLimit":{"creditsUsed":9.007199254740993,"creditsUsedNanoAiu":"9007199254740993"}}}`),
		json.RawMessage(`{"state":{"id":3,"objective":"Publish the SDK","status":"completed","turnCount":4,"completionSummary":"Published","creditCountNanoAiu":"9007199254740994","creditLimit":{"credits":2.5,"creditsUsed":1.25,"creditsUsedNanoAiu":"1250000000"}}}`),
	}
	callIndex := 0
	server.SetRequestHandler("session.autopilotObjective.getState", func(params json.RawMessage) (json.RawMessage, *jsonrpc2.Error) {
		var request struct {
			SessionID string `json:"sessionId"`
		}
		if err := json.Unmarshal(params, &request); err != nil {
			return nil, &jsonrpc2.Error{Code: -32602, Message: err.Error()}
		}
		if request.SessionID != "session-1" {
			return nil, &jsonrpc2.Error{Code: -32602, Message: "unexpected session ID"}
		}
		response := responses[callIndex]
		callIndex++
		return response, nil
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

	api := NewSessionRPC(client, "session-1").AutopilotObjective
	results := make([]*AutopilotObjectiveGetStateResult, 0, len(responses))
	for range responses {
		result, err := api.GetState(t.Context())
		if err != nil {
			t.Fatalf("get objective state: %v", err)
		}
		results = append(results, result)
	}

	if results[0].State != nil {
		t.Fatalf("state = %#v, want nil", results[0].State)
	}

	active := results[1].State
	if active == nil || active.Status != AutopilotObjectiveStatusActive {
		t.Fatalf("active state = %#v", active)
	}
	if active.PauseReason != nil || active.CompletionSummary != nil || active.CreditLimit != nil {
		t.Fatalf("active optional fields = %#v", active)
	}

	paused := results[2].State
	if paused == nil || paused.Status != AutopilotObjectiveStatusPaused {
		t.Fatalf("paused state = %#v", paused)
	}
	if paused.PauseReason == nil || *paused.PauseReason != "Approval required" {
		t.Fatalf("pause reason = %v", paused.PauseReason)
	}
	if paused.CreditLimit == nil || paused.CreditLimit.Credits != nil {
		t.Fatalf("paused credit limit = %#v", paused.CreditLimit)
	}
	if paused.CreditCountNanoAiu != "9007199254740993" {
		t.Fatalf("credit count = %q", paused.CreditCountNanoAiu)
	}

	completed := results[3].State
	if completed == nil || completed.Status != AutopilotObjectiveStatusCompleted {
		t.Fatalf("completed state = %#v", completed)
	}
	if completed.CompletionSummary == nil || *completed.CompletionSummary != "Published" {
		t.Fatalf("completion summary = %v", completed.CompletionSummary)
	}
	if completed.CreditLimit == nil || completed.CreditLimit.Credits == nil || *completed.CreditLimit.Credits != 2.5 {
		t.Fatalf("completed credit limit = %#v", completed.CreditLimit)
	}
	if completed.CreditLimit.CreditsUsedNanoAiu != "1250000000" {
		t.Fatalf("credit usage = %q", completed.CreditLimit.CreditsUsedNanoAiu)
	}
}
