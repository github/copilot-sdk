package copilot

import "github.com/github/copilot-sdk/go/rpc"

// Deprecated: use WorkflowRunUpdatedData.
type FactoryRunUpdatedData = rpc.FactoryRunUpdatedData

// Deprecated: use WorkflowRunStartedData.
type FactoryRunStartedData = rpc.FactoryRunStartedData

// Deprecated: use WorkflowRunSettledData.
type FactoryRunSettledData = rpc.FactoryRunSettledData

// Deprecated: use WorkflowRunSettledStatus.
type FactoryRunSettledStatus = rpc.FactoryRunSettledStatus

const (
	FactoryRunSettledStatusCancelled = rpc.FactoryRunSettledStatusCancelled
	FactoryRunSettledStatusCompleted = rpc.FactoryRunSettledStatusCompleted
	FactoryRunSettledStatusError     = rpc.FactoryRunSettledStatusError
	FactoryRunSettledStatusHalted    = rpc.FactoryRunSettledStatusHalted
	FactoryRunSettledStatusPaused    = rpc.FactoryRunSettledStatusPaused

	SessionEventTypeFactoryRunSettled = rpc.SessionEventTypeFactoryRunSettled
	SessionEventTypeFactoryRunStarted = rpc.SessionEventTypeFactoryRunStarted
	SessionEventTypeFactoryRunUpdated = rpc.SessionEventTypeFactoryRunUpdated
)
