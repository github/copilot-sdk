package rpc

import (
	"encoding/json"
	"testing"
)

func TestQueuePendingItemsMessageIDJSONCompatibility(t *testing.T) {
	var item QueuePendingItems
	if err := json.Unmarshal([]byte(`{
		"id": "queue-1",
		"messageId": "message-1",
		"kind": "message",
		"displayText": "hello",
		"agentMode": "interactive"
	}`), &item); err != nil {
		t.Fatal(err)
	}
	if item.MessageID == nil || *item.MessageID != "message-1" {
		t.Fatalf("MessageID = %v, want message-1", item.MessageID)
	}

	encoded, err := json.Marshal(item)
	if err != nil {
		t.Fatal(err)
	}
	var wire map[string]any
	if err := json.Unmarshal(encoded, &wire); err != nil {
		t.Fatal(err)
	}
	if got := wire["messageId"]; got != "message-1" {
		t.Fatalf("messageId = %v, want message-1", got)
	}

	var olderItem QueuePendingItems
	if err := json.Unmarshal([]byte(`{
		"id": "queue-2",
		"kind": "command",
		"displayText": "/help",
		"agentMode": "interactive"
	}`), &olderItem); err != nil {
		t.Fatal(err)
	}
	if olderItem.MessageID != nil {
		t.Fatalf("MessageID = %v, want nil", olderItem.MessageID)
	}

	encoded, err = json.Marshal(olderItem)
	if err != nil {
		t.Fatal(err)
	}
	wire = nil
	if err := json.Unmarshal(encoded, &wire); err != nil {
		t.Fatal(err)
	}
	if _, ok := wire["messageId"]; ok {
		t.Fatal("messageId should be omitted when absent")
	}
}

func TestUserMessageDataMessageIDJSONCompatibility(t *testing.T) {
	var message UserMessageData
	if err := json.Unmarshal([]byte(`{"content":"hello","messageId":"message-1"}`), &message); err != nil {
		t.Fatal(err)
	}
	if message.MessageID == nil || *message.MessageID != "message-1" {
		t.Fatalf("MessageID = %v, want message-1", message.MessageID)
	}

	encoded, err := json.Marshal(message)
	if err != nil {
		t.Fatal(err)
	}
	var wire map[string]any
	if err := json.Unmarshal(encoded, &wire); err != nil {
		t.Fatal(err)
	}
	if got := wire["messageId"]; got != "message-1" {
		t.Fatalf("messageId = %v, want message-1", got)
	}

	var olderMessage UserMessageData
	if err := json.Unmarshal([]byte(`{"content":"hello"}`), &olderMessage); err != nil {
		t.Fatal(err)
	}
	if olderMessage.MessageID != nil {
		t.Fatalf("MessageID = %v, want nil", olderMessage.MessageID)
	}

	encoded, err = json.Marshal(olderMessage)
	if err != nil {
		t.Fatal(err)
	}
	wire = nil
	if err := json.Unmarshal(encoded, &wire); err != nil {
		t.Fatal(err)
	}
	if _, ok := wire["messageId"]; ok {
		t.Fatal("messageId should be omitted when absent")
	}
}
