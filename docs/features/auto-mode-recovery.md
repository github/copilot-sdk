<!-- Copyright (c) Microsoft Corporation. All rights reserved. -->

# Recover an active request with Auto

Explicitly enrolled SDK sessions can offer to recover an eligible, unfinished request using Auto after `model_degraded`, `model_overloaded`, or `user_model_rate_limited`. The runtime retains the original request while your application displays the offer and returns the user's choice. Your application must not resend the prompt, rewind history, restore files, or switch models itself.

Recovery retries once through Auto with the session's tier and configured on-device offers. It can select the original model or another eligible model.

## Enroll on create and resume

Recovery requires runtime rollout enablement and explicit enrollment. Set the language-specific `autoModeRecovery` option and supply an Auto-mode-switch handler on create and resume. A handler alone keeps its legacy behavior. Event subscriptions do not enroll a session.

| SDK     | Create and resume option         | Required handler                      | Runtime acknowledgment                                                                             |
| ------- | -------------------------------- | ------------------------------------- | -------------------------------------------------------------------------------------------------- |
| Node.js | `autoModeRecovery: true`         | `onAutoModeSwitchRequest`             | `session.capabilities.ui?.autoModeRecovery === true`                                               |
| Python  | `auto_mode_recovery=True`        | `on_auto_mode_switch_request`         | `session.capabilities.get("ui", {}).get("autoModeRecovery") is True`                               |
| Go      | `CreateSessionWithAutoModeRecovery` / `ResumeSessionWithAutoModeRecovery` | `AutoModeRecoveryOptions.OnAutoModeSwitchRequest` | `session.CapabilitiesWithAutoModeRecovery().UI.AutoModeRecovery` when `UI` is non-nil |
| .NET    | `AutoModeRecovery = true`        | `OnAutoModeSwitchRequest`             | `session.Capabilities.Ui?.AutoModeRecovery == true`                                                |
| Java    | `.setAutoModeRecovery(true)`     | `.setOnAutoModeSwitch(...)`           | `session.getCapabilities().getUi().getAutoModeRecovery().orElse(false)` when `getUi()` is non-null |
| Rust    | `.with_auto_mode_recovery(true)` | `.with_auto_mode_switch_handler(...)` | `session.auto_mode_recovery_supported()`                                                           |

Enrollment sends `requestAutoModeRecovery: true` apart from `requestAutoModeSwitch`; missing handlers fail before RPC. Missing/false acknowledgment fails. New attachments detach; SDKs delete rejected server-assigned cloud creates. Rejected resumes restore owners; accepted resident resumes missing acknowledgment retain new owners in all SDKs. SDKs fence IDs through cleanup and uncertain failure until reconnect. Python/Go/Rust/.NET cancellation preserves cleanup callbacks. Go/Python reject same-ID initialization during SessionFS callbacks; Node.js rejects callback re-entry and teardown opens. Events update acknowledgment.

The acknowledgment requires rollout enablement, not permission to recover every request. Model policy, runtime safety checks, emergency-off, and session mode still apply.

Direct runtime clients enroll differently from the language SDK create/resume options: call `session.ui.registerDirectAutoModeSwitchHandler({autoModeRecovery: true})` and require `autoModeRecovery: true` in its result. Attach the actual handler through the normal event-subscription mechanism separately. Registration suppresses server-bridge dispatch to avoid competing responders; it does not attach a listener. Keep its returned handle while the responder is active, and unregister it when the responder is removed. The existing no-argument registration remains available for legacy switching.

Complete enrollment and attach the responder before starting the turn. This contract does not supply CLI presentation.

These examples assume you have implemented `presentAutoOffer` (or its language equivalent) and your permission handler. The Auto handler must show the reason and await an explicit user choice; it must not automatically return acceptance. It must still support legacy requests with no recovery context.

<details open>
<summary><strong>TypeScript</strong></summary>

<!-- docs-validate: hidden -->
```typescript
import { CopilotClient, approveAll } from "@github/copilot-sdk";
import type { AutoModeSwitchHandler } from "@github/copilot-sdk";

declare const presentAutoOffer: AutoModeSwitchHandler;
declare const existingSessionId: string;
const permissionHandler = approveAll;
const client = new CopilotClient();

const config = {
    autoModeRecovery: true,
    onPermissionRequest: permissionHandler,
    onAutoModeSwitchRequest: presentAutoOffer,
};
const session = await client.createSession(config);
// Re-register support when resuming; enrollment is not a saved preference.
const resumed = await client.resumeSession(existingSessionId, config);
console.log(session.sessionId, resumed.sessionId);
```
<!-- /docs-validate: hidden -->

```typescript
const config = {
    autoModeRecovery: true,
    onPermissionRequest: permissionHandler,
    onAutoModeSwitchRequest: presentAutoOffer,
};
const session = await client.createSession(config);
// Re-register support when resuming; enrollment is not a saved preference.
const resumed = await client.resumeSession(existingSessionId, config);
```

</details>
<details>
<summary><strong>Python</strong></summary>

<!-- docs-validate: hidden -->
```python
from copilot import AutoModeSwitchRequest, AutoModeSwitchResponse, CopilotClient, PermissionHandler

permission_handler = PermissionHandler.approve_all


def present_auto_offer(request: AutoModeSwitchRequest, invocation: dict[str, str]) -> AutoModeSwitchResponse:
    return "no"


async def main(client: CopilotClient, existing_session_id: str) -> None:
    session = await client.create_session(
        auto_mode_recovery=True,
        on_permission_request=permission_handler,
        on_auto_mode_switch_request=present_auto_offer,
    )
    resumed = await client.resume_session(
        existing_session_id,
        auto_mode_recovery=True,
        on_permission_request=permission_handler,
        on_auto_mode_switch_request=present_auto_offer,
    )
    print(session.session_id, resumed.session_id)
```
<!-- /docs-validate: hidden -->

```python
session = await client.create_session(
    auto_mode_recovery=True,
    on_permission_request=permission_handler,
    on_auto_mode_switch_request=present_auto_offer,
)
resumed = await client.resume_session(
    existing_session_id,
    auto_mode_recovery=True,
    on_permission_request=permission_handler,
    on_auto_mode_switch_request=present_auto_offer,
)
```

</details>
<details>
<summary><strong>Go</strong></summary>

<!-- docs-validate: hidden -->
```go
package main

import (
	"context"

	copilot "github.com/github/copilot-sdk/go"
)

func presentAutoOffer(request copilot.AutoModeSwitchRequestWithAutoModeRecovery, invocation copilot.AutoModeSwitchInvocation) (copilot.AutoModeSwitchResponse, error) {
	return copilot.AutoModeSwitchResponseNo, nil
}

var permissionHandler = copilot.PermissionHandler.ApproveAll

func run(ctx context.Context, client *copilot.Client, existingSessionID string) error {
	session, err := client.CreateSessionWithAutoModeRecovery(ctx, &copilot.SessionConfig{
		OnPermissionRequest: permissionHandler,
	}, &copilot.AutoModeRecoveryOptions{
		OnAutoModeSwitchRequest: presentAutoOffer,
	})
	if err != nil {
		return err
	}
	resumed, err := client.ResumeSessionWithAutoModeRecovery(ctx, existingSessionID, &copilot.ResumeSessionConfig{
		OnPermissionRequest: permissionHandler,
	}, &copilot.AutoModeRecoveryOptions{
		OnAutoModeSwitchRequest: presentAutoOffer,
	})
	_, _ = session, resumed
	return err
}

func main() {
	_ = run(context.Background(), copilot.NewClient(nil), "existing-session-id")
}
```
<!-- /docs-validate: hidden -->

```go
session, err := client.CreateSessionWithAutoModeRecovery(ctx, &copilot.SessionConfig{
    OnPermissionRequest: permissionHandler,
}, &copilot.AutoModeRecoveryOptions{
    OnAutoModeSwitchRequest: presentAutoOffer,
})
if err != nil {
    return err
}
resumed, err := client.ResumeSessionWithAutoModeRecovery(ctx, existingSessionID, &copilot.ResumeSessionConfig{
    OnPermissionRequest: permissionHandler,
}, &copilot.AutoModeRecoveryOptions{
    OnAutoModeSwitchRequest: presentAutoOffer,
})
```

</details>
<details>
<summary><strong>.NET</strong></summary>

<!-- docs-validate: hidden -->
```csharp
using GitHub.Copilot;

var client = new CopilotClient();
var permissionHandler = PermissionHandler.ApproveAll;
var existingSessionId = "existing-session-id";

await using var session = await client.CreateSessionAsync(new SessionConfig
{
    AutoModeRecovery = true,
    OnPermissionRequest = permissionHandler,
    OnAutoModeSwitchRequest = PresentAutoOfferAsync
});
await using var resumed = await client.ResumeSessionAsync(existingSessionId, new ResumeSessionConfig
{
    AutoModeRecovery = true,
    OnPermissionRequest = permissionHandler,
    OnAutoModeSwitchRequest = PresentAutoOfferAsync
});

static Task<AutoModeSwitchResponse> PresentAutoOfferAsync(AutoModeSwitchRequest request, AutoModeSwitchInvocation invocation) =>
    Task.FromResult(AutoModeSwitchResponse.No);
```
<!-- /docs-validate: hidden -->

```csharp
await using var session = await client.CreateSessionAsync(new SessionConfig
{
    AutoModeRecovery = true,
    OnPermissionRequest = permissionHandler,
    OnAutoModeSwitchRequest = PresentAutoOfferAsync
});
await using var resumed = await client.ResumeSessionAsync(existingSessionId, new ResumeSessionConfig
{
    AutoModeRecovery = true,
    OnPermissionRequest = permissionHandler,
    OnAutoModeSwitchRequest = PresentAutoOfferAsync
});
```

</details>
<details>
<summary><strong>Java</strong></summary>

<!-- docs-validate: hidden -->
```java
import com.github.copilot.CopilotClient;
import com.github.copilot.rpc.AutoModeSwitchHandler;
import com.github.copilot.rpc.AutoModeSwitchResponse;
import com.github.copilot.rpc.PermissionHandler;
import com.github.copilot.rpc.ResumeSessionConfig;
import com.github.copilot.rpc.SessionConfig;
import java.util.concurrent.CompletableFuture;

public class AutoModeRecoveryExample {
    public static void main(String[] args) throws Exception {
        var client = new CopilotClient();
        PermissionHandler permissionHandler = PermissionHandler.APPROVE_ALL;
        AutoModeSwitchHandler presentAutoOffer =
            (request, invocation) -> CompletableFuture.completedFuture(AutoModeSwitchResponse.NO);
        var existingSessionId = "existing-session-id";

        var session = client.createSession(new SessionConfig()
            .setAutoModeRecovery(true)
            .setOnPermissionRequest(permissionHandler)
            .setOnAutoModeSwitch(presentAutoOffer));
        var resumed = client.resumeSession(existingSessionId, new ResumeSessionConfig()
            .setAutoModeRecovery(true)
            .setOnPermissionRequest(permissionHandler)
            .setOnAutoModeSwitch(presentAutoOffer));
        session.get();
        resumed.get();
    }
}
```
<!-- /docs-validate: hidden -->

```java
var session = client.createSession(new SessionConfig()
    .setAutoModeRecovery(true)
    .setOnPermissionRequest(permissionHandler)
    .setOnAutoModeSwitch(presentAutoOffer));
var resumed = client.resumeSession(existingSessionId, new ResumeSessionConfig()
    .setAutoModeRecovery(true)
    .setOnPermissionRequest(permissionHandler)
    .setOnAutoModeSwitch(presentAutoOffer));
```

</details>
<details>
<summary><strong>Rust</strong></summary>

<!-- docs-validate: hidden -->
```rust
struct PresentAutoOffer;

#[async_trait]
impl AutoModeSwitchHandler for PresentAutoOffer {
    async fn handle(
        &self,
        _session_id: SessionId,
        _error_code: Option<String>,
        _retry_after_seconds: Option<f64>,
    ) -> github_copilot_sdk::handler::AutoModeSwitchResponse {
        github_copilot_sdk::handler::AutoModeSwitchResponse::No
    }
}

let permission_handler: Arc<dyn PermissionHandler> = Arc::new(ApproveAllHandler);
let present_auto_offer: Arc<dyn AutoModeSwitchHandler> = Arc::new(PresentAutoOffer);
let existing_session_id = SessionId::from("existing-session-id");

let session = client.create_session(SessionConfig::default()
    .with_auto_mode_recovery(true)
    .with_permission_handler(permission_handler.clone())
    .with_auto_mode_switch_handler(present_auto_offer.clone())).await?;
let resumed = client.resume_session(ResumeSessionConfig::new(existing_session_id)
    .with_auto_mode_recovery(true)
    .with_permission_handler(permission_handler)
    .with_auto_mode_switch_handler(present_auto_offer)).await?;
let _ = (session, resumed);
```
<!-- /docs-validate: hidden -->

```rust
let session = client.create_session(SessionConfig::default()
    .with_auto_mode_recovery(true)
    .with_permission_handler(permission_handler.clone())
    .with_auto_mode_switch_handler(present_auto_offer.clone())).await?;
let resumed = client.resume_session(ResumeSessionConfig::new(existing_session_id)
    .with_auto_mode_recovery(true)
    .with_permission_handler(permission_handler)
    .with_auto_mode_switch_handler(present_auto_offer)).await?;
```

</details>

Rust preserves `AutoModeSwitchHandler::handle(session_id, error_code, retry_after_seconds)` for legacy offers. Implement the additional `handle_auto_mode_recovery(session_id, error_code, retry_after_seconds, auto_mode_recovery)` method to receive an `AutoModeRecoveryContext`. Its default returns `No`, so a legacy implementation cannot silently accept new recovery offers.

## Present and answer the offer

The existing `autoModeSwitch.request` callback retains `errorCode` and `retryAfterSeconds` and adds an optional `autoModeRecovery` object:

```json
{
    "errorCode": "model_overloaded",
    "retryAfterSeconds": 30,
    "autoModeRecovery": {
        "requestId": "offer-id",
        "originalModel": "explicit-model",
        "allowAlways": false,
        "interactionId": "original-interaction-id",
        "originatingMessageId": "original-user-message-id"
    }
}
```

`originalModel` is the explicit model that the failed request used. Use it in your offer copy. The outcome event repeats it so you can attribute the result after the session switches to Auto, including when an existing preference accepts without an offer.

`interactionId`, `originatingMessageId`, and `retryAfterSeconds` are optional. Correlate the offer by `requestId`, not by transcript order or the nearest user message. A recovery request ID is a one-use consent identifier, not a token for replaying a finished request.

- With no `autoModeRecovery` object, keep your existing future-message Auto-switch presentation and handling.
- `allowAlways: true` permits **Yes, always** (`yes_always`) to remember consent. The runtime rejects `yes_always` when the flag is false.
- The current runtime permits this only for per-model rate limits. Degradation and overload offer only **Yes** and **No**; no global/user preference is written.
- Return the existing response values: `yes`, `yes_always`, or `no` (or the language's equivalent enum).

## Auto routing and request lifetime

After consent, the runtime prepares an ordinary `POST /auto` route for the retained request before changing the session selection.

The route uses the session's committed Auto tier. You can configure ordinary session settings before a turn; recovery does not choose or change a tier in the consent callback. A session that starts on an explicit model usually has no tier. In that case the request omits `tier`, and the Copilot API applies its default tier, which is currently Balance for most users. The session keeps the same tier after it switches to Auto. See [Auto tier persistence](./session-persistence.md#auto-tier-persistence) for create/resume validation, restored preferences, and pending live tier changes; recovery does not change those rules.

Auto selects a model under its ordinary routing policy, and the runtime accepts any selection that passes the normal checks, including the original model. The selected concrete model and its paired session token must satisfy ordinary ownership, policy, expiry, input, route compatibility, and supported-execution checks. Recovery does not restore an ineligible model or add it to a pool.

After recovery, token renewal, rerouting, runtime hook corrections, and later requests follow normal Auto behavior. Resuming a session does not revive an unanswered recovery offer.

Your application handles only consent and normal events, with ordinary model attribution. There is no second prompt, model selector, or new consent-response value. During consent, do not switch models or tiers directly, manufacture a saved consent preference, or regenerate a completed response. The current runtime remembers consent only for per-model rate limits.

Because Auto can select the original model, recovery copy must not promise a different model. For example:

> Retry this request with Auto? Auto can choose the same model or a different available model.

## When no usable selection can be admitted

The following preparation failures produce `auto_mode_recovery.outcome` with `status: "unavailable"` and `autoModeRecoveryAdmitted: false`, before selection commits:

| Preparation result                    | Meaning                                                                                              |
| ------------------------------------- | ---------------------------------------------------------------------------------------------------- |
| HTTP 403                              | Auto routing is not permitted for this request                                                       |
| HTTP 503                              | Auto routing is unavailable                                                                          |
| HTTP 400                              | CAPI rejects the Auto routing request                                                                |
| An invalid or unusable routing result | The result fails existing model/token validity, policy, compatibility, or supported-execution checks |

The original explicit selection remains unchanged. Failed preparation does not fall back to `/models/session` or `/models/session/intent`, or resend the user message. The original request finishes through its normal terminal error and completion signals; accepting the offer is not a success result. Selecting the original model alone is not an error.

Dismiss the offer on its `auto_mode_recovery_consent.completed` event, stop the working indicator on normal completion, and associate the final error with the original request. Suggested text when no usable selection is available:

> Could not retry with Auto. No eligible model could be selected for this request. Your selected model has not changed.

## Observe completion and outcome separately

The runtime sends three required-ephemeral event types; they are not persisted or replayed on resume:

| Event                                  | Meaning                                                                                              |
| -------------------------------------- | ---------------------------------------------------------------------------------------------------- |
| `auto_mode_recovery_consent.requested` | Show the active-request offer, including its ID, reason, original model, and allowed choices         |
| `auto_mode_recovery_consent.completed` | Dismiss the offer, using `requestId` and `reason`; `response` is present only when the user answered |
| `auto_mode_recovery.outcome`           | Observe the authoritative recovery disposition, with `status` and `autoModeRecoveryAdmitted`         |

A completed consent event means only that the offer closed, not that the user consented; check `reason` and `response`. An accepted user reply produces completion reason `accepted`; a successful recovered inference produces outcome status `succeeded`. Other current reasons distinguish decline, invalid responses, saved-preference acceptance, cancellation, handler loss or replacement, model/session changes, supersession, disablement, and unsafe recovery. Reasons and statuses are strings, not closed enums; tolerate unknown values rather than treating them as success.

When a saved `yes_always` rate-limit preference accepts automatically, the runtime sends `auto_mode_recovery_consent.completed` with reason `accepted_by_saved_preference` and no preceding `requested` event, so there is no offer to dismiss.

Consent acceptance is not recovery success. Neither a completion, a recovery outcome notification, nor a model-change event replaces the normal terminal assistant response/error and session completion signals used by waiting-call APIs. The runtime keeps the logical request open through consent and recovery; the SDK does not reopen a failed waiting call. Legacy `auto_mode_switch.*` events keep their existing meaning.

At recovery admission, the runtime records `session.model_change` with `source: "automatic"`, `cause: "auto_mode_recovery"`, and `newModel: "auto"`. This signals that the prepared Auto route was committed and the retry was allowed to proceed. The event is recorded before provider dispatch and delivered asynchronously, so it does not prove the network request has started. It carries no recovery `requestId` or `interactionId`; there is no exact request-correlated retry-dispatch start notification.

`autoModeRecoveryAdmitted` is reported retrospectively on the final outcome: it means a recovered inference was allowed to proceed, not that it succeeded.

There is no runtime consent timer. A connected responder can leave the offer pending until a response, cancellation, disconnect, handler loss, or another invalidating runtime action. Dismiss your dialog on the matching `auto_mode_recovery_consent.completed` event, including cancellation or supersession; a late answer must not restart work. A caller-side `sendAndWait` deadline is not runtime cancellation or consent expiry. If your application abandons the operation, explicitly abort or disconnect as appropriate.

Also dismiss any open offer when you resume the session. If the runtime rebuilds the session while an offer is open, for example on resume, it withdraws the offer without sending `auto_mode_recovery_consent.completed`. As with elicitation completions, recovery events reach only the session instance that created them.

Changing the visible screen is not itself session resume. Your host can keep an offer across UI navigation only while the same live session instance and responder remain active. Handler replacement or loss can invalidate it. Keep each request ID bound to its originating live session, and dismiss the offer on actual resume, rebuild, or disconnect rather than carrying it into a replacement session.

## Safety and session behavior

Recovery requires an original explicit model, no previous recovery attempt, and no user-visible model output or tool activity anywhere in the current top-level turn. Already-Auto requests do not use this recovery path. Auto preparation must succeed before the runtime admits a recovered inference and commits the session to Auto. Once admitted, Auto remains selected for subsequent messages, including when the recovered inference fails. New-session defaults remain unchanged.

The Auto selection belongs to this session; its persistence follows the normal session rules. It does not change the default model or tier for newly created sessions.

An enrolled session applies the same safety checks to existing rate-limit preferences and legacy future-message switches. Other rate-limit codes remain separate legacy switches, not recovery of the failed request. New immediate input supersedes an unanswered recovery offer; explicitly enqueued input stays queued.

Autopilot does not open new consent offers. The current runtime recovers in Autopilot only with the per-model-rate-limit preference and the same safety checks. Degradation and overload do not receive implicit consent.

Entering Autopilot stops an in-progress recovery that has not yet been admitted, including an unanswered offer or accepted consent whose Auto route is still being prepared. An unanswered offer closes with completion reason `mode_changed`. Already-accepted consent is not completed again; its stopped recovery reports outcome `mode_changed` with `autoModeRecoveryAdmitted: false`. Switching back does not revive it. A mode transition that also changes or restores the model can independently invalidate recovery, preserving that model change and its withdrawal reason.

Once the runtime commits the retry, marked by `session.model_change` with cause `auto_mode_recovery`, entering Autopilot neither rolls back Auto nor aborts the recovered inference. Explicit cancellation remains separate and follows the normal request lifecycle.

Runtime rollout is off by default, including for enrolled sessions: no offers or recovery acknowledgment. Enable `AUTO_MODE_RECOVERY` through runtime feature flags or ExP key `copilot_auto_mode_recovery` for rollout cohorts. Either true enables recovery after ExP resolves; unresolved assignments fail closed. Emergency-off `AUTO_MODE_RECOVERY_DISABLED` / `copilot_auto_mode_recovery_disabled` remains default false; either true always wins over rollout enablement. Disabling rollout or enabling emergency-off withdraws unanswered offers; already-admitted recovery finishes normally. These controls use the existing feature-flag service, not SDK preferences or new settings.

This contract applies to the SDK's existing in-process/C ABI, stdio, and networked JSON-RPC transports where those transports are available. It does not enroll special AHP, ACP, or relay integrations, or supply App, VS Code, or CLI presentation.
