// Copyright (c) Microsoft Corporation. All rights reserved.

package rpc

import (
	"context"
	"encoding/json"
	"strings"
	"testing"
)

var _ interface {
	HandlePendingElicitationWithOptions(context.Context, *UIHandlePendingElicitationOptions) (*UIElicitationResult, error)
	HandlePendingUserInputWithOptions(context.Context, *UIHandlePendingUserInputOptions) (*UIHandlePendingResult, error)
	HandlePendingExitPlanModeWithOptions(context.Context, *UIHandlePendingExitPlanModeOptions) (*UIHandlePendingResult, error)
} = (*UIAPI)(nil)

// The published prompt-answer requests keep their positional literals; the
// label travels only through the options requests.
func TestUIHandlePendingRequestsPreserveUnkeyedLiterals(t *testing.T) {
	for _, request := range []any{
		UIHandlePendingElicitationRequest{"elicitation", UIElicitationResponse{Action: UIElicitationResponseActionAccept}},
		UIHandlePendingUserInputRequest{"input", UIUserInputResponse{Answer: "yes"}},
		UIHandlePendingExitPlanModeRequest{"plan", UIExitPlanModeResponse{Approved: true}},
	} {
		data, err := json.Marshal(request)
		if err != nil {
			t.Fatal(err)
		}
		if strings.Contains(string(data), "decisionContext") {
			t.Fatalf("legacy request %T carried a label: %s", request, data)
		}
	}
}
