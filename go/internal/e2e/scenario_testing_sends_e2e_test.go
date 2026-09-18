// Copyright (c) Microsoft Corporation. All rights reserved.

package e2e

import (
	"context"
	"encoding/json"
	"errors"
	"fmt"
	"sync"
	"sync/atomic"
	"testing"
	"time"

	copilot "github.com/github/copilot-sdk/go"
	"github.com/github/copilot-sdk/go/internal/jsonrpc2"
	"github.com/github/copilot-sdk/go/rpc"
)

func TestScenarioTestingSendsE2E(t *testing.T) {
	t.Run("should send complete scenario message wire shape", func(t *testing.T) {
		ctx, cancel := context.WithTimeout(t.Context(), 10*time.Second)
		defer cancel()
		f := newGeneratedRPCFixture(t, ctx)

		var captured map[string]any
		f.server.SetRequestHandler("session.send", func(params json.RawMessage) (json.RawMessage, *jsonrpc2.Error) {
			if err := json.Unmarshal(params, &captured); err != nil {
				return nil, &jsonrpc2.Error{Code: -32000, Message: err.Error()}
			}
			return json.RawMessage(`{"messageId":"scenario-client-message"}`), nil
		})

		canvasID := "diff"
		instanceID := "diff-17"
		blobData := "QVBQX0JMT0I="
		blobName := "scenario-wire-blob.txt"
		messageID, err := f.session.Send(ctx, copilot.MessageOptions{
			Prompt:        "Use the hidden scenario context.",
			DisplayPrompt: "Review selected scenario context",
			Mode:          "enqueue",
			AgentMode:     copilot.AgentModeInteractive,
			Source:        copilot.MessageSourceAgent("scenario-client"),
			RequestHeaders: map[string]string{
				"x-scenario-request": "wire-shape",
			},
			Attachments: []copilot.Attachment{
				rpc.AttachmentFile{
					DisplayName: "scenario-wire-file.txt",
					Path:        `Q:\scenario-wire-file.txt`,
					LineRange:   &rpc.AttachmentFileLineRange{Start: 3, End: 9},
				},
				rpc.AttachmentDirectory{
					DisplayName: "scenario-wire-directory",
					Path:        `Q:\scenario-wire-directory`,
				},
				rpc.AttachmentSelection{
					DisplayName: "Program.cs",
					FilePath:    `Q:\Program.cs`,
					Text:        "SCENARIO_SELECTION",
					Selection: rpc.AttachmentSelectionDetails{
						Start: rpc.AttachmentSelectionDetailsStart{Line: 16, Character: 0},
						End:   rpc.AttachmentSelectionDetailsEnd{Line: 16, Character: 13},
					},
				},
				rpc.AttachmentGitHubReference{
					Number:        610,
					ReferenceType: rpc.AttachmentGitHubReferenceTypePr,
					State:         "open",
					Title:         "Scenario-shaped E2E coverage",
					URL:           "https://github.com/github/copilot-sdk/pull/610",
				},
				rpc.AttachmentBlob{
					Data:        &blobData,
					MIMEType:    "text/plain",
					DisplayName: &blobName,
				},
				rpc.AttachmentExtensionContext{
					CapturedAt:  time.Date(2026, 9, 17, 20, 0, 0, 0, time.UTC),
					ExtensionID: "scenario-client:code-review",
					CanvasID:    &canvasID,
					InstanceID:  &instanceID,
					Title:       "Selected change",
					Payload:     map[string]any{"selection": "SCENARIO_SELECTION", "line": float64(17)},
				},
			},
		})
		if err != nil {
			t.Fatal(err)
		}
		if messageID != "scenario-client-message" {
			t.Fatalf("Message ID = %q", messageID)
		}

		assertJSONSubset(t, "session.send request", map[string]any{
			"sessionId":     f.session.SessionID,
			"prompt":        "Use the hidden scenario context.",
			"displayPrompt": "Review selected scenario context",
			"mode":          "enqueue",
			"agentMode":     "interactive",
			"source":        "agent-scenario-client",
			"requestHeaders": map[string]any{
				"x-scenario-request": "wire-shape",
			},
		}, captured)

		attachments, ok := captured["attachments"].([]any)
		if !ok || len(attachments) != 6 {
			t.Fatalf("attachments = %#v", captured["attachments"])
		}
		wantTypes := []string{"file", "directory", "selection", "github_reference", "blob", "extension_context"}
		for i, want := range wantTypes {
			attachment := attachments[i].(map[string]any)
			if attachment["type"] != want {
				t.Fatalf("attachment %d type = %#v, want %q", i, attachment["type"], want)
			}
		}
		file := attachments[0].(map[string]any)
		assertJSONSubset(t, "file attachment", map[string]any{
			"path": `Q:\scenario-wire-file.txt`,
			"lineRange": map[string]any{
				"start": float64(3),
				"end":   float64(9),
			},
		}, file)
		extension := attachments[5].(map[string]any)
		assertJSONSubset(t, "extension attachment", map[string]any{
			"extensionId": "scenario-client:code-review",
			"canvasId":    "diff",
			"instanceId":  "diff-17",
			"payload": map[string]any{
				"selection": "SCENARIO_SELECTION",
				"line":      float64(17),
			},
		}, extension)
	})

	for _, mode := range []string{"", "enqueue", "immediate"} {
		name := mode
		if name == "" {
			name = "default"
		}
		t.Run("should not invoke send when scenario cancels before dispatch "+name, func(t *testing.T) {
			ctx, cancel := context.WithTimeout(t.Context(), 10*time.Second)
			defer cancel()
			f := newGeneratedRPCFixture(t, ctx)
			var calls atomic.Int64
			f.server.SetRequestHandler("session.send", func(json.RawMessage) (json.RawMessage, *jsonrpc2.Error) {
				calls.Add(1)
				return json.RawMessage(`{"messageId":"unexpected"}`), nil
			})

			cancelled, cancelSend := context.WithCancel(ctx)
			cancelSend()
			_, err := f.session.Send(cancelled, copilot.MessageOptions{
				Prompt:        "This message must never be invoked.",
				DisplayPrompt: "Cancelled scenario message",
				Mode:          mode,
				Source:        copilot.MessageSourceAgent("scenario-client"),
			})
			if !errors.Is(err, context.Canceled) {
				t.Fatalf("Send error = %v, want context cancellation", err)
			}
			if calls.Load() != 0 {
				t.Fatalf("session.send calls = %d, want 0", calls.Load())
			}
		})

		t.Run("should not replay scenario send after ambiguous transport loss "+name, func(t *testing.T) {
			ctx, cancel := context.WithTimeout(t.Context(), 10*time.Second)
			defer cancel()
			f := newGeneratedRPCFixture(t, ctx)
			var calls atomic.Int64
			var captured map[string]any
			f.server.SetRequestHandler("session.send", func(params json.RawMessage) (json.RawMessage, *jsonrpc2.Error) {
				calls.Add(1)
				_ = json.Unmarshal(params, &captured)
				_ = f.conn.Close()
				return nil, nil
			})

			_, err := f.session.Send(ctx, copilot.MessageOptions{
				Prompt:        "AMBIGUOUS_SCENARIO_SEND",
				DisplayPrompt: "Ambiguous scenario send",
				Mode:          mode,
				Source:        copilot.MessageSourceAgent("scenario-client"),
			})
			if err == nil {
				t.Fatal("Expected transport loss")
			}
			if errors.Is(err, context.DeadlineExceeded) {
				t.Fatalf("Send waited for its deadline instead of reporting transport loss: %v", err)
			}
			if calls.Load() != 1 {
				t.Fatalf("session.send calls = %d, want 1", calls.Load())
			}
			if mode == "" {
				if _, exists := captured["mode"]; exists {
					t.Fatalf("Default mode should be omitted: %#v", captured)
				}
			} else if captured["mode"] != mode {
				t.Fatalf("mode = %#v, want %q", captured["mode"], mode)
			}
		})
	}

	t.Run("should order idle queued and immediate scenario delivery", func(t *testing.T) {
		f := newGeneratedRPCFixture(t, t.Context())
		var mu sync.Mutex
		var events []copilot.SessionEvent
		var sends int
		f.server.SetRequestHandler("session.send", func(params json.RawMessage) (json.RawMessage, *jsonrpc2.Error) {
			var request struct {
				Prompt string `json:"prompt"`
				Mode   string `json:"mode"`
			}
			if err := json.Unmarshal(params, &request); err != nil {
				t.Errorf("Unmarshal session.send failed: %v", err)
			}
			mu.Lock()
			defer mu.Unlock()
			sends++
			messageID := fmt.Sprintf("scenario-message-%d", sends)
			delivery := copilot.UserMessageDeliveryIdle
			switch sends {
			case 4, 5:
				delivery = copilot.UserMessageDeliverySteering
			case 6:
				delivery = copilot.UserMessageDeliveryQueued
			}
			events = append(events, scenarioEvent(messageID, &copilot.UserMessageData{
				Content:            request.Prompt,
				Delivery:           &delivery,
				MessageID:          &messageID,
				TransformedContent: &request.Prompt,
			}))
			return mustJSON(t, map[string]any{"messageId": messageID}), nil
		})
		f.server.SetRequestHandler("session.getMessages", func(_ json.RawMessage) (json.RawMessage, *jsonrpc2.Error) {
			mu.Lock()
			defer mu.Unlock()
			return mustJSON(t, map[string]any{"events": append([]copilot.SessionEvent(nil), events...)}), nil
		})

		send := func(prompt, mode string) string {
			t.Helper()
			result, err := f.session.Send(t.Context(), copilot.MessageOptions{
				Prompt: prompt,
				Mode:   mode,
				Source: copilot.MessageSourceAgent("scenario-client"),
			})
			if err != nil {
				t.Fatalf("Send(%q, %q) failed: %v", prompt, mode, err)
			}
			return result
		}

		idleEnqueueID := send("IDLE_ENQUEUE", "enqueue")
		idleImmediateID := send("IDLE_IMMEDIATE", "immediate")
		_ = send("START_BLOCKING_TURN", "")
		steeringID := send("FIRST_STEERING", "immediate")
		immediateBehindID := send("SECOND_IMMEDIATE", "immediate")
		queuedID := send("FINAL_QUEUED", "enqueue")

		observed, err := f.session.GetEvents(t.Context())
		if err != nil {
			t.Fatalf("GetEvents failed: %v", err)
		}
		find := func(id string) (int, *copilot.UserMessageData) {
			t.Helper()
			for i, event := range observed {
				data, ok := event.Data.(*copilot.UserMessageData)
				if ok && data.MessageID != nil && *data.MessageID == id {
					return i, data
				}
			}
			t.Fatalf("Did not find user.message for %q", id)
			return -1, nil
		}
		assertDelivery := func(id string, want copilot.UserMessageDelivery) int {
			t.Helper()
			index, data := find(id)
			if data.Delivery == nil || *data.Delivery != want {
				t.Fatalf("Delivery for %q = %#v, want %q", id, data.Delivery, want)
			}
			return index
		}

		assertDelivery(idleEnqueueID, copilot.UserMessageDeliveryIdle)
		assertDelivery(idleImmediateID, copilot.UserMessageDeliveryIdle)
		steeringIndex := assertDelivery(steeringID, copilot.UserMessageDeliverySteering)
		behindIndex := assertDelivery(immediateBehindID, copilot.UserMessageDeliverySteering)
		queuedIndex := assertDelivery(queuedID, copilot.UserMessageDeliveryQueued)
		if !(steeringIndex < behindIndex && behindIndex < queuedIndex) {
			t.Fatalf("Unexpected delivery order: steering=%d behind=%d queued=%d", steeringIndex, behindIndex, queuedIndex)
		}
	})
}
