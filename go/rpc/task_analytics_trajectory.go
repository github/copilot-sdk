// Copyright (c) Microsoft Corporation. All rights reserved.

package rpc

import "encoding/json"

// SessionEventsAsTrajectory converts recorded session events, such as the
// result of Session.GetEvents or Session.GetRawEvents, into the Events slice of
// TaskAnalyticsClassifyTrajectoryRequest. Use GetRawEvents for exact archive
// identity across retries; typed events can normalize or discard JSON fields.
// Go does not convert either slice to []any implicitly.
//
// Experimental: this helper is part of an experimental API and may change or
// be removed.
func SessionEventsAsTrajectory[T SessionEvent | json.RawMessage](events []T) []any {
	trajectory := make([]any, len(events))
	for i, event := range events {
		trajectory[i] = event
	}
	return trajectory
}
