// Copyright (c) Microsoft Corporation. All rights reserved.

package copilot

import (
	"bufio"
	"context"
	"encoding/json"
	"errors"
	"fmt"
	"io"
	"net"
	"strconv"
	"strings"
	"sync"
	"testing"
	"time"

	"github.com/github/copilot-sdk/go/internal/jsonrpc2"
	"github.com/github/copilot-sdk/go/rpc"
)

const installationConfirmationFixture = `{
  "confirmationId": "confirmation-a",
  "operationId": "operation-a",
  "policySessionId": "original-session",
  "expiresAt": "2026-09-24T03:00:00Z",
  "reviewFingerprint": "fingerprint-a",
  "review": {
    "resource": "mcp",
    "review": {
      "action": "install",
      "identity": {
        "canonicalName": "io.example/server",
        "serverName": "example"
      },
      "provenance": {
        "authority": "cards.example.test",
        "validatedAt": "2026-09-24T02:59:00Z",
        "cardDigest": {
          "algorithm": "sha256-rfc8785",
          "value": "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef"
        },
        "mediaType": "application/mcp-server-card+json"
      },
      "target": { "scope": "user", "configKey": "example" },
      "policy": { "decision": "allowed", "source": "none" },
      "selectedChoice": {
        "choiceId": "remote-choice",
        "installMethod": "remote",
        "endpoint": "https://example.test/mcp",
        "transport": "streamable-http",
        "requiredValues": [],
        "secretPlaceholders": []
      },
      "configurationChange": {
        "operation": "add",
        "scope": "user",
        "configKey": "example",
        "changedFields": ["type", "url", "headers", "tools"],
        "secretReferences": []
      },
      "inputs": [],
      "suppliedSecrets": [],
      "secretStorage": "keychain",
      "effectiveConfiguration": {
        "transport": "streamable-http",
        "url": "https://example.test/mcp",
        "headers": { "X-Region": "eu" },
        "tools": ["*"]
      }
    }
  }
}`

const skillInstallationConfirmationFixture = `{
  "confirmationId": "confirmation-skill",
  "operationId": "operation-skill",
  "policySessionId": "original-session",
  "expiresAt": "2026-09-24T03:00:00Z",
  "reviewFingerprint": "fingerprint-skill",
  "review": {
    "resource": "skill",
    "review": {
      "action": "install",
      "name": "demo",
      "description": "Demo Skill",
      "catalogue": {
        "resourceId": "123",
        "displayName": "Demo Skill",
        "description": "Demo Skill",
        "publisher": "Octo",
        "source": "https://agentfinder.github.com"
      },
      "source": {
        "resourceId": "123",
        "catalogRevisionId": "1",
        "repositoryId": "42",
        "repository": "octo/demo",
        "revision": "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa",
        "root": "",
        "descriptorDigest": "sha256:1111111111111111111111111111111111111111111111111111111111111111",
        "bundleDigest": "sha256:2222222222222222222222222222222222222222222222222222222222222222"
      },
      "target": {
        "scope": "personal",
        "displayLabel": "~/.copilot/skills/demo",
        "relativePath": "skills/demo",
        "diagnosticsAbsolutePath": "/home/skills/demo"
      },
      "installsDisabled": true,
      "files": [{
        "path": "SKILL.md",
        "sizeBytes": 12,
        "mediaType": "text/markdown",
        "executable": false,
        "digest": "sha256:3333333333333333333333333333333333333333333333333333333333333333"
      }],
      "totalBytes": 12,
      "entrypointPath": "SKILL.md",
      "entrypointContent": "---\nname: demo\n---"
    }
  }
}`

type installationConfirmationCall struct {
	request InstallationConfirmationRequest
	context InstallationConfirmationContext
}

type installationConfirmationPeer struct {
	t      *testing.T
	conn   net.Conn
	reader *bufio.Reader
	writeM sync.Mutex
}

type installationConfirmationMessage struct {
	JSONRPC string          `json:"jsonrpc"`
	ID      json.RawMessage `json:"id,omitempty"`
	Method  string          `json:"method,omitempty"`
	Params  json.RawMessage `json:"params,omitempty"`
	Result  json.RawMessage `json:"result,omitempty"`
	Error   *jsonrpc2.Error `json:"error,omitempty"`
}

func newInstallationConfirmationHarness(t *testing.T, handler InstallationConfirmationHandler) (*Client, *installationConfirmationPeer) {
	t.Helper()

	clientConn, serverConn := net.Pipe()
	rpcClient := jsonrpc2.NewClient(clientConn, clientConn)
	client := &Client{
		client: rpcClient,
		options: ClientOptions{
			InstallationConfirmationHandler: handler,
		},
		sessions:             make(map[string]*Session),
		gitHubTokenProviders: make(map[string]GitHubTokenProvider),
		state:                stateConnected,
	}
	client.setupNotificationHandler()
	rpcClient.Start()
	peer := &installationConfirmationPeer{t: t, conn: serverConn, reader: bufio.NewReader(serverConn)}
	t.Cleanup(func() {
		client.ForceStop()
		_ = serverConn.Close()
		_ = clientConn.Close()
	})
	return client, peer
}

func installationConfirmationRequest(t *testing.T, operation string) InstallationConfirmationRequest {
	t.Helper()
	var request InstallationConfirmationRequest
	if err := json.Unmarshal([]byte(installationConfirmationFixture), &request); err != nil {
		t.Fatal(err)
	}
	request.OperationID = operation
	request.ConfirmationID = "challenge-" + operation
	request.ReviewFingerprint = "fingerprint-" + operation
	return request
}

func (p *installationConfirmationPeer) request(id any, method string, params any) {
	p.t.Helper()
	p.write(map[string]any{
		"jsonrpc": "2.0",
		"id":      id,
		"method":  method,
		"params":  params,
	})
}

func (p *installationConfirmationPeer) notify(method string, params any) {
	p.t.Helper()
	p.write(map[string]any{
		"jsonrpc": "2.0",
		"method":  method,
		"params":  params,
	})
}

func (p *installationConfirmationPeer) write(message any) {
	p.t.Helper()
	body, err := json.Marshal(message)
	if err != nil {
		p.t.Fatal(err)
	}
	p.writeM.Lock()
	defer p.writeM.Unlock()
	if _, err := fmt.Fprintf(p.conn, "Content-Length: %d\r\n\r\n%s", len(body), body); err != nil {
		p.t.Fatalf("write frame: %v", err)
	}
}

func (p *installationConfirmationPeer) readResponse(id any) installationConfirmationMessage {
	p.t.Helper()
	want := fmt.Sprint(id)
	for {
		message := p.readMessage()
		if len(message.ID) == 0 {
			continue
		}
		got, ok := jsonIDString(message.ID)
		if ok && got == want {
			return message
		}
		p.t.Fatalf("received response id %s, want %s: %+v", message.ID, want, message)
	}
}

func (p *installationConfirmationPeer) readMessage() installationConfirmationMessage {
	p.t.Helper()
	if err := p.conn.SetReadDeadline(time.Now().Add(10 * time.Second)); err != nil {
		p.t.Fatal(err)
	}
	defer p.conn.SetReadDeadline(time.Time{})
	length := 0
	for {
		line, err := p.reader.ReadString('\n')
		if err != nil {
			p.t.Fatalf("read header: %v", err)
		}
		line = strings.TrimSpace(line)
		if line == "" {
			break
		}
		name, value, ok := strings.Cut(line, ":")
		if ok && name == "Content-Length" {
			length, err = strconv.Atoi(strings.TrimSpace(value))
			if err != nil {
				p.t.Fatal(err)
			}
		}
	}
	if length <= 0 || length > 1024*1024 {
		p.t.Fatalf("invalid Content-Length %d", length)
	}
	body := make([]byte, length)
	if _, err := io.ReadFull(p.reader, body); err != nil {
		p.t.Fatalf("read body: %v", err)
	}
	var message installationConfirmationMessage
	if err := json.Unmarshal(body, &message); err != nil {
		p.t.Fatalf("unmarshal message %s: %v", body, err)
	}
	return message
}

func jsonIDString(id json.RawMessage) (string, bool) {
	decoder := json.NewDecoder(strings.NewReader(string(id)))
	decoder.UseNumber()
	var value any
	if err := decoder.Decode(&value); err != nil {
		return "", false
	}
	switch value := value.(type) {
	case json.Number:
		return value.String(), true
	case string:
		return value, true
	default:
		return "", false
	}
}

func waitInstallationConfirmationCall(t *testing.T, calls <-chan installationConfirmationCall) installationConfirmationCall {
	t.Helper()
	select {
	case call := <-calls:
		return call
	case <-time.After(10 * time.Second):
		t.Fatal("timed out waiting for installation confirmation handler")
		return installationConfirmationCall{}
	}
}

func waitForClosed(t *testing.T, ch <-chan struct{}, name string) {
	t.Helper()
	select {
	case <-ch:
	case <-time.After(10 * time.Second):
		t.Fatalf("timed out waiting for %s", name)
	}
}

func assertContextOpen(t *testing.T, ctx context.Context, name string) {
	t.Helper()
	select {
	case <-ctx.Done():
		t.Fatalf("%s was cancelled", name)
	default:
	}
}

func decodeInstallationConfirmationResult(t *testing.T, message installationConfirmationMessage) rpc.InstallationConfirmationResponse {
	t.Helper()
	if message.Error != nil {
		t.Fatalf("unexpected error response: %v", message.Error)
	}
	var response rpc.InstallationConfirmationResponse
	if err := json.Unmarshal(message.Result, &response); err != nil {
		t.Fatal(err)
	}
	return response
}

func TestInstallationConfirmationCancelledBeforeCallbackDoesNotStartHandler(t *testing.T) {
	for i := 0; i < 50; i++ {
		called := false
		connectionClosed := make(chan struct{})
		adapter := newInstallationConfirmationAdapter(InstallationConfirmationHandlerFunc(func(InstallationConfirmationRequest, InstallationConfirmationContext) (InstallationConfirmationDecision, error) {
			called = true
			return InstallationConfirmationDecisionConfirm, nil
		}), connectionClosed)

		ctx, cancel := context.WithCancel(context.Background())
		cancel()
		raw, err := json.Marshal(installationConfirmationRequest(t, fmt.Sprintf("pre-cancel-%d", i)))
		if err != nil {
			t.Fatal(err)
		}
		result, rpcErr := adapter.handle(ctx, raw)
		adapter.close()

		if rpcErr == nil || rpcErr.Code != installationConfirmationRequestCancelledCode {
			t.Fatalf("iteration %d: expected request-cancelled response, got result %s error %+v", i, result, rpcErr)
		}
		if called {
			t.Fatalf("iteration %d: handler started after cancellation", i)
		}
	}
}

func TestInstallationConfirmationCancellationBeatsDecisionReadyTogether(t *testing.T) {
	for i := 0; i < 200; i++ {
		connectionClosed := make(chan struct{})
		ctx, cancel := context.WithCancel(context.Background())
		called := make(chan struct{}, 1)
		adapter := newInstallationConfirmationAdapter(InstallationConfirmationHandlerFunc(func(InstallationConfirmationRequest, InstallationConfirmationContext) (InstallationConfirmationDecision, error) {
			called <- struct{}{}
			cancel()
			return InstallationConfirmationDecisionConfirm, nil
		}), connectionClosed)

		raw, err := json.Marshal(installationConfirmationRequest(t, fmt.Sprintf("race-%d", i)))
		if err != nil {
			t.Fatal(err)
		}
		result, rpcErr := adapter.handle(ctx, raw)
		adapter.close()

		if rpcErr == nil || rpcErr.Code != installationConfirmationRequestCancelledCode {
			t.Fatalf("iteration %d: expected request-cancelled response, got result %s error %+v", i, result, rpcErr)
		}
		select {
		case <-called:
		default:
			t.Fatalf("iteration %d: handler was not called", i)
		}
	}
}

func TestInstallationConfirmationSkillReviewDeserializesAsTypedVariant(t *testing.T) {
	var request InstallationConfirmationRequest
	if err := json.Unmarshal([]byte(skillInstallationConfirmationFixture), &request); err != nil {
		t.Fatal(err)
	}
	skillReview, ok := request.Review.(*rpc.InstallationReviewSkill)
	if !ok {
		t.Fatalf("review type = %T, want *rpc.InstallationReviewSkill", request.Review)
	}
	install, ok := skillReview.Review.(*rpc.SkillInstallationReviewInstall)
	if !ok {
		t.Fatalf("skill review type = %T, want *rpc.SkillInstallationReviewInstall", skillReview.Review)
	}
	if install.Name != "demo" || !install.InstallsDisabled {
		t.Fatalf("unexpected skill install review: %+v", install)
	}
}

func TestInstallationConfirmationTypedReviewEchoesChallengeAndFingerprint(t *testing.T) {
	calls := make(chan installationConfirmationCall, 1)
	release := make(chan struct{})
	_, peer := newInstallationConfirmationHarness(t, InstallationConfirmationHandlerFunc(func(request InstallationConfirmationRequest, context InstallationConfirmationContext) (InstallationConfirmationDecision, error) {
		calls <- installationConfirmationCall{request: request, context: context}
		mcpReview, ok := request.Review.(*rpc.InstallationReviewMCP)
		if !ok {
			t.Fatalf("review type = %T, want *rpc.InstallationReviewMCP", request.Review)
		}
		if mcpReview.Review.Action() != rpc.MCPInstallationReviewActionInstall {
			t.Fatalf("review action = %q", mcpReview.Review.Action())
		}
		request.ConfirmationID = "mutated-by-handler"
		request.ReviewFingerprint = "mutated-by-handler"
		<-release
		return InstallationConfirmationDecisionConfirm, nil
	}))

	peer.request(101, "installations.confirm", installationConfirmationRequest(t, "a"))
	call := waitInstallationConfirmationCall(t, calls)
	if call.request.OperationID != "a" || call.request.PolicySessionID == nil || *call.request.PolicySessionID != "original-session" {
		t.Fatalf("unexpected request identity: %+v", call.request)
	}
	assertContextOpen(t, call.context, "confirmation context")

	close(release)
	response := decodeInstallationConfirmationResult(t, peer.readResponse(101))
	if response.ConfirmationID != "challenge-a" || response.ReviewFingerprint != "fingerprint-a" || response.Decision != InstallationConfirmationDecisionConfirm {
		t.Fatalf("unexpected response: %+v", response)
	}
}

func TestInstallationConfirmationConcurrentOutOfOrderAndOtherRPC(t *testing.T) {
	decisions := map[string]chan InstallationConfirmationDecision{
		"a": make(chan InstallationConfirmationDecision, 1),
		"b": make(chan InstallationConfirmationDecision, 1),
	}
	calls := make(chan installationConfirmationCall, 2)
	_, peer := newInstallationConfirmationHarness(t, InstallationConfirmationHandlerFunc(func(request InstallationConfirmationRequest, context InstallationConfirmationContext) (InstallationConfirmationDecision, error) {
		calls <- installationConfirmationCall{request: request, context: context}
		select {
		case decision := <-decisions[request.OperationID]:
			return decision, nil
		case <-context.Done():
			return InstallationConfirmationDecisionConfirm, nil
		}
	}))

	peer.request(201, "installations.confirm", installationConfirmationRequest(t, "a"))
	peer.request(202, "installations.confirm", installationConfirmationRequest(t, "b"))
	waitInstallationConfirmationCall(t, calls)
	waitInstallationConfirmationCall(t, calls)

	peer.request(203, "gitHubToken.getToken", &rpc.GitHubTokenAcquireRequest{
		RegistrationID: "missing",
		Host:           "github.com",
		Reason:         rpc.GitHubTokenAcquireReasonInitial,
	})
	tokenResponse := peer.readResponse(203)
	if tokenResponse.Error == nil || !strings.Contains(tokenResponse.Error.Message, "unknown GitHub token provider") {
		t.Fatalf("expected token provider error while reviews are pending, got %+v", tokenResponse)
	}

	decisions["b"] <- InstallationConfirmationDecisionDecline
	responseB := decodeInstallationConfirmationResult(t, peer.readResponse(202))
	if responseB.Decision != InstallationConfirmationDecisionDecline {
		t.Fatalf("response B decision = %q", responseB.Decision)
	}
	decisions["a"] <- InstallationConfirmationDecisionConfirm
	responseA := decodeInstallationConfirmationResult(t, peer.readResponse(201))
	if responseA.Decision != InstallationConfirmationDecisionConfirm {
		t.Fatalf("response A decision = %q", responseA.Decision)
	}
}

func TestInstallationConfirmationCancelRequestRetiresOnlyCancelledReview(t *testing.T) {
	decisions := map[string]chan InstallationConfirmationDecision{
		"a": make(chan InstallationConfirmationDecision, 1),
		"b": make(chan InstallationConfirmationDecision, 1),
	}
	calls := make(chan installationConfirmationCall, 2)
	_, peer := newInstallationConfirmationHarness(t, InstallationConfirmationHandlerFunc(func(request InstallationConfirmationRequest, context InstallationConfirmationContext) (InstallationConfirmationDecision, error) {
		calls <- installationConfirmationCall{request: request, context: context}
		select {
		case decision := <-decisions[request.OperationID]:
			return decision, nil
		case <-context.Done():
			return InstallationConfirmationDecisionConfirm, nil
		}
	}))

	peer.request(301, "installations.confirm", installationConfirmationRequest(t, "a"))
	peer.request(302, "installations.confirm", installationConfirmationRequest(t, "b"))
	first := waitInstallationConfirmationCall(t, calls)
	second := waitInstallationConfirmationCall(t, calls)
	contexts := map[string]InstallationConfirmationContext{
		first.request.OperationID:  first.context,
		second.request.OperationID: second.context,
	}
	peer.notify("$/cancelRequest", map[string]any{"id": 301})
	cancelled := peer.readResponse(301)
	if cancelled.Error == nil || cancelled.Error.Code != installationConfirmationRequestCancelledCode {
		t.Fatalf("expected request-cancelled response, got %+v", cancelled)
	}
	waitForClosed(t, contexts["a"].Done(), "request A cancellation")
	assertContextOpen(t, contexts["b"], "request B context")

	decisions["a"] <- InstallationConfirmationDecisionConfirm
	decisions["b"] <- InstallationConfirmationDecisionDecline
	responseB := decodeInstallationConfirmationResult(t, peer.readResponse(302))
	if responseB.Decision != InstallationConfirmationDecisionDecline {
		t.Fatalf("response B decision = %q", responseB.Decision)
	}
}

func TestInstallationConfirmationStaleUnknownAndStringCancelDontTouchSuccessor(t *testing.T) {
	decisions := make(map[string]chan InstallationConfirmationDecision)
	calls := make(chan installationConfirmationCall, 2)
	_, peer := newInstallationConfirmationHarness(t, InstallationConfirmationHandlerFunc(func(request InstallationConfirmationRequest, context InstallationConfirmationContext) (InstallationConfirmationDecision, error) {
		calls <- installationConfirmationCall{request: request, context: context}
		return <-decisions[request.OperationID], nil
	}))

	peer.notify("$/cancelRequest", map[string]any{"id": 402})
	decisions["a"] = make(chan InstallationConfirmationDecision, 1)
	peer.request(401, "installations.confirm", installationConfirmationRequest(t, "a"))
	callA := waitInstallationConfirmationCall(t, calls)
	peer.notify("$/cancelRequest", map[string]any{"id": "401"})
	peer.notify("$/cancelRequest", map[string]any{"id": 999})
	assertContextOpen(t, callA.context, "request A context")
	decisions["a"] <- InstallationConfirmationDecisionConfirm
	responseA := decodeInstallationConfirmationResult(t, peer.readResponse(401))
	if responseA.Decision != InstallationConfirmationDecisionConfirm {
		t.Fatalf("response A decision = %q", responseA.Decision)
	}

	decisions["b"] = make(chan InstallationConfirmationDecision, 1)
	peer.request(402, "installations.confirm", installationConfirmationRequest(t, "b"))
	callB := waitInstallationConfirmationCall(t, calls)
	assertContextOpen(t, callB.context, "successor request context")
	decisions["b"] <- InstallationConfirmationDecisionDecline
	responseB := decodeInstallationConfirmationResult(t, peer.readResponse(402))
	if responseB.Decision != InstallationConfirmationDecisionDecline {
		t.Fatalf("response B decision = %q", responseB.Decision)
	}
}

func TestInstallationConfirmationConnectionCloseDistinctAndPerConnection(t *testing.T) {
	firstCalls := make(chan installationConfirmationCall, 1)
	_, firstPeer := newInstallationConfirmationHarness(t, InstallationConfirmationHandlerFunc(func(request InstallationConfirmationRequest, context InstallationConfirmationContext) (InstallationConfirmationDecision, error) {
		firstCalls <- installationConfirmationCall{request: request, context: context}
		<-context.Done()
		return InstallationConfirmationDecisionConfirm, nil
	}))
	_, secondPeer := newInstallationConfirmationHarness(t, InstallationConfirmationHandlerFunc(func(InstallationConfirmationRequest, InstallationConfirmationContext) (InstallationConfirmationDecision, error) {
		return InstallationConfirmationDecisionDecline, nil
	}))

	firstPeer.request(501, "installations.confirm", installationConfirmationRequest(t, "a"))
	firstCall := waitInstallationConfirmationCall(t, firstCalls)
	if err := firstPeer.conn.Close(); err != nil {
		t.Fatal(err)
	}
	waitForClosed(t, firstCall.context.Done(), "first confirmation context")

	secondPeer.request(502, "installations.confirm", installationConfirmationRequest(t, "b"))
	response := decodeInstallationConfirmationResult(t, secondPeer.readResponse(502))
	if response.Decision != InstallationConfirmationDecisionDecline {
		t.Fatalf("second response decision = %q", response.Decision)
	}
}

func TestInstallationConfirmationConnectionCloseCancelsAllOutstandingReviews(t *testing.T) {
	calls := make(chan installationConfirmationCall, 2)
	_, peer := newInstallationConfirmationHarness(t, InstallationConfirmationHandlerFunc(func(request InstallationConfirmationRequest, context InstallationConfirmationContext) (InstallationConfirmationDecision, error) {
		calls <- installationConfirmationCall{request: request, context: context}
		<-context.Done()
		return InstallationConfirmationDecisionConfirm, nil
	}))

	peer.request(551, "installations.confirm", installationConfirmationRequest(t, "a"))
	peer.request(552, "installations.confirm", installationConfirmationRequest(t, "b"))
	first := waitInstallationConfirmationCall(t, calls)
	second := waitInstallationConfirmationCall(t, calls)
	contexts := map[string]InstallationConfirmationContext{
		first.request.OperationID:  first.context,
		second.request.OperationID: second.context,
	}

	if err := peer.conn.Close(); err != nil {
		t.Fatal(err)
	}
	waitForClosed(t, contexts["a"].Done(), "request A connection cancellation")
	waitForClosed(t, contexts["b"].Done(), "request B connection cancellation")
}

func TestInstallationConfirmationClientStopRetiresPendingReviews(t *testing.T) {
	calls := make(chan installationConfirmationCall, 1)
	client, peer := newInstallationConfirmationHarness(t, InstallationConfirmationHandlerFunc(func(request InstallationConfirmationRequest, context InstallationConfirmationContext) (InstallationConfirmationDecision, error) {
		calls <- installationConfirmationCall{request: request, context: context}
		<-context.Done()
		return InstallationConfirmationDecisionConfirm, nil
	}))

	peer.request(601, "installations.confirm", installationConfirmationRequest(t, "a"))
	call := waitInstallationConfirmationCall(t, calls)
	client.ForceStop()
	waitForClosed(t, call.context.Done(), "client stop signal")
}

func TestInstallationConfirmationRefusalsNeverApprove(t *testing.T) {
	t.Run("missing handler", func(t *testing.T) {
		_, peer := newInstallationConfirmationHarness(t, nil)
		peer.request(701, "installations.confirm", installationConfirmationRequest(t, "a"))
		response := peer.readResponse(701)
		if response.Error == nil || !strings.Contains(response.Error.Message, "No installations client-global handler registered") {
			t.Fatalf("expected missing handler error, got %+v", response)
		}
	})

	t.Run("invalid review", func(t *testing.T) {
		_, peer := newInstallationConfirmationHarness(t, InstallationConfirmationHandlerFunc(func(InstallationConfirmationRequest, InstallationConfirmationContext) (InstallationConfirmationDecision, error) {
			return InstallationConfirmationDecisionConfirm, nil
		}))
		peer.request(702, "installations.confirm", map[string]any{"confirmationId": 1})
		response := peer.readResponse(702)
		if response.Error == nil || response.Error.Code != jsonrpc2.ErrInvalidParams.Code {
			t.Fatalf("expected invalid params error, got %+v", response)
		}
	})

	t.Run("handler error", func(t *testing.T) {
		refusal := errors.New("unknown original operation")
		_, peer := newInstallationConfirmationHarness(t, InstallationConfirmationHandlerFunc(func(InstallationConfirmationRequest, InstallationConfirmationContext) (InstallationConfirmationDecision, error) {
			return "", refusal
		}))
		peer.request(703, "installations.confirm", installationConfirmationRequest(t, "a"))
		response := peer.readResponse(703)
		if response.Error == nil || !strings.Contains(response.Error.Message, refusal.Error()) {
			t.Fatalf("expected handler error, got %+v", response)
		}
	})

	t.Run("handler panic", func(t *testing.T) {
		_, peer := newInstallationConfirmationHarness(t, InstallationConfirmationHandlerFunc(func(InstallationConfirmationRequest, InstallationConfirmationContext) (InstallationConfirmationDecision, error) {
			panic("review UI failed")
		}))
		peer.request(704, "installations.confirm", installationConfirmationRequest(t, "a"))
		response := peer.readResponse(704)
		if response.Error == nil || !strings.Contains(response.Error.Message, "review UI failed") {
			t.Fatalf("expected panic error, got %+v", response)
		}
	})

	t.Run("unknown decision", func(t *testing.T) {
		_, peer := newInstallationConfirmationHarness(t, InstallationConfirmationHandlerFunc(func(InstallationConfirmationRequest, InstallationConfirmationContext) (InstallationConfirmationDecision, error) {
			return InstallationConfirmationDecision("approve"), nil
		}))
		peer.request(705, "installations.confirm", installationConfirmationRequest(t, "a"))
		response := peer.readResponse(705)
		if response.Error == nil || !strings.Contains(response.Error.Message, "Invalid installation confirmation decision") {
			t.Fatalf("expected invalid decision error, got %+v", response)
		}
	})
}

func TestInstallationConfirmationDoesNotInferOptionalLegacySession(t *testing.T) {
	calls := make(chan installationConfirmationCall, 1)
	_, peer := newInstallationConfirmationHarness(t, InstallationConfirmationHandlerFunc(func(request InstallationConfirmationRequest, context InstallationConfirmationContext) (InstallationConfirmationDecision, error) {
		calls <- installationConfirmationCall{request: request, context: context}
		return InstallationConfirmationDecisionDecline, nil
	}))
	request := installationConfirmationRequest(t, "legacy")
	request.PolicySessionID = nil
	peer.request(801, "installations.confirm", request)
	call := waitInstallationConfirmationCall(t, calls)
	if call.request.PolicySessionID != nil {
		t.Fatalf("policy session was inferred: %q", *call.request.PolicySessionID)
	}
	response := decodeInstallationConfirmationResult(t, peer.readResponse(801))
	if response.Decision != InstallationConfirmationDecisionDecline {
		t.Fatalf("response decision = %q", response.Decision)
	}
}
