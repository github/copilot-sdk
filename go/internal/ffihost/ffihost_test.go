package ffihost

import "testing"

func TestDisposeUnregistersOutboundTarget(t *testing.T) {
	token := uintptr(nextOutboundToken.Add(1))
	host := &Host{
		recv:          newReceiveBuffer(),
		callbackToken: token,
	}
	outboundTargets.Store(token, host)

	host.Dispose()

	if _, ok := outboundTargets.Load(token); ok {
		t.Fatal("Expected disposed host to be removed from outbound callback registry")
	}
}
