# Shell password prompts

Commands the agent runs have no terminal, so `sudo`, `ssh`, and `git` can't ask for a password, and they fail. With a shell authentication handler, your app can show those prompts to the user instead. The answer goes only to the program that asked: the model never sees it, and it isn't recorded in session events.

## How it works

1. Your app sets a shell authentication handler when it creates or resumes a session.
1. When a command that runs outside the sandbox needs a password, the SDK calls the handler with the program, its prompt, the command, and its working directory.
1. Your app shows a masked input and returns the answer, or cancels.
1. The answer goes to the program that asked. If the program prints it anyway, it is redacted from that session's command output, tool results, and hook input. An answer shorter than four characters is not redacted, because redacting it would hide ordinary text. Other sessions are not affected.

The handler is called for these prompts:

* `sudo -A` passwords on macOS and Linux
* `ssh` passwords, key passphrases, and host-key confirmations
* `git` user names and passwords for HTTP and HTTPS remotes

## Handling a request

Set the handler in the session configuration. In these examples, the text and yes-or-no functions stand for your app's own dialogs, and each one closes when the request is cancelled.

<details open>
<summary><strong>TypeScript</strong></summary>

```typescript
import { CopilotClient } from "@github/copilot-sdk";

// Your app's own dialogs. Each closes when `signal` aborts.
declare function askForText(options: {
  message: string;
  masked: boolean;
  signal: AbortSignal;
}): Promise<string | undefined>;
declare function askYesNo(message: string, signal: AbortSignal): Promise<boolean>;

const client = new CopilotClient();
await client.start();

const session = await client.createSession({
  onPermissionRequest: async () => ({ kind: "approve-once" }),
  onShellAuthenticationRequest: async (request, { signal }) => {
    const message = `${request.program} asks: ${request.prompt}`;
    if (request.inputKind === "confirm") {
      return (await askYesNo(message, signal))
        ? { action: "accept", value: "yes" }
        : { action: "cancel" };
    }
    const value = await askForText({
      message,
      masked: request.inputKind === "secret",
      signal,
    });
    return value === undefined ? { action: "cancel" } : { action: "accept", value };
  },
});
```

</details>
<details>
<summary><strong>Python</strong></summary>

```python
import asyncio

from copilot import (
    CopilotClient,
    ShellAuthenticationInvocation,
    ShellAuthenticationRequest,
    ShellAuthenticationResponse,
)
from copilot.session import PermissionHandler


# Your app's own dialogs. Each closes when `cancelled` is set.
async def ask_for_text(
    message: str, *, masked: bool, cancelled: asyncio.Event
) -> str | None:
    raise NotImplementedError


async def ask_yes_no(message: str, cancelled: asyncio.Event) -> bool:
    raise NotImplementedError


async def on_shell_authentication_request(
    request: ShellAuthenticationRequest, invocation: ShellAuthenticationInvocation
) -> ShellAuthenticationResponse:
    message = f"{request['program']} asks: {request['prompt']}"
    cancelled = invocation["cancelled"]
    if request["inputKind"] == "confirm":
        if await ask_yes_no(message, cancelled):
            return {"action": "accept", "value": "yes"}
        return {"action": "cancel"}
    value = await ask_for_text(
        message,
        masked=request["inputKind"] == "secret",
        cancelled=cancelled,
    )
    if value is None:
        return {"action": "cancel"}
    return {"action": "accept", "value": value}


async def main():
    client = CopilotClient()
    await client.start()
    session = await client.create_session(
        on_permission_request=PermissionHandler.approve_all,
        on_shell_authentication_request=on_shell_authentication_request,
    )


asyncio.run(main())
```

</details>
<details>
<summary><strong>Go</strong></summary>

```go
package main

import (
	"context"
	"fmt"

	copilot "github.com/github/copilot-sdk/go"
)

// Your app's own dialogs. Each closes when ctx is done.
func askForText(ctx context.Context, message string, masked bool) (string, bool) {
	panic("not implemented")
}

func askYesNo(ctx context.Context, message string) bool {
	panic("not implemented")
}

func onShellAuthenticationRequest(
	request copilot.ShellAuthenticationRequest,
	invocation copilot.ShellAuthenticationInvocation,
) (copilot.ShellAuthenticationResponse, error) {
	cancel := copilot.ShellAuthenticationResponse{Action: copilot.ShellAuthenticationActionCancel}
	message := fmt.Sprintf("%s asks: %s", request.Program, request.Prompt)
	if request.InputKind == copilot.ShellAuthenticationInputKindConfirm {
		if !askYesNo(invocation.Context, message) {
			return cancel, nil
		}
		return copilot.ShellAuthenticationResponse{Action: copilot.ShellAuthenticationActionAccept, Value: "yes"}, nil
	}
	masked := request.InputKind == copilot.ShellAuthenticationInputKindSecret
	value, ok := askForText(invocation.Context, message, masked)
	if !ok {
		return cancel, nil
	}
	return copilot.ShellAuthenticationResponse{Action: copilot.ShellAuthenticationActionAccept, Value: value}, nil
}

func main() {
	ctx := context.Background()
	client := copilot.NewClient(nil)
	if err := client.Start(ctx); err != nil {
		panic(err)
	}
	defer client.Stop()

	session, err := client.CreateSession(ctx, &copilot.SessionConfig{
		OnPermissionRequest:  copilot.PermissionHandler.ApproveAll,
		OnShellAuthenticationRequest: onShellAuthenticationRequest,
	})
	if err != nil {
		panic(err)
	}
	_ = session
}
```

</details>
<details>
<summary><strong>.NET</strong></summary>

```csharp
using GitHub.Copilot;

await using var client = new CopilotClient();
await client.StartAsync();

await using var session = await client.CreateSessionAsync(new SessionConfig
{
    OnPermissionRequest = PermissionHandler.ApproveAll,
    OnShellAuthenticationRequest = async (request, cancellationToken) =>
    {
        var message = $"{request.Program} asks: {request.Prompt}";
        if (request.InputKind == ShellAuthenticationInputKind.Confirm)
        {
            return await AskYesNoAsync(message, cancellationToken)
                ? ShellAuthenticationResponse.Accept("yes")
                : ShellAuthenticationResponse.Cancel();
        }

        var value = await AskForTextAsync(
            message,
            masked: request.InputKind == ShellAuthenticationInputKind.Secret,
            cancellationToken);
        return value is null ? ShellAuthenticationResponse.Cancel() : ShellAuthenticationResponse.Accept(value);
    },
});

// Your app's own dialogs. Each closes when the token is cancelled.
static Task<string?> AskForTextAsync(
    string message, bool masked, CancellationToken cancellationToken) =>
    throw new NotImplementedException();

static Task<bool> AskYesNoAsync(string message, CancellationToken cancellationToken) =>
    throw new NotImplementedException();
```

</details>
<details>
<summary><strong>Java</strong></summary>

```java
import com.github.copilot.CopilotClient;
import com.github.copilot.rpc.PermissionHandler;
import com.github.copilot.rpc.ShellAuthenticationResponse;
import com.github.copilot.rpc.SessionConfig;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

public class ShellPasswordPromptsExample {
    public static void main(String[] args) throws Exception {
        try (var client = new CopilotClient()) {
            client.start().get();

            var session = client.createSession(new SessionConfig()
                    .setOnPermissionRequest(PermissionHandler.APPROVE_ALL)
                    .setOnShellAuthenticationRequest((request, invocation) -> {
                        var message = request.getProgram() + " asks: " + request.getPrompt();
                        if ("confirm".equals(request.getInputKind())) {
                            return askYesNo(message, invocation.getCancellation())
                                    .thenApply(yes -> yes
                                            ? ShellAuthenticationResponse.accept("yes")
                                            : ShellAuthenticationResponse.cancel());
                        }
                        var masked = "secret".equals(request.getInputKind());
                        return askForText(message, masked, invocation.getCancellation())
                                .thenApply(value -> value
                                        .map(ShellAuthenticationResponse::accept)
                                        .orElseGet(ShellAuthenticationResponse::cancel));
                    }))
                    .get();
        }
    }

    // Your app's own dialogs. Each closes when `cancellation` completes.
    static CompletableFuture<Optional<String>> askForText(
            String message, boolean masked, CompletionStage<Void> cancellation) {
        throw new UnsupportedOperationException();
    }

    static CompletableFuture<Boolean> askYesNo(String message, CompletionStage<Void> cancellation) {
        throw new UnsupportedOperationException();
    }
}
```

</details>
<details>
<summary><strong>Rust</strong></summary>

<!-- docs-validate: skip -->

```rust
use std::sync::Arc;

use async_trait::async_trait;
use github_copilot_sdk::handler::{
    ShellAuthenticationHandler, ShellAuthenticationInputKind, ShellAuthenticationRequest, ShellAuthenticationResponse,
};
use github_copilot_sdk::types::{SessionConfig, SessionId};
use tokio_util::sync::CancellationToken;

// Your app's own dialogs. Each closes when `cancellation` fires.
async fn ask_for_text(
    message: &str,
    masked: bool,
    cancellation: &CancellationToken,
) -> Option<String> {
    todo!()
}

async fn ask_yes_no(message: &str, cancellation: &CancellationToken) -> bool {
    todo!()
}

struct PasswordPrompts;

#[async_trait]
impl ShellAuthenticationHandler for PasswordPrompts {
    async fn handle(
        &self,
        _session_id: SessionId,
        request: ShellAuthenticationRequest,
        cancellation: CancellationToken,
    ) -> ShellAuthenticationResponse {
        let message = format!("{} asks: {}", request.program, request.prompt);
        let answer = if request.input_kind == ShellAuthenticationInputKind::Confirm {
            ask_yes_no(&message, &cancellation)
                .await
                .then(|| "yes".to_owned())
        } else {
            let masked = request.input_kind == ShellAuthenticationInputKind::Secret;
            ask_for_text(&message, masked, &cancellation).await
        };
        match answer {
            Some(value) => ShellAuthenticationResponse::Accept(value),
            None => ShellAuthenticationResponse::Cancel,
        }
    }
}

let session = client
    .create_session(
        SessionConfig::default()
            .approve_all_permissions()
            .with_shell_authentication_handler(Arc::new(PasswordPrompts)),
    )
    .await?;
```

</details>

The request has these fields. Each SDK follows its own naming conventions, for example `input_kind` in Rust:

| Field | Description |
|---|---|
| `program` | `sudo`, `ssh`, or `git` |
| `prompt` | The prompt text the program supplied |
| `inputKind` | `secret` for a password or passphrase, which you mask. `text` for a visible answer, such as a user name. `confirm` for a yes or no question: answer `"yes"`, or cancel to decline |
| `command` | The command that triggered the prompt |
| `cwd` | The command's working directory |

The handler's cancellation signal fires when the answer is no longer needed, for example because the command ended, the prompt timed out after 10 minutes, or the session ended or switched to autopilot. Close the input when it does. The signal is `signal` in TypeScript, `cancelled` in Python, the invocation's `Context` in Go, the handler's `CancellationToken` parameter in .NET, `getCancellation()` in Java, and the `cancellation` token in Rust. If the handler fails or returns a malformed answer, the prompt is cancelled, so the program that asked fails instead of receiving a wrong value. The SDK logs a handler failure with only the error's type, never its message, which could contain the answer. In Rust, the process's panic hook still reports a handler panic, by default by printing its message to stderr, so don't put the answer in a panic message.

## When prompts are shown

A command asks the user only when someone can answer. Sessions without a shell authentication handler, and sessions in autopilot, never wait for input: a command that needs a password fails as it would without this feature. On macOS and Linux, the agent in a session that can show these prompts is told to run commands that need `sudo` with `sudo -A`, so sudo asks through your handler instead of failing. The handler is called when the SDK runs the Copilot runtime, `copilot-runtime`, as every SDK does by default, whether as a process or [in-process](../setup/in-process-runtime.md). A Copilot CLI server, which `copilot --headless` starts, doesn't forward these prompts: not when `cliPath` points the SDK at the `copilot` CLI, as in the [local CLI setup](../setup/local-cli.md), and not when the SDK connects to one through `cliUrl`. Its commands fail as they would without a handler. In the interactive Copilot CLI, this is experimental: after `/experimental on`, the session you're viewing shows these prompts in a dialog that masks passwords. Non-interactive `copilot -p` runs never show them.

## SDK support

Every SDK can set a shell authentication handler:

| SDK | Handler option |
|---|---|
| Node.js / TypeScript | `onShellAuthenticationRequest` |
| Python | `on_shell_authentication_request` |
| Go | `OnShellAuthenticationRequest` |
| .NET | `OnShellAuthenticationRequest` |
| Java | `setOnShellAuthenticationRequest` |
| Rust | `with_shell_authentication_handler` |
