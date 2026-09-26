package rpc

import (
	"encoding/json"
	"reflect"
	"testing"
)

func checkAdmissionCorrelationJSON[T any](t *testing.T, base string) {
	t.Helper()
	for _, value := range []any{nil, "01234567-89ab-4cde-8f01-23456789abcd",
		"01234567-89AB-4CDE-8F01-23456789ABCD", "not-a-uuid", ""} {
		var expected map[string]any
		if err := json.Unmarshal([]byte(base), &expected); err != nil {
			t.Fatal(err)
		}
		if value != nil {
			expected["clientCorrelationId"] = value
		}
		encoded, err := json.Marshal(expected)
		if err != nil {
			t.Fatal(err)
		}
		for _, input := range [][]byte{encoded, append(encoded[:len(encoded)-1:len(encoded)-1], []byte(`,"futureField":true}`)...)} {
			var decoded T
			if err := json.Unmarshal(input, &decoded); err != nil {
				t.Fatal(err)
			}
			roundTrip, err := json.Marshal(decoded)
			if err != nil {
				t.Fatal(err)
			}
			var actual map[string]any
			if err := json.Unmarshal(roundTrip, &actual); err != nil {
				t.Fatal(err)
			}
			if !reflect.DeepEqual(actual, expected) {
				t.Fatalf("round trip changed admission metadata: got %#v, want %#v", actual, expected)
			}
		}
	}
	var decoded T
	input := base[:len(base)-1] + `,"clientCorrelationId":null}`
	if err := json.Unmarshal([]byte(input), &decoded); err != nil {
		t.Fatal(err)
	}
	encoded, err := json.Marshal(decoded)
	if err != nil {
		t.Fatal(err)
	}
	var actual map[string]any
	if err := json.Unmarshal(encoded, &actual); err != nil {
		t.Fatal(err)
	}
	if _, exists := actual["clientCorrelationId"]; exists {
		t.Fatal("nil admission metadata must be omitted")
	}
}

func TestAdmissionCorrelationJSONCompatibility(t *testing.T) {
	checkAdmissionCorrelationJSON[SendRequest](t, `{"prompt":"hello"}`)
	checkAdmissionCorrelationJSON[SendMessageItem](t, `{"prompt":"hello"}`)
	checkAdmissionCorrelationJSON[QueuePendingItems](t, `{"id":"queue-1","messageId":"canonical-1","kind":"message","displayText":"hello","agentMode":"interactive"}`)
	checkAdmissionCorrelationJSON[UserMessageData](t, `{"content":"hello","messageId":"canonical-1","interactionId":"agent-loop-1"}`)
}

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

func TestToolExecutionStartTraceContextJSONCompatibility(t *testing.T) {
	for _, context := range []map[string]string{
		{},
		{
			"traceparent": "00-11111111111111111111111111111111-2222222222222222-01",
			"tracestate":  "vendor=value",
		},
		{"traceparent": "00-11111111111111111111111111111111-2222222222222222-00"},
		{"traceparent": "invalid", "tracestate": "invalid"},
		{"traceparent": ""},
	} {
		wire := map[string]string{"toolCallId": "tool-call-a", "toolName": "client-tool"}
		for key, value := range context {
			wire[key] = value
		}
		encoded, err := json.Marshal(wire)
		if err != nil {
			t.Fatal(err)
		}
		var data ToolExecutionStartData
		if err := json.Unmarshal(encoded, &data); err != nil {
			t.Fatal(err)
		}
		for key, value := range map[string]*string{
			"traceparent": data.Traceparent,
			"tracestate":  data.Tracestate,
		} {
			expected, present := context[key]
			if present && (value == nil || *value != expected) || !present && value != nil {
				t.Fatalf("%s presence/value changed", key)
			}
		}
		encoded, err = json.Marshal(data)
		if err != nil {
			t.Fatal(err)
		}
		var roundTrip map[string]string
		if err := json.Unmarshal(encoded, &roundTrip); err != nil {
			t.Fatal(err)
		}
		if !reflect.DeepEqual(wire, roundTrip) {
			t.Fatal("tool-start data changed during round trip")
		}
	}
}
