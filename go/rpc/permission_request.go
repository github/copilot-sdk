package rpc

import (
	"bytes"
	"encoding/json"
)

// PermissionRequestEnvelope owns a decoded request and its original JSON.
// Edits to Request merge into the original payload, including nested edits.
// Untouched unknown fields, explicit nulls, and numeric precision survive.
// Constructing an envelope with only Request uses ordinary typed encoding.
type PermissionRequestEnvelope struct {
	Request PermissionRequest

	originalJSON   json.RawMessage
	originalFields json.RawMessage
}

// UnmarshalJSON decodes a permission request and retains its original payload.
func (r *PermissionRequestEnvelope) UnmarshalJSON(data []byte) error {
	var discriminator struct {
		Kind PermissionRequestKind `json:"kind"`
	}
	if err := json.Unmarshal(data, &discriminator); err != nil {
		return err
	}
	var request PermissionRequest
	switch discriminator.Kind {
	case PermissionRequestKindCustomTool:
		request = &PermissionRequestCustomTool{}
	case PermissionRequestKindMCP:
		request = &PermissionRequestMCP{}
	case PermissionRequestKindHook:
		request = &PermissionRequestHook{}
	}
	var err error
	if request != nil {
		decoder := json.NewDecoder(bytes.NewReader(data))
		decoder.UseNumber()
		err = decoder.Decode(request)
	} else {
		request, err = unmarshalPermissionRequest(bytes.Clone(data))
	}
	if err != nil {
		return err
	}
	fields, err := json.Marshal(request)
	if err != nil {
		return err
	}
	*r = PermissionRequestEnvelope{
		Request:        request,
		originalJSON:   bytes.Clone(data),
		originalFields: fields,
	}
	return nil
}

// MarshalJSON applies typed field edits to the original request.
// Setting an optional field to nil removes it. Object edits and same-length
// single-entry array edits preserve untouched nested properties by key and index.
// Multiple changed array entries and copies of other entries return an error.
// An edit to a multi-entry array also fails when its original payload differs
// from its typed projection, because copies followed by edits are ambiguous.
// For lossless entry copies, replacements, or reorders, marshal the unedited
// envelope. Edit that JSON and decode a new envelope.
// Resized arrays and scalar values replace their originals.
func (r PermissionRequestEnvelope) MarshalJSON() ([]byte, error) {
	current, err := json.Marshal(r.Request)
	if err != nil || r.originalJSON == nil {
		return current, err
	}
	return marshalPermissionRequestWithOriginalFields(r.originalJSON, r.originalFields, current)
}

// Replace discards the original payload and replaces the complete request.
// Use this instead of field edits to discard original unknown properties.
func (r *PermissionRequestEnvelope) Replace(request PermissionRequest) {
	*r = PermissionRequestEnvelope{Request: request}
}
