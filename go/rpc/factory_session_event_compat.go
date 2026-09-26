package rpc

// FactoryRunUpdatedData is the legacy factory-run invalidation event payload.
//
// Use WorkflowRunUpdatedData for current runtime events.
type FactoryRunUpdatedData struct {
	Revision int64  `json:"revision"`
	RunID    string `json:"runId"`
}

func (*FactoryRunUpdatedData) sessionEventData() {}
func (*FactoryRunUpdatedData) Type() SessionEventType {
	return SessionEventTypeFactoryRunUpdated
}

// FactoryRunStartedData is the legacy factory-run start event payload.
//
// Use WorkflowRunStartedData for current runtime events.
type FactoryRunStartedData struct {
	Attempt     int64  `json:"attempt"`
	FactoryName string `json:"factoryName"`
	RunID       string `json:"runId"`
}

func (*FactoryRunStartedData) sessionEventData() {}
func (*FactoryRunStartedData) Type() SessionEventType {
	return SessionEventTypeFactoryRunStarted
}

// FactoryRunSettledData is the legacy factory-run terminal event payload.
//
// Use WorkflowRunSettledData for current runtime events.
type FactoryRunSettledData struct {
	ConsumedNanoAiu   int64                   `json:"consumedNanoAiu"`
	ConsumedSubagents int64                   `json:"consumedSubagents"`
	ElapsedMs         int64                   `json:"elapsedMs"`
	FailureType       *string                 `json:"failureType,omitempty"`
	RunID             string                  `json:"runId"`
	Status            FactoryRunSettledStatus `json:"status"`
}

func (*FactoryRunSettledData) sessionEventData() {}
func (*FactoryRunSettledData) Type() SessionEventType {
	return SessionEventTypeFactoryRunSettled
}

// FactoryRunSettledStatus is the legacy factory-run terminal status.
//
// Use WorkflowRunSettledStatus for current runtime events.
type FactoryRunSettledStatus string

const (
	FactoryRunSettledStatusCancelled FactoryRunSettledStatus = "cancelled"
	FactoryRunSettledStatusCompleted FactoryRunSettledStatus = "completed"
	FactoryRunSettledStatusError     FactoryRunSettledStatus = "error"
	FactoryRunSettledStatusHalted    FactoryRunSettledStatus = "halted"
	FactoryRunSettledStatusPaused    FactoryRunSettledStatus = "paused"
)

const (
	SessionEventTypeFactoryRunSettled SessionEventType = "factory.run_settled"
	SessionEventTypeFactoryRunStarted SessionEventType = "factory.run_started"
	SessionEventTypeFactoryRunUpdated SessionEventType = "factory.run_updated"
)
