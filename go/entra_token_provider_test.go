// Copyright (c) Microsoft Corporation. All rights reserved.

package copilot

import (
	"bufio"
	"context"
	"encoding/json"
	"errors"
	"net"
	"reflect"
	"testing"
	"time"

	"github.com/github/copilot-sdk/go/internal/jsonrpc2"
	"github.com/github/copilot-sdk/go/rpc"
)

func entraHarness(t *testing.T, provider EntraTokenProvider) (*Client, *installationConfirmationPeer) {
	t.Helper()
	local, remote := net.Pipe()
	transport := jsonrpc2.NewClient(local, local)
	client := &Client{
		client:               transport,
		options:              ClientOptions{Entra: &EntraAuthOptions{ClientID: "host-client", TokenProvider: provider}},
		sessions:             make(map[string]*Session),
		gitHubTokenProviders: make(map[string]GitHubTokenProvider),
		state:                stateConnected,
	}
	client.setupNotificationHandler()
	transport.Start()
	t.Cleanup(func() {
		client.ForceStop()
		_ = remote.Close()
		_ = local.Close()
	})
	return client, &installationConfirmationPeer{t: t, conn: remote, reader: bufio.NewReader(remote)}
}

func entraParams(kind string) map[string]any {
	request := map[string]any{"kind": kind, "clientId": "host-client", "reason": "signIn"}
	if kind == "token" {
		request = map[string]any{
			"kind": kind, "clientId": "resource-client", "reason": "renewal",
			"accountId": "host-account", "scopes": []string{"resource/.default"},
			"audience": "resource", "tenant": "tenant", "skipCachedAccessToken": true,
		}
	}
	return map[string]any{"request": request}
}

func TestEntraTokenProviderTypedIdentityAndResource(t *testing.T) {
	requests := make(chan rpc.EntraTokenAcquireRequest, 2)
	_, peer := entraHarness(t, func(ctx context.Context, request rpc.EntraTokenAcquireRequest) (rpc.EntraTokenAcquireResult, error) {
		requests <- request
		if request.Kind() == rpc.EntraTokenAcquireRequestKindIdentity {
			return &rpc.EntraTokenAcquireResultIdentity{AccountID: "host-account"}, nil
		}
		return &rpc.EntraTokenAcquireResultToken{AccountID: "host-account", AccessToken: "resource-token"}, nil
	})
	for index, kind := range []string{"identity", "token"} {
		peer.request(index+1, "entraToken.getToken", entraParams(kind))
		response := peer.readResponse(index + 1)
		if response.Error != nil {
			t.Fatalf("acquisition failed: %v", response.Error)
		}
		var result map[string]any
		if err := json.Unmarshal(response.Result, &result); err != nil {
			t.Fatal(err)
		}
		if result["kind"] != kind || result["accountId"] != "host-account" {
			t.Fatalf("unexpected result: %v", result)
		}
		request := waitForEntraValue(t, requests)
		encoded, err := json.Marshal(request)
		if err != nil {
			t.Fatal(err)
		}
		var observed map[string]any
		if err := json.Unmarshal(encoded, &observed); err != nil {
			t.Fatal(err)
		}
		expected, err := json.Marshal(entraParams(kind)["request"])
		if err != nil {
			t.Fatal(err)
		}
		var expectedValue map[string]any
		if err := json.Unmarshal(expected, &expectedValue); err != nil {
			t.Fatal(err)
		}
		if !reflect.DeepEqual(observed, expectedValue) {
			t.Fatalf("got %v, want %v", observed, expectedValue)
		}
	}
}

func TestEntraTokenProviderCancellationRejectsLateSuccess(t *testing.T) {
	entered := make(chan context.Context, 1)
	finished := make(chan struct{})
	_, peer := entraHarness(t, func(ctx context.Context, request rpc.EntraTokenAcquireRequest) (rpc.EntraTokenAcquireResult, error) {
		if request.Kind() == rpc.EntraTokenAcquireRequestKindToken {
			return &rpc.EntraTokenAcquireResultUnavailable{}, nil
		}
		entered <- ctx
		<-ctx.Done()
		defer close(finished)
		return &rpc.EntraTokenAcquireResultIdentity{AccountID: "late-account"}, nil
	})
	peer.request(101, "entraToken.getToken", entraParams("identity"))
	ctx := waitForEntraValue(t, entered)
	peer.notify("$/cancelRequest", map[string]any{"id": 101})
	response := peer.readResponse(101)
	if response.Error == nil || response.Error.Code != -32800 {
		t.Fatalf("unexpected cancellation: %+v", response)
	}
	waitForClosed(t, ctx.Done(), "Entra request cancellation")
	waitForClosed(t, finished, "Entra provider completion")
	peer.request(102, "entraToken.getToken", entraParams("token"))
	if response := peer.readResponse(102); response.Error != nil {
		t.Fatalf("subsequent request failed: %v", response.Error)
	}
}

func TestEntraTokenProviderConnectionCloseCancelsContext(t *testing.T) {
	entered := make(chan context.Context, 1)
	finished := make(chan struct{})
	_, peer := entraHarness(t, func(ctx context.Context, _ rpc.EntraTokenAcquireRequest) (rpc.EntraTokenAcquireResult, error) {
		entered <- ctx
		<-ctx.Done()
		close(finished)
		return &rpc.EntraTokenAcquireResultCancelled{}, nil
	})
	peer.request(201, "entraToken.getToken", entraParams("identity"))
	ctx := waitForEntraValue(t, entered)
	if err := peer.conn.Close(); err != nil {
		t.Fatal(err)
	}
	waitForClosed(t, ctx.Done(), "Entra connection cancellation")
	waitForClosed(t, finished, "Entra provider completion")
}

func waitForEntraValue[T any](t *testing.T, values <-chan T) T {
	t.Helper()
	select {
	case value := <-values:
		return value
	case <-time.After(10 * time.Second):
		t.Fatal("timed out waiting for Entra provider invocation")
		var zero T
		return zero
	}
}

func TestEntraTokenProviderErrorsDoNotExposeCredentials(t *testing.T) {
	_, peer := entraHarness(t, func(context.Context, rpc.EntraTokenAcquireRequest) (rpc.EntraTokenAcquireResult, error) {
		return nil, errors.New("credential-from-host-exception")
	})
	peer.request(301, "entraToken.getToken", entraParams("identity"))
	response := peer.readResponse(301)
	if response.Error == nil || response.Error.Code != jsonrpc2.ErrInternal.Code ||
		response.Error.Message != "Entra token provider failed" {
		t.Fatalf("unexpected callback error: %+v", response.Error)
	}
}
