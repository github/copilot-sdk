// Copyright (c) GitHub. All rights reserved.

package rpc

import "encoding/json"

// MarshalJSON preserves the required array without changing the caller's patch.
func (patch FeatureFlagConfigPatch) MarshalJSON() ([]byte, error) {
	type wirePatch FeatureFlagConfigPatch
	if patch.UndefinedKeys == nil {
		patch.UndefinedKeys = []string{}
	}
	return json.Marshal(wirePatch(patch))
}
