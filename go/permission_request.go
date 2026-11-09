package copilot

import (
	"encoding/json"

	"github.com/github/copilot-sdk/go/rpc"
)

// PermissionRequestEnvelope retains original JSON for lossless forwarding.
// Pass the envelope, not its Request, to ManagedReviewRequired.
type PermissionRequestEnvelope = rpc.PermissionRequestEnvelope

// PermissionRequestedDataWithEnvelope exposes the original permission payload.
// Pass Envelope, not PermissionRequest, to ManagedReviewRequired.
// Its envelope is independent of the permission callback's envelope.
type PermissionRequestedDataWithEnvelope struct {
	*PermissionRequestedData
	Envelope *PermissionRequestEnvelope `json:"-"`
}

func legacyPermissionEventHandler(handler SessionEventHandler) SessionEventHandler {
	if handler == nil {
		return nil
	}
	return func(event SessionEvent) {
		if data, ok := event.Data.(*PermissionRequestedDataWithEnvelope); ok {
			event.Data = data.PermissionRequestedData
		}
		handler(event)
	}
}

func (r *sessionEventRequest) UnmarshalJSON(data []byte) error {
	type wire sessionEventRequest
	var decoded wire
	if err := json.Unmarshal(data, &decoded); err != nil {
		return err
	}
	permissionData, isPermission := decoded.Event.Data.(*PermissionRequestedData)
	_, isExternalTool := decoded.Event.Data.(*ExternalToolRequestedData)
	if isPermission || isExternalTool {
		var raw struct {
			Event struct {
				Data struct {
					PermissionRequest json.RawMessage `json:"permissionRequest"`
					HasHandler        *bool           `json:"hasHandler"`
				} `json:"data"`
			} `json:"event"`
		}
		if err := json.Unmarshal(data, &raw); err != nil {
			return err
		}
		if isExternalTool {
			decoded.sdkHasHandler = raw.Event.Data.HasHandler
		}
		if isPermission && raw.Event.Data.PermissionRequest != nil {
			var envelope PermissionRequestEnvelope
			if err := json.Unmarshal(raw.Event.Data.PermissionRequest, &envelope); err != nil {
				return err
			}
			decoded.permissionEnvelope = &envelope
			var subscriberEnvelope PermissionRequestEnvelope
			if err := json.Unmarshal(raw.Event.Data.PermissionRequest, &subscriberEnvelope); err != nil {
				return err
			}
			decoded.Event.Data = &PermissionRequestedDataWithEnvelope{
				PermissionRequestedData: permissionData,
				Envelope:                &subscriberEnvelope,
			}
		}
	}
	*r = sessionEventRequest(decoded)
	return nil
}
