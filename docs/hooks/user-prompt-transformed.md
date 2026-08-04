# User prompt transformed hook

The `userPromptTransformed` hook runs after the runtime adds generated context to a submitted prompt, but before the resulting content is persisted to session history or sent to the model.

Use it when you need to inspect or replace the exact model-facing prompt. The `prompt` input contains the user prompt after any `userPromptSubmitted` hooks have run, while `transformedPrompt` also contains runtime-generated context such as `<current_datetime>`.

## Input and output

| Input field | Type | Description |
| --- | --- | --- |
| `sessionId` | string | Runtime session ID |
| `timestamp` | date/time | Time the hook was invoked |
| `cwd` / `workingDirectory` | string | Current working directory |
| `prompt` | string | Prompt after `userPromptSubmitted` hooks |
| `transformedPrompt` | string | Model-facing prompt after runtime transformations |

Return no value to leave the transformed prompt unchanged. Return `modifiedTransformedPrompt` to replace the content that is stored in session history and sent to the model.

## Examples

<details open>
<summary><strong>Node.js / TypeScript</strong></summary>

```typescript
const session = await client.createSession({
  hooks: {
    onUserPromptTransformed: async (input) => ({
      modifiedTransformedPrompt: redact(input.transformedPrompt),
    }),
  },
});
```

</details>

<details>
<summary><strong>Python</strong></summary>

```python
session = await client.create_session(
    hooks={
        "on_user_prompt_transformed": lambda input_data, invocation: {
            "modifiedTransformedPrompt": redact(input_data["transformedPrompt"])
        }
    }
)
```

</details>

<details>
<summary><strong>Go</strong></summary>

```go
session, err := client.CreateSession(ctx, &copilot.SessionConfig{
	Hooks: &copilot.SessionHooks{
		OnUserPromptTransformed: func(input copilot.UserPromptTransformedHookInput, invocation copilot.HookInvocation) (*copilot.UserPromptTransformedHookOutput, error) {
			return &copilot.UserPromptTransformedHookOutput{
				ModifiedTransformedPrompt: copilot.String(redact(input.TransformedPrompt)),
			}, nil
		},
	},
})
```

</details>

<details>
<summary><strong>.NET</strong></summary>

```csharp
var session = await client.CreateSessionAsync(new SessionConfig
{
    Hooks = new SessionHooks
    {
        OnUserPromptTransformed = (input, invocation) =>
            Task.FromResult<UserPromptTransformedHookOutput?>(new()
            {
                ModifiedTransformedPrompt = Redact(input.TransformedPrompt),
            }),
    },
});
```

</details>

<details>
<summary><strong>Java</strong></summary>

```java
var hooks = new SessionHooks().setOnUserPromptTransformed((input, invocation) ->
    CompletableFuture.completedFuture(
        new UserPromptTransformedHookOutput(redact(input.transformedPrompt()))));
```

</details>

<details>
<summary><strong>Rust</strong></summary>

```rust
#[async_trait]
impl SessionHooks for MyHooks {
    async fn on_user_prompt_transformed(
        &self,
        input: UserPromptTransformedInput,
        _ctx: HookContext,
    ) -> Option<UserPromptTransformedOutput> {
        Some(UserPromptTransformedOutput {
            modified_transformed_prompt: Some(redact(&input.transformed_prompt)),
        })
    }
}
```

</details>

The replacement is persisted as the user message content, so resumed sessions replay the modified content unchanged.
