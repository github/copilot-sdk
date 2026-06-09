package e2e

import (
	"strings"
	"testing"

	"github.com/github/copilot-sdk/go/internal/e2e/testharness"
	"github.com/github/copilot-sdk/go/rpc"
)

func TestRpcServerRemoteControl(t *testing.T) {
	ctx := testharness.NewTestContext(t)

	t.Run("should_report_remote_control_status_as_off", func(t *testing.T) {
		ctx.ConfigureForTest(t)
		client := newStartedPortedClient(t, ctx)
		defer client.ForceStop()

		result, err := client.RPC.Sessions.GetRemoteControlStatus(t.Context())
		if err != nil {
			t.Fatalf("Sessions.GetRemoteControlStatus failed: %v", err)
		}
		assertPortedRemoteControlOff(t, result.Status)
	})

	t.Run("should_treat_set_steering_as_no_op_when_off", func(t *testing.T) {
		ctx.ConfigureForTest(t)
		client := newStartedPortedClient(t, ctx)
		defer client.ForceStop()

		result, err := client.RPC.Sessions.SetRemoteControlSteering(t.Context(), &rpc.SessionsSetRemoteControlSteeringRequest{Enabled: false})
		if err != nil {
			t.Fatalf("Sessions.SetRemoteControlSteering failed: %v", err)
		}
		assertPortedRemoteControlOff(t, result.Status)
	})

	t.Run("should_report_not_stopped_when_remote_control_is_off", func(t *testing.T) {
		ctx.ConfigureForTest(t)
		client := newStartedPortedClient(t, ctx)
		defer client.ForceStop()

		result, err := client.RPC.Sessions.StopRemoteControl(t.Context(), &rpc.SessionsStopRemoteControlRequest{})
		if err != nil {
			t.Fatalf("Sessions.StopRemoteControl failed: %v", err)
		}
		if result.Stopped {
			t.Fatalf("Expected Stopped=false, got %+v", result)
		}
		assertPortedRemoteControlOff(t, result.Status)
	})

	t.Run("should_reject_transfer_when_off_with_compare_and_swap", func(t *testing.T) {
		ctx.ConfigureForTest(t)
		client := newStartedPortedClient(t, ctx)
		defer client.ForceStop()

		from := "rc-from-" + randomHex(t)
		result, err := client.RPC.Sessions.TransferRemoteControl(t.Context(), &rpc.SessionsTransferRemoteControlRequest{
			ToSessionID:           "rc-to-" + randomHex(t),
			ExpectedFromSessionID: &from,
		})
		if err != nil {
			t.Fatalf("Sessions.TransferRemoteControl failed: %v", err)
		}
		if result.Transferred {
			t.Fatalf("Expected Transferred=false, got %+v", result)
		}
		assertPortedRemoteControlOff(t, result.Status)
	})

	t.Run("should_reach_runtime_when_starting_remote_control_for_unknown_session", func(t *testing.T) {
		ctx.ConfigureForTest(t)
		client := newStartedPortedClient(t, ctx)
		defer client.ForceStop()
		defer func() {
			force := true
			_, _ = client.RPC.Sessions.StopRemoteControl(t.Context(), &rpc.SessionsStopRemoteControlRequest{Force: &force})
		}()

		_, err := client.RPC.Sessions.StartRemoteControl(t.Context(), &rpc.SessionsStartRemoteControlRequest{
			SessionID: "missing-session-" + randomHex(t),
			Config: rpc.RemoteControlConfig{
				Remote:    false,
				Explicit:  false,
				Silent:    true,
				Steerable: false,
			},
		})
		if err == nil {
			t.Fatal("Expected StartRemoteControl for an unknown session to fail")
		}
		message := err.Error()
		assertPortedNoUnhandledMethod(t, message)
		if !strings.Contains(strings.ToLower(message), "session") && !strings.Contains(strings.ToLower(message), "remote") {
			t.Fatalf("Expected error to mention session or remote, got %s", message)
		}
	})
}

func assertPortedRemoteControlOff(t *testing.T, status rpc.RemoteControlStatus) {
	t.Helper()
	if status == nil {
		t.Fatal("Expected remote control status, got nil")
	}
	if status.State() != rpc.RemoteControlStatusStateOff {
		t.Fatalf("Expected remote control state off, got %s (%T)", status.State(), status)
	}
	if _, ok := status.(*rpc.RemoteControlStatusOff); !ok {
		t.Fatalf("Expected *RemoteControlStatusOff, got %T", status)
	}
}
