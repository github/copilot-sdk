// Copyright (c) Microsoft Corporation. All rights reserved.

package e2e

import (
	"encoding/json"
	"reflect"
	"sync"
	"testing"

	"github.com/github/copilot-sdk/go/internal/jsonrpc2"
	"github.com/github/copilot-sdk/go/rpc"
)

func TestScenarioTestingServerControlE2E(t *testing.T) {
	t.Run("searches server catalog with category contract", func(t *testing.T) {
		tests := []struct {
			name         string
			kinds        []rpc.CatalogCandidateKind
			capabilities []string
		}{
			{
				name:         "all",
				kinds:        []rpc.CatalogCandidateKind{rpc.CatalogCandidateKindMCPServer, rpc.CatalogCandidateKindAiSkill},
				capabilities: []string{"mcp-server-card", "ai-skill-discovery"},
			},
			{
				name:         "mcp",
				kinds:        []rpc.CatalogCandidateKind{rpc.CatalogCandidateKindMCPServer},
				capabilities: []string{"mcp-server-card"},
			},
			{
				name:         "skills",
				kinds:        []rpc.CatalogCandidateKind{rpc.CatalogCandidateKindAiSkill},
				capabilities: []string{"ai-skill-discovery"},
			},
		}
		for _, test := range tests {
			t.Run(test.name, func(t *testing.T) {
				fixture := newGeneratedRPCFixture(t, t.Context())
				var captured map[string]any
				fixture.server.SetRequestHandler("catalog.search", func(request json.RawMessage) (json.RawMessage, *jsonrpc2.Error) {
					if err := json.Unmarshal(request, &captured); err != nil {
						t.Errorf("Unmarshal catalog request failed: %v", err)
					}
					return mustJSON(t, map[string]any{
						"kind":       "succeeded",
						"candidates": []any{},
						"negotiated": map[string]any{
							"grantedCapabilities":    test.capabilities,
							"runtimeProtocolVersion": 3,
						},
						"searchId":  "scenario-search",
						"truncated": false,
					}), nil
				})

				limit := int32(50)
				result, err := fixture.client.RPC.Catalog.Search(t.Context(), &rpc.CatalogSearchRequest{
					Contract: rpc.CatalogClientContract{
						ProtocolVersion:      3,
						RequiredCapabilities: test.capabilities,
					},
					Kinds: test.kinds,
					Limit: &limit,
					Query: "scenario search",
				})
				if err != nil {
					t.Fatalf("Catalog.Search failed: %v", err)
				}
				succeeded, ok := result.(*rpc.CatalogSearchSucceeded)
				if !ok {
					t.Fatalf("Expected CatalogSearchSucceeded, got %T", result)
				}
				if len(succeeded.Candidates) != 0 || succeeded.SearchID != "scenario-search" || succeeded.Truncated {
					t.Fatalf("Unexpected catalog result: %#v", succeeded)
				}
				if succeeded.Negotiated.RuntimeProtocolVersion != 3 ||
					!reflect.DeepEqual(succeeded.Negotiated.GrantedCapabilities, catalogCapabilities(test.capabilities)) {
					t.Fatalf("Unexpected negotiated contract: %#v", succeeded.Negotiated)
				}

				assertJSONSubset(t, "catalog.search", map[string]any{
					"query": "scenario search",
					"limit": float64(50),
					"contract": map[string]any{
						"protocolVersion":      float64(3),
						"requiredCapabilities": stringsToAny(test.capabilities),
					},
					"kinds": catalogKindsToAny(test.kinds),
				}, captured)
			})
		}
	})

	t.Run("observes pages and cancels factory run", func(t *testing.T) {
		fixture := newGeneratedRPCFixture(t, t.Context())
		var mu sync.Mutex
		captured := map[string]map[string]any{}
		setFactoryHandler := func(method string, result any) {
			fixture.server.SetRequestHandler(method, func(request json.RawMessage) (json.RawMessage, *jsonrpc2.Error) {
				var params map[string]any
				if err := json.Unmarshal(request, &params); err != nil {
					t.Errorf("Unmarshal %s failed: %v", method, err)
				}
				mu.Lock()
				captured[method] = params
				mu.Unlock()
				return mustJSON(t, result), nil
			})
		}
		setFactoryHandler("session.factory.listRuns", map[string]any{
			"runs": []any{map[string]any{
				"runId":       "factory-run-1",
				"factoryName": "scenario-factory",
				"status":      "running",
			}},
			"oldestSeq":    7,
			"newestSeq":    7,
			"hasMoreNewer": false,
		})
		setFactoryHandler("session.factory.getRunDetail", map[string]any{
			"runId":       "factory-run-1",
			"factoryName": "scenario-factory",
			"status":      "running",
			"revision":    4,
		})
		setFactoryHandler("session.factory.getRunProgress", map[string]any{
			"records": []any{map[string]any{
				"attempt":    1,
				"kind":       "log",
				"phaseId":    "verify",
				"recordedAt": 1234,
				"seq":        12,
				"text":       "Validation complete",
			}},
			"revision": 4,
		})
		setFactoryHandler("session.factory.cancel", map[string]any{
			"runId":  "factory-run-1",
			"status": "cancelled",
			"reason": "cancelled by user",
		})

		after, before, limit := int64(3), int64(20), int32(10)
		runs, err := fixture.session.RPC.Factory.ListRuns(t.Context(), &rpc.FactoryListRunsRequest{
			AfterSeq:  &after,
			BeforeSeq: &before,
			Limit:     &limit,
		})
		if err != nil {
			t.Fatalf("Factory.ListRuns failed: %v", err)
		}
		if len(runs.Runs) != 1 || runs.Runs[0].RunID != "factory-run-1" ||
			runs.Runs[0].FactoryName != "scenario-factory" || runs.Runs[0].Status != rpc.FactoryRunStatusRunning ||
			runs.OldestSeq == nil || *runs.OldestSeq != 7 || runs.NewestSeq == nil || *runs.NewestSeq != 7 ||
			runs.HasMoreNewer == nil || *runs.HasMoreNewer {
			t.Fatalf("Unexpected factory run page: %#v", runs)
		}

		detail, err := fixture.session.RPC.Factory.GetRunDetail(t.Context(), &rpc.FactoryGetRunRequest{RunID: "factory-run-1"})
		if err != nil {
			t.Fatalf("Factory.GetRunDetail failed: %v", err)
		}
		if detail.RunID != "factory-run-1" || detail.FactoryName != "scenario-factory" ||
			detail.Status != rpc.FactoryRunStatusRunning || detail.Revision != 4 {
			t.Fatalf("Unexpected factory detail: %#v", detail)
		}

		progressAfter, progressBefore, progressLimit := int64(5), int64(20), int32(25)
		phaseID := "verify"
		progress, err := fixture.session.RPC.Factory.GetRunProgress(t.Context(), &rpc.FactoryGetRunProgressRequest{
			RunID:     "factory-run-1",
			PhaseID:   &phaseID,
			AfterSeq:  &progressAfter,
			BeforeSeq: &progressBefore,
			Limit:     &progressLimit,
		})
		if err != nil {
			t.Fatalf("Factory.GetRunProgress failed: %v", err)
		}
		if len(progress.Records) != 1 || progress.Records[0].Seq != 12 ||
			progress.Records[0].PhaseID == nil || *progress.Records[0].PhaseID != "verify" ||
			progress.Records[0].Kind != rpc.FactoryLogLineKindLog ||
			progress.Records[0].Text != "Validation complete" {
			t.Fatalf("Unexpected factory progress: %#v", progress)
		}

		cancelled, err := fixture.session.RPC.Factory.Cancel(t.Context(), &rpc.FactoryCancelRequest{RunID: "factory-run-1"})
		if err != nil {
			t.Fatalf("Factory.Cancel failed: %v", err)
		}
		if cancelled.RunID != "factory-run-1" || cancelled.Status != rpc.FactoryRunStatusCancelled ||
			cancelled.Reason == nil || *cancelled.Reason != "cancelled by user" {
			t.Fatalf("Unexpected cancelled factory run: %#v", cancelled)
		}

		mu.Lock()
		defer mu.Unlock()
		assertJSONSubset(t, "session.factory.listRuns", map[string]any{
			"afterSeq":  float64(3),
			"beforeSeq": float64(20),
			"limit":     float64(10),
		}, captured["session.factory.listRuns"])
		assertJSONSubset(t, "session.factory.getRunProgress", map[string]any{
			"runId":     "factory-run-1",
			"phaseId":   "verify",
			"afterSeq":  float64(5),
			"beforeSeq": float64(20),
			"limit":     float64(25),
		}, captured["session.factory.getRunProgress"])
	})

	t.Run("reads autopilot state and enables remote mode", func(t *testing.T) {
		tests := []struct {
			mode      rpc.RemoteSessionMode
			steerable bool
		}{
			{mode: rpc.RemoteSessionModeOn, steerable: true},
			{mode: rpc.RemoteSessionModeExport, steerable: false},
		}
		for _, test := range tests {
			t.Run(string(test.mode), func(t *testing.T) {
				fixture := newGeneratedRPCFixture(t, t.Context())
				fixture.server.SetRequestHandler("session.autopilotObjective.getState", func(_ json.RawMessage) (json.RawMessage, *jsonrpc2.Error) {
					return json.RawMessage(`{"state":{"id":17,"objective":"Ship the scenario.","status":"active","turnCount":3,"creditCountNanoAiu":"1250000000","creditLimit":{"credits":5,"creditsUsed":1.25,"creditsUsedNanoAiu":"1250000000"}}}`), nil
				})
				var remoteRequest map[string]any
				fixture.server.SetRequestHandler("session.remote.enable", func(request json.RawMessage) (json.RawMessage, *jsonrpc2.Error) {
					if err := json.Unmarshal(request, &remoteRequest); err != nil {
						t.Errorf("Unmarshal remote enable failed: %v", err)
					}
					return mustJSON(t, map[string]any{
						"remoteSteerable": test.steerable,
						"url":             "https://example.test/sessions/" + fixture.session.SessionID,
					}), nil
				})

				state, err := fixture.session.RPC.AutopilotObjective.GetState(t.Context())
				if err != nil {
					t.Fatalf("AutopilotObjective.GetState failed: %v", err)
				}
				objective := state.State
				if objective == nil || objective.ID != 17 || objective.Objective != "Ship the scenario." ||
					objective.Status != rpc.AutopilotObjectiveStatusActive || objective.TurnCount != 3 ||
					objective.CreditCountNanoAiu != "1250000000" || objective.CreditLimit == nil ||
					objective.CreditLimit.Credits == nil || *objective.CreditLimit.Credits != 5 ||
					objective.CreditLimit.CreditsUsed != 1.25 ||
					objective.CreditLimit.CreditsUsedNanoAiu != "1250000000" {
					t.Fatalf("Unexpected autopilot objective: %#v", objective)
				}

				enabled, err := fixture.session.RPC.Remote.Enable(t.Context(), &rpc.RemoteEnableRequest{Mode: &test.mode})
				if err != nil {
					t.Fatalf("Remote.Enable failed: %v", err)
				}
				expectedURL := "https://example.test/sessions/" + fixture.session.SessionID
				if enabled.RemoteSteerable != test.steerable || enabled.URL == nil || *enabled.URL != expectedURL {
					t.Fatalf("Unexpected remote result: %#v", enabled)
				}
				assertJSONSubset(t, "session.remote.enable", map[string]any{
					"sessionId": fixture.session.SessionID,
					"mode":      string(test.mode),
				}, remoteRequest)
			})
		}
	})

	t.Run("edits reorders duplicates removes and sends queued items", func(t *testing.T) {
		fixture := newGeneratedRPCFixture(t, t.Context())
		queue := newScenarioQueue(t, fixture.server)

		if _, err := fixture.session.RPC.Queue.SetDrainPaused(t.Context(), &rpc.QueueSetDrainPausedRequest{Paused: true}); err != nil {
			t.Fatalf("Queue.SetDrainPaused(true) failed: %v", err)
		}
		firstDisplay := "First visible prompt"
		first, err := fixture.session.RPC.Queue.InsertAt(t.Context(), &rpc.QueueInsertAtRequest{
			Position: 0,
			Message: rpc.QueueInsertMessage{
				Prompt:        "First hidden prompt",
				DisplayPrompt: &firstDisplay,
				AgentMode:     ptr(rpc.SendAgentModeInteractive),
			},
		})
		if err != nil {
			t.Fatalf("Queue.InsertAt(first) failed: %v", err)
		}
		secondDisplay := "Second visible prompt"
		second, err := fixture.session.RPC.Queue.InsertAt(t.Context(), &rpc.QueueInsertAtRequest{
			Position: 1,
			Message: rpc.QueueInsertMessage{
				Prompt:        "Second hidden prompt",
				DisplayPrompt: &secondDisplay,
				AgentMode:     ptr(rpc.SendAgentModePlan),
			},
		})
		if err != nil {
			t.Fatalf("Queue.InsertAt(second) failed: %v", err)
		}

		updatedDisplay := "Updated visible prompt"
		updated, err := fixture.session.RPC.Queue.UpdateText(t.Context(), &rpc.QueueUpdateTextRequest{
			ID:            first.ID,
			Prompt:        "Updated hidden prompt",
			DisplayPrompt: &updatedDisplay,
		})
		if err != nil || !updated.Updated {
			t.Fatalf("Queue.UpdateText: result=%#v err=%v", updated, err)
		}
		duplicate, err := fixture.session.RPC.Queue.DuplicateAt(t.Context(), &rpc.QueueDuplicateAtRequest{ID: first.ID})
		if err != nil || duplicate.ID == first.ID {
			t.Fatalf("Queue.DuplicateAt: result=%#v err=%v", duplicate, err)
		}
		moved, err := fixture.session.RPC.Queue.MoveItem(t.Context(), &rpc.QueueMoveItemRequest{
			ID:         second.ID,
			ToPosition: 0,
		})
		if err != nil || !moved.Changed {
			t.Fatalf("Queue.MoveItem: result=%#v err=%v", moved, err)
		}

		reordered, err := fixture.session.RPC.Queue.PendingItems(t.Context())
		if err != nil {
			t.Fatalf("Queue.PendingItems failed: %v", err)
		}
		gotIDs := []string{reordered.Items[0].ID, reordered.Items[1].ID, reordered.Items[2].ID}
		wantIDs := []string{second.ID, first.ID, duplicate.ID}
		if !reflect.DeepEqual(gotIDs, wantIDs) {
			t.Fatalf("Queue order: got %v, want %v", gotIDs, wantIDs)
		}
		if reordered.Items[1].DisplayText != updatedDisplay ||
			reordered.Items[1].AgentMode != rpc.SendAgentModeInteractive {
			t.Fatalf("Unexpected edited queue item: %#v", reordered.Items[1])
		}

		sent, err := fixture.session.RPC.Queue.SendNow(t.Context(), &rpc.QueueSendNowRequest{ID: second.ID})
		if err != nil || !sent.Steered {
			t.Fatalf("Queue.SendNow: result=%#v err=%v", sent, err)
		}
		removed, err := fixture.session.RPC.Queue.RemoveAt(t.Context(), &rpc.QueueRemoveAtRequest{ID: duplicate.ID})
		if err != nil || !removed.Removed {
			t.Fatalf("Queue.RemoveAt: result=%#v err=%v", removed, err)
		}
		remaining, err := fixture.session.RPC.Queue.PendingItems(t.Context())
		if err != nil {
			t.Fatalf("Queue.PendingItems after edits failed: %v", err)
		}
		if len(remaining.Items) != 1 || remaining.Items[0].ID != first.ID ||
			remaining.Items[0].DisplayText != updatedDisplay ||
			len(remaining.SteeringMessages) != 1 || remaining.SteeringMessages[0] != secondDisplay {
			t.Fatalf("Unexpected remaining queue state: %#v", remaining)
		}
		if _, err := fixture.session.RPC.Queue.SetDrainPaused(t.Context(), &rpc.QueueSetDrainPausedRequest{Paused: false}); err != nil {
			t.Fatalf("Queue.SetDrainPaused(false) failed: %v", err)
		}
		if !reflect.DeepEqual(queue.pauseRequests, []bool{true, false}) {
			t.Fatalf("Pause requests: got %v, want [true false]", queue.pauseRequests)
		}
	})
}

func catalogCapabilities(values []string) []rpc.CatalogCapability {
	result := make([]rpc.CatalogCapability, len(values))
	for i, value := range values {
		result[i] = rpc.CatalogCapability(value)
	}
	return result
}

func stringsToAny(values []string) []any {
	result := make([]any, len(values))
	for i, value := range values {
		result[i] = value
	}
	return result
}

func catalogKindsToAny(values []rpc.CatalogCandidateKind) []any {
	result := make([]any, len(values))
	for i, value := range values {
		result[i] = string(value)
	}
	return result
}

func ptr[T any](value T) *T {
	return &value
}

type scenarioQueueItem struct {
	id          string
	prompt      string
	displayText string
	agentMode   rpc.SendAgentMode
}

type scenarioQueue struct {
	t                *testing.T
	mu               sync.Mutex
	items            []scenarioQueueItem
	steeringMessages []string
	pauseRequests    []bool
	nextID           int
}

func newScenarioQueue(t *testing.T, server *jsonrpc2.Client) *scenarioQueue {
	q := &scenarioQueue{t: t}
	server.SetRequestHandler("session.queue.setDrainPaused", q.setDrainPaused)
	server.SetRequestHandler("session.queue.insertAt", q.insertAt)
	server.SetRequestHandler("session.queue.updateText", q.updateText)
	server.SetRequestHandler("session.queue.duplicateAt", q.duplicateAt)
	server.SetRequestHandler("session.queue.moveItem", q.moveItem)
	server.SetRequestHandler("session.queue.pendingItems", q.pendingItems)
	server.SetRequestHandler("session.queue.sendNow", q.sendNow)
	server.SetRequestHandler("session.queue.removeAt", q.removeAt)
	return q
}

func (q *scenarioQueue) setDrainPaused(request json.RawMessage) (json.RawMessage, *jsonrpc2.Error) {
	var params struct {
		Paused bool `json:"paused"`
	}
	if err := json.Unmarshal(request, &params); err != nil {
		q.t.Errorf("Unmarshal setDrainPaused: %v", err)
	}
	q.mu.Lock()
	defer q.mu.Unlock()
	q.pauseRequests = append(q.pauseRequests, params.Paused)
	return json.RawMessage(`{}`), nil
}

func (q *scenarioQueue) insertAt(request json.RawMessage) (json.RawMessage, *jsonrpc2.Error) {
	var params struct {
		Position int `json:"position"`
		Message  struct {
			Prompt        string             `json:"prompt"`
			DisplayPrompt *string            `json:"displayPrompt"`
			AgentMode     *rpc.SendAgentMode `json:"agentMode"`
		} `json:"message"`
	}
	if err := json.Unmarshal(request, &params); err != nil {
		q.t.Errorf("Unmarshal insertAt: %v", err)
	}
	q.mu.Lock()
	defer q.mu.Unlock()
	q.nextID++
	item := scenarioQueueItem{
		id:          "queue-" + string(rune('0'+q.nextID)),
		prompt:      params.Message.Prompt,
		displayText: params.Message.Prompt,
		agentMode:   rpc.SendAgentModeInteractive,
	}
	if params.Message.DisplayPrompt != nil {
		item.displayText = *params.Message.DisplayPrompt
	}
	if params.Message.AgentMode != nil {
		item.agentMode = *params.Message.AgentMode
	}
	position := params.Position
	if position < 0 {
		position = 0
	}
	if position > len(q.items) {
		position = len(q.items)
	}
	q.items = append(q.items, scenarioQueueItem{})
	copy(q.items[position+1:], q.items[position:])
	q.items[position] = item
	return mustJSON(q.t, map[string]any{"id": item.id}), nil
}

func (q *scenarioQueue) updateText(request json.RawMessage) (json.RawMessage, *jsonrpc2.Error) {
	var params struct {
		ID            string  `json:"id"`
		Prompt        string  `json:"prompt"`
		DisplayPrompt *string `json:"displayPrompt"`
	}
	if err := json.Unmarshal(request, &params); err != nil {
		q.t.Errorf("Unmarshal updateText: %v", err)
	}
	q.mu.Lock()
	defer q.mu.Unlock()
	for i := range q.items {
		if q.items[i].id == params.ID {
			q.items[i].prompt = params.Prompt
			q.items[i].displayText = params.Prompt
			if params.DisplayPrompt != nil {
				q.items[i].displayText = *params.DisplayPrompt
			}
			return json.RawMessage(`{"updated":true}`), nil
		}
	}
	return json.RawMessage(`{"updated":false}`), nil
}

func (q *scenarioQueue) duplicateAt(request json.RawMessage) (json.RawMessage, *jsonrpc2.Error) {
	var params struct {
		ID string `json:"id"`
	}
	if err := json.Unmarshal(request, &params); err != nil {
		q.t.Errorf("Unmarshal duplicateAt: %v", err)
	}
	q.mu.Lock()
	defer q.mu.Unlock()
	for i, item := range q.items {
		if item.id == params.ID {
			q.nextID++
			duplicate := item
			duplicate.id = "queue-" + string(rune('0'+q.nextID))
			q.items = append(q.items, scenarioQueueItem{})
			copy(q.items[i+2:], q.items[i+1:])
			q.items[i+1] = duplicate
			return mustJSON(q.t, map[string]any{"id": duplicate.id}), nil
		}
	}
	return nil, &jsonrpc2.Error{Code: -32602, Message: "queue item not found"}
}

func (q *scenarioQueue) moveItem(request json.RawMessage) (json.RawMessage, *jsonrpc2.Error) {
	var params struct {
		ID         string `json:"id"`
		ToPosition int    `json:"toPosition"`
	}
	if err := json.Unmarshal(request, &params); err != nil {
		q.t.Errorf("Unmarshal moveItem: %v", err)
	}
	q.mu.Lock()
	defer q.mu.Unlock()
	from := -1
	for i := range q.items {
		if q.items[i].id == params.ID {
			from = i
			break
		}
	}
	if from < 0 {
		return nil, &jsonrpc2.Error{Code: -32602, Message: "queue item not found"}
	}
	to := params.ToPosition
	if to < 0 {
		to = 0
	}
	if to >= len(q.items) {
		to = len(q.items) - 1
	}
	if from == to {
		return json.RawMessage(`{"changed":false}`), nil
	}
	item := q.items[from]
	q.items = append(q.items[:from], q.items[from+1:]...)
	q.items = append(q.items, scenarioQueueItem{})
	copy(q.items[to+1:], q.items[to:])
	q.items[to] = item
	return json.RawMessage(`{"changed":true}`), nil
}

func (q *scenarioQueue) pendingItems(_ json.RawMessage) (json.RawMessage, *jsonrpc2.Error) {
	q.mu.Lock()
	defer q.mu.Unlock()
	items := make([]map[string]any, len(q.items))
	for i, item := range q.items {
		messageID := "message-" + item.id
		items[i] = map[string]any{
			"agentMode":   item.agentMode,
			"displayText": item.displayText,
			"id":          item.id,
			"kind":        "message",
			"messageId":   messageID,
		}
	}
	return mustJSON(q.t, map[string]any{
		"items":            items,
		"steeringMessages": append([]string(nil), q.steeringMessages...),
	}), nil
}

func (q *scenarioQueue) sendNow(request json.RawMessage) (json.RawMessage, *jsonrpc2.Error) {
	var params struct {
		ID string `json:"id"`
	}
	if err := json.Unmarshal(request, &params); err != nil {
		q.t.Errorf("Unmarshal sendNow: %v", err)
	}
	q.mu.Lock()
	defer q.mu.Unlock()
	for i, item := range q.items {
		if item.id == params.ID {
			q.items = append(q.items[:i], q.items[i+1:]...)
			q.steeringMessages = append(q.steeringMessages, item.displayText)
			return json.RawMessage(`{"steered":true}`), nil
		}
	}
	return json.RawMessage(`{"steered":false}`), nil
}

func (q *scenarioQueue) removeAt(request json.RawMessage) (json.RawMessage, *jsonrpc2.Error) {
	var params struct {
		ID string `json:"id"`
	}
	if err := json.Unmarshal(request, &params); err != nil {
		q.t.Errorf("Unmarshal removeAt: %v", err)
	}
	q.mu.Lock()
	defer q.mu.Unlock()
	for i, item := range q.items {
		if item.id == params.ID {
			q.items = append(q.items[:i], q.items[i+1:]...)
			return json.RawMessage(`{"removed":true}`), nil
		}
	}
	return json.RawMessage(`{"removed":false}`), nil
}
