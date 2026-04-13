package rpc

import "encoding/json"

// MarshalJSON serializes ToolsHandlePendingToolCall as the appropriate JSON variant:
// a plain string when String is set, or the ToolCallResult object otherwise.
// The generated struct has no custom marshaler, so without this the Go
// struct fields would serialize as {"ToolCallResult":...,"String":...}
// instead of the union the server expects.
func (r ToolsHandlePendingToolCall) MarshalJSON() ([]byte, error) {
	if r.String != nil {
		return json.Marshal(*r.String)
	}
	if r.ToolCallResult != nil {
		return json.Marshal(*r.ToolCallResult)
	}
	return []byte("null"), nil
}

// UnmarshalJSON deserializes a JSON value into the appropriate ToolsHandlePendingToolCall variant.
func (r *ToolsHandlePendingToolCall) UnmarshalJSON(data []byte) error {
	// Try string first
	var s string
	if err := json.Unmarshal(data, &s); err == nil {
		r.String = &s
		return nil
	}
	// Try ToolCallResult object
	var rr ToolCallResult
	if err := json.Unmarshal(data, &rr); err == nil {
		r.ToolCallResult = &rr
		return nil
	}
	return nil
}
