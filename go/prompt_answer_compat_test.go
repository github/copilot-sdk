// Copyright (c) Microsoft Corporation. All rights reserved.

package copilot_test

import (
	"testing"

	copilot "github.com/github/copilot-sdk/go"
	legacy "github.com/github/copilot-sdk/go/testdata/prompt_answer_legacy"
)

// The legacy package compiles only while the prompt answer structs keep their
// positional fields and the plain handler config fields keep their types. The
// comparisons also require the structs to stay comparable.
func TestPromptAnswerLegacyPositionalLiterals(t *testing.T) {
	if want := (copilot.UserInputResponse{Answer: "Only src/app.ts", WasFreeform: true}); legacy.UserInput != want {
		t.Errorf("positional UserInputResponse = %+v, want %+v", legacy.UserInput, want)
	}
	if want := (copilot.ExitPlanModeResult{Approved: true, SelectedAction: "interactive", Feedback: "Looks good"}); legacy.PlanReview != want {
		t.Errorf("positional ExitPlanModeResult = %+v, want %+v", legacy.PlanReview, want)
	}
	if legacy.Elicitation.Action != copilot.ElicitationActionAccept || len(legacy.Elicitation.Content) != 1 || legacy.Elicitation.Content["region"] != "us-east" {
		t.Errorf("positional ElicitationResult = %+v, want accept with region us-east", legacy.Elicitation)
	}
	if legacy.Create.OnUserInputRequest == nil || legacy.Create.OnExitPlanModeRequest == nil || legacy.Create.OnElicitationRequest == nil ||
		legacy.Resume.OnUserInputRequest == nil || legacy.Resume.OnExitPlanModeRequest == nil || legacy.Resume.OnElicitationRequest == nil {
		t.Error("plain prompt handlers were not retained in the session configs")
	}
}
