// Copyright (c) Microsoft Corporation. All rights reserved.

package rpc

import (
	"encoding/json"
	"testing"
)

func TestLegacyFactoryEventsDecodeToCompatibilityTypes(t *testing.T) {
	tests := []struct {
		name string
		wire string
		want SessionEventData
	}{
		{
			name: "updated",
			wire: `{"type":"factory.run_updated","data":{"revision":2,"runId":"run-1"}}`,
			want: &FactoryRunUpdatedData{Revision: 2, RunID: "run-1"},
		},
		{
			name: "started",
			wire: `{"type":"factory.run_started","data":{"attempt":3,"factoryName":"fix-ci","runId":"run-1"}}`,
			want: &FactoryRunStartedData{Attempt: 3, FactoryName: "fix-ci", RunID: "run-1"},
		},
		{
			name: "settled",
			wire: `{"type":"factory.run_settled","data":{"consumedNanoAiu":4,"consumedSubagents":1,"elapsedMs":5,"runId":"run-1","status":"completed"}}`,
			want: &FactoryRunSettledData{
				ConsumedNanoAiu:   4,
				ConsumedSubagents: 1,
				ElapsedMs:         5,
				RunID:             "run-1",
				Status:            FactoryRunSettledStatusCompleted,
			},
		},
	}

	for _, test := range tests {
		t.Run(test.name, func(t *testing.T) {
			var event SessionEvent
			if err := json.Unmarshal([]byte(test.wire), &event); err != nil {
				t.Fatal(err)
			}
			switch want := test.want.(type) {
			case *FactoryRunUpdatedData:
				got, ok := event.Data.(*FactoryRunUpdatedData)
				if !ok || *got != *want {
					t.Fatalf("data = %#v", event.Data)
				}
			case *FactoryRunStartedData:
				got, ok := event.Data.(*FactoryRunStartedData)
				if !ok || *got != *want {
					t.Fatalf("data = %#v", event.Data)
				}
			case *FactoryRunSettledData:
				got, ok := event.Data.(*FactoryRunSettledData)
				if !ok || *got != *want {
					t.Fatalf("data = %#v", event.Data)
				}
			}
		})
	}
}
