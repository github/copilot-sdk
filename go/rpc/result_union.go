package rpc

import "encoding/json"

// MarshalJSON serializes ExternalToolResult as the appropriate JSON variant:
// a plain string when String is set, or the ExternalToolTextResultForLlm object otherwise.
// The generated struct has no custom marshaler, so without this the Go
// struct fields would serialize as {"ExternalToolTextResultForLlm":...,"String":...}
// instead of the union the server expects.
func (r ExternalToolResult) MarshalJSON() ([]byte, error) {
	if r.String != nil {
		return json.Marshal(*r.String)
	}
	if r.ExternalToolTextResultForLlm != nil {
		return json.Marshal(*r.ExternalToolTextResultForLlm)
	}
	return []byte("null"), nil
}

// UnmarshalJSON deserializes a JSON value into the appropriate ExternalToolResult variant.
func (r *ExternalToolResult) UnmarshalJSON(data []byte) error {
	// Try string first
	var s string
	if err := json.Unmarshal(data, &s); err == nil {
		r.String = &s
		return nil
	}
	// Try ExternalToolTextResultForLlm object
	var rr ExternalToolTextResultForLlm
	if err := json.Unmarshal(data, &rr); err == nil {
		r.ExternalToolTextResultForLlm = &rr
		return nil
	}
	return nil
}
