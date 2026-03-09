package rpc

import "encoding/json"

// MarshalJSON serializes ResultUnion as the appropriate JSON variant:
// a plain string when String is set, or the ResultResult object otherwise.
// The generated struct has no custom marshaler, so without this the Go
// struct fields would serialize as {"ResultResult":...,"String":...}
// instead of the union the server expects.
func (r ResultUnion) MarshalJSON() ([]byte, error) {
	if r.String != nil {
		return json.Marshal(*r.String)
	}
	if r.ResultResult != nil {
		return json.Marshal(*r.ResultResult)
	}
	return []byte("null"), nil
}

// UnmarshalJSON deserializes a JSON value into the appropriate ResultUnion variant.
func (r *ResultUnion) UnmarshalJSON(data []byte) error {
	// Try string first
	var s string
	if err := json.Unmarshal(data, &s); err == nil {
		r.String = &s
		return nil
	}
	// Try ResultResult object
	var rr ResultResult
	if err := json.Unmarshal(data, &rr); err == nil {
		r.ResultResult = &rr
		return nil
	}
	return nil
}
