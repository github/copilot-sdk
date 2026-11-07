// Copyright (c) Microsoft Corporation. All rights reserved.

package legacy

import (
	copilot "github.com/github/copilot-sdk/go"
	"github.com/github/copilot-sdk/go/rpc"
)

var (
	_ = copilot.CapabilitiesChangedUI{nil, nil, nil}
	_ = rpc.CapabilitiesChangedUI{nil, nil, nil}
	_ = copilot.UICapabilities{true, false}
	_ = copilot.AutoModeSwitchRequest{nil, nil}
	_ = rpc.UIRegisterDirectAutoModeSwitchHandlerResult{"registration"}
	_ = copilot.SessionConfig{
		OnAutoModeSwitchRequest: func(copilot.AutoModeSwitchRequest, copilot.AutoModeSwitchInvocation) (copilot.AutoModeSwitchResponse, error) {
			return copilot.AutoModeSwitchResponseNo, nil
		},
	}
)
