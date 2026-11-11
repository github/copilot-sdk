package rpc

import (
	"encoding/json"
	"reflect"
	"testing"
)

func TestFetchResponseModesMatchRequestModesAndRoundTrip(t *testing.T) {
	for _, mode := range []FetchResponseMode{FetchResponseModeStream, FetchResponseModeText, FetchResponseModeFile} {
		t.Run(string(mode), func(t *testing.T) {
			wire := map[string]any{
				"responseMode": string(mode), "url": "https://example.com",
				"status": float64(200), "statusText": "OK", "headers": []any{},
			}
			switch mode {
			case FetchResponseModeText:
				wire["bodyText"] = "hello"
				wire["endedAtUnverifiedEof"] = false
			case FetchResponseModeFile:
				wire["path"] = "response.txt"
				wire["sizeBytes"] = float64(5)
				wire["endedAtUnverifiedEof"] = false
			}
			data, err := json.Marshal(wire)
			if err != nil {
				t.Fatal(err)
			}
			result, err := unmarshalFetchSendResult(data)
			if err != nil {
				t.Fatal(err)
			}
			if result.ResponseMode() != mode {
				t.Fatalf("mode = %v, want %v", result.ResponseMode(), mode)
			}
			data, err = json.Marshal(result)
			if err != nil {
				t.Fatal(err)
			}
			var roundTrip map[string]any
			if err := json.Unmarshal(data, &roundTrip); err != nil {
				t.Fatal(err)
			}
			if !reflect.DeepEqual(wire, roundTrip) {
				t.Fatalf("round trip = %#v, want %#v", roundTrip, wire)
			}
		})
	}
}
