// Copyright (c) Microsoft Corporation. All rights reserved.

package copilot

import (
	"encoding/json"
	"os"
	"reflect"
	"testing"

	"github.com/github/copilot-sdk/go/rpc"
)

type workerCausalityCorpus struct {
	Valid []struct {
		Name   string
		Event  json.RawMessage
		Result json.RawMessage
	}
	Invalid []struct {
		Name  string
		Value any
	}
	Boundaries []struct {
		Name     string
		Value    any
		Accepted bool
	}
}

func workerCorpus(t *testing.T) workerCausalityCorpus {
	t.Helper()
	data, err := os.ReadFile("../test/worker-causality.json")
	if err != nil {
		t.Fatal(err)
	}
	var corpus workerCausalityCorpus
	if err := json.Unmarshal(data, &corpus); err != nil {
		t.Fatal(err)
	}
	return corpus
}

func workerProduct(t *testing.T, value map[string]any, event bool) []byte {
	t.Helper()
	wire, err := json.Marshal(value)
	if err != nil {
		t.Fatal(err)
	}
	var product any = &rpc.TasksSendMessageResult{}
	if event {
		product = &SessionEvent{}
	}
	if err := json.Unmarshal(wire, product); err != nil {
		t.Fatal(err)
	}
	result, err := json.Marshal(product)
	if err != nil {
		t.Fatal(err)
	}
	return result
}

func workerPayload(t *testing.T, wire []byte, event bool) (map[string]any, map[string]any) {
	t.Helper()
	var value map[string]any
	if err := json.Unmarshal(wire, &value); err != nil {
		t.Fatal(err)
	}
	if event {
		return value, value["data"].(map[string]any)
	}
	return value, value
}

func TestWorkerCausalityPublicReaders(t *testing.T) {
	corpus := workerCorpus(t)
	for _, test := range corpus.Valid {
		t.Run(test.Name, func(t *testing.T) {
			event := len(test.Event) > 0
			wire := test.Result
			if event {
				wire = test.Event
			}
			value, payload := workerPayload(t, wire, event)
			_, result := workerPayload(t, workerProduct(t, value, event), event)
			if !reflect.DeepEqual(result["workerCausality"], payload["workerCausality"]) {
				t.Fatal("typed reader changed captured source identities")
			}
			delete(payload, "workerCausality")
			baseline := string(workerProduct(t, value, event))
			for _, invalid := range corpus.Invalid {
				payload["workerCausality"] = invalid.Value
				if actual := string(workerProduct(t, value, event)); actual != baseline {
					t.Fatalf("%s changed product bytes: %s", invalid.Name, actual)
				}
			}
		})
	}
}

func TestWorkerCausalityExactUTF8Budget(t *testing.T) {
	corpus := workerCorpus(t)
	for _, test := range corpus.Boundaries {
		t.Run(test.Name, func(t *testing.T) {
			value, payload := workerPayload(t, corpus.Valid[0].Event, true)
			payload["workerCausality"] = test.Value
			_, result := workerPayload(t, workerProduct(t, value, true), true)
			if _, present := result["workerCausality"]; present != test.Accepted {
				t.Fatalf("metadata presence = %v, want %v", present, test.Accepted)
			}
			if result["content"] != payload["content"] {
				t.Fatal("product content changed")
			}
		})
	}
}
