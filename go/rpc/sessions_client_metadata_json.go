// Copyright (c) Microsoft Corporation. All rights reserved.

package rpc

import (
	"encoding/json"
	"fmt"
)

// UnmarshalJSON decodes the discriminated metadata result entries.
func (r *SessionsGetClientMetadataResult) UnmarshalJSON(data []byte) error {
	var entries []json.RawMessage
	if err := json.Unmarshal(data, &entries); err != nil {
		return err
	}
	if entries == nil {
		*r = nil
		return nil
	}

	result := make(SessionsGetClientMetadataResult, 0, len(entries))
	for index, entry := range entries {
		value, err := unmarshalSessionsClientMetadataEntry(entry)
		if err != nil {
			return fmt.Errorf("decode sessions client metadata entry %d: %w", index, err)
		}
		result = append(result, value)
	}

	*r = result
	return nil
}
